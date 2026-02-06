"""
DeepScan News - 네이버/다음 뉴스 본문 이미지 및 메타데이터 추출 크롤러
"""

import re
import json
import hashlib
from urllib.parse import urljoin, urlparse
from typing import Optional
from datetime import datetime, timezone
from pathlib import Path

import requests
from bs4 import BeautifulSoup
from newspaper import Article


# 필터링할 이미지 URL 키워드
NOISE_KEYWORDS = [
    'logo', 'banner', 'ad', 'icon', 'button', 'sprite',
    'blank', 'spacer', 'pixel', 'tracking', 'badge',
    'avatar', 'profile', 'reporter', 'journalist',
    'recommend', 'related', 'sidebar', 'widget',
    'sns', 'share', 'comment', 'emoticon', 'emoji',
]

# 최소 이미지 크기 (픽셀)
MIN_IMAGE_WIDTH = 100
MIN_IMAGE_HEIGHT = 100

# 요청 헤더
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
    'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7',
}

# 데이터 저장 경로
DATA_DIR = Path(__file__).parent / 'data'

# 크롤링 기록 파일
CRAWLED_URLS_FILE = DATA_DIR / 'crawled_urls.json'

# 배치 크기
BATCH_SIZE = 50


def is_naver_news(url: str) -> bool:
    """네이버 뉴스 URL인지 확인"""
    return 'news.naver.com' in url


def is_daum_news(url: str) -> bool:
    """다음 뉴스 URL인지 확인"""
    return 'news.daum.net' in url or 'v.daum.net/v/' in url


def get_portal_type(url: str) -> str:
    """URL에서 포털 타입 반환 (naver, daum, others)"""
    if is_naver_news(url):
        return 'naver'
    elif is_daum_news(url):
        return 'daum'
    else:
        return 'others'


def generate_filename(url: str) -> str:
    """URL 기반 고유 파일명 생성"""
    url_hash = hashlib.md5(url.encode()).hexdigest()[:12]
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    return f'{timestamp}_{url_hash}.json'


def load_crawled_urls() -> set:
    """이미 크롤링한 URL 목록 로드"""
    if not CRAWLED_URLS_FILE.exists():
        return set()

    with open(CRAWLED_URLS_FILE, 'r', encoding='utf-8') as f:
        data = json.load(f)
        return set(data.get('urls', []))


def save_crawled_urls(urls: set) -> None:
    """크롤링한 URL 목록 저장"""
    DATA_DIR.mkdir(parents=True, exist_ok=True)

    with open(CRAWLED_URLS_FILE, 'w', encoding='utf-8') as f:
        json.dump({
            'urls': list(urls),
            'count': len(urls),
            'updated_at': datetime.now(timezone.utc).isoformat() + 'Z',
        }, f, ensure_ascii=False, indent=2)


def filter_new_urls(urls: list[str]) -> list[str]:
    """이미 크롤링한 URL 제외하고 새로운 URL만 반환"""
    crawled = load_crawled_urls()
    new_urls = [url for url in urls if url not in crawled]
    print(f'전체 {len(urls)}개 중 새로운 URL: {len(new_urls)}개 (기존 {len(urls) - len(new_urls)}개 제외)')
    return new_urls


def save_news_data(data: dict, base_dir: Optional[Path] = None) -> str:
    """
    뉴스 데이터를 포털별 폴더에 JSON 파일로 저장합니다.

    Args:
        data: extract_news_data()가 반환한 딕셔너리
        base_dir: 저장 기본 경로 (기본값: ./data)

    Returns:
        str: 저장된 파일 경로
    """
    if base_dir is None:
        base_dir = DATA_DIR

    source_url = data.get('source_url', '')
    portal_type = get_portal_type(source_url)

    # 포털별 폴더 생성
    portal_dir = base_dir / portal_type
    portal_dir.mkdir(parents=True, exist_ok=True)

    # 파일명 생성 및 저장
    filename = generate_filename(source_url)
    file_path = portal_dir / filename

    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    return str(file_path)


def extract_and_save(url: str, base_dir: Optional[Path] = None) -> tuple[dict, str]:
    """
    뉴스 URL에서 데이터를 추출하고 포털별 폴더에 저장합니다.

    Args:
        url: 뉴스 기사 URL
        base_dir: 저장 기본 경로 (기본값: ./data)

    Returns:
        tuple: (추출된 데이터 딕셔너리, 저장된 파일 경로)
    """
    data = extract_news_data(url)
    file_path = save_news_data(data, base_dir)
    return data, file_path


def extract_batch(urls: list[str], base_dir: Optional[Path] = None) -> list[dict]:
    """
    여러 뉴스 URL을 배치로 처리하고 포털별 폴더에 저장합니다.

    Args:
        urls: 뉴스 기사 URL 리스트
        base_dir: 저장 기본 경로 (기본값: ./data)

    Returns:
        list: 처리 결과 리스트 [{url, success, path, error}, ...]
    """
    results = []
    total = len(urls)

    for i, url in enumerate(urls, 1):
        print(f'[{i}/{total}] 처리 중: {url[:60]}...')

        try:
            data, path = extract_and_save(url, base_dir)
            has_error = 'error' in data

            results.append({
                'url': url,
                'success': not has_error,
                'path': path,
                'title': data.get('title'),
                'images_count': len(data.get('images', [])),
                'error': data.get('error'),
            })

            status = '실패' if has_error else f"성공 (이미지 {len(data.get('images', []))}개)"
            print(f'    → {status}')

        except Exception as e:
            results.append({
                'url': url,
                'success': False,
                'path': None,
                'error': str(e),
            })
            print(f'    → 오류: {e}')

    # 요약 출력
    success_count = sum(1 for r in results if r['success'])
    print(f'\n완료: {success_count}/{total} 성공')

    return results


def crawl_and_save_batch(
    urls: list[str],
    base_dir: Optional[Path] = None,
    batch_size: int = BATCH_SIZE,
) -> dict:
    """
    여러 뉴스 URL을 크롤링하고 50개씩 묶어서 포털별 JSON 파일로 저장
    이미 크롤링한 URL은 자동으로 제외

    Args:
        urls: 뉴스 기사 URL 리스트
        base_dir: 저장 기본 경로 (기본값: ./data)
        batch_size: 하나의 JSON 파일에 저장할 기사 수 (기본값: 50)

    Returns:
        dict: {saved_files: [...], stats: {total, success, skipped, failed}}
    """
    if base_dir is None:
        base_dir = DATA_DIR

    # 중복 URL 제외
    new_urls = filter_new_urls(urls)

    if not new_urls:
        print('새로운 기사가 없습니다.')
        return {'saved_files': [], 'stats': {'total': 0, 'success': 0, 'skipped': len(urls), 'failed': 0}}

    # 기존 크롤링 기록 로드
    crawled_urls = load_crawled_urls()

    # 포털별로 기사 분류
    portal_articles = {'naver': [], 'daum': [], 'others': []}
    stats = {'total': len(new_urls), 'success': 0, 'skipped': len(urls) - len(new_urls), 'failed': 0, 'no_images': 0}

    for i, url in enumerate(new_urls, 1):
        print(f'[{i}/{len(new_urls)}] 크롤링: {url[:60]}...')

        try:
            data = extract_news_data(url)

            if 'error' in data:
                print(f'    → 실패: {data["error"][:50]}')
                stats['failed'] += 1
            else:
                images = data.get('images', [])
                if not images:
                    # 이미지 없는 기사는 제외
                    print(f'    → 제외 (이미지 없음)')
                    stats['no_images'] += 1
                    crawled_urls.add(url)  # 다음에 다시 크롤링하지 않도록 기록
                else:
                    portal = get_portal_type(url)
                    portal_articles[portal].append(data)
                    crawled_urls.add(url)
                    stats['success'] += 1
                    print(f'    → 성공 (이미지 {len(images)}개)')

        except Exception as e:
            print(f'    → 오류: {e}')
            stats['failed'] += 1

    # 포털별로 50개씩 묶어서 저장
    saved_files = []
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')

    for portal, articles in portal_articles.items():
        if not articles:
            continue

        portal_dir = base_dir / portal
        portal_dir.mkdir(parents=True, exist_ok=True)

        # batch_size개씩 분할
        for batch_idx in range(0, len(articles), batch_size):
            batch = articles[batch_idx:batch_idx + batch_size]
            batch_num = batch_idx // batch_size + 1

            filename = f'{timestamp}_batch{batch_num:03d}_{len(batch)}articles.json'
            file_path = portal_dir / filename

            batch_data = {
                'portal': portal,
                'count': len(batch),
                'crawled_at': datetime.now(timezone.utc).isoformat() + 'Z',
                'articles': batch,
            }

            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(batch_data, f, ensure_ascii=False, indent=2)

            saved_files.append(str(file_path))
            print(f'\n저장: {file_path} ({len(batch)}개 기사)')

    # 크롤링 기록 업데이트
    save_crawled_urls(crawled_urls)

    # 최종 요약
    print(f'\n{"="*50}')
    print(f'크롤링 완료!')
    print(f'  - 전체: {stats["total"]}개')
    print(f'  - 성공: {stats["success"]}개')
    print(f'  - 이미지 없음: {stats["no_images"]}개')
    print(f'  - 중복 스킵: {stats["skipped"]}개')
    print(f'  - 실패: {stats["failed"]}개')
    print(f'  - 저장 파일: {len(saved_files)}개')
    print(f'{"="*50}')

    return {'saved_files': saved_files, 'stats': stats}


def fetch_naver_news_urls(section: str = 'all', count: int = 100) -> list[str]:
    """
    네이버 뉴스에서 최신 기사 URL 목록을 가져옵니다.

    Args:
        section: 뉴스 섹션 (all, politics, economy, society, culture, world, science)
        count: 가져올 기사 수 (최대값은 페이지 구조에 따라 다름)

    Returns:
        list: 뉴스 기사 URL 리스트
    """
    section_map = {
        'all': 'https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1=001',
        'politics': 'https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1=100',
        'economy': 'https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1=101',
        'society': 'https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1=102',
        'culture': 'https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1=103',
        'world': 'https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1=104',
        'science': 'https://news.naver.com/main/list.naver?mode=LSD&mid=sec&sid1=105',
    }

    base_url = section_map.get(section, section_map['all'])
    urls = set()
    page = 1

    print(f'네이버 뉴스 [{section}] 섹션에서 기사 URL 수집 중...')

    while len(urls) < count:
        try:
            page_url = f'{base_url}&page={page}'
            response = requests.get(page_url, headers=HEADERS, timeout=10)
            response.raise_for_status()

            soup = BeautifulSoup(response.text, 'html.parser')

            # 기사 링크 추출
            links = soup.select('ul.type06_headline li a, ul.type06 li a')

            if not links:
                break

            for link in links:
                href = link.get('href', '')
                if 'news.naver.com' in href and '/article/' in href:
                    # n.news.naver.com 형식으로 변환
                    if 'n.news.naver.com' not in href:
                        href = href.replace('news.naver.com', 'n.news.naver.com')
                    urls.add(href)

                if len(urls) >= count:
                    break

            page += 1

            if page > 20:  # 최대 20페이지
                break

        except Exception as e:
            print(f'페이지 {page} 수집 실패: {e}')
            break

    url_list = list(urls)[:count]
    print(f'수집 완료: {len(url_list)}개 URL')
    return url_list


def fetch_and_crawl(section: str = 'all', count: int = 100) -> dict:
    """
    네이버 뉴스에서 최신 기사를 가져와 크롤링하고 저장합니다.

    Args:
        section: 뉴스 섹션 (all, politics, economy, society, culture, world, science)
        count: 가져올 기사 수

    Returns:
        dict: crawl_and_save_batch 결과
    """
    urls = fetch_naver_news_urls(section, count)
    return crawl_and_save_batch(urls)


def fetch_daum_news_urls(section: str = 'all', count: int = 100) -> list[str]:
    """
    다음 뉴스에서 최신 기사 URL 목록을 가져옵니다.

    Args:
        section: 뉴스 섹션 (all, politics, economy, society, culture, world, science)
        count: 가져올 기사 수

    Returns:
        list: 뉴스 기사 URL 리스트
    """
    section_map = {
        'all': 'https://news.daum.net/',
        'politics': 'https://news.daum.net/politics',
        'economy': 'https://news.daum.net/economic',
        'society': 'https://news.daum.net/society',
        'culture': 'https://news.daum.net/culture',
        'world': 'https://news.daum.net/foreign',
        'science': 'https://news.daum.net/digital',
    }

    base_url = section_map.get(section, section_map['all'])
    urls = set()

    print(f'다음 뉴스 [{section}] 섹션에서 기사 URL 수집 중...')

    try:
        response = requests.get(base_url, headers=HEADERS, timeout=10)
        response.raise_for_status()

        soup = BeautifulSoup(response.text, 'html.parser')

        # 다음 뉴스 기사 링크 추출
        links = soup.select('a[href*="v.daum.net/v/"]')

        for link in links:
            href = link.get('href', '')
            if 'v.daum.net/v/' in href:
                urls.add(href)
                if len(urls) >= count:
                    break

    except Exception as e:
        print(f'다음 뉴스 수집 실패: {e}')

    url_list = list(urls)[:count]
    print(f'수집 완료: {len(url_list)}개 URL')
    return url_list


