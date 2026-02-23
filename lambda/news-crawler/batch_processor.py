"""
SQS 배치 처리기
크롤링된 JSON 파일을 SQS로 보내고 Sightengine API로 이미지 분석
"""

import os
import json
import glob
import boto3
from datetime import datetime
from sightengine_client import get_sightengine_credentials, check_ai_generated_image

# 설정
DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'data')
PROCESSED_LOG = os.path.join(DATA_DIR, 'processed_files.json')
RESULTS_DIR = os.path.join(DATA_DIR, 'analysis_results')


def load_processed_files() -> set:
    """처리 완료된 파일 목록 로드"""
    if os.path.exists(PROCESSED_LOG):
        with open(PROCESSED_LOG, 'r', encoding='utf-8') as f:
            return set(json.load(f))
    return set()


def save_processed_files(processed: set):
    """처리 완료된 파일 목록 저장"""
    with open(PROCESSED_LOG, 'w', encoding='utf-8') as f:
        json.dump(list(processed), f, ensure_ascii=False, indent=2)


def get_unprocessed_files() -> list:
    """
    아직 처리되지 않은 JSON 파일 목록 가져오기

    Returns:
        list: 미처리 JSON 파일 경로 리스트
    """
    processed = load_processed_files()
    all_files = []

    # naver와 daum 디렉토리에서 JSON 파일 검색
    for portal in ['naver', 'daum']:
        portal_dir = os.path.join(DATA_DIR, portal)
        if os.path.exists(portal_dir):
            pattern = os.path.join(portal_dir, '*.json')
            all_files.extend(glob.glob(pattern))

    # 이미 처리된 파일 제외
    unprocessed = [f for f in all_files if os.path.basename(f) not in processed]
    return sorted(unprocessed)


def send_to_sqs(queue_url: str, file_paths: list, region_name: str = "ap-northeast-2"):
    """
    파일 경로들을 SQS 큐에 전송

    Args:
        queue_url: SQS 큐 URL
        file_paths: 처리할 파일 경로 리스트
        region_name: AWS 리전
    """
    sqs = boto3.client('sqs', region_name=region_name)

    for file_path in file_paths:
        message_body = json.dumps({
            'file_path': file_path,
            'timestamp': datetime.now().isoformat()
        })

        sqs.send_message(
            QueueUrl=queue_url,
            MessageBody=message_body
        )
        print(f"SQS에 전송: {os.path.basename(file_path)}")


def process_json_file(file_path: str, credentials: dict) -> dict:
    """
    JSON 파일 내 모든 이미지를 Sightengine으로 분석

    Args:
        file_path: 분석할 JSON 파일 경로
        credentials: Sightengine API credentials

    Returns:
        dict: 분석 결과
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        articles = json.load(f)

    # 단일 기사 객체인 경우 리스트로 변환
    if isinstance(articles, dict):
        articles = [articles]

    results = []
    for article in articles:
        article_result = {
            'title': article.get('title'),
            'source_url': article.get('source_url'),
            'images': []
        }

        for image_url in article.get('images', []):
            try:
                analysis = check_ai_generated_image(image_url, credentials)
                ai_score = None

                if analysis.get('status') == 'success':
                    # genai 모델 결과에서 AI 생성 확률 추출
                    ai_score = analysis.get('type', {}).get('ai_generated')

                article_result['images'].append({
                    'url': image_url,
                    'ai_generated_score': ai_score,
                    'raw_result': analysis
                })
                print(f"  분석 완료: {image_url[:50]}... (AI 확률: {ai_score})")

            except Exception as e:
                article_result['images'].append({
                    'url': image_url,
                    'error': str(e)
                })
                print(f"  분석 실패: {image_url[:50]}... ({e})")

        results.append(article_result)

    return {
        'source_file': file_path,
        'analyzed_at': datetime.now().isoformat(),
        'articles': results
    }


def save_analysis_result(result: dict, source_file: str):
    """분석 결과 저장"""
    os.makedirs(RESULTS_DIR, exist_ok=True)

    basename = os.path.basename(source_file)
    result_file = os.path.join(RESULTS_DIR, f"analysis_{basename}")

    with open(result_file, 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"결과 저장: {result_file}")
    return result_file


def process_sqs_messages(queue_url: str, max_messages: int = 10,
                         region_name: str = "ap-northeast-2"):
    """
    SQS 큐에서 메시지를 가져와 처리

    Args:
        queue_url: SQS 큐 URL
        max_messages: 한 번에 가져올 최대 메시지 수
        region_name: AWS 리전
    """
    sqs = boto3.client('sqs', region_name=region_name)
    credentials = get_sightengine_credentials()
    processed = load_processed_files()

    while True:
        response = sqs.receive_message(
            QueueUrl=queue_url,
            MaxNumberOfMessages=min(max_messages, 10),  # SQS 최대 10개
            WaitTimeSeconds=20  # Long polling
        )

        messages = response.get('Messages', [])
        if not messages:
            print("처리할 메시지가 없습니다.")
            break

        for message in messages:
            body = json.loads(message['Body'])
            file_path = body['file_path']
            filename = os.path.basename(file_path)

            print(f"\n처리 중: {filename}")

            try:
                result = process_json_file(file_path, credentials)
                save_analysis_result(result, file_path)

                # 처리 완료 기록
                processed.add(filename)
                save_processed_files(processed)

                # SQS에서 메시지 삭제
                sqs.delete_message(
                    QueueUrl=queue_url,
                    ReceiptHandle=message['ReceiptHandle']
                )
                print(f"완료: {filename}")

            except Exception as e:
                print(f"에러 ({filename}): {e}")


def process_local_batch(max_files: int = None):
    """
    SQS 없이 로컬에서 직접 배치 처리 (테스트용)

    Args:
        max_files: 처리할 최대 파일 수 (None이면 전체)
    """
    credentials = get_sightengine_credentials()
    processed = load_processed_files()
    unprocessed = get_unprocessed_files()

    if max_files:
        unprocessed = unprocessed[:max_files]

    print(f"처리 대상: {len(unprocessed)}개 파일")

    for file_path in unprocessed:
        filename = os.path.basename(file_path)
        print(f"\n처리 중: {filename}")

        try:
            result = process_json_file(file_path, credentials)
            save_analysis_result(result, file_path)

            processed.add(filename)
            save_processed_files(processed)
            print(f"완료: {filename}")

        except Exception as e:
            print(f"에러 ({filename}): {e}")


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description='Sightengine 배치 처리기')
    parser.add_argument('--list', action='store_true', help='미처리 파일 목록 출력')
    parser.add_argument('--local', action='store_true', help='로컬 모드로 처리')
    parser.add_argument('--max', type=int, help='처리할 최대 파일 수')
    parser.add_argument('--queue-url', help='SQS 큐 URL')
    parser.add_argument('--send', action='store_true', help='SQS에 메시지 전송')
    parser.add_argument('--receive', action='store_true', help='SQS에서 메시지 처리')

    args = parser.parse_args()

    if args.list:
        unprocessed = get_unprocessed_files()
        print(f"미처리 파일: {len(unprocessed)}개")
        for f in unprocessed:
            print(f"  - {os.path.basename(f)}")

    elif args.local:
        process_local_batch(args.max)

    elif args.send and args.queue_url:
        unprocessed = get_unprocessed_files()
        if args.max:
            unprocessed = unprocessed[:args.max]
        send_to_sqs(args.queue_url, unprocessed)

    elif args.receive and args.queue_url:
        process_sqs_messages(args.queue_url)

    else:
        parser.print_help()
