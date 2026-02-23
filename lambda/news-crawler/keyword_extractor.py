"""
뉴스 기사 핵심 키워드 추출

- Lambda 환경: 정규식 기반 명사 추출 (konlpy/sklearn 불필요)
- 로컬 환경: TF-IDF + KoNLPy Okt (가능 시)
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

STOPWORDS = {
    '기자', '뉴스', '무단', '배포', '전재', '복제', '금지', '저작권',
    '연합뉴스', '특파원', '입력', '수정', '관련', '제공', '사진', '영상',
    '출처', '댓글', '구독', '이메일', '기사', '본문', '내용', '단독',
    '속보', '종합', '지난', '이번', '대한', '통해', '위해', '라고',
    '하는', '있는', '에서', '으로', '까지', '부터', '에서는',
}

_KOREAN_WORD_RE = re.compile(r'[가-힣]{2,}')


def _tokenize(text: str) -> list[str]:
    if _USE_KONLPY and _okt:
        nouns = _okt.nouns(text)
    else:
        nouns = _KOREAN_WORD_RE.findall(text)
    return [w for w in nouns if len(w) > 1 and w not in STOPWORDS]


def _extract_by_frequency(text: str, top_n: int) -> str:
    tokens = _tokenize(text)
    if not tokens:
        return ''
    counter = Counter(tokens)
    top_words = [word for word, _ in counter.most_common(top_n)]
    return ','.join(top_words)


def extract_keywords(text_list: list[str], top_n: int = 5) -> list[str]:
    if not text_list:
        return []

    if not _USE_KONLPY or len(text_list) <= 1:
        return [_extract_by_frequency(text, top_n) for text in text_list]

    vectorizer = TfidfVectorizer(tokenizer=_tokenize, token_pattern=None)
    try:
        tfidf_matrix = vectorizer.fit_transform(text_list)
    except ValueError:
        return ['' for _ in text_list]

    feature_names = vectorizer.get_feature_names_out()
    results = []
    for row_idx in range(tfidf_matrix.shape[0]):
        row = tfidf_matrix.getrow(row_idx).toarray().flatten()
        if row.sum() == 0:
            results.append(_extract_by_frequency(text_list[row_idx], top_n))
            continue
        top_indices = row.argsort()[::-1][:top_n]
        top_words = [feature_names[i] for i in top_indices if row[i] > 0]
        results.append(','.join(top_words))

    return results
