import { useEffect, useRef, useState } from 'react'
import * as d3 from 'd3'

export default function BubbleChart({ data, width = 900, height = 700 }) {
  const svgRef = useRef(null)
  const containerRef = useRef(null)
  const [tooltip, setTooltip] = useState({ show: false, x: 0, y: 0, data: null })
  const [dimensions, setDimensions] = useState({ width, height })

  // 반응형: 컨테이너 크기에 맞춤
  useEffect(() => {
    const observe = () => {
      if (!containerRef.current) return
      const rect = containerRef.current.getBoundingClientRect()
      const w = Math.max(rect.width, 320)
      const h = Math.max(window.innerHeight - 160, 400)
      setDimensions({ width: w, height: h })
    }
    observe()
    window.addEventListener('resize', observe)
    return () => window.removeEventListener('resize', observe)
  }, [])

  useEffect(() => {
    if (!data || data.length === 0 || !svgRef.current) return

    const { width: w, height: h } = dimensions
    const svg = d3.select(svgRef.current)
    svg.selectAll('*').remove()

    // 스케일 설정
    const maxCount = d3.max(data, d => d.total_count)
    const minCount = d3.min(data, d => d.total_count)
    const radiusScale = d3.scaleSqrt()
      .domain([minCount, maxCount])
      .range([18, 70])

    // 색상: ratio 0 → 연한 파란색, ratio 1 → 진한 파란색
    const colorScale = d3.scaleLinear()
      .domain([0, 0.5, 1])
      .range(['#1e3a5f', '#3b82f6', '#93c5fd'])

    // 불투명도: ratio 높을수록 진하게
    const opacityScale = d3.scaleLinear()
      .domain([0, 1])
      .range([0.5, 1])

    // 노드 데이터 생성
    const nodes = data.map(d => ({
      ...d,
      r: radiusScale(d.total_count),
    }))

    // Force simulation
    const simulation = d3.forceSimulation(nodes)
      .force('center', d3.forceCenter(w / 2, h / 2))
      .force('charge', d3.forceManyBody().strength(5))
      .force('collide', d3.forceCollide(d => d.r + 3).strength(0.9))
      .force('x', d3.forceX(w / 2).strength(0.05))
      .force('y', d3.forceY(h / 2).strength(0.05))

    // 그룹 생성
    const g = svg.append('g')

    const nodeGroup = g.selectAll('g.node')
      .data(nodes)
      .join('g')
      .attr('class', 'node')
      .style('cursor', 'pointer')

    // 원 그리기
    nodeGroup.append('circle')
      .attr('r', d => d.r)
      .attr('fill', d => colorScale(d.ratio))
      .attr('opacity', d => opacityScale(d.ratio))
      .attr('stroke', d => d.ratio > 0.7 ? '#93c5fd' : '#3f3f46')
      .attr('stroke-width', d => d.ratio > 0.7 ? 2 : 1)

    // 키워드 텍스트
    nodeGroup.append('text')
      .text(d => d.keyword)
      .attr('text-anchor', 'middle')
      .attr('dominant-baseline', 'central')
      .attr('fill', '#f4f4f5')
      .attr('font-size', d => Math.max(d.r / 3.5, 9))
      .attr('font-weight', d => d.r > 40 ? 700 : 500)
      .attr('pointer-events', 'none')
      .each(function(d) {
        // 텍스트가 원보다 넓으면 잘라내기
        const textLen = this.getComputedTextLength()
        if (textLen > d.r * 1.8) {
          const ratio = (d.r * 1.8) / textLen
          const chars = Math.floor(d.keyword.length * ratio)
          d3.select(this).text(d.keyword.slice(0, chars) + '..')
        }
      })

    // 마우스 이벤트
    nodeGroup
      .on('mouseenter', function (event, d) {
        d3.select(this).select('circle')
          .transition().duration(150)
          .attr('stroke', '#93c5fd')
          .attr('stroke-width', 3)
          .attr('opacity', 1)

        const rect = containerRef.current.getBoundingClientRect()
        setTooltip({
          show: true,
          x: event.clientX - rect.left,
          y: event.clientY - rect.top - 10,
          data: d,
        })
      })
      .on('mousemove', function (event) {
        const rect = containerRef.current.getBoundingClientRect()
        setTooltip(prev => ({
          ...prev,
          x: event.clientX - rect.left,
          y: event.clientY - rect.top - 10,
        }))
      })
      .on('mouseleave', function (event, d) {
        d3.select(this).select('circle')
          .transition().duration(150)
          .attr('stroke', d.ratio > 0.7 ? '#93c5fd' : '#3f3f46')
          .attr('stroke-width', d.ratio > 0.7 ? 2 : 1)
          .attr('opacity', opacityScale(d.ratio))

        setTooltip({ show: false, x: 0, y: 0, data: null })
      })

    // 드래그
    const drag = d3.drag()
      .on('start', (event, d) => {
        if (!event.active) simulation.alphaTarget(0.3).restart()
        d.fx = d.x
        d.fy = d.y
      })
      .on('drag', (event, d) => {
        d.fx = event.x
        d.fy = event.y
      })
      .on('end', (event, d) => {
        if (!event.active) simulation.alphaTarget(0)
        d.fx = null
        d.fy = null
      })

    nodeGroup.call(drag)

    // tick마다 위치 갱신
    simulation.on('tick', () => {
      nodeGroup.attr('transform', d => `translate(${d.x}, ${d.y})`)
    })

    return () => simulation.stop()
  }, [data, dimensions])

  return (
    <div ref={containerRef} style={styles.container}>
      <svg
        ref={svgRef}
        width={dimensions.width}
        height={dimensions.height}
        style={styles.svg}
      />

      {/* 툴팁 */}
      {tooltip.show && tooltip.data && (
        <div style={{
          ...styles.tooltip,
          left: tooltip.x,
          top: tooltip.y,
        }}>
          <div style={styles.tooltipKeyword}>{tooltip.data.keyword}</div>
          <div style={styles.tooltipRow}>
            <span style={styles.tooltipLabel}>전체 뉴스</span>
            <span style={styles.tooltipValue}>{tooltip.data.total_count}건</span>
          </div>
          <div style={styles.tooltipRow}>
            <span style={styles.tooltipLabel}>AI 뉴스</span>
            <span style={styles.tooltipValue}>{tooltip.data.ai_count}건</span>
          </div>
          <div style={styles.tooltipRow}>
            <span style={styles.tooltipLabel}>AI 비율</span>
            <span style={{
              ...styles.tooltipValue,
              color: tooltip.data.ratio > 0.7 ? '#93c5fd' : tooltip.data.ratio > 0.4 ? '#3b82f6' : '#a1a1aa',
            }}>
              {(tooltip.data.ratio * 100).toFixed(1)}%
            </span>
          </div>
        </div>
      )}

      {/* 범례 */}
      <div style={styles.legend}>
        <div style={styles.legendTitle}>AI 뉴스 비율</div>
        <div style={styles.legendBar}>
          <div style={styles.legendGradient} />
          <div style={styles.legendLabels}>
            <span>0%</span>
            <span>50%</span>
            <span>100%</span>
          </div>
        </div>
        <div style={styles.legendSizeRow}>
          <span style={styles.legendTitle}>원 크기</span>
          <span style={{ fontSize: 12, color: '#a1a1aa' }}>= 전체 뉴스 수</span>
        </div>
      </div>
    </div>
  )
}

