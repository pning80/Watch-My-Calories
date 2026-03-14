import fs from 'fs';
import path from 'path';
import { createLogger } from './logger';
const log = createLogger('apple-root-ca');

let appleRootCaPem: string | undefined;
try {
    appleRootCaPem = fs.readFileSync(
        path.join(__dirname, '..', '..', 'apple_attest_root_ca.pem'),
        'utf8'
    );
} catch {
    log.warn('apple_attest_root_ca.pem not found — App Attest verification will be unavailable');
}

export function getAppleRootCa(): string | undefined { return appleRootCaPem; }
export function setAppleRootCa(pem: string | undefined): void { appleRootCaPem = pem; }
