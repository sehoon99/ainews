"""
MySQL 데이터베이스 클라이언트
- providers, authors, articles 테이블 INSERT
- URL 중복 체크
- AWS Secrets Manager에서 자격증명 자동 로드
"""

import hashlib
import json
import os
from datetime import datetime
from typing import Optional

import boto3
import pymysql
from pymysql.cursors import DictCursor

# 기본 설정
DEFAULT_REGION = 'ap-northeast-2'
DEFAULT_SECRET_NAME = 'ainews-prod-db-credentials'


class DBClient:
    """MySQL 데이터베이스 클라이언트"""

    def __init__(
        self,
        host: str = None,
        port: int = None,
        user: str = None,
        password: str = None,
        database: str = None,
    ):
        """
        DB 클라이언트 초기화

        환경 변수 또는 직접 파라미터로 설정 가능.
        from_secrets_manager()를 사용하면 AWS에서 자동 로드.
        """
        self.host = host or os.environ.get('DB_HOST', 'localhost')
        self.port = port or int(os.environ.get('DB_PORT', 3306))
        self.user = user or os.environ.get('DB_USER', 'admin')
        self.password = password or os.environ.get('DB_PASSWORD', '')
        self.database = database or os.environ.get('DB_NAME', 'ainews')
        self._connection = None

    @classmethod
    def from_secrets_manager(
        cls,
        secret_name: str = DEFAULT_SECRET_NAME,
        region: str = DEFAULT_REGION,
    ) -> 'DBClient':
        """
        AWS Secrets Manager에서 DB 자격증명을 읽어서 클라이언트 생성

        Args:
            secret_name: Secrets Manager 시크릿 이름
            region: AWS 리전

        Returns:
            DBClient 인스턴스
        """
        client = boto3.client('secretsmanager', region_name=region)
        response = client.get_secret_value(SecretId=secret_name)
        secret = json.loads(response['SecretString'])

        return cls(
            host=secret['host'],
            port=int(secret.get('port', 3306)),
            user=secret['username'],
            password=secret['password'],
            database=secret['dbname'],
        )

    def connect(self) -> pymysql.Connection:
        """DB 연결"""
        if self._connection is None or not self._connection.open:
            self._connection = pymysql.connect(
                host=self.host,
                port=self.port,
                user=self.user,
                password=self.password,
                database=self.database,
                charset='utf8mb4',
                cursorclass=DictCursor,
                autocommit=False,
            )
        return self._connection

    def close(self):
        """DB 연결 종료"""
        if self._connection and self._connection.open:
            self._connection.close()
            self._connection = None

    def __enter__(self):
        self.connect()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()

    @staticmethod
    def hash_url(url: str) -> str:
        """URL을 SHA256 해시로 변환"""
        return hashlib.sha256(url.encode('utf-8')).hexdigest()

    def url_exists(self, source_url: str) -> bool:
        """URL 중복 체크 (해시 기반)"""
        url_hash = self.hash_url(source_url)
        conn = self.connect()
        with conn.cursor() as cursor:
            cursor.execute(
                'SELECT 1 FROM articles WHERE source_url_hash = %s LIMIT 1',
                (url_hash,)
            )
            return cursor.fetchone() is not None

    def get_or_create_provider(self, name: str) -> int:
        """
        언론사 조회 또는 생성

        Args:
            name: 언론사명

        Returns:
            provider_id
        """
        if not name:
            name = 'Unknown'

        conn = self.connect()
        with conn.cursor() as cursor:
            # 기존 provider 조회
            cursor.execute(
                'SELECT id FROM providers WHERE name = %s',
                (name,)
            )
            result = cursor.fetchone()

            if result:
                return result['id']

            # 새 provider 생성
            cursor.execute(
                'INSERT INTO providers (name) VALUES (%s)',
                (name,)
            )
            conn.commit()
            return cursor.lastrowid

    def get_or_create_author(
        self,
        provider_id: int,
        name: str,
        email: Optional[str] = None,
    ) -> Optional[int]:
        """
        기자 조회 또는 생성

        Args:
            provider_id: 언론사 ID
            name: 기자명
            email: 이메일 (선택)

        Returns:
            author_id or None
        """
        if not name:
            return None

        conn = self.connect()
        with conn.cursor() as cursor:
            # 기존 author 조회 (provider_id + name)
            cursor.execute(
                'SELECT id FROM authors WHERE provider_id = %s AND name = %s',
                (provider_id, name)
            )
            result = cursor.fetchone()

            if result:
                return result['id']

            # 새 author 생성
            cursor.execute(
                'INSERT INTO authors (provider_id, name, email) VALUES (%s, %s, %s)',
                (provider_id, name, email)
            )
            conn.commit()
            return cursor.lastrowid

    def insert_article(
        self,
        provider_id: int,
        author_id: Optional[int],
        portal: str,
        title: str,
        content: str,
        source_url: str,
        published_at: Optional[datetime] = None,
    ) -> Optional[int]:
        """
        기사 INSERT (중복 URL은 무시)

        Args:
            provider_id: 언론사 ID
            author_id: 기자 ID (nullable)
            portal: 포털 타입 (naver, daum, others)
            title: 기사 제목
            content: 본문
            source_url: 원본 URL
            published_at: 발행일시

        Returns:
            article_id or None (중복 시)
        """
        url_hash = self.hash_url(source_url)
        conn = self.connect()
        try:
            with conn.cursor() as cursor:
                cursor.execute(
                    '''
                    INSERT INTO articles
                        (provider_id, author_id, portal, title, content, source_url, source_url_hash, published_at)
                    VALUES
                        (%s, %s, %s, %s, %s, %s, %s, %s)
                    ''',
                    (provider_id, author_id, portal, title, content, source_url, url_hash, published_at)
                )
                conn.commit()
                return cursor.lastrowid
        except pymysql.IntegrityError as e:
            # Duplicate entry for source_url
            if e.args[0] == 1062:
                conn.rollback()
                return None
            raise

    def delete_article(self, article_id: int) -> None:
        """Delete an article when its downstream SQS message was not accepted."""
        conn = self.connect()
        with conn.cursor() as cursor:
            cursor.execute('DELETE FROM articles WHERE id = %s', (article_id,))
        conn.commit()

    def insert_news_data(self, data: dict, portal: str) -> Optional[int]:
        """
        크롤링된 뉴스 데이터를 DB에 INSERT

        Args:
            data: extract_news_data()가 반환한 딕셔너리
            portal: 포털 타입 (naver, daum, others)

        Returns:
            article_id or None
        """
        # 필수 필드 확인
        if 'error' in data:
            return None

        title = data.get('title')
        content = data.get('content')
        source_url = data.get('source_url')

        if not title or not content or not source_url:
            return None

        # URL 중복 체크
        if self.url_exists(source_url):
            return None

        # Provider 처리
        provider_name = data.get('provider') or 'Unknown'
        provider_id = self.get_or_create_provider(provider_name)

        # Author 처리
        journalist = data.get('journalist')
        author_id = None
        if journalist:
            author_id = self.get_or_create_author(provider_id, journalist)

        # published_at 파싱
        published_at = None
        published_str = data.get('published_at')
        if published_str:
            try:
                # 다양한 형식 지원
                for fmt in [
                    '%Y-%m-%d %H:%M:%S',
                    '%Y-%m-%dT%H:%M:%S',
                    '%Y.%m.%d. %H:%M',
                    '%Y.%m.%d %H:%M',
                ]:
                    try:
                        published_at = datetime.strptime(published_str, fmt)
                        break
                    except ValueError:
                        continue
            except Exception:
                pass

        # Article INSERT
        return self.insert_article(
            provider_id=provider_id,
            author_id=author_id,
            portal=portal,
            title=title,
            content=content,
            source_url=source_url,
            published_at=published_at,
        )

    def update_article_keywords(self, article_id: int, keywords: str) -> None:
        """
        기사의 keywords 컬럼 업데이트

        Args:
            article_id: 기사 ID
            keywords: 콤마 구분 키워드 문자열
        """
        conn = self.connect()
        with conn.cursor() as cursor:
            cursor.execute(
                'UPDATE articles SET keywords = %s WHERE id = %s',
                (keywords, article_id)
            )
        conn.commit()

    def update_batch_keywords(self, pairs: list[tuple[int, str]]) -> int:
        """
        여러 기사의 keywords를 배치 업데이트

        Args:
            pairs: [(article_id, "키워드1,키워드2,..."), ...] 리스트

        Returns:
            업데이트된 행 수
        """
        if not pairs:
            return 0

        conn = self.connect()
        updated = 0
        with conn.cursor() as cursor:
            for article_id, keywords in pairs:
                if not keywords:
                    continue
                cursor.execute(
                    'UPDATE articles SET keywords = %s WHERE id = %s',
                    (keywords, article_id)
                )
                updated += cursor.rowcount
        conn.commit()
        return updated

    def get_article_images(self, article_id: int) -> list[str]:
        """
        기사의 이미지 URL 목록 조회 (articles 테이블에는 images 컬럼이 없으므로,
        이 메서드는 별도 images 테이블이 있을 경우 사용)
        """
        # 현재 스키마에서는 images 테이블이 없으므로 빈 리스트 반환
        return []
