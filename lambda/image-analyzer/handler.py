"""
Lambda 이미지 분석 핸들러
- SQS 트리거로 article_id, image_urls 수신
- Sightengine API로 AI 생성 확률 분석
- image_analyses 테이블에 결과 INSERT
"""

import os
import json
import hashlib
from datetime import datetime

import boto3
import pymysql
import requests
from pymysql.cursors import DictCursor


# 환경 변수
DB_HOST = os.environ.get('DB_HOST')
DB_PORT = int(os.environ.get('DB_PORT', 3306))
DB_NAME = os.environ.get('DB_NAME')
DB_SECRET_ARN = os.environ.get('DB_SECRET_ARN')
SSM_SIGHTENGINE_API_USER = os.environ.get('SSM_SIGHTENGINE_API_USER')
SSM_SIGHTENGINE_API_SECRET = os.environ.get('SSM_SIGHTENGINE_API_SECRET')

# Sightengine API 엔드포인트
SIGHTENGINE_API_URL = 'https://api.sightengine.com/1.0/check.json'

# 클라이언트 캐시 (Lambda warm start 대응)
_ssm_client = None
_secrets_client = None
_db_credentials = None
_sightengine_credentials = None


def get_ssm_client():
    """SSM 클라이언트 (캐시)"""
    global _ssm_client
    if _ssm_client is None:
        _ssm_client = boto3.client('ssm')
    return _ssm_client


def get_secrets_client():
    """Secrets Manager 클라이언트 (캐시)"""
    global _secrets_client
    if _secrets_client is None:
        _secrets_client = boto3.client('secretsmanager')
    return _secrets_client


def get_db_credentials():
    """DB 자격증명 조회 (Secrets Manager)"""
    global _db_credentials
    if _db_credentials is None:
        client = get_secrets_client()
        response = client.get_secret_value(SecretId=DB_SECRET_ARN)
        _db_credentials = json.loads(response['SecretString'])
    return _db_credentials


def get_sightengine_credentials():
    """Sightengine API 자격증명 조회 (SSM)"""
    global _sightengine_credentials
    if _sightengine_credentials is None:
        client = get_ssm_client()
        response = client.get_parameters(
            Names=[SSM_SIGHTENGINE_API_USER, SSM_SIGHTENGINE_API_SECRET],
            WithDecryption=True,
        )
        _sightengine_credentials = {
            p['Name']: p['Value'] for p in response['Parameters']
        }
    return _sightengine_credentials


def get_db_connection():
    """DB 연결 생성"""
    creds = get_db_credentials()
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=creds['username'],
        password=creds['password'],
        database=DB_NAME,
        charset='utf8mb4',
        cursorclass=DictCursor,
        autocommit=False,
    )


def calculate_image_hash(image_url: str) -> str:
    """이미지 URL의 SHA-256 해시 계산"""
    return hashlib.sha256(image_url.encode('utf-8')).hexdigest()


def get_existing_analysis(conn, image_hash: str) -> dict:
    """기존 분석 결과 조회 (image_hash 기준)"""
    with conn.cursor() as cursor:
        cursor.execute(
            '''
            SELECT id, ai_probability, api_name
            FROM image_analyses
            WHERE image_hash = %s
            LIMIT 1
            ''',
            (image_hash,)
        )
        return cursor.fetchone()


def insert_image_analysis(
    conn,
    article_id: int,
    image_url: str,
    image_hash: str,
    api_name: str,
    ai_probability: float,
) -> int:
    """이미지 분석 결과 INSERT"""
    with conn.cursor() as cursor:
        cursor.execute(
            '''
            INSERT INTO image_analyses
                (article_id, image_url, image_hash, api_name, ai_probability)
            VALUES
                (%s, %s, %s, %s, %s)
            ''',
            (article_id, image_url, image_hash, api_name, ai_probability)
        )
        conn.commit()
        return cursor.lastrowid