const styles = {
  container: {
    position: 'relative',
    width: '100%',
    overflow: 'hidden',
  },
  svg: {
    display: 'block',
  },
  tooltip: {
    position: 'absolute',
    transform: 'translate(-50%, -100%)',
    backgroundColor: '#27272a',
    border: '1px solid #3f3f46',
    borderRadius: 10,
    padding: '12px 16px',
    pointerEvents: 'none',
    zIndex: 10,
    minWidth: 160,
    boxShadow: '0 8px 24px rgba(0,0,0,0.5)',
  },
  tooltipKeyword: {
    fontSize: 15,
    fontWeight: 700,
    color: '#f4f4f5',
    marginBottom: 8,
    paddingBottom: 8,
    borderBottom: '1px solid #3f3f46',
  },
  tooltipRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '2px 0',
  },
  tooltipLabel: {
    fontSize: 12,
    color: '#a1a1aa',
  },
  tooltipValue: {
    fontSize: 13,
    fontWeight: 600,
    color: '#e4e4e7',
  },
  legend: {
    position: 'absolute',
    bottom: 16,
    right: 16,
    backgroundColor: 'rgba(24,24,27,0.85)',
    border: '1px solid #3f3f46',
    borderRadius: 10,
    padding: '12px 16px',
    backdropFilter: 'blur(8px)',
  },
  legendTitle: {
    fontSize: 12,
    fontWeight: 600,
    color: '#a1a1aa',
    marginBottom: 6,
  },
  legendBar: {
    marginBottom: 8,
  },
  legendGradient: {
    height: 8,
    borderRadius: 4,
    background: 'linear-gradient(to right, #1e3a5f, #3b82f6, #93c5fd)',
    marginBottom: 4,
  },
  legendLabels: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: 10,
    color: '#71717a',
  },
  legendSizeRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    marginTop: 4,
  },
}