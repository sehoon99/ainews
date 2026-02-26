import { useState } from 'react'

const API_URL = import.meta.env.VITE_API_URL || ''

export default function EmailSubscription() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!email.trim()) return

    setLoading(true)
    setMessage(null)
    setError(null)

    try {
      const res = await fetch(`${API_URL}/api/subscribers`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim() }),
      })

      if (res.ok) {
        setMessage('구독 신청이 완료되었습니다! 매일 오전 9시에 키워드 변동 리포트를 받아보실 수 있습니다.')
        setEmail('')
      } else {
        const data = await res.json().catch(() => null)
        setError(data?.message || '구독 신청에 실패했습니다. 다시 시도해주세요.')
      }
    } catch (e) {
      setError('네트워크 오류가 발생했습니다. 다시 시도해주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <div style={styles.iconWrap}>
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="4" width="20" height="16" rx="2" />
            <path d="M22 4L12 13L2 4" />
          </svg>
        </div>

        <h2 style={styles.title}>이메일 구독</h2>
        <p style={styles.desc}>
          매일 오전 9시 키워드 변동 리포트를 받아보세요.<br />
          신규 진입, 순위 상승/하락, 퇴출 키워드를 한눈에 확인할 수 있습니다.
        </p>

        <form onSubmit={handleSubmit} style={styles.form}>
          <div style={styles.inputWrap}>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="이메일 주소를 입력하세요"
              style={styles.input}
              required
            />
            <button
              type="submit"
              style={{ ...styles.button, opacity: loading ? 0.6 : 1 }}
              disabled={loading}
            >
              {loading ? '처리 중...' : '구독하기'}
            </button>
          </div>
        </form>

        {message && <div style={styles.success}>{message}</div>}
        {error && <div style={styles.error}>{error}</div>}

        <div style={styles.info}>
          <h3 style={styles.infoTitle}>리포트 내용</h3>
          <ul style={styles.infoList}>
            <li style={styles.infoItem}>
              <span style={{ ...styles.badge, backgroundColor: '#22c55e22', color: '#22c55e' }}>NEW</span>
              신규 진입 키워드
            </li>
            <li style={styles.infoItem}>
              <span style={{ ...styles.badge, backgroundColor: '#3b82f622', color: '#3b82f6' }}>UP</span>
              순위 상승 키워드
            </li>
            <li style={styles.infoItem}>
              <span style={{ ...styles.badge, backgroundColor: '#f59e0b22', color: '#f59e0b' }}>DOWN</span>
              순위 하락 키워드
            </li>
            <li style={styles.infoItem}>
              <span style={{ ...styles.badge, backgroundColor: '#ef444422', color: '#ef4444' }}>OUT</span>
              퇴출 키워드
            </li>
          </ul>
        </div>

        <p style={styles.footerNote}>
          구독 해제는 수신된 이메일 하단의 링크를 통해 언제든 가능합니다.
        </p>
      </div>
    </div>
  )
}

const styles = {
  container: {
    display: 'flex',
    justifyContent: 'center',
    padding: '40px 24px',
  },
  card: {
    maxWidth: 480,
    width: '100%',
    backgroundColor: '#111827',
    border: '1px solid #1e2d4a',
    borderRadius: 16,
    padding: '40px 32px',
    textAlign: 'center',
  },
  iconWrap: {
    marginBottom: 20,
  },
  title: {
    fontSize: 22,
    fontWeight: 700,
    color: '#f4f4f5',
    margin: '0 0 8px 0',
  },
  desc: {
    fontSize: 14,
    color: '#a1a1aa',
    lineHeight: 1.6,
    margin: '0 0 24px 0',
  },
  form: {
    marginBottom: 16,
  },
  inputWrap: {
    display: 'flex',
    gap: 8,
  },
  input: {
    flex: 1,
    padding: '10px 16px',
    fontSize: 14,
    fontFamily: 'inherit',
    color: '#f4f4f5',
    backgroundColor: '#0c1222',
    border: '1px solid #1e2d4a',
    borderRadius: 8,
    outline: 'none',
  },
  button: {
    padding: '10px 24px',
    fontSize: 14,
    fontWeight: 600,
    color: '#fff',
    backgroundColor: '#3b82f6',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  success: {
    padding: '12px 16px',
    backgroundColor: '#16653422',
    border: '1px solid #166534',
    borderRadius: 8,
    color: '#86efac',
    fontSize: 13,
    marginBottom: 16,
  },
  error: {
    padding: '12px 16px',
    backgroundColor: '#450a0a22',
    border: '1px solid #450a0a',
    borderRadius: 8,
    color: '#fca5a5',
    fontSize: 13,
    marginBottom: 16,
  },
  info: {
    textAlign: 'left',
    padding: '20px',
    backgroundColor: '#0c1222',
    border: '1px solid #1e2d4a',
    borderRadius: 12,
    marginTop: 24,
    marginBottom: 16,
  },
  infoTitle: {
    fontSize: 14,
    fontWeight: 600,
    color: '#f4f4f5',
    margin: '0 0 12px 0',
  },
  infoList: {
    listStyle: 'none',
    padding: 0,
    margin: 0,
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
  },
  infoItem: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    fontSize: 13,
    color: '#a1a1aa',
  },
  badge: {
    fontSize: 11,
    fontWeight: 700,
    padding: '2px 8px',
    borderRadius: 4,
    minWidth: 36,
    textAlign: 'center',
  },
  footerNote: {
    fontSize: 12,
    color: '#64748b',
    margin: 0,
  },
}