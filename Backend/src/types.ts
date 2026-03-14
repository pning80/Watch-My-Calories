import { KeyObject } from 'crypto';
import { Request } from 'express';

export interface AttestedKeyData {
    publicKey: KeyObject;
    counter: number;
    hmac: string;
}

export interface AttestationObject {
    fmt: string;
    attStmt: {
        x5c: Buffer[];
        [key: string]: unknown;
    };
    authData: Buffer;
}

export interface AssertionFields {
    authenticatorData: Buffer;
    signature: Buffer;
}

export interface RawBodyRequest extends Request {
    rawBody: Buffer;
}

export interface DerLengthResult {
    value: number;
    bytesRead: number;
}

export interface FirestoreKeyDoc {
    publicKeyPem: string;
    counter: number;
    hmac: string;
    createdAt: Date;
    lastUsedAt: Date;
}

export function isAttestationObject(obj: unknown): obj is AttestationObject {
    if (!obj || typeof obj !== 'object') return false;
    const o = obj as Record<string, unknown>;
    return typeof o.fmt === 'string' &&
        o.attStmt !== null && typeof o.attStmt === 'object' &&
        o.authData !== undefined;
}

export function isAssertionFields(obj: unknown): obj is AssertionFields {
    if (!obj || typeof obj !== 'object') return false;
    // CBOR may decode as Map or plain object
    if (obj instanceof Map) {
        return obj.has('authenticatorData') && obj.has('signature');
    }
    const o = obj as Record<string, unknown>;
    return o.authenticatorData !== undefined && o.signature !== undefined;
}
