"""
TF-IDF + KoNLPy Okt 기반 뉴스 기사 핵심 키워드 추출

- 여러 기사를 배치로 받아 TF-IDF 계산
- 기사별 상위 N개 키워드를 콤마 구분 문자열로 반환
- 동의어 정규화: 같은 의미의 단어를 대표 키워드로 통합
- 기사 1개 이하일 때는 단순 명사 빈도수 기반 fallback
"""

import re
from collections import Counter

from konlpy.tag import Okt
from sklearn.feature_extraction.text import TfidfVectorizer

# KoNLPy Okt 형태소 분석기 (모듈 로드 시 한 번만 초기화)
_okt = Okt()

# 불용어 리스트
STOPWORDS = {
    '기자', '뉴스', '무단', '배포', '전재', '복제', '금지', '저작권',
    '연합뉴스', '특파원', '입력', '수정', '관련', '제공', '사진', '영상',
    '출처', '댓글', '구독', '이메일', '기사', '본문', '내용', '단독',
    '속보', '종합',
}

# 동의어 → 대표 키워드 정규화 사전
# key = 대표 키워드, value = 동의어 리스트
SYNONYM_GROUPS = {
    'AI': ['인공지능', '에이아이', 'AI', '인공 지능'],
    '생성형AI': ['생성형AI', '생성형 AI', '생성형인공지능', '제너레이티브AI', '제너레이티브'],
    'LLM': ['LLM', '대규모언어모델', '거대언어모델', '대형언어모델', '언어모델'],
    '챗GPT': ['챗GPT', '챗지피티', 'ChatGPT', '챗봇GPT'],
    'GPT': ['GPT', '지피티'],
    '딥러닝': ['딥러닝', '딥 러닝', '심층학습'],
    '머신러닝': ['머신러닝', '머신 러닝', '기계학습'],
    '반도체': ['반도체', '칩', '칩셋'],
    'GPU': ['GPU', '지피유', '그래픽처리장치'],
    '자율주행': ['자율주행', '자율 주행', '자율운전', '자율 운전'],
    '전기차': ['전기차', '전기자동차', '전기 자동차', 'EV'],
    '블록체인': ['블록체인', '블록 체인', '분산원장'],
    '메타버스': ['메타버스', '메타 버스'],
    '클라우드': ['클라우드', '클라우드컴퓨팅'],
    '빅데이터': ['빅데이터', '빅 데이터'],
    '사이버보안': ['사이버보안', '사이버 보안', '정보보안', '정보 보안'],
    '핀테크': ['핀테크', '핀 테크', '금융기술'],
    '스타트업': ['스타트업', '스타트 업'],
    '삼성전자': ['삼성전자', '삼성'],
    'SK하이닉스': ['SK하이닉스', '하이닉스'],
    '엔비디아': ['엔비디아', 'NVIDIA', '엔비디아'],
    '마이크로소프트': ['마이크로소프트', 'MS', '마소'],
    '구글': ['구글', '알파벳'],
    '네이버': ['네이버', 'NAVER'],
    '카카오': ['카카오', 'Kakao'],
    '로봇': ['로봇', '로보틱스', '로보틱'],
    'IoT': ['IoT', '사물인터넷', '사물 인터넷'],
    'NFT': ['NFT', '대체불가토큰', '대체 불가 토큰'],
    '드론': ['드론', '무인항공기', '무인기'],
    '디지털전환': ['디지털전환', '디지털 전환', 'DX'],
}

# 역방향 매핑: 동의어 → 대표 키워드 (자동 생성)
_NORMALIZE_MAP = {}
for _canonical, _synonyms in SYNONYM_GROUPS.items():
    for _syn in _synonyms:
        _NORMALIZE_MAP[_syn] = _canonical
        _NORMALIZE_MAP[_syn.lower()] = _canonical

# 텍스트에서 직접 탐지할 패턴 (Okt가 못 잡는 영어/혼합어)
# 긴 패턴부터 매칭되도록 길이 내림차순 정렬
_DIRECT_PATTERNS = sorted(
    _NORMALIZE_MAP.keys(),
    key=len,
    reverse=True,
)
_DIRECT_REGEX = re.compile(
    '|'.join(re.escape(p) for p in _DIRECT_PATTERNS),
    re.IGNORECASE,
)


def _normalize(word: str) -> str:
    """동의어를 대표 키워드로 정규화"""
    return _NORMALIZE_MAP.get(word, _NORMALIZE_MAP.get(word.lower(), word))


def _tokenize(text: str) -> list[str]:
    """Okt 명사 추출 + 직접 패턴 매칭 + 불용어 필터 + 동의어 정규화"""
    # 1) Okt 명사 추출 → 정규화
    nouns = _okt.nouns(text)
    tokens = [_normalize(w) for w in nouns if len(w) > 1 and w not in STOPWORDS]

    # 2) Okt가 못 잡는 영어/혼합어를 regex로 직접 탐지
    for match in _DIRECT_REGEX.finditer(text):
        canonical = _normalize(match.group())
        tokens.append(canonical)

    return tokens


def _extract_by_frequency(text: str, top_n: int) -> str:
    """단순 명사 빈도수 기반 키워드 추출 (fallback)"""
    tokens = _tokenize(text)
    if not tokens:
        return ''
    counter = Counter(tokens)
    top_words = [word for word, _ in counter.most_common(top_n)]
    return ','.join(top_words)


def extract_keywords(text_list: list[str], top_n: int = 5) -> list[str]:
    """
    기사 본문 리스트에서 TF-IDF 기반 핵심 키워드를 추출합니다.

    Args:
        text_list: 기사 본문 텍스트 리스트
        top_n: 기사당 추출할 키워드 수 (기본값: 5)

    Returns:
        기사별 콤마 구분 키워드 문자열 리스트
        예: ["삼성전자,반도체,AI,수율,영업이익", "현대차,전기차,수출,판매,미국"]
    """
    if not text_list:
        return []

    # 기사 1개 이하 → 빈도수 기반 fallback (TF-IDF는 문서 간 비교가 핵심)
    if len(text_list) <= 1:
        return [_extract_by_frequency(text, top_n) for text in text_list]

    # TF-IDF 행렬 계산
    vectorizer = TfidfVectorizer(tokenizer=_tokenize, token_pattern=None)

    try:
        tfidf_matrix = vectorizer.fit_transform(text_list)
    except ValueError:
        # 모든 문서에서 유효한 토큰이 없는 경우
        return ['' for _ in text_list]

    feature_names = vectorizer.get_feature_names_out()

    # 기사별 상위 N개 키워드 추출
    results = []
    for row_idx in range(tfidf_matrix.shape[0]):
        row = tfidf_matrix.getrow(row_idx).toarray().flatten()

        if row.sum() == 0:
            # TF-IDF 점수가 전부 0인 경우 빈도수 fallback
            results.append(_extract_by_frequency(text_list[row_idx], top_n))
            continue

        # 점수 내림차순 정렬
        top_indices = row.argsort()[::-1][:top_n]
        top_words = [feature_names[i] for i in top_indices if row[i] > 0]
        results.append(','.join(top_words))

    return results