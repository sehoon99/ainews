import { useState } from 'react'
import BubbleChart from './components/BubbleChart'
import mockKeywords from './data/mockKeywords'

const API_URL = import.meta.env.VITE_API_URL || ''

const TABS = {
  MAIN: 'main',
  QUERY: 'query',
}

export default function App() {
  const [tab, setTab] = useState(TABS.MAIN)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchFocused, setSearchFocused] = useState(false)
  const [sql, setSql] = useState('SELECT * FROM articles LIMIT 10;')
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const executeQuery = async () => {
    if (!sql.trim()) return
    setLoading(true)
    setError(null)
    setResult(null)

    try {
      const res = await fetch(`${API_URL}/api/query`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sql: sql.trim() }),
      })
      const data = await res.json()

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
              원의 크기는 전체 뉴스 수, 색상 농도는 AI 뉴스 비율을 나타냅니다. 마우스를 올려 상세 정보를 확인하세요.
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
                    // TODO: OpenSearch 연동
                    console.log('search:', searchQuery)
                  }
                }}
                placeholder="뉴스 키워드를 검색하세요..."
              />
              {searchQuery && (
                <button
                  style={styles.searchClear}
                  onClick={() => setSearchQuery('')}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              )}
            </div>
          </div>

          <BubbleChart data={mockKeywords} />
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
    backgroundColor: '#09090b',
    minHeight: '100vh',
  },
  // 헤더
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '12px 24px',
    backgroundColor: '#18181b',
    borderBottom: '1px solid #27272a',
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
    backgroundColor: '#27272a',
    border: '1px solid #3f3f46',
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
    backgroundColor: '#18181b',
    border: '1px solid #3f3f46',
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
    backgroundColor: '#27272a',
    border: 'none',
    borderRadius: 6,
    color: '#a1a1aa',
    cursor: 'pointer',
    flexShrink: 0,
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
    backgroundColor: '#27272a',
    color: '#e4e4e7',
    border: '1px solid #3f3f46',
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
    border: '1px solid #3f3f46',
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
    backgroundColor: '#27272a',
    color: '#a1a1aa',
    fontWeight: 600,
    borderBottom: '1px solid #3f3f46',
    whiteSpace: 'nowrap',
  },
  td: {
    padding: '6px 12px',
    borderBottom: '1px solid #27272a',
    maxWidth: 300,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  evenRow: {
    backgroundColor: '#1f1f23',
  },
  null: {
    color: '#71717a',
    fontStyle: 'italic',
  },
}