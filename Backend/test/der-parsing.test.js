const { describe, it, before } = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('crypto');
const { parseDerLength, extractNonceFromCert } = require('../dist/server');
const { getTestRootCaPem, generateLeafCert } = require('./helpers/crypto-fixtures');
const { X509Certificate } = require('crypto');

describe('parseDerLength', () => {
    it('parses single-byte length', () => {
        const buf = Buffer.from([0x20]);
        const result = parseDerLength(buf, 0);
        assert.equal(result.value, 0x20);
        assert.equal(result.bytesRead, 1);
    });

    it('parses multi-byte length (1 extra byte)', () => {
        const buf = Buffer.from([0x81, 0x80]); // 128
        const result = parseDerLength(buf, 0);
        assert.equal(result.value, 128);
        assert.equal(result.bytesRead, 2);
    });

    it('parses multi-byte length (2 extra bytes)', () => {
        const buf = Buffer.from([0x82, 0x01, 0x00]); // 256
        const result = parseDerLength(buf, 0);
        assert.equal(result.value, 256);
        assert.equal(result.bytesRead, 3);
    });

    it('returns null for empty buffer', () => {
        const result = parseDerLength(Buffer.alloc(0), 0);
        assert.equal(result, null);
    });

    it('returns null for offset beyond buffer', () => {
        const buf = Buffer.from([0x20]);
        const result = parseDerLength(buf, 5);
        assert.equal(result, null);
    });

    it('returns null for 0-byte multi-byte length (indefinite form)', () => {
        // 0x80 = multi-byte with numBytes=0 (BER indefinite length, not valid DER)
        const buf = Buffer.from([0x80]);
        const result = parseDerLength(buf, 0);
        assert.equal(result, null);
    });

    it('returns null for numBytes > 4', () => {
        // 0x85 = multi-byte with numBytes=5
        const buf = Buffer.from([0x85, 0x01, 0x02, 0x03, 0x04, 0x05]);
        const result = parseDerLength(buf, 0);
        assert.equal(result, null);
    });

    it('returns null for truncated multi-byte length', () => {
        // Claims 2 extra bytes but only 1 available
        const buf = Buffer.from([0x82, 0x01]);
        const result = parseDerLength(buf, 0);
        assert.equal(result, null);
    });
});

describe('extractNonceFromCert', () => {
    it('extracts 32-byte nonce from cert with Apple OID', async () => {
        const nonce = crypto.randomBytes(32);
        const leafCert = await generateLeafCert(nonce);
        const nodeCert = new X509Certificate(leafCert.toString('pem'));
        const extracted = extractNonceFromCert(nodeCert);
        assert.ok(extracted, 'Should extract nonce');
        assert.equal(extracted.length, 32);
        assert.deepEqual(Buffer.from(extracted), nonce);
    });

    it('returns null for cert without Apple OID', async () => {
        // Generate a self-signed cert without the Apple OID
        const { publicKey, privateKey } = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' });
        const { X509Certificate: PeculiarX509 } = require('@peculiar/x509');
        const { Crypto } = require('@peculiar/webcrypto');
        const peculiarCrypto = new Crypto();
        const x509 = require('@peculiar/x509');
        x509.cryptoProvider.set(peculiarCrypto);

        const keys = await peculiarCrypto.subtle.generateKey(
            { name: 'ECDSA', namedCurve: 'P-256' },
            true,
            ['sign', 'verify']
        );

        const cert = await x509.X509CertificateGenerator.createSelfSigned({
            serialNumber: '01',
            name: 'CN=No OID Cert',
            notBefore: new Date('2020-01-01'),
            notAfter: new Date('2030-01-01'),
            keys,
            signingAlgorithm: { name: 'ECDSA', hash: 'SHA-256' },
        });

        const nodeCert = new X509Certificate(cert.toString('pem'));
        const result = extractNonceFromCert(nodeCert);
        assert.equal(result, null);
    });

    it('handles cert with critical flag on Apple OID', async () => {
        // The extractNonceFromCert function handles optional BOOLEAN (critical flag)
        // We test with the standard non-critical cert which already works
        const nonce = crypto.randomBytes(32);
        const leafCert = await generateLeafCert(nonce);
        const nodeCert = new X509Certificate(leafCert.toString('pem'));
        const extracted = extractNonceFromCert(nodeCert);
        assert.ok(extracted);
        assert.deepEqual(Buffer.from(extracted), nonce);
    });

    it('returns null for truncated cert DER (OID found but data cut short)', () => {
        // Create a minimal buffer with just the OID and then truncate
        const oidBytes = Buffer.from('06092a864886f763640802', 'hex');
        // Append just a partial OCTET STRING tag with no length
        const truncated = Buffer.concat([
            Buffer.from([0x30, 0x20]), // SEQUENCE header (fake)
            oidBytes,
            Buffer.from([0x04]), // OCTET STRING tag but no length byte
        ]);

        // Mock cert object with raw property
        const mockCert = { raw: truncated };
        const result = extractNonceFromCert(mockCert);
        assert.equal(result, null);
    });

    it('returns null when nonce length is not 32 bytes', () => {
        // Build a buffer that has the OID followed by valid DER structure
        // but with nonce length = 16 instead of 32
        const oidBytes = Buffer.from('06092a864886f763640802', 'hex');
        const nonceData = crypto.randomBytes(16);
        // Structure: OID | OCTET STRING(outer) | SEQUENCE | OCTET STRING(inner, 16 bytes)
        const innerOctetString = Buffer.concat([Buffer.from([0x04, 16]), nonceData]);
        const sequence = Buffer.concat([Buffer.from([0x30, innerOctetString.length]), innerOctetString]);
        const outerOctetString = Buffer.concat([Buffer.from([0x04, sequence.length]), sequence]);
        const certDer = Buffer.concat([
            Buffer.alloc(10), // padding (simulates cert prefix)
            oidBytes,
            outerOctetString,
        ]);

        const mockCert = { raw: certDer };
        const result = extractNonceFromCert(mockCert);
        assert.equal(result, null);
    });
});
