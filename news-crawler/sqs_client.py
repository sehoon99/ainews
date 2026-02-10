"""
AWS SQS 클라이언트
- 이미지 분석 큐에 메시지 전송
"""

import os
import json
from typing import Optional

import boto3
from botocore.exceptions import ClientError

# 기본 설정
DEFAULT_REGION = 'ap-northeast-2'
DEFAULT_QUEUE_URL = 'https://sqs.ap-northeast-2.amazonaws.com/633309913072/ainews-prod-image-analysis'


class SQSClient:
    """AWS SQS 클라이언트"""

    def __init__(
        self,
        queue_url: str = None,
        region_name: str = None,
    ):
        """
        SQS 클라이언트 초기화

        환경 변수:
            SQS_QUEUE_URL, AWS_REGION
        """
        self.queue_url = queue_url or os.environ.get('SQS_QUEUE_URL')
        self.region_name = region_name or os.environ.get('AWS_REGION', 'ap-northeast-2')

        if not self.queue_url:
            raise ValueError('SQS_QUEUE_URL 환경변수가 설정되지 않았습니다.')

        self._client = None

    @property
    def client(self):
        """Lazy boto3 SQS client"""
        if self._client is None:
            self._client = boto3.client('sqs', region_name=self.region_name)
        return self._client

    def send_message(
        self,
        article_id: int,
        image_urls: list[str],
        source_url: Optional[str] = None,
    ) -> Optional[str]:
        """
        이미지 분석 메시지 전송

        Args:
            article_id: 기사 ID
            image_urls: 이미지 URL 리스트
            source_url: 원본 기사 URL (선택, 디버깅용)

        Returns:
            message_id or None
        """
        if not image_urls:
            return None

        message_body = {
            'article_id': article_id,
            'image_urls': image_urls,
        }

        if source_url:
            message_body['source_url'] = source_url

        try:
            response = self.client.send_message(
                QueueUrl=self.queue_url,
                MessageBody=json.dumps(message_body, ensure_ascii=False),
            )
            return response.get('MessageId')
        except ClientError as e:
            print(f'SQS 전송 실패: {e}')
            return None

    def send_batch(
        self,
        messages: list[dict],
    ) -> dict:
        """
        배치 메시지 전송 (최대 10개)

        Args:
            messages: [{'article_id': int, 'image_urls': list, 'source_url': str}, ...]

        Returns:
            {'successful': [...], 'failed': [...]}
        """
        if not messages:
            return {'successful': [], 'failed': []}

        # 10개씩 분할
        results = {'successful': [], 'failed': []}

        for i in range(0, len(messages), 10):
            batch = messages[i:i + 10]
            entries = []

            for idx, msg in enumerate(batch):
                if not msg.get('image_urls'):
                    continue

                message_body = {
                    'article_id': msg['article_id'],
                    'image_urls': msg['image_urls'],
                }
                if msg.get('source_url'):
                    message_body['source_url'] = msg['source_url']

                entries.append({
                    'Id': str(idx),
                    'MessageBody': json.dumps(message_body, ensure_ascii=False),
                })

            if not entries:
                continue

            try:
                response = self.client.send_message_batch(
                    QueueUrl=self.queue_url,
                    Entries=entries,
                )
                results['successful'].extend(response.get('Successful', []))
                results['failed'].extend(response.get('Failed', []))
            except ClientError as e:
                print(f'SQS 배치 전송 실패: {e}')
                results['failed'].extend([{'Id': e['Id']} for e in entries])

        return results


def create_sqs_client(queue_url: str = None) -> SQSClient:
    """
    SQS 클라이언트 생성

    Args:
        queue_url: SQS 큐 URL (없으면 환경변수 또는 기본값 사용)

    Returns:
        SQSClient 인스턴스
    """
    url = queue_url or os.environ.get('SQS_QUEUE_URL') or DEFAULT_QUEUE_URL
    return SQSClient(queue_url=url)
