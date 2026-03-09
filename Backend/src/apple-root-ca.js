const fs = require('fs');
const path = require('path');

let appleRootCaPem;
try {
    appleRootCaPem = fs.readFileSync(
        path.join(__dirname, '..', 'apple_attest_root_ca.pem'),
        'utf8'
    );
} catch {
    console.warn('Warning: apple_attest_root_ca.pem not found — App Attest verification will be unavailable.');
}

function getAppleRootCa() { return appleRootCaPem; }
function setAppleRootCa(pem) { appleRootCaPem = pem; }

module.exports = { getAppleRootCa, setAppleRootCa };
