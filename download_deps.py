#!/usr/bin/env python3
"""Download only the pinned build dependencies; verify SHA256 before installing."""
from pathlib import Path
import hashlib
import json
import urllib.request

root = Path(__file__).resolve().parent
directory = root / 'deps'
directory.mkdir(exist_ok=True)
for dependency in json.loads((root / 'dependencies.json').read_text()):
    target = directory / dependency['name']
    if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() == dependency['sha256']:
        continue
    with urllib.request.urlopen(dependency['url'], timeout=60) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != dependency['sha256']:
        raise SystemExit('Dependency hash mismatch: ' + dependency['name'])
    temporary = target.with_suffix(target.suffix + '.new')
    temporary.write_bytes(data)
    temporary.replace(target)
    print(dependency['name'])
