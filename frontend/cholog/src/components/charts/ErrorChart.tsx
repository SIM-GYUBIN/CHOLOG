import React, { useRef, useEffect } from "react";
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


  useEffect(() => {
    if (projectId) {
      dispatch(fetchErrorTimeline({ projectId }));
    }
    console.log(errorTimeline[0]);
  }, [dispatch, projectId]);

  // API 응답 데이터를 차트 데이터 형식으로 변환
  const chartData = errorTimeline.map(item => ({
    period: new Date(item.timestamp).getHours().toString(),
    count: item.errorCount
  }));

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
      <div
        ref={scrollRef}
        className="scroll-hidden overflow-x-auto cursor-grab active:cursor-grabbing"
      >
        <div style={{ width: `${chartData.length * 60}px`, height: "160px", minWidth: "100%" }}>
          <AreaChart data={chartData} width={chartData.length * 60} height={160}>
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
              labelFormatter={() => ""}
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
    </div>
  );
};

export default ErrorCountChart;
