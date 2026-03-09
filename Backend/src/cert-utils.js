function extractNonceFromCert(cert) {
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
    if (certDer[pos] === 0x01) {
        pos += 3; // BOOLEAN tag(1) + length(1) + value(1)
    }

    // Outer OCTET STRING
    if (certDer[pos] !== 0x04) return null;
    pos++;
    const outerLen = parseDerLength(certDer, pos);
    pos += outerLen.bytesRead;

    // SEQUENCE
    if (certDer[pos] !== 0x30) return null;
    pos++;
    const seqLen = parseDerLength(certDer, pos);
    pos += seqLen.bytesRead;

    // Context-specific [1] wrapping the OCTET STRING
    // Tag 0xA1 = context-specific, constructed, tag number 1
    if (certDer[pos] === 0xa1) {
        pos++;
        const ctxLen = parseDerLength(certDer, pos);
        pos += ctxLen.bytesRead;
    }

    // Inner OCTET STRING (the actual nonce, 32 bytes)
    if (certDer[pos] !== 0x04) return null;
    pos++;
    const innerLen = parseDerLength(certDer, pos);
    pos += innerLen.bytesRead;

    return certDer.subarray(pos, pos + innerLen.value);
}

function parseDerLength(buf, offset) {
    const first = buf[offset];
    if (first < 0x80) {
        return { value: first, bytesRead: 1 };
    }
    const numBytes = first & 0x7f;
    let value = 0;
    for (let i = 0; i < numBytes; i++) {
        value = (value << 8) | buf[offset + 1 + i];
    }
    return { value, bytesRead: 1 + numBytes };
}

module.exports = { extractNonceFromCert, parseDerLength };
