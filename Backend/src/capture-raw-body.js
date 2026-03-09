const captureRawBody = (req, res, next) => {
    const chunks = [];
    req.on('data', chunk => chunks.push(chunk));
    req.on('end', () => {
        req.rawBody = Buffer.concat(chunks);
        // Also parse JSON so req.body is available
        if (req.rawBody.length > 0) {
            try {
                req.body = JSON.parse(req.rawBody);
            } catch {
                // Not JSON — leave req.body undefined
            }
        }
        next();
    });
};

module.exports = { captureRawBody };
