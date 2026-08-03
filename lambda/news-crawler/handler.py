"""
News Crawler Lambda Handler
- EventBridge 스케줄(4시간마다) 또는 수동 트리거로 실행
- 네이버/다음 뉴스 크롤링 → DB INSERT → SQS 이미지 분석 요청
"""

import os
import json
import traceback

from main import crawl_all_to_db


def lambda_handler(event, context):
    """
    Lambda 엔트리포인트

    EventBridge event 예시:
        {"source": "aws.events", "detail-type": "Scheduled Event", ...}

    수동 테스트 event 예시:
        {"source": "manual-test"}
        {"source": "manual-test", "count_per_section": 5}
    """
    print(f"Event: {json.dumps(event, default=str)}")

    count_per_section = int(os.environ.get('COUNT_PER_SECTION', '15'))

    # 수동 테스트 시 count 오버라이드 가능
    if isinstance(event, dict) and 'count_per_section' in event:
        count_per_section = int(event['count_per_section'])

    print(f"Starting crawl with count_per_section={count_per_section}")

    try:
        result = crawl_all_to_db(
            count_per_section=count_per_section,
            save_json=False,
        )

        stats = result.get('stats', {})
        print(f"Crawl completed: {json.dumps(stats, default=str)}")

        failed_count = int(stats.get('failed', 0))
        if failed_count > 0:
            raise RuntimeError(f'Crawl completed with {failed_count} failed article(s)')

        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Crawl completed successfully',
                'stats': stats,
            }),
        }

    except Exception:
        error_msg = traceback.format_exc()
        print(f"Crawl failed: {error_msg}")
        raise
