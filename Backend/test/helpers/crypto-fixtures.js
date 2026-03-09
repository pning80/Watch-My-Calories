/**
 * Test infrastructure for App Attest verification tests.
 *
 * Generates self-signed P-256 cert chains with Apple's custom OID extension,
 * and builds synthetic CBOR attestation/assertion objects.
 */

const crypto = require('crypto');
const { Crypto } = require('@peculiar/webcrypto');
const x509 = require('@peculiar/x509');
const { encode } = require('cbor-x');

// Use @peculiar/webcrypto for cert generation
const peculiarCrypto = new Crypto();
x509.cryptoProvider.set(peculiarCrypto);

const APPLE_OID = '1.2.840.113635.100.8.2';
const BUNDLE_ID = 'com.pning80.WatchMyCalories';

// Cache the root CA so we generate it only once per test run
let _rootCa = null;
let _intermediateCa = null;

async function _ensureRootCa() {
    if (_rootCa) return;

    const rootKeys = await peculiarCrypto.subtle.generateKey(
        { name: 'ECDSA', namedCurve: 'P-256' },
        true,
        ['sign', 'verify']
    );

    _rootCa = await x509.X509CertificateGenerator.createSelfSigned({
        serialNumber: '01',
        name: 'CN=Test Apple Root CA',
        notBefore: new Date('2020-01-01'),
        notAfter: new Date('2030-01-01'),
        keys: rootKeys,
        signingAlgorithm: { name: 'ECDSA', hash: 'SHA-256' },
        extensions: [
            new x509.BasicConstraintsExtension(true, undefined, true),
        ],
    });

    const intermediateKeys = await peculiarCrypto.subtle.generateKey(
        { name: 'ECDSA', namedCurve: 'P-256' },
        true,
        ['sign', 'verify']
    );

    _intermediateCa = {
        cert: await x509.X509CertificateGenerator.create({
            serialNumber: '02',
            subject: 'CN=Test Apple Intermediate CA',
            issuer: _rootCa.subject,
            notBefore: new Date('2020-01-01'),
            notAfter: new Date('2030-01-01'),
            publicKey: intermediateKeys.publicKey,
            signingKey: rootKeys.privateKey,
            signingAlgorithm: { name: 'ECDSA', hash: 'SHA-256' },
            extensions: [
                new x509.BasicConstraintsExtension(true, undefined, true),
            ],
        }),
        keys: intermediateKeys,
    };
}

/**
 * Get the test root CA PEM string.
 */
async function getTestRootCaPem() {
    await _ensureRootCa();
    return _rootCa.toString('pem');
}

/**
 * Generate a leaf certificate with the Apple App Attest nonce OID extension.
 *
 * The extension structure is:
 *   OCTET STRING { SEQUENCE { [1] { OCTET STRING { nonce } } } }
 */
async function generateLeafCert(nonce) {
    await _ensureRootCa();

    const leafKeys = await peculiarCrypto.subtle.generateKey(
        { name: 'ECDSA', namedCurve: 'P-256' },
        true,
        ['sign', 'verify']
    );

    // Build the inner DER structure for the nonce extension:
    // SEQUENCE { [1] { OCTET STRING { nonce } } }
    const innerOctetString = Buffer.concat([
        Buffer.from([0x04, nonce.length]),
        nonce,
    ]);
    const contextTag1 = Buffer.concat([
        Buffer.from([0xa1, innerOctetString.length]),
        innerOctetString,
    ]);
    const sequence = Buffer.concat([
        Buffer.from([0x30, contextTag1.length]),
        contextTag1,
    ]);

    const nonceExtension = new x509.Extension(
        APPLE_OID,
        false,
        new Uint8Array(sequence)
    );

    const cert = await x509.X509CertificateGenerator.create({
        serialNumber: crypto.randomBytes(8).toString('hex'),
        subject: 'CN=Test Leaf',
        issuer: _intermediateCa.cert.subject,
        notBefore: new Date('2020-01-01'),
        notAfter: new Date('2030-01-01'),
        publicKey: leafKeys.publicKey,
        signingKey: _intermediateCa.keys.privateKey,
        signingAlgorithm: { name: 'ECDSA', hash: 'SHA-256' },
        extensions: [nonceExtension],
    });

    return cert;
}

/**
 * Build authenticator data bytes.
 *
 * Format: rpIdHash(32) + flags(1) + counter(4) + aaguid(16) + credIdLen(2) + credId + COSE_key
 */
