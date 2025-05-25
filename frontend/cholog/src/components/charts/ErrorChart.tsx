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
import dayjs from "dayjs";

interface ErrorChartProps {
  projectId?: number;
  startDate?: string;
  endDate?: string;
}

const ErrorCountChart: React.FC<ErrorChartProps> = ({
  projectId,
  startDate,
  endDate,
}) => {
  const scrollRef = useRef<HTMLDivElement>(null);
  const dispatch = useDispatch<AppDispatch>();
  const { errorTimeline } = useSelector((state: RootState) => state.log);

  useEffect(() => {
    if (projectId) {
      const today = new Date();
      const yesterday = new Date(today);
      yesterday.setDate(yesterday.getDate() - 1);

      dispatch(
        fetchErrorTimeline({
          projectId,
          // startDate: startDate || yesterday.toISOString().split("T")[0],
          // endDate: endDate || today.toISOString().split("T")[0],
        })
      );
    }
  }, [dispatch, projectId, startDate, endDate]);

  // ✅ 0인 데이터 생략
  const chartData = errorTimeline
    .filter((item) => item.logCount > 0)
    .map((item) => ({
      period: dayjs(item.timestamp).format("MM-DD HH시"),
      count: item.logCount,
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
      <h2 className="text-center text-xl font-semibold text-[var(--text)] mb-4">
        로그 발생 추이
      </h2>
      <div
        ref={scrollRef}
        className="scroll-hidden overflow-x-auto cursor-grab active:cursor-grabbing"
      >
        <div
          style={{
            width: `${Math.max(chartData.length * 60, 600)}px`,
            height: "200px",
            minWidth: "100%"
          }}
        >
          <AreaChart
            data={chartData}
            width={Math.max(chartData.length * 60, 600)}
            height={200}
            margin={{ top: 20, right: 30, bottom: 40, left: 20 }}
          >
            <defs>
              <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#4B5563" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#4B5563" stopOpacity={0.05} />
              </linearGradient>
            </defs>
            <CartesianGrid
              vertical={false}
              strokeDasharray="3 3"
              strokeOpacity={0.1}
              stroke="#4B5563"
            />
            <XAxis
              dataKey="period"
              tick={{ fill: "#9CA3AF", fontSize: 12 }}
              angle={-30}
              textAnchor="end"
              height={50}
              stroke="#4B5563"
              tickMargin={8}
            />
            <YAxis 
              tick={{ fill: "#9CA3AF", fontSize: 12 }}
              stroke="#4B5563"
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => `${value}건`}
            />
            <Tooltip
              formatter={(value: number) => [`${value}건`, "발생 건수"]}
              labelFormatter={(label) => `${label}`}
              contentStyle={{
                backgroundColor: "#374151",
                borderRadius: "8px",
                border: "none",
                color: "#fff",
                fontSize: "14px",
                padding: "8px 12px"
              }}
              itemStyle={{ color: "#fff" }}
            />
            <Area
              type="monotone"
              dataKey="count"
              stroke="#4B5563"
              strokeWidth={2}
              fill="url(#colorCount)"
              dot={false}
              activeDot={{
                r: 4,
                stroke: "#4B5563",
                strokeWidth: 2,
                fill: "white"
              }}
            />
          </AreaChart>
        </div>
      </div>
    </div>
  );
};

export default ErrorCountChart;
