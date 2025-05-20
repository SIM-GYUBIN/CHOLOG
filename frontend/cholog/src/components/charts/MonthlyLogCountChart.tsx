import React, { useEffect, useRef, useState } from "react";
import api from "../../api/axios";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";

interface LogData {
  timestamp: string;
  logCount: number;
}

interface MonthlyLogCountChartProps {
  projectId: number;
  startDate?: string; // yyyy-MM-dd
  endDate?: string; // yyyy-MM-dd
  token: string;
}

const MonthlyLogCountChart: React.FC<MonthlyLogCountChartProps> = ({
  projectId,
  startDate,
  endDate,
  token,
}) => {
  const [data, setData] = useState<{ day: string; count: number }[]>([]);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await api.get(`/log/${projectId}/timeline`, {
          headers: {
            "Content-Type": "application/json",
          },
          params: {
            startDate,
            endDate,
          },
        });

        const raw: LogData[] = response.data.data;
        const dateMap: { [key: string]: number } = {};

        raw.forEach((item) => {
          const date = new Date(item.timestamp);
          const day = date.getDate();
          dateMap[day] = item.logCount;
        });

        const daysInMonth = Array.from({ length: 31 }, (_, i) => i + 1);
        const parsed = daysInMonth.map((day) => ({
          day: day.toString(),
          count: dateMap[day] ?? 0,
        }));

        setData(parsed);
      } catch (err) {
        console.error("Failed to fetch log data", err);
        setData([]);
      }
    };

    fetchData();
  }, [projectId, startDate, endDate, token]);

  const chartWidth = Math.max(data.length * 40 + 40, 400); // 양 끝 여백 포함

  useEffect(() => {
    const container = scrollRef.current;
    if (!container) return;

    container.scrollLeft = 0;

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
    <div className="w-full p-6 rounded-2xl h-full">
      <div
        ref={scrollRef}
        className="overflow-x-auto overflow-y-hidden cursor-grab active:cursor-grabbing"
        style={{ WebkitOverflowScrolling: "touch" }}
      >
        <div
          style={{
            width: `${chartWidth}px`,
            minWidth: "100%",
            height: "160px",
            paddingLeft: "4px",
            paddingRight: "4px",
          }}
        >
          <AreaChart
            data={data}
            width={chartWidth}
            height={160}
            margin={{ left: 2, right: 10 }}
          >
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
            <XAxis
              dataKey="day"
              tick={{ fill: "#9CA3AF" }}
              interval={0}
              allowDataOverflow={true}
            />
            <YAxis hide />
            <Tooltip
              formatter={(value: number) => [`${value}`]}
              labelFormatter={() => ""}
              contentStyle={{
                backgroundColor: "#4B5563",
                borderRadius: "8px",
                border: "none",
                color: "#fff",
                fontSize: "1rem",
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

export default MonthlyLogCountChart;
