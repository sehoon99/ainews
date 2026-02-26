import { useState, useEffect } from 'react'

const API_URL = import.meta.env.VITE_API_URL || ''

export default function WeeklyRankPanel({ refreshTrigger, onKeywordClick }) {
  const [ranks, setRanks] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchRanks = async () => {
      try {
        const res = await fetch(`${API_URL}/api/keyword-ranks/top`)
        if (!res.ok) {
          setRanks([])
          return
        }
        const data = await res.json()
        setRanks(Array.isArray(data) ? data : [])
      } catch (e) {
        console.error('인기 검색어 로드 실패:', e)
        setRanks([])
      } finally {
        setLoading(false)
      }
    }
    fetchRanks()
  }, [refreshTrigger])

  const maxCount = ranks.length > 0 ? ranks[0].searchCount : 1

  return (
    <div style={styles.panel}>
      <div style={styles.header}>
        <h3 style={styles.title}>주간 인기 검색어</h3>
        <span style={styles.badge}>TOP 10</span>
      </div>

      {loading ? (
        <div style={styles.loading}>로딩 중...</div>
      ) : ranks.length === 0 ? (
        <div style={styles.empty}>아직 검색 기록이 없습니다</div>
      ) : (
        <div style={styles.list}>
          {ranks.map((item, idx) => {
            const rank = idx + 1
            const isTop3 = rank <= 3
            const barWidth = `${(item.searchCount / maxCount) * 100}%`

            return (
              <div
                key={item.id}
                style={styles.item}
                onClick={() => onKeywordClick && onKeywordClick(item.keyword)}
              >
                <span style={{
                  ...styles.rank,
                  color: isTop3 ? '#f59e0b' : '#64748b',
                  fontWeight: isTop3 ? 700 : 500,
                }}>
                  {rank}
                </span>
                <div style={styles.barContainer}>
                  <div style={{
                    ...styles.bar,
                    width: barWidth,
                    backgroundColor: isTop3 ? '#3b82f6' : '#1e3a5f',
                  }} />
                  <span style={styles.keyword}>{item.keyword}</span>
                </div>
                <span style={styles.count}>{item.searchCount}</span>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

const styles = {
  panel: {
    backgroundColor: '#111827',
    border: '1px solid #1e2d4a',
    borderRadius: 12,
    padding: '20px 16px',
    maxHeight: 'calc(100vh - 240px)',
    overflowY: 'auto',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 16,
    paddingBottom: 12,
    borderBottom: '1px solid #1e2d4a',
  },
  title: {
    fontSize: 15,
    fontWeight: 700,
    color: '#f4f4f5',
    margin: 0,
  },
  badge: {
    fontSize: 11,
    fontWeight: 600,
    padding: '2px 8px',
    borderRadius: 4,
    backgroundColor: '#1e3a5f',
    color: '#93c5fd',
  },
  loading: {
    textAlign: 'center',
    padding: '24px 0',
    fontSize: 13,
    color: '#71717a',
  },
  empty: {
    textAlign: 'center',
    padding: '24px 0',
    fontSize: 13,
    color: '#71717a',
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
  },
  item: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '8px 6px',
    borderRadius: 8,
    cursor: 'pointer',
    transition: 'background-color 0.15s',
  },
  rank: {
    fontSize: 14,
    minWidth: 20,
    textAlign: 'center',
  },
  barContainer: {
    flex: 1,
    position: 'relative',
    height: 28,
    backgroundColor: '#0c1222',
    borderRadius: 6,
    overflow: 'hidden',
  },
  bar: {
    position: 'absolute',
    top: 0,
    left: 0,
    height: '100%',
    borderRadius: 6,
    opacity: 0.3,
    transition: 'width 0.3s',
  },
  keyword: {
    position: 'relative',
    zIndex: 1,
    lineHeight: '28px',
    paddingLeft: 10,
    fontSize: 13,
    fontWeight: 500,
    color: '#e4e4e7',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  count: {
    fontSize: 12,
    fontWeight: 600,
    color: '#94a3b8',
    minWidth: 28,
    textAlign: 'right',
  },
}