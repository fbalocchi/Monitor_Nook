#!/usr/bin/env python3
"""Generate a new, local development identity; never overwrite an existing key."""
from pathlib import Path
import os
import subprocess

root = Path(__file__).resolve().parents[1]
directory = root / 'signing'
directory.mkdir(mode=0o700, exist_ok=True)
key, cert = directory / 'key.pem', directory / 'cert.pem'
if key.exists() or cert.exists():
    raise SystemExit('Existing signing material left unchanged. Reuse it for updates.')
previous = os.umask(0o077)
try:
    subprocess.run([
        'openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-sha256', '-nodes',
        '-keyout', str(key), '-out', str(cert), '-days', '10000',
        '-subj', '/CN=Panel NOOK local development/'
    ], check=True)
finally:
    os.umask(previous)
print('Local key created in signing/. Keep it private and backed up.')
