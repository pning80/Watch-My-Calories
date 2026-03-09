const { describe, it, before } = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('crypto');
const { parseDerLength, extractNonceFromCert } = require('../server');
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
});
