#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# DB credentials are loaded from AWS Secrets Manager by DBClient.

"$SCRIPT_DIR/venv/bin/python" "$SCRIPT_DIR/main.py" --db --count 200
