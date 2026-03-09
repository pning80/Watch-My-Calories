let db = null;
try {
    const { Firestore } = require('@google-cloud/firestore');
    db = new Firestore();
    console.log('Firestore initialized successfully.');
} catch (err) {
    console.warn('Firestore unavailable — running with in-memory key storage only:', err.message);
}

function getDb() { return db; }
function setDb(mockDb) { db = mockDb; }

module.exports = { getDb, setDb };
