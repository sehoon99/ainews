#!/bin/bash
# 뉴스 크롤러 주기적 실행 스크립트 (4시간마다)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="/tmp/ainews-crawler.log"

# DB credentials are loaded from AWS Secrets Manager by DBClient.

source "$SCRIPT_DIR/venv/bin/activate"

echo "스케줄러 시작 (4시간 간격)" | tee -a "$LOG_FILE"

while true; do
    echo "" | tee -a "$LOG_FILE"
    echo "===== $(date '+%Y-%m-%d %H:%M:%S') 크롤링 시작 =====" | tee -a "$LOG_FILE"
    python "$SCRIPT_DIR/main.py" --db --count 15 2>&1 | tee -a "$LOG_FILE"
    echo "===== 크롤링 완료, 4시간 후 재실행 =====" | tee -a "$LOG_FILE"
    sleep 14400  # 4시간
done
