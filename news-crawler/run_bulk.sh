#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export DB_HOST=ainews-prod-mysql.cjmucplwks6l.ap-northeast-2.rds.amazonaws.com
export DB_USER=admin
export DB_PASSWORD=RVpE0lcuR1H5N5SW
export DB_NAME=ainews

"$SCRIPT_DIR/venv/bin/python" "$SCRIPT_DIR/main.py" --db --count 200
