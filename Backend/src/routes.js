function registerRoutes(app, { geminiLimiter, captureRawBody, verifyRequest }) {
    // Relay generateContent requests to Gemini API
    app.post('/v1beta/models/*', geminiLimiter, captureRawBody, verifyRequest, async (req, res) => {
        const geminiKey = process.env.GEMINI_API_KEY;
        const model = process.env.GEMINI_MODEL_NAME;

        if (!geminiKey) {
            console.error('CRITICAL: GEMINI_API_KEY is not configured on the server.');
            return res.status(500).json({ error: 'Server Configuration Error' });
        }

        if (!model) {
            console.error('CRITICAL: GEMINI_MODEL_NAME is not configured on the server.');
            return res.status(500).json({ error: 'Server Configuration Error' });
        }

        const targetURL = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiKey}`;

        try {
            const response = await fetch(targetURL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(req.body),
            });

            const data = await response.json();

            if (!response.ok) {
                console.error(`Gemini API returned status ${response.status}:`, JSON.stringify(data));
            }

            res.status(response.status).json(data);
        } catch (err) {
            console.error('Relay Error:', err);
            res.status(502).json({ error: 'Bad Gateway' });
        }
    });

    // Basic Health Check
    app.get('/', (req, res) => {
        res.send('WatchMyCalories Backend is running.');
    });
}

module.exports = { registerRoutes };
