"""
Sightengine API 클라이언트
- AI 이미지 생성 확률 분석
- SSM Parameter Store에서 API 키 로드
"""

import os
from typing import Optional

import requests
import boto3
from botocore.exceptions import ClientError


class SightengineClient:
    """Sightengine API 클라이언트"""

    API_URL = 'https://api.sightengine.com/1.0/check.json'

    def __init__(
        self,
        api_user: str = None,
        api_secret: str = None,
        region_name: str = None,
    ):
        """
        Sightengine 클라이언트 초기화

        환경 변수 또는 SSM에서 키 로드:
            SIGHTENGINE_API_USER, SIGHTENGINE_API_SECRET
            또는
            SSM_SIGHTENGINE_API_USER, SSM_SIGHTENGINE_API_SECRET
        """
        self.region_name = region_name or os.environ.get('AWS_REGION', 'ap-northeast-2')
        self._api_user = api_user
        self._api_secret = api_secret
        self._ssm_client = None

    @property
    def ssm_client(self):
        """Lazy boto3 SSM client"""
        if self._ssm_client is None:
            self._ssm_client = boto3.client('ssm', region_name=self.region_name)
        return self._ssm_client

    def _get_ssm_parameter(self, name: str) -> Optional[str]:
        """SSM Parameter Store에서 값 조회"""
        try:
            response = self.ssm_client.get_parameter(
                Name=name,
                WithDecryption=True,
            )
            return response['Parameter']['Value']
        except ClientError as e:
            print(f'SSM 파라미터 조회 실패 ({name}): {e}')
            return None

    @property
    def api_user(self) -> str:
        """API User (lazy load)"""
        if self._api_user is None:
            # 환경변수 우선
            self._api_user = os.environ.get('SIGHTENGINE_API_USER')

            # SSM에서 로드
            if not self._api_user:
                ssm_name = os.environ.get('SSM_SIGHTENGINE_API_USER')
                if ssm_name:
                    self._api_user = self._get_ssm_parameter(ssm_name)

        if not self._api_user:
            raise ValueError('Sightengine API user가 설정되지 않았습니다.')

        return self._api_user

    @property
    def api_secret(self) -> str:
        """API Secret (lazy load)"""
        if self._api_secret is None:
            # 환경변수 우선
            self._api_secret = os.environ.get('SIGHTENGINE_API_SECRET')

            # SSM에서 로드
            if not self._api_secret:
                ssm_name = os.environ.get('SSM_SIGHTENGINE_API_SECRET')
                if ssm_name:
                    self._api_secret = self._get_ssm_parameter(ssm_name)

        if not self._api_secret:
            raise ValueError('Sightengine API secret이 설정되지 않았습니다.')

        return self._api_secret

    def analyze_image(self, image_url: str) -> dict:
        """
        이미지 AI 생성 확률 분석

        Args:
            image_url: 분석할 이미지 URL

        Returns:
            {
                'success': bool,
                'ai_probability': float (0.0 ~ 1.0) or None,
                'error': str or None,
                'raw_response': dict,
            }
        """
        try:
            response = requests.get(
                self.API_URL,
                params={
                    'url': image_url,
                    'models': 'genai',
                    'api_user': self.api_user,
                    'api_secret': self.api_secret,
                },
                timeout=30,
            )
            response.raise_for_status()
            data = response.json()

            if data.get('status') == 'success':
                # genai 모델 결과 추출
                ai_prob = data.get('type', {}).get('ai_generated', 0.0)
                return {
                    'success': True,
                    'ai_probability': ai_prob,
                    'error': None,
                    'raw_response': data,
                }
            else:
                return {
                    'success': False,
                    'ai_probability': None,
                    'error': data.get('error', {}).get('message', 'Unknown error'),
                    'raw_response': data,
                }

        except requests.RequestException as e:
            return {
                'success': False,
                'ai_probability': None,
                'error': str(e),
                'raw_response': None,
            }
        except Exception as e:
            return {
                'success': False,
                'ai_probability': None,
                'error': str(e),
                'raw_response': None,
            }


def create_sightengine_client() -> SightengineClient:
    """Sightengine 클라이언트 팩토리"""
    return SightengineClient()
