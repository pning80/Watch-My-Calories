const { describe, it, beforeEach, afterEach, mock } = require('node:test');
const assert = require('node:assert/strict');

describe('hmac-secret module', () => {
    let initHmacSecret, getHmacSecret, setHmacSecret;
    const savedEnv = {};

    beforeEach(() => {
        // Save and clean env vars
        savedEnv.ATTEST_HMAC_SECRET = process.env.ATTEST_HMAC_SECRET;
        savedEnv.ATTEST_HMAC_SECRET_NAME = process.env.ATTEST_HMAC_SECRET_NAME;
        delete process.env.ATTEST_HMAC_SECRET;
        delete process.env.ATTEST_HMAC_SECRET_NAME;

        // Fresh import each time (cachedSecret is module-level state)
        ({ initHmacSecret, getHmacSecret, setHmacSecret } = require('../dist/src/hmac-secret'));
        setHmacSecret(null);
    });

    afterEach(() => {
        // Restore env vars
        if (savedEnv.ATTEST_HMAC_SECRET !== undefined) {
            process.env.ATTEST_HMAC_SECRET = savedEnv.ATTEST_HMAC_SECRET;
        } else {
            delete process.env.ATTEST_HMAC_SECRET;
        }
        if (savedEnv.ATTEST_HMAC_SECRET_NAME !== undefined) {
            process.env.ATTEST_HMAC_SECRET_NAME = savedEnv.ATTEST_HMAC_SECRET_NAME;
        } else {
            delete process.env.ATTEST_HMAC_SECRET_NAME;
        }
        mock.restoreAll();
    });

    describe('initHmacSecret', () => {
        it('loads secret from ATTEST_HMAC_SECRET env var', async () => {
            process.env.ATTEST_HMAC_SECRET = 'my-env-secret-value';
            await initHmacSecret();
            assert.equal(getHmacSecret(), 'my-env-secret-value');
        });

        it('loads secret from Secret Manager when ATTEST_HMAC_SECRET_NAME is set', async () => {
            process.env.ATTEST_HMAC_SECRET_NAME = 'my-secret-name';

            const mockAccess = mock.fn(async () => [{
                payload: { data: Buffer.from('sm-secret-value') },
            }]);

            // Mock require('@google-cloud/secret-manager') by temporarily replacing it
            const Module = require('module');
            const origResolve = Module._resolveFilename;
            const fakeModulePath = '__mock_secret_manager__';

            Module._resolveFilename = function (request, ...args) {
                if (request === '@google-cloud/secret-manager') return fakeModulePath;
                return origResolve.call(this, request, ...args);
            };

            require.cache[fakeModulePath] = {
                id: fakeModulePath,
                filename: fakeModulePath,
                loaded: true,
                exports: {
                    SecretManagerServiceClient: class {
                        accessSecretVersion = mockAccess;
                        getProjectId = async () => 'test-project';
                    },
                },
            };

            try {
                await initHmacSecret();
                assert.equal(getHmacSecret(), 'sm-secret-value');
                assert.equal(mockAccess.mock.calls.length, 1);
                const callArg = mockAccess.mock.calls[0].arguments[0];
                assert.equal(callArg.name, 'projects/test-project/secrets/my-secret-name/versions/latest');
            } finally {
                Module._resolveFilename = origResolve;
                delete require.cache[fakeModulePath];
            }
        });

        it('logs warning and leaves secret null when neither env var is set', async () => {
            await initHmacSecret();
            assert.equal(getHmacSecret(), null);
        });

        it('leaves secret null when Secret Manager throws', async () => {
            process.env.ATTEST_HMAC_SECRET_NAME = 'my-secret-name';

            const Module = require('module');
            const origResolve = Module._resolveFilename;
            const fakeModulePath = '__mock_secret_manager_err__';

            Module._resolveFilename = function (request, ...args) {
                if (request === '@google-cloud/secret-manager') return fakeModulePath;
                return origResolve.call(this, request, ...args);
            };

            require.cache[fakeModulePath] = {
                id: fakeModulePath,
                filename: fakeModulePath,
                loaded: true,
                exports: {
                    SecretManagerServiceClient: class {
                        accessSecretVersion = async () => { throw new Error('SM unavailable'); };
                        getProjectId = async () => 'test-project';
                    },
                },
            };

            try {
                await initHmacSecret();
                assert.equal(getHmacSecret(), null);
            } finally {
                Module._resolveFilename = origResolve;
                delete require.cache[fakeModulePath];
            }
        });

        it('env var takes priority over Secret Manager', async () => {
            process.env.ATTEST_HMAC_SECRET = 'env-var-wins';
            process.env.ATTEST_HMAC_SECRET_NAME = 'should-not-be-used';

            const mockAccess = mock.fn(async () => [{
                payload: { data: Buffer.from('sm-value') },
            }]);

            const Module = require('module');
            const origResolve = Module._resolveFilename;
            const fakeModulePath = '__mock_secret_manager_priority__';

            Module._resolveFilename = function (request, ...args) {
                if (request === '@google-cloud/secret-manager') return fakeModulePath;
                return origResolve.call(this, request, ...args);
            };

            require.cache[fakeModulePath] = {
                id: fakeModulePath,
                filename: fakeModulePath,
                loaded: true,
                exports: {
                    SecretManagerServiceClient: class {
                        accessSecretVersion = mockAccess;
                        getProjectId = async () => 'test-project';
                    },
                },
            };

            try {
                await initHmacSecret();
                assert.equal(getHmacSecret(), 'env-var-wins');
                assert.equal(mockAccess.mock.calls.length, 0, 'Secret Manager should not be called');
            } finally {
                Module._resolveFilename = origResolve;
                delete require.cache[fakeModulePath];
            }
        });
    });

    describe('getHmacSecret / setHmacSecret', () => {
        it('returns null before initialization', () => {
            assert.equal(getHmacSecret(), null);
        });

        it('setHmacSecret overrides cached value', () => {
            setHmacSecret('override-value');
            assert.equal(getHmacSecret(), 'override-value');

            setHmacSecret('another-value');
            assert.equal(getHmacSecret(), 'another-value');

            setHmacSecret(null);
            assert.equal(getHmacSecret(), null);
        });
    });
});
