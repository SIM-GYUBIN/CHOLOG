import React, { useRef, useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { AppDispatch, RootState } from "../../store/store";
import { fetchErrorTimeline } from "../../store/slices/logSlice";

interface ErrorChartProps {
  projectId?: number;
}

const ErrorCountChart: React.FC<ErrorChartProps> = ({ projectId }) => {
  const scrollRef = useRef<HTMLDivElement>(null);
  const dispatch = useDispatch<AppDispatch>();
  const { errorTimeline } = useSelector((state: RootState) => state.log);
  const [chartData, setChartData] = useState<any[]>([]);

  useEffect(() => {
    if (projectId) {
      dispatch(fetchErrorTimeline({ projectId }));
    }
  }, [dispatch, projectId]);

  useEffect(() => {
    if (Array.isArray(errorTimeline) && errorTimeline.length > 0) {
      const formattedData = errorTimeline.map(item => {
        const date = new Date(item.timestamp);
        // 월-일 시:분 형식으로 포맷팅 (년도 제외)
        const formattedDate = `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:00`;
        return {
          period: formattedDate,
          count: item.logCount || 0
        };
      });
      setChartData(formattedData);
    }
  }, [errorTimeline]);

  useEffect(() => {
    const container = scrollRef.current;
    if (!container) return;

    container.scrollLeft = container.scrollWidth;

    let isDown = false;
    let startX = 0;
    let scrollLeft = 0;

    const onMouseDown = (e: MouseEvent) => {
      isDown = true;
      startX = e.pageX - container.offsetLeft;
      scrollLeft = container.scrollLeft;
    };

    const onMouseLeave = () => (isDown = false);
    const onMouseUp = () => (isDown = false);

    const onMouseMove = (e: MouseEvent) => {
      if (!isDown) return;
      e.preventDefault();
      const x = e.pageX - container.offsetLeft;
      const walk = (x - startX) * 1.5;
      container.scrollLeft = scrollLeft - walk;
    };

    container.addEventListener("mousedown", onMouseDown);
    container.addEventListener("mouseleave", onMouseLeave);
    container.addEventListener("mouseup", onMouseUp);
    container.addEventListener("mousemove", onMouseMove);

    return () => {
      container.removeEventListener("mousedown", onMouseDown);
      container.removeEventListener("mouseleave", onMouseLeave);
      container.removeEventListener("mouseup", onMouseUp);
      container.removeEventListener("mousemove", onMouseMove);
    };
  }, []);

  return (
    <div className="w-full p-6 bg-white/5 rounded-2xl h-full border border-[var(--line)]">
      <h2 className="text-left text-xl font-semibold text-[var(--text)] mb-4">
        시간별 로그 발생량
      </h2>
      {chartData.length === 0 ? (
        <div className="flex justify-center items-center h-[160px]">
          <p>표시할 데이터가 없습니다</p>
        </div>
      ) : (
        <div
          ref={scrollRef}
          className="scroll-hidden overflow-x-auto cursor-grab active:cursor-grabbing"
        >
          <div style={{ width: `${Math.max(chartData.length * 60, 300)}px`, height: "160px", minWidth: "100%" }}>
            <AreaChart data={chartData} width={Math.max(chartData.length * 60, 300)} height={160}>
              <defs>
                <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#9CA3AF" stopOpacity={0.4} />
                  <stop offset="95%" stopColor="#9CA3AF" stopOpacity={0.05} />
                </linearGradient>
              </defs>
              <CartesianGrid
                vertical={false}
                strokeDasharray="3 3"
                strokeOpacity={0.1}
              />
              <XAxis dataKey="period" tick={{ fill: "#9CA3AF" }} />
              <YAxis hide />
              <Tooltip
                formatter={(value: number) => [`${value}`]}
                labelFormatter={(label) => `${label}`}
                contentStyle={{
                  backgroundColor: "#4B5563",
                  borderRadius: "8px",
                  border: "none",
                  color: "#fff",
                  fontSize: "14px",
                  padding: "2px 6px",
                }}
                itemStyle={{ color: "#fff" }}
              />
              <Area
                type="monotone"
                dataKey="count"
                stroke="#9CA3AF"
                strokeWidth={2}
                fill="url(#colorCount)"
                dot={false}
                activeDot={{
                  r: 5,
                  stroke: "#6B7280",
                  strokeWidth: 2,
                  fill: "white",
                }}
              />
            </AreaChart>
          </div>
        </div>
      )}
    </div>
  );
};

export default ErrorCountChart;