def analyze_image_with_sightengine(image_url: str) -> dict:
    """Sightengine API로 이미지 분석"""
    creds = get_sightengine_credentials()
    api_user = creds.get(SSM_SIGHTENGINE_API_USER)
    api_secret = creds.get(SSM_SIGHTENGINE_API_SECRET)

    try:
        response = requests.get(
            SIGHTENGINE_API_URL,
            params={
                'url': image_url,
                'models': 'genai',
                'api_user': api_user,
                'api_secret': api_secret,
            },
            timeout=30,
        )
        response.raise_for_status()
        data = response.json()

        if data.get('status') == 'success':
            ai_prob = data.get('type', {}).get('ai_generated', 0.0)
            return {
                'success': True,
                'ai_probability': ai_prob,
                'error': None,
            }
        else:
            return {
                'success': False,
                'ai_probability': None,
                'error': data.get('error', {}).get('message', 'Unknown error'),
            }

    except requests.RequestException as e:
        return {
            'success': False,
            'ai_probability': None,
            'error': str(e),
        }


def process_message(message: dict) -> dict:
    """
    단일 SQS 메시지 처리

    Args:
        message: {'article_id': int, 'image_urls': list, 'source_url': str}

    Returns:
        {'article_id': int, 'processed': int, 'reused': int, 'failed': int}
    """
    article_id = message.get('article_id')
    image_urls = message.get('image_urls', [])
    source_url = message.get('source_url', '')

    print(f'Processing article_id={article_id}, images={len(image_urls)}, source={source_url[:50]}')

    result = {
        'article_id': article_id,
        'processed': 0,
        'reused': 0,
        'failed': 0,
    }

    conn = get_db_connection()
    try:
        for image_url in image_urls:
            image_hash = calculate_image_hash(image_url)

            # 기존 분석 결과 확인
            existing = get_existing_analysis(conn, image_hash)
            if existing:
                # 기존 결과 재사용 (동일 이미지를 다른 기사에서 사용한 경우)
                insert_image_analysis(
                    conn,
                    article_id=article_id,
                    image_url=image_url,
                    image_hash=image_hash,
                    api_name=existing['api_name'],
                    ai_probability=existing['ai_probability'],
                )
                result['reused'] += 1
                print(f'  Reused: {image_url[:50]}... (prob={existing["ai_probability"]})')
                continue

            # Sightengine API 호출
            analysis = analyze_image_with_sightengine(image_url)

            if analysis['success']:
                insert_image_analysis(
                    conn,
                    article_id=article_id,
                    image_url=image_url,
                    image_hash=image_hash,
                    api_name='sightengine',
                    ai_probability=analysis['ai_probability'],
                )
                result['processed'] += 1
                print(f'  Analyzed: {image_url[:50]}... (prob={analysis["ai_probability"]})')
            else:
                result['failed'] += 1
                print(f'  Failed: {image_url[:50]}... (error={analysis["error"]})')

    finally:
        conn.close()

    return result


def lambda_handler(event, context):
    """
    Lambda 핸들러 (SQS 트리거)

    Event structure:
    {
        "Records": [
            {
                "body": "{\"article_id\": 123, \"image_urls\": [...]}",
                ...
            }
        ]
    }
    """
    print(f'Received {len(event.get("Records", []))} records')

    results = []
    batch_item_failures = []

    for record in event.get('Records', []):
        message_id = record.get('messageId', 'unknown')
        try:
            body = json.loads(record['body'])
            result = process_message(body)
            results.append(result)
            print(f'Message {message_id} processed: {result}')

        except Exception as e:
            print(f'Error processing message {message_id}: {e}')
            # 실패한 메시지는 DLQ로 이동하도록 batch item failure 반환
            batch_item_failures.append({
                'itemIdentifier': message_id,
            })

    # 처리 결과 요약
    total_processed = sum(r['processed'] for r in results)
    total_reused = sum(r['reused'] for r in results)
    total_failed = sum(r['failed'] for r in results)

    print(f'Summary: processed={total_processed}, reused={total_reused}, failed={total_failed}')

    # Partial batch response (실패한 메시지만 재처리)
    return {
        'batchItemFailures': batch_item_failures,
    }
