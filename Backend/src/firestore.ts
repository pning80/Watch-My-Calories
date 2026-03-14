import { createLogger } from './logger';
const log = createLogger('firestore');

type FirestoreDb = import('@google-cloud/firestore').Firestore;

let db: FirestoreDb | null = null;
try {
    const { Firestore } = require('@google-cloud/firestore');
    db = new Firestore();
    log.info('Firestore initialized successfully');
} catch (err: any) {
    log.warn({ error: err.message }, 'Firestore unavailable — running with in-memory key storage only');
}

export function getDb(): FirestoreDb | null { return db; }
export function setDb(mockDb: FirestoreDb | null): void { db = mockDb; }
