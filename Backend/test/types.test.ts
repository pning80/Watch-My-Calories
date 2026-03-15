import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

// Import the type guards — they are not re-exported from server.ts,
// so import directly from the compiled output.
import { isAttestationObject, isAssertionFields } from '../dist/src/types';

describe('isAttestationObject', () => {
    it('returns true for a valid attestation object', () => {
        const obj = {
            fmt: 'apple-appattest',
            attStmt: { x5c: [Buffer.from('cert')] },
            authData: Buffer.from('authdata'),
        };
        assert.equal(isAttestationObject(obj), true);
    });

    it('returns false when fmt is not a string', () => {
        const obj = {
            fmt: 123,
            attStmt: { x5c: [] },
            authData: Buffer.from('data'),
        };
        assert.equal(isAttestationObject(obj), false);
    });

    it('returns false when attStmt is null', () => {
        const obj = {
            fmt: 'apple-appattest',
            attStmt: null,
            authData: Buffer.from('data'),
        };
        assert.equal(isAttestationObject(obj), false);
    });

    it('returns false when authData is undefined', () => {
        const obj = {
            fmt: 'apple-appattest',
            attStmt: { x5c: [] },
        };
        assert.equal(isAttestationObject(obj), false);
    });

    it('returns false for null', () => {
        assert.equal(isAttestationObject(null), false);
    });

    it('returns false for undefined', () => {
        assert.equal(isAttestationObject(undefined), false);
    });

    it('returns false for a non-object (string)', () => {
        assert.equal(isAttestationObject('hello'), false);
    });
});

describe('isAssertionFields', () => {
    it('returns true for a plain object with required fields', () => {
        const obj = {
            authenticatorData: Buffer.from('authdata'),
            signature: Buffer.from('sig'),
        };
        assert.equal(isAssertionFields(obj), true);
    });

    it('returns true for a Map with required keys', () => {
        const map = new Map<string, Buffer>();
        map.set('authenticatorData', Buffer.from('authdata'));
        map.set('signature', Buffer.from('sig'));
        assert.equal(isAssertionFields(map), true);
    });

    it('returns false for a Map missing authenticatorData', () => {
        const map = new Map<string, Buffer>();
        map.set('signature', Buffer.from('sig'));
        assert.equal(isAssertionFields(map), false);
    });

    it('returns false for a Map missing signature', () => {
        const map = new Map<string, Buffer>();
        map.set('authenticatorData', Buffer.from('authdata'));
        assert.equal(isAssertionFields(map), false);
    });

    it('returns false for a plain object missing authenticatorData', () => {
        const obj = { signature: Buffer.from('sig') };
        assert.equal(isAssertionFields(obj), false);
    });

    it('returns false for null', () => {
        assert.equal(isAssertionFields(null), false);
    });

    it('returns false for undefined', () => {
        assert.equal(isAssertionFields(undefined), false);
    });

    it('returns false for a primitive', () => {
        assert.equal(isAssertionFields(42), false);
    });
});
