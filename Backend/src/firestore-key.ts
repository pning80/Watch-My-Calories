/**
 * Firestore document IDs cannot contain '/'.
 * App Attest keyIDs are standard base64 which may include '/', '+', and '='.
 * These helpers convert between the original keyID and a Firestore-safe doc ID
 * using base64url encoding.
 */

export function keyIdToDocId(keyID: string): string {
    return Buffer.from(keyID, 'base64').toString('base64url');
}

export function docIdToKeyId(docId: string): string {
    return Buffer.from(docId, 'base64url').toString('base64');
}
