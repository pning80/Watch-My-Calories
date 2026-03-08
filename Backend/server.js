// Load environment variables locally
require('dotenv').config();

const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 8080;

app.use(cors());

// Middleware to verify iOS App Request
const verifyAppToken = (req, res, next) => {
    const token = req.headers['x-backend-key'];
    const validToken = process.env.APP_BACKEND_API_KEY;

    if (!validToken) {
        console.error("CRITICAL: APP_BACKEND_API_KEY is not configured on the server.");
        return res.status(500).json({ error: "Server Configuration Error" });
    }

    if (token !== validToken) {
        console.warn(`Unauthorized request attempt with token: ${token}`);
        return res.status(401).json({ error: "Unauthorized access" });
    }

    next();
};

// Relay generateContent requests to Gemini API
// Wildcard avoids Express param parsing issues with colons in the path
app.post('/v1beta/models/*', verifyAppToken, express.json({ limit: '50mb' }), async (req, res) => {
    const geminiKey = process.env.GEMINI_API_KEY;
    const model = process.env.GEMINI_MODEL_NAME;

    if (!geminiKey) {
        console.error("CRITICAL: GEMINI_API_KEY is not configured on the server.");
        return res.status(500).json({ error: "Server Configuration Error" });
    }

    if (!model) {
        console.error("CRITICAL: GEMINI_MODEL_NAME is not configured on the server.");
        return res.status(500).json({ error: "Server Configuration Error" });
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
        console.error("Relay Error:", err);
        res.status(502).json({ error: "Bad Gateway" });
    }
});

// Basic Health Check
app.get('/', (req, res) => {
    res.send('WatchMyCalories Backend is running.');
});

app.listen(PORT, () => {
    console.log(`Backend server listening on port ${PORT}`);
    console.log(`Enforcing Model: ${process.env.GEMINI_MODEL_NAME || "NOT SET"}`);
});
