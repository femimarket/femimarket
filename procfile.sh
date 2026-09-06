#!/bin/sh
set -e
cd "$(dirname "$0")"
out="$HOME/procapps/femi"
mkdir -p "$out"
python3.13 - "$out" <<'PY'
import sys
import tomllib
with open('app.toml', 'rb') as f:
    services = tomllib.load(f)
lines = []
for name, service in services.items():
    parts = []
    if 'dir' in service:
        parts.append(f"cd {service['dir']} &&")
    for key, value in service.get('env', {}).items():
        parts.append(f"{key}={value}")
    parts.append(service['cmd'])
    lines.append(f"{name}: {' '.join(parts)}")
with open(f'{sys.argv[1]}/Procfile', 'w') as f:
    f.write('\n'.join(lines) + '\n')
print('\n'.join(lines))
PY
