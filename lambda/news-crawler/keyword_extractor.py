"""
뉴스 기사 핵심 키워드 추출

- 로컬 (konlpy 설치됨): TF-IDF + KoNLPy Okt 형태소 분석
- Lambda (konlpy 없음): 정규식 + 빈도수 기반 fallback
- 기사 1개 이하일 때는 단순 명사 빈도수 기반 fallback
"""

import re
from collections import Counter

try:
    from konlpy.tag import Okt
    from sklearn.feature_extraction.text import TfidfVectorizer
    _okt = Okt()
    _USE_KONLPY = True
except ImportError:
    _okt = None
    _USE_KONLPY = False

# 한글 2글자 이상 단어 추출 패턴 (konlpy 없을 때 사용)
_KOREAN_WORD_RE = re.compile(r'[가-힣]{2,}')

# 불용어 리스트
STOPWORDS = {
    '기자', '뉴스', '무단', '배포', '전재', '복제', '금지', '저작권',
    '연합뉴스', '특파원', '입력', '수정', '관련', '제공', '사진', '영상',
    '출처', '댓글', '구독', '이메일', '기사', '본문', '내용', '단독',
    '속보', '종합', '지난', '이번', '대한', '통해', '위해', '라고',
    '하는', '있는', '에서', '으로', '까지', '부터', '에서는',
    '있다', '없다', '했다', '됐다', '된다', '한다', '하다', '같다',
    '이다', '있고', '없고', '하고', '되고', '했고', '됐고', '라며',
    '에서도', '에서의', '으로는', '으로도', '이라고', '라면서',
    '대해', '따라', '함께', '또한', '그리고', '하지만', '때문',
    '것으로', '것이다', '수도', '해야', '하며', '되며', '이라',
}

# 용언 어미 패턴 (정규식 fallback에서 필터링)
_VERB_ENDING_RE = re.compile(
    r'.+(하다|했다|된다|됐다|한다|있다|없다|같다|이다|시다|겠다|였다|렸다'
    r'|하고|되고|했고|됐고|있고|없고'
    r'|하는|되는|있는|없는|했는|됐는'
    r'|하면|되면|있으면|없으면'
    r'|하여|되어|하게|되게)$'
)


def _tokenize(text: str) -> list[str]:
    """명사 추출 + 1글자 제거 + 불용어 필터"""
    if _USE_KONLPY and _okt:
        nouns = _okt.nouns(text)
    else:
        nouns = _KOREAN_WORD_RE.findall(text)
    filtered = [w for w in nouns if len(w) > 1 and w not in STOPWORDS]
    if not _USE_KONLPY:
        filtered = [w for w in filtered if not _VERB_ENDING_RE.match(w)]
    return filtered


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

    # konlpy 없거나 기사 1개 이하 → 빈도수 기반 fallback
    if not _USE_KONLPY or len(text_list) <= 1:
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