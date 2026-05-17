import { KeyObject } from 'crypto';
import { Request } from 'express';

export type Platform = 'ios' | 'android';

export interface AttestedKeyData {
    publicKey: KeyObject;
    counter: number;
    hmac: string;
    // Both fields below are optional for backward-compat with iOS docs already in
    // Firestore that predate the Android port. Missing `platform` is treated as 'ios'.
    platform?: Platform;
    androidAssertionSecret?: string; // hex-encoded 32 bytes; only present for Android keys
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
    // PORTING_CRITERIA.md T1.9.c, T1.10.d. Both nullable on read for backward-compat.
    // iOS docs created before these fields existed read back as platform='ios',
    // androidAssertionSecret=undefined. No backfill migration is run.
    platform?: Platform;
    androidAssertionSecret?: string;
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