function buildAuthData(rpIdHash, credentialPublicKey, opts = {}) {
    const flags = Buffer.from([opts.flags ?? 0x41]); // AT flag set
    const counter = Buffer.alloc(4);
    counter.writeUInt32BE(opts.counter ?? 0);

    const aaguid = opts.aaguid ?? Buffer.alloc(16, 0); // "appattestdevelop" or zeros for test
    const credId = opts.credId ?? crypto.randomBytes(32);
    const credIdLen = Buffer.alloc(2);
    credIdLen.writeUInt16BE(credId.length);

    // Build COSE key Map for the credential public key
    const coseKey = credentialPublicKey; // should be CBOR-encodable Map

    const coseKeyBytes = encode(coseKey);

    return Buffer.concat([
        rpIdHash,
        flags,
        counter,
        aaguid,
        credIdLen,
        credId,
        coseKeyBytes,
    ]);
}

/**
 * Build a CBOR-encoded attestation object.
 *
 * @param {Buffer} authData - authenticator data bytes
 * @param {string} challenge - the challenge string
 * @param {object} opts - options: { fmt, skipCert }
 * @returns {{ base64: string, leafCert: object }} base64-encoded CBOR attestation
 */
async function buildAttestationObject(authData, challenge, opts = {}) {
    // Compute nonce: SHA256(authData || SHA256(challenge))
    const challengeHash = crypto.createHash('sha256').update(challenge).digest();
    const nonceData = Buffer.concat([authData, challengeHash]);
    const nonce = crypto.createHash('sha256').update(nonceData).digest();

    await _ensureRootCa();

    const leafCert = await generateLeafCert(nonce);

    const leafDer = Buffer.from(leafCert.rawData);
    const intermediateDer = Buffer.from(_intermediateCa.cert.rawData);

    const x5c = opts.shortChain
        ? [leafDer]
        : [leafDer, intermediateDer];

    const attestObj = {
        fmt: opts.fmt ?? 'apple-appattest',
        attStmt: { x5c },
        authData: authData,
    };

    const encoded = encode(attestObj);
    return { base64: encoded.toString('base64'), leafCert };
}

/**
 * Build a CBOR-encoded assertion object.
 */
function buildAssertionObject(authenticatorData, signature) {
    const obj = { authenticatorData, signature };
    return encode(obj).toString('base64');
}

/**
 * Generate a P-256 key pair for assertion tests.
 * Returns Node.js crypto KeyObject pair + raw x/y coordinates.
 */
function generateP256KeyPair() {
    const { publicKey, privateKey } = crypto.generateKeyPairSync('ec', {
        namedCurve: 'P-256',
    });

    // Export raw x, y for COSE key
    const jwk = publicKey.export({ format: 'jwk' });
    const x = Buffer.from(jwk.x, 'base64url');
    const y = Buffer.from(jwk.y, 'base64url');

    return { publicKey, privateKey, x, y };
}

/**
 * Build a COSE key Map from x, y coordinates.
 */
function buildCoseKey(x, y) {
    const m = new Map();
    m.set(1, 2);   // kty: EC2
    m.set(3, -7);  // alg: ES256
    m.set(-1, 1);  // crv: P-256
    m.set(-2, x);  // x coordinate
    m.set(-3, y);  // y coordinate
    return m;
}

/**
 * Compute the RP ID hash for a given team ID and bundle ID.
 */
function computeRpIdHash(teamId, bundleId = BUNDLE_ID) {
    return crypto.createHash('sha256').update(`${teamId}.${bundleId}`).digest();
}

/**
 * Sign an assertion (authenticatorData + clientDataHash) with a private key.
 */
function signAssertion(privateKey, authenticatorData, clientDataHash) {
    const compositeHash = crypto.createHash('sha256')
        .update(Buffer.concat([authenticatorData, clientDataHash]))
        .digest();

    return crypto.sign('sha256', compositeHash, {
        key: privateKey,
        dsaEncoding: 'der',
    });
}

/**
 * Build assertion authenticator data (simpler than attestation — no cred ID/COSE key).
 * Format: rpIdHash(32) + flags(1) + counter(4)
 */
function buildAssertionAuthData(rpIdHash, counter, flags = 0x01) {
    const buf = Buffer.alloc(37);
    rpIdHash.copy(buf, 0);
    buf[32] = flags;
    buf.writeUInt32BE(counter, 33);
    return buf;
}

module.exports = {
    getTestRootCaPem,
    generateLeafCert,
    buildAuthData,
    buildAttestationObject,
    buildAssertionObject,
    generateP256KeyPair,
    buildCoseKey,
    computeRpIdHash,
    signAssertion,
    buildAssertionAuthData,
    BUNDLE_ID,
};
