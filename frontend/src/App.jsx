import { useState } from 'react'

const API_URL = import.meta.env.VITE_API_URL || ''

export default function App() {
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
    <div style={styles.container}>
      <h1 style={styles.title}>AI News DB Query</h1>

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
  )
}

const styles = {
  container: {
    maxWidth: 1200,
    margin: '0 auto',
    padding: '24px',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    color: '#e4e4e7',
    backgroundColor: '#18181b',
    minHeight: '100vh',
  },
  title: {
    fontSize: 20,
    fontWeight: 600,
    marginBottom: 16,
    color: '#f4f4f5',
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