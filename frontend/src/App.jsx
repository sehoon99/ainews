import { useState, useEffect } from 'react'
import BubbleChart from './components/BubbleChart'

const API_URL = import.meta.env.VITE_API_URL || ''

const TABS = {
  MAIN: 'main',
  QUERY: 'query',
}

const KEYWORDS_SQL = `
SELECT keyword, COUNT(*) as total_count,
  SUM(CASE WHEN portal = 'naver' THEN 1 ELSE 0 END) as naver_count,
  SUM(CASE WHEN portal = 'daum' THEN 1 ELSE 0 END) as daum_count
FROM (
  SELECT TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(a.keywords, ',', n.n), ',', -1)) as keyword, a.portal
  FROM articles a
  CROSS JOIN (SELECT 1 as n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) n
  WHERE a.keywords IS NOT NULL
  AND n.n <= 1 + LENGTH(a.keywords) - LENGTH(REPLACE(a.keywords, ',', ''))
) t
WHERE keyword != ''
GROUP BY keyword
ORDER BY total_count DESC
LIMIT 50`

const runQuery = async (sql) => {
  const res = await fetch(`${API_URL}/api/query`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sql }),
  })
  return res.json()
}

export default function App() {
  const [tab, setTab] = useState(TABS.MAIN)
  const [keywords, setKeywords] = useState([])
  const [keywordsLoading, setKeywordsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchFocused, setSearchFocused] = useState(false)
  const [searchResult, setSearchResult] = useState(null)
  const [searchLoading, setSearchLoading] = useState(false)
  const [sql, setSql] = useState('SELECT * FROM articles LIMIT 10;')
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  // 페이지 로드 시 DB에서 키워드 데이터 가져오기
  useEffect(() => {
    const fetchKeywords = async () => {
      try {
        const data = await runQuery(KEYWORDS_SQL)
        if (data.rows && data.rows.length > 0) {
          const mapped = data.rows.map(r => ({
            keyword: r.keyword,
            total_count: Number(r.total_count),
            naver_count: Number(r.naver_count),
            daum_count: Number(r.daum_count),
            ratio: Number(r.naver_count) / Math.max(Number(r.total_count), 1),
          }))
          setKeywords(mapped)
        }
      } catch (e) {
        console.error('키워드 로드 실패:', e)
      } finally {
        setKeywordsLoading(false)
      }
    }
    fetchKeywords()
  }, [])

  // 키워드 검색
  const searchKeyword = async (query) => {
    if (!query.trim()) return
    setSearchLoading(true)
    setSearchResult(null)
    try {
      const searchSql = `SELECT portal, COUNT(*) as cnt FROM articles WHERE keywords LIKE '%${query.trim()}%' GROUP BY portal`
      const data = await runQuery(searchSql)
      if (data.rows) {
        let naver = 0, daum = 0
        data.rows.forEach(r => {
          if (r.portal === 'naver') naver = Number(r.cnt)
          if (r.portal === 'daum') daum = Number(r.cnt)
        })
        const total = naver + daum
        setSearchResult({ keyword: query.trim(), total, naver, daum })
      }
    } catch (e) {
      console.error('검색 실패:', e)
    } finally {
      setSearchLoading(false)
    }
  }

  const handleBubbleClick = (keyword) => {
    setSearchQuery(keyword)
    searchKeyword(keyword)
  }

  const executeQuery = async () => {
    if (!sql.trim()) return
    setLoading(true)
    setError(null)
    setResult(null)

    try {
      const data = await runQuery(sql.trim())
      if (data.error) {
        setError(data.error)
      } else {
        setResult(data)
      }
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      executeQuery()
    }
  }

  return (
    <div style={styles.root}>
      {/* 헤더 */}
      <header style={styles.header}>
        <h1 style={styles.logo}>AI News Tracker</h1>
        <nav style={styles.nav}>
          <button
            style={tab === TABS.MAIN ? styles.tabActive : styles.tab}
            onClick={() => setTab(TABS.MAIN)}
          >
            키워드 시각화
          </button>
          <button
            style={tab === TABS.QUERY ? styles.tabActive : styles.tab}
            onClick={() => setTab(TABS.QUERY)}
          >
            DB Query
          </button>
        </nav>
      </header>

      {/* 메인: 버블 차트 */}
      {tab === TABS.MAIN && (
        <div style={styles.mainContent}>
          <div style={styles.pageHeader}>
            <h2 style={styles.pageTitle}>뉴스 키워드 버블 차트</h2>
            <p style={styles.pageDesc}>
              원의 크기는 전체 뉴스 수, 색상 농도는 네이버 뉴스 비율을 나타냅니다. 버블을 클릭하면 검색됩니다.
            </p>
          </div>

          {/* 검색창 */}
          <div style={styles.searchWrapper}>
            <div style={{
              ...styles.searchBox,
              borderColor: searchFocused ? '#3b82f6' : '#3f3f46',
              boxShadow: searchFocused ? '0 0 0 3px rgba(59,130,246,0.15)' : 'none',
            }}>
              <svg style={styles.searchIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                style={styles.searchInput}
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => setSearchFocused(true)}
                onBlur={() => setSearchFocused(false)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && searchQuery.trim()) {
                    searchKeyword(searchQuery)
                  }
                }}
                placeholder="뉴스 키워드를 검색하세요..."
              />
              {searchQuery && (
                <button
                  style={styles.searchClear}
                  onClick={() => { setSearchQuery(''); setSearchResult(null) }}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              )}
            </div>
          </div>

          {/* 검색 결과 */}
          {searchLoading && <div style={styles.searchLoading}>검색 중...</div>}
          {searchResult && (
            <div style={styles.searchResultCard}>
              <div style={styles.searchResultHeader}>
                <span style={styles.searchResultKeyword}>{searchResult.keyword}</span>
                <span style={styles.searchResultTotal}>총 {searchResult.total}건</span>
              </div>
              <div style={styles.portalRow}>
                <div style={styles.portalBadgeNaver}>네이버</div>
                <span style={styles.portalCount}>{searchResult.naver}건</span>
                <div style={styles.progressBar}>
                  <div style={{ ...styles.progressFill, width: `${searchResult.total ? (searchResult.naver / searchResult.total * 100) : 0}%`, backgroundColor: '#22c55e' }} />
                </div>
                <span style={styles.portalPercent}>{searchResult.total ? (searchResult.naver / searchResult.total * 100).toFixed(1) : 0}%</span>
              </div>
              <div style={styles.portalRow}>
                <div style={styles.portalBadgeDaum}>다음</div>
                <span style={styles.portalCount}>{searchResult.daum}건</span>
                <div style={styles.progressBar}>
                  <div style={{ ...styles.progressFill, width: `${searchResult.total ? (searchResult.daum / searchResult.total * 100) : 0}%`, backgroundColor: '#3b82f6' }} />
                </div>
                <span style={styles.portalPercent}>{searchResult.total ? (searchResult.daum / searchResult.total * 100).toFixed(1) : 0}%</span>
              </div>
            </div>
          )}

          {keywordsLoading ? (
            <div style={{ textAlign: 'center', padding: 60, color: '#71717a' }}>키워드 데이터 로딩 중...</div>
          ) : keywords.length > 0 ? (
            <BubbleChart data={keywords} onBubbleClick={handleBubbleClick} />
          ) : (
            <div style={{ textAlign: 'center', padding: 60, color: '#71717a' }}>키워드 데이터가 없습니다</div>
          )}
        </div>
      )}

      {/* DB Query */}
      {tab === TABS.QUERY && (
        <div style={styles.queryContent}>
          <textarea
            style={styles.textarea}
            value={sql}
            onChange={(e) => setSql(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="SQL 쿼리를 입력하세요..."
            rows={6}
          />

          <div style={styles.toolbar}>
            <button
              style={{ ...styles.button, opacity: loading ? 0.6 : 1 }}
              onClick={executeQuery}
              disabled={loading}
            >
              {loading ? '실행 중...' : '실행 (Ctrl+Enter)'}
            </button>
            {result && (
              <span style={styles.meta}>
                {result.rowCount}건 | {result.executionTime}ms
                {result.message && ` | ${result.message}`}
              </span>
            )}
          </div>

          {error && <pre style={styles.error}>{error}</pre>}

          {result && result.columns && result.columns.length > 0 && (
            <div style={styles.tableWrap}>
              <table style={styles.table}>
                <thead>
                  <tr>
                    {result.columns.map((col) => (
                      <th key={col} style={styles.th}>{col}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {result.rows.map((row, i) => (
                    <tr key={i} style={i % 2 === 0 ? styles.evenRow : {}}>
                      {result.columns.map((col) => (
                        <td key={col} style={styles.td}>
                          {row[col] === null ? <span style={styles.null}>NULL</span> : String(row[col])}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

const styles = {
  root: {
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    color: '#e4e4e7',
    backgroundColor: '#0c1222',
    minHeight: '100vh',
  },
  // 헤더
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '12px 24px',
    backgroundColor: '#181800',
    borderBottom: '1px solid #1e2d4a',
  },
  logo: {
    fontSize: 18,
    fontWeight: 700,
    color: '#f4f4f5',
    margin: 0,
  },
  nav: {
    display: 'flex',
    gap: 4,
  },
  tab: {
    padding: '6px 16px',
    fontSize: 13,
    fontWeight: 500,
    color: '#a1a1aa',
    backgroundColor: 'transparent',
    border: '1px solid transparent',
    borderRadius: 6,
    cursor: 'pointer',
  },
  tabActive: {
    padding: '6px 16px',
    fontSize: 13,
    fontWeight: 500,
    color: '#f4f4f5',
    backgroundColor: '#1e2d4a',
    border: '1px solid #2a3f5f',
    borderRadius: 6,
    cursor: 'pointer',
  },
  // 메인 페이지
  mainContent: {
    padding: '24px',
  },
  pageHeader: {
    marginBottom: 16,
  },
  pageTitle: {
    fontSize: 22,
    fontWeight: 700,
    color: '#f4f4f5',
    margin: '0 0 6px 0',
  },
  pageDesc: {
    fontSize: 14,
    color: '#71717a',
    margin: 0,
  },
  // 검색창
  searchWrapper: {
    marginBottom: 20,
    maxWidth: 560,
  },
  searchBox: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '10px 16px',
    backgroundColor: '#111827',
    border: '1px solid #1e2d4a',
    borderRadius: 12,
    transition: 'border-color 0.2s, box-shadow 0.2s',
  },
  searchIcon: {
    width: 18,
    height: 18,
    color: '#71717a',
    flexShrink: 0,
  },
  searchInput: {
    flex: 1,
    fontSize: 15,
    fontFamily: 'inherit',
    color: '#f4f4f5',
    backgroundColor: 'transparent',
    border: 'none',
    outline: 'none',
  },
  searchClear: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: 24,
    height: 24,
    padding: 0,
    backgroundColor: '#1e2d4a',
    border: 'none',
    borderRadius: 6,
    color: '#94a3b8',
    cursor: 'pointer',
    flexShrink: 0,
  },
  // 검색 결과
  searchLoading: {
    padding: '12px 0',
    fontSize: 14,
    color: '#a1a1aa',
  },
  searchResultCard: {
    marginBottom: 20,
    padding: '16px 20px',
    backgroundColor: '#111827',
    border: '1px solid #1e2d4a',
    borderRadius: 12,
    maxWidth: 560,
  },
  searchResultHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
    paddingBottom: 10,
    borderBottom: '1px solid #1e2d4a',
  },
  searchResultKeyword: {
    fontSize: 16,
    fontWeight: 700,
    color: '#f4f4f5',
  },
  searchResultTotal: {
    fontSize: 14,
    fontWeight: 600,
    color: '#a1a1aa',
  },
  portalRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '6px 0',
  },
  portalBadgeNaver: {
    fontSize: 11,
    fontWeight: 600,
    padding: '2px 8px',
    borderRadius: 4,
    backgroundColor: '#166534',
    color: '#86efac',
    minWidth: 42,
    textAlign: 'center',
  },
  portalBadgeDaum: {
    fontSize: 11,
    fontWeight: 600,
    padding: '2px 8px',
    borderRadius: 4,
    backgroundColor: '#1e3a5f',
    color: '#93c5fd',
    minWidth: 42,
    textAlign: 'center',
  },
  portalCount: {
    fontSize: 13,
    fontWeight: 600,
    color: '#e4e4e7',
    minWidth: 40,
    textAlign: 'right',
  },
  progressBar: {
    flex: 1,
    height: 6,
    backgroundColor: '#1e2d4a',
    borderRadius: 3,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    borderRadius: 3,
    transition: 'width 0.3s',
  },
  portalPercent: {
    fontSize: 12,
    color: '#a1a1aa',
    minWidth: 44,
    textAlign: 'right',
  },
  // 쿼리 페이지
  queryContent: {
    maxWidth: 1200,
    margin: '0 auto',
    padding: '24px',
  },
  textarea: {
    width: '100%',
    padding: '12px',
    fontSize: 14,
    fontFamily: '"SF Mono", "Fira Code", "Fira Mono", Menlo, monospace',
    backgroundColor: '#111827',
    color: '#e4e4e7',
    border: '1px solid #1e2d4a',
    borderRadius: 8,
    resize: 'vertical',
    outline: 'none',
    boxSizing: 'border-box',
  },
  toolbar: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginTop: 12,
    marginBottom: 16,
  },
  button: {
    padding: '8px 20px',
    fontSize: 14,
    fontWeight: 500,
    backgroundColor: '#3b82f6',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
  },
  meta: {
    fontSize: 13,
    color: '#a1a1aa',
  },
  error: {
    padding: 12,
    backgroundColor: '#450a0a',
    color: '#fca5a5',
    borderRadius: 8,
    fontSize: 13,
    fontFamily: 'monospace',
    whiteSpace: 'pre-wrap',
    overflowX: 'auto',
  },
  tableWrap: {
    overflowX: 'auto',
    borderRadius: 8,
    border: '1px solid #1e2d4a',
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    fontSize: 13,
    fontFamily: '"SF Mono", "Fira Code", monospace',
  },
  th: {
    textAlign: 'left',
    padding: '8px 12px',
    backgroundColor: '#111827',
    color: '#94a3b8',
    fontWeight: 600,
    borderBottom: '1px solid #1e2d4a',
    whiteSpace: 'nowrap',
  },
  td: {
    padding: '6px 12px',
    borderBottom: '1px solid #152035',
    maxWidth: 300,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  evenRow: {
    backgroundColor: '#0f1a2e',
  },
  null: {
    color: '#71717a',
    fontStyle: 'italic',
  },
}