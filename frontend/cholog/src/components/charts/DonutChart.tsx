import React, { useState } from "react";

interface DonutChartProps {
  data: {
    name: string;
    value: number;
    color: string;
  }[];
  size?: number;
  thickness?: number;
  gapAngle?: number;
}

const DonutChart: React.FC<DonutChartProps> = ({
  data,
  size = 200,
  thickness = 20,
  gapAngle = 0.03, // radians (~1.7도)
}) => {
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);
  const radius = size / 2;
  const center = size / 2;
  const total = data.reduce((sum, item) => sum + item.value, 0);

  const getArcPath = (startAngle: number, endAngle: number): string => {
    const rOuter = radius;
    const rInner = radius - thickness;

    const startOuter = {
      x: center + rOuter * Math.cos(startAngle),
      y: center + rOuter * Math.sin(startAngle),
    };
    const endOuter = {
      x: center + rOuter * Math.cos(endAngle),
      y: center + rOuter * Math.sin(endAngle),
    };
    const startInner = {
      x: center + rInner * Math.cos(endAngle),
      y: center + rInner * Math.sin(endAngle),
    };
    const endInner = {
      x: center + rInner * Math.cos(startAngle),
      y: center + rInner * Math.sin(startAngle),
    };

    const largeArc = endAngle - startAngle > Math.PI ? 1 : 0;

    return [
      `M ${startOuter.x} ${startOuter.y}`,
      `A ${rOuter} ${rOuter} 0 ${largeArc} 1 ${endOuter.x} ${endOuter.y}`,
      `L ${startInner.x} ${startInner.y}`,
      `A ${rInner} ${rInner} 0 ${largeArc} 0 ${endInner.x} ${endInner.y}`,
      "Z",
    ].join(" ");
  };

  let currentAngle = -Math.PI / 2;

  return (
    <div className="flex items-center justify-center gap-8 overflow-visible">
      <div className="relative flex items-center overflow-visible">
        <svg
          width={size}
          height={size}
          viewBox={`0 0 ${size} ${size}`}
          overflow="visible"
        >
          {/* 중앙 배경 원 (더 작게) */}
          <circle
            cx={center}
            cy={center}
            r={radius - thickness - 6}
            fill="#0000000d"
          />

          {/* 도넛 조각 */}
          {data.map((item, idx) => {
            const valueRatio = item.value / total;
            const arcAngle = valueRatio * 2 * Math.PI - gapAngle;
            const start = currentAngle + gapAngle / 2;
            const end = start + arcAngle;

            const pathD = getArcPath(start, end);

            const midAngle = (start + end) / 2;
            const labelRadius = radius - thickness / 2;
            const labelX = center + labelRadius * Math.cos(midAngle);
            const labelY = center + labelRadius * Math.sin(midAngle);

            currentAngle += valueRatio * 2 * Math.PI;

            return (
              <g
                key={idx}
                onMouseEnter={() => setHoveredIdx(idx)}
                onMouseLeave={() => setHoveredIdx(null)}
              >
                <path
                  d={pathD}
                  fill={item.color}
                  //   stroke="#333"
                  //   strokeWidth={1.5}
                  style={{
                    transition: "opacity 0.3s ease",
                    opacity:
                      hoveredIdx === null || hoveredIdx === idx ? 1 : 0.3,
                    cursor: "pointer",
                  }}
                />

                {hoveredIdx === idx && (
                  <foreignObject
                    x={labelX - 20}
                    y={labelY - 12}
                    width={40}
                    height={265}
                  >
                    <div
                      className="text-[10px] px-2 py-1 rounded-full bg-white/80 border shadow text-center"
                      style={{ borderColor: item.color }}
                    >
                      {Math.round((item.value / total) * 100)}%
                    </div>
                  </foreignObject>
                )}
              </g>
            );
          })}

          {/* 중앙 텍스트 */}
          <text
            x={center}
            y={center - 5}
            textAnchor="middle"
            fontSize="24"
            fontWeight="bold"
            style={{ fill: "var(--text)" }}
          >
            {total.toLocaleString()}
          </text>
          <text
            x={center}
            y={center + 18}
            textAnchor="middle"
            fontSize="10"
            style={{ fill: "var(--helpertext)" }}
          >
            Total Logs
          </text>

          {/* 중앙 텍스트 */}
          {/* <defs>
            <linearGradient id="gradient" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stopColor="#6366f1" />
              <stop offset="100%" stopColor="#ec4899" />
            </linearGradient>
          </defs> */}
        </svg>
      </div>

      {/* 오른쪽 정보 텍스트 영역 */}
      <div className="flex flex-col gap-3 text-[var(--text)] text-sm self-center">
        {data.map((item, idx) => (
          <div key={idx} className="flex items-start gap-2">
            <div
              className="mt-1 w-3 h-3 rounded-full shrink-0"
              style={{ backgroundColor: item.color }}
            />
            <div className="flex flex-col text-left">
              <span className="font-medium leading-tight">{item.name}</span>
              <span className="text-xs text-[var(--helpertext)] leading-snug">
                {item.value.toLocaleString()} (
                {Math.round((item.value / total) * 100)}%)
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default DonutChart;
