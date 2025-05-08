import React, { useRef, useEffect } from "react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";

const ErrorCountChart = () => {
  const scrollRef = useRef<HTMLDivElement>(null);

  const mockData = [
    { period: "8", count: 120 },
    { period: "9", count: 200 },
    { period: "10", count: 160 },
    { period: "11", count: 350 },
    { period: "12", count: 220 },
    { period: "13", count: 180 },
    { period: "14", count: 240 },
    { period: "15", count: 230 },
    { period: "16", count: 210 },
    { period: "17", count: 190 },
    { period: "18", count: 300 },
    { period: "19", count: 280 },
  ];

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
    <div className="w-full max-w-md border border-[var(--line)] p-8 bg-white/5 rounded-2xl shadow-md">
      <h2 className="text-left text-2xl font-semibold text-[var(--text)] mb-2">
        Hourly Log Count
      </h2>
      <div
        ref={scrollRef}
        className="scroll-hidden overflow-x-auto cursor-grab active:cursor-grabbing"
      >
        <div style={{ width: `${mockData.length * 60}px`, height: "180px" }}>
          <AreaChart data={mockData} width={mockData.length * 60} height={180}>
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
