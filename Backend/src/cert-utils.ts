import { X509Certificate } from 'crypto';
import { DerLengthResult } from './types';

export function extractNonceFromCert(cert: X509Certificate | { raw: Buffer }): Buffer | null {
    // The nonce is in a custom extension with OID 1.2.840.113635.100.8.2
    // Node's X509Certificate doesn't expose arbitrary extensions directly,
    // so we parse the raw DER to find it.
    const certDer = cert.raw;
    const oidHex = '06092a864886f763640802'; // DER encoding of 1.2.840.113635.100.8.2

    const oidBytes = Buffer.from(oidHex, 'hex');
    const idx = certDer.indexOf(oidBytes);
    if (idx === -1) return null;

    // After OID: skip OID bytes, then OCTET STRING wrapper(s)
    // The structure is: OID | critical? | OCTET STRING (outer) | SEQUENCE | OCTET STRING (inner, 32 bytes)
    let pos = idx + oidBytes.length;

    // Skip optional BOOLEAN (critical flag)
    if (pos >= certDer.length) return null;
    if (certDer[pos] === 0x01) {
        pos += 3; // BOOLEAN tag(1) + length(1) + value(1)
        if (pos >= certDer.length) return null;
    }

    // Outer OCTET STRING
    if (pos >= certDer.length || certDer[pos] !== 0x04) return null;
    pos++;
    const outerLen = parseDerLength(certDer, pos);
    if (!outerLen) return null;
    pos += outerLen.bytesRead;

    // SEQUENCE
    if (pos >= certDer.length || certDer[pos] !== 0x30) return null;
    pos++;
    const seqLen = parseDerLength(certDer, pos);
    if (!seqLen) return null;
    pos += seqLen.bytesRead;

    // Context-specific [1] wrapping the OCTET STRING
    // Tag 0xA1 = context-specific, constructed, tag number 1
    if (pos < certDer.length && certDer[pos] === 0xa1) {
        pos++;
        const ctxLen = parseDerLength(certDer, pos);
        if (!ctxLen) return null;
        pos += ctxLen.bytesRead;
    }

    // Inner OCTET STRING (the actual nonce, 32 bytes)
    if (pos >= certDer.length || certDer[pos] !== 0x04) return null;
    pos++;
    const innerLen = parseDerLength(certDer, pos);
    if (!innerLen) return null;
    pos += innerLen.bytesRead;

    // Validate nonce length is exactly 32 bytes and data is available
    if (innerLen.value !== 32 || pos + 32 > certDer.length) return null;

    return certDer.subarray(pos, pos + 32);
}

export function parseDerLength(buf: Buffer, offset: number): DerLengthResult | null {
    if (offset >= buf.length) return null;

    const first = buf[offset];
    if (first < 0x80) {
        return { value: first, bytesRead: 1 };
    }
    const numBytes = first & 0x7f;
    if (numBytes === 0 || numBytes > 4) return null;
    if (offset + 1 + numBytes > buf.length) return null;

    let value = 0;
    for (let i = 0; i < numBytes; i++) {
        value = (value << 8) | buf[offset + 1 + i];
    }
    return { value, bytesRead: 1 + numBytes };
}