def fetch_all_news_urls(count_per_section: int = 30) -> list[str]:
    """
    네이버 + 다음 뉴스 모든 섹션에서 기사 URL을 수집합니다.

    Args:
        count_per_section: 섹션당 가져올 기사 수

    Returns:
        list: 전체 뉴스 기사 URL 리스트
    """
    sections = ['politics', 'economy', 'society', 'culture', 'world', 'science']
    all_urls = set()

    print('=' * 50)
    print('네이버 + 다음 뉴스 전체 섹션 URL 수집')
    print('=' * 50)

    # 네이버 뉴스
    for section in sections:
        urls = fetch_naver_news_urls(section, count_per_section)
        all_urls.update(urls)

    # 다음 뉴스
    for section in sections:
        urls = fetch_daum_news_urls(section, count_per_section)
        all_urls.update(urls)

    print(f'\n총 수집: {len(all_urls)}개 URL')
    return list(all_urls)


def crawl_all(count_per_section: int = 30) -> dict:
    """
    네이버 + 다음 뉴스 모든 섹션을 크롤링하고 저장합니다.
    이미 크롤링한 기사는 자동으로 제외됩니다.

    Args:
        count_per_section: 섹션당 가져올 기사 수

    Returns:
        dict: crawl_and_save_batch 결과
    """
    urls = fetch_all_news_urls(count_per_section)
    return crawl_and_save_batch(urls)


def is_noise_image(img_url: str) -> bool:
    """노이즈 이미지인지 확인 (광고, 로고, 아이콘 등)"""
    img_url_lower = img_url.lower()
    return any(keyword in img_url_lower for keyword in NOISE_KEYWORDS)


def parse_image_dimensions(img_tag: BeautifulSoup) -> tuple[Optional[int], Optional[int]]:
    """이미지 태그에서 크기 정보 추출"""
    width = img_tag.get('width')
    height = img_tag.get('height')

    # data-* 속성에서도 크기 확인
    if not width:
        width = img_tag.get('data-width')
    if not height:
        height = img_tag.get('data-height')

    # style 속성에서 크기 추출
    style = img_tag.get('style', '')
    if not width:
        width_match = re.search(r'width:\s*(\d+)', style)
        if width_match:
            width = width_match.group(1)
    if not height:
        height_match = re.search(r'height:\s*(\d+)', style)
        if height_match:
            height = height_match.group(1)

    try:
        width = int(width) if width else None
    except (ValueError, TypeError):
        width = None

    try:
        height = int(height) if height else None
    except (ValueError, TypeError):
        height = None

    return width, height


def is_valid_image_size(img_tag: BeautifulSoup) -> bool:
    """이미지 크기가 유효한지 확인 (너무 작은 이미지 제외)"""
    width, height = parse_image_dimensions(img_tag)

    # 크기 정보가 있고 너무 작으면 제외
    if width is not None and width < MIN_IMAGE_WIDTH:
        return False
    if height is not None and height < MIN_IMAGE_HEIGHT:
        return False

    return True


def get_image_url(img_tag: BeautifulSoup, base_url: str) -> Optional[str]:
    """이미지 태그에서 실제 이미지 URL 추출"""
    # 우선순위: data-src > data-original > src
    img_url = (
        img_tag.get('data-src') or
        img_tag.get('data-original') or
        img_tag.get('data-lazy-src') or
        img_tag.get('src')
    )

    if not img_url:
        return None

    # base64 인코딩된 placeholder 제외
    if img_url.startswith('data:'):
        return None

    # 상대 경로를 절대 경로로 변환
    if not img_url.startswith(('http://', 'https://')):
        img_url = urljoin(base_url, img_url)

    return img_url


def extract_naver_news(url: str, soup: BeautifulSoup) -> dict:
    """네이버 뉴스 전용 파싱"""
    result = {
        'title': None,
        'content': None,
        'images': [],
        'published_at': None,
        'provider': None,
        'journalist': None,
        'source_url': url,
    }

    # 제목 추출
    title_tag = soup.select_one('h2#title_area span') or soup.select_one('.media_end_head_headline')
    if title_tag:
        result['title'] = title_tag.get_text(strip=True)

    # 언론사명 추출
    provider_tag = soup.select_one('.media_end_head_top_logo img') or soup.select_one('a.media_end_head_top_logo')
    if provider_tag:
        result['provider'] = provider_tag.get('alt') or provider_tag.get('title', '').strip()

    # 기자명 추출
    journalist_tag = (
        soup.select_one('.media_end_head_journalist_name') or
        soup.select_one('em.media_end_head_journalist_name') or
        soup.select_one('.byline_s') or
        soup.select_one('.journalist_name')
    )
    if journalist_tag:
        journalist_name = journalist_tag.get_text(strip=True)
        # 이메일, 괄호, "기자" 접미사 정리
        journalist_name = re.sub(r'\([^)]*\)', '', journalist_name)  # 괄호 내용 제거
        journalist_name = re.sub(r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}', '', journalist_name)  # 이메일 제거
        journalist_name = re.sub(r'\s*기자.*$', '', journalist_name)  # "기자" 및 이후 내용 제거
        journalist_name = journalist_name.strip()
        if journalist_name:
            result['journalist'] = journalist_name

    # 작성 시간 추출
    time_tag = soup.select_one('.media_end_head_info_datestamp_time')
    if time_tag:
        raw_time = time_tag.get('data-date-time') or time_tag.get_text(strip=True)
        result['published_at'] = raw_time

    # 본문 영역 추출 (#dic_area가 네이버 뉴스 본문 컨테이너)
    content_area = soup.select_one('#dic_area') or soup.select_one('#newsct_article')

    if content_area:
        # 본문 텍스트 추출
        result['content'] = content_area.get_text(separator=' ', strip=True)

        # 본문 내 이미지 추출
        for img in content_area.find_all('img'):
            img_url = get_image_url(img, url)

            if not img_url:
                continue

            # 노이즈 이미지 필터링
            if is_noise_image(img_url):
                continue

            # 크기 필터링
            if not is_valid_image_size(img):
                continue

            # 중복 제거
            if img_url not in result['images']:
                result['images'].append(img_url)

    return result


def extract_daum_news(url: str, soup: BeautifulSoup) -> dict:
    """다음 뉴스 전용 파싱"""
    result = {
        'title': None,
        'content': None,
        'images': [],
        'published_at': None,
        'provider': None,
        'journalist': None,
        'source_url': url,
    }

    # 제목 추출
    title_tag = soup.select_one('h3.tit_view') or soup.select_one('.head_view .tit_g')
    if title_tag:
        result['title'] = title_tag.get_text(strip=True)

    # 언론사명 추출 - og:article:author 메타태그 또는 link_press
    provider_meta = soup.select_one('meta[property="og:article:author"]')
    if provider_meta:
        result['provider'] = provider_meta.get('content')
    else:
        provider_tag = soup.select_one('.info_view .link_press') or soup.select_one('.thumb_g .link_thumb')
        if provider_tag:
            provider_name = provider_tag.get('title') or provider_tag.get_text(strip=True)
            result['provider'] = provider_name

    # 기자명 추출 - .txt_info 중 날짜가 아닌 것
    txt_info_tags = soup.select('.info_view > .txt_info')
    for tag in txt_info_tags:
        # 날짜 태그가 아닌 경우 기자명으로 처리
        if not tag.select_one('.num_date'):
            journalist_name = tag.get_text(strip=True)
            # 이메일, 괄호, "기자" 접미사 정리
            journalist_name = re.sub(r'\([^)]*\)', '', journalist_name)  # 괄호 내용 제거
            journalist_name = re.sub(r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}', '', journalist_name)  # 이메일 제거
            journalist_name = re.sub(r'\s*기자.*$', '', journalist_name)  # "기자" 및 이후 내용 제거
            journalist_name = journalist_name.strip()
            if journalist_name:
                result['journalist'] = journalist_name
                break

    # 작성 시간 추출
    time_tag = soup.select_one('.info_view .num_date') or soup.select_one('.head_view .txt_info:last-child')
    if time_tag:
        result['published_at'] = time_tag.get_text(strip=True)

    # 본문 영역 추출
    content_area = soup.select_one('#harmonyContainer') or soup.select_one('.news_view')

    if content_area:
        # 본문 텍스트 추출
        result['content'] = content_area.get_text(separator=' ', strip=True)

        # 본문 내 이미지 추출
        for img in content_area.find_all('img'):
            img_url = get_image_url(img, url)

            if not img_url:
                continue

            if is_noise_image(img_url):
                continue

            if not is_valid_image_size(img):
                continue

            if img_url not in result['images']:
                result['images'].append(img_url)

    return result


def extract_with_newspaper3k(url: str) -> dict:
    """Newspaper3k를 사용한 일반 뉴스 사이트 파싱 (fallback)"""
    result = {
        'title': None,
        'content': None,
        'images': [],
        'published_at': None,
        'provider': None,
        'journalist': None,
        'source_url': url,
    }

    article = Article(url, language='ko')
    article.download()
    article.parse()

    result['title'] = article.title
    result['content'] = article.text
    result['provider'] = article.source_url or urlparse(url).netloc

    # 기자명 추출 (newspaper3k의 authors 사용)
    if article.authors:
        cleaned_authors = []
        for author in article.authors:
            # 이메일, 괄호, "기자" 접미사 정리
            author = re.sub(r'\([^)]*\)', '', author)
            author = re.sub(r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}', '', author)
            author = re.sub(r'\s*기자.*$', '', author)
            author = author.strip()
            if author:
                cleaned_authors.append(author)
        if cleaned_authors:
            result['journalist'] = ', '.join(cleaned_authors)

    if article.publish_date:
        result['published_at'] = article.publish_date.isoformat()

    # Newspaper3k가 추출한 이미지 필터링
    for img_url in article.images:
        if is_noise_image(img_url):
            continue
        if img_url not in result['images']:
            result['images'].append(img_url)

    # top_image도 추가 (중복 체크)
    if article.top_image and article.top_image not in result['images']:
        if not is_noise_image(article.top_image):
            result['images'].insert(0, article.top_image)

    return result


def extract_news_data(url: str) -> dict:
    """
    뉴스 URL에서 메타데이터와 본문 이미지를 추출합니다.

    Args:
        url: 뉴스 기사 URL

    Returns:
        dict: AWS SQS에 전송할 JSON 형태의 딕셔너리
            - title: 기사 제목
            - content: 본문 텍스트
            - images: 본문 이미지 URL 리스트 (노이즈 제외)
            - published_at: 작성 시간
            - provider: 언론사명
            - source_url: 원본 URL
            - extracted_at: 추출 시간 (ISO format)
    """
    try:
        # HTTP 요청
        response = requests.get(url, headers=HEADERS, timeout=15)
        response.raise_for_status()
        response.encoding = response.apparent_encoding or 'utf-8'

        soup = BeautifulSoup(response.text, 'html.parser')

        # 플랫폼별 파싱
        if is_naver_news(url):
            result = extract_naver_news(url, soup)
        elif is_daum_news(url):
            result = extract_daum_news(url, soup)
        else:
            # 일반 뉴스 사이트는 Newspaper3k fallback 사용
            result = extract_with_newspaper3k(url)

        # 네이버/다음에서 추출 실패 시 Newspaper3k fallback
        if not result.get('title') or not result.get('content'):
            fallback_result = extract_with_newspaper3k(url)

            if not result.get('title'):
                result['title'] = fallback_result['title']
            if not result.get('content'):
                result['content'] = fallback_result['content']
            if not result.get('provider'):
                result['provider'] = fallback_result['provider']
            if not result.get('published_at'):
                result['published_at'] = fallback_result['published_at']
            if not result.get('images'):
                result['images'] = fallback_result['images']

        # 추출 시간 추가
        result['extracted_at'] = datetime.now(timezone.utc).isoformat() + 'Z'

        return result

    except requests.RequestException as e:
        return {
            'error': f'HTTP 요청 실패: {str(e)}',
            'source_url': url,
            'extracted_at': datetime.now(timezone.utc).isoformat() + 'Z',
        }
    except Exception as e:
        return {
            'error': f'파싱 실패: {str(e)}',
            'source_url': url,
            'extracted_at': datetime.now(timezone.utc).isoformat() + 'Z',
        }


if __name__ == '__main__':
    print('=' * 60)
    print('DeepScan News - 네이버 + 다음 뉴스 크롤러')
    print('=' * 60)

    # 모든 섹션에서 섹션당 30개씩 크롤링
    result = crawl_all(count_per_section=30)
