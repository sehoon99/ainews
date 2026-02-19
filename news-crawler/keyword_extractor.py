"""
TF-IDF + KoNLPy Okt 기반 뉴스 기사 핵심 키워드 추출

- 여러 기사를 배치로 받아 TF-IDF 계산
- 기사별 상위 N개 키워드를 콤마 구분 문자열로 반환
- 기사 1개 이하일 때는 단순 명사 빈도수 기반 fallback
"""

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


def _tokenize(text: str) -> list[str]:
    """Okt 명사 추출 + 1글자 제거 + 불용어 필터"""
    nouns = _okt.nouns(text)
    return [w for w in nouns if len(w) > 1 and w not in STOPWORDS]


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