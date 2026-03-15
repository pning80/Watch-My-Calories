import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { keyIdToDocId, docIdToKeyId } from '../dist/server';

describe('keyIdToDocId / docIdToKeyId', () => {
    it('round-trips a standard base64 keyID', () => {
        const keyID = 'dGVzdEtleUlk'; // "testKeyId" in base64
        const docId = keyIdToDocId(keyID);
        const recovered = docIdToKeyId(docId);
        assert.equal(recovered, keyID);
    });

    it('converts / to _ in the doc ID', () => {
        // Base64 with '/' character
        const keyID = Buffer.from([0xff, 0xfe, 0xfd]).toString('base64'); // '//79'
        const docId = keyIdToDocId(keyID);
        assert.ok(!docId.includes('/'), 'Doc ID must not contain /');
        assert.equal(docIdToKeyId(docId), keyID);
    });

    it('converts + to - in the doc ID', () => {
        // Base64 with '+' character
        const keyID = Buffer.from([0xfb, 0xef]).toString('base64'); // 'u+8='
        const docId = keyIdToDocId(keyID);
        assert.ok(!docId.includes('+'), 'Doc ID must not contain +');
        assert.equal(docIdToKeyId(docId), keyID);
    });

    it('strips = padding from the doc ID', () => {
        // 1 byte → 4-char base64 with == padding
        const keyID = Buffer.from([0x41]).toString('base64'); // 'QQ=='
        const docId = keyIdToDocId(keyID);
        assert.ok(!docId.includes('='), 'Doc ID must not contain =');
        assert.equal(docIdToKeyId(docId), keyID);
    });

    it('round-trips a 32-byte key (typical App Attest keyID length)', () => {
        const raw = Buffer.alloc(32);
        for (let i = 0; i < 32; i++) raw[i] = i;
        const keyID = raw.toString('base64');
        const docId = keyIdToDocId(keyID);
        assert.equal(docIdToKeyId(docId), keyID);
    });

    it('handles alphanumeric-only base64 (no special chars)', () => {
        const keyID = Buffer.from('HelloWorld').toString('base64');
        const docId = keyIdToDocId(keyID);
        assert.equal(docIdToKeyId(docId), keyID);
    });
});
