import React, { useEffect, useState } from 'react';

interface DonutChartProps {
    data: {
        name: string;
        value: number;
        color: string;
    }[];
    size?: number;
    thickness?: number;
}

const DonutChart: React.FC<DonutChartProps> = ({
    data,
    size = 200,
    thickness = 40,
}) => {
    const [progress, setProgress] = useState(0);
    const radius = size / 2;
    const center = size / 2;
    const total = data.reduce((sum, item) => sum + item.value, 0);

    useEffect(() => {
        const timer = setTimeout(() => setProgress(1), 100);
        return () => clearTimeout(timer);
    }, []);

    const getArcPath = (startAngle: number, endAngle: number): string => {
        const r = radius;
        const innerR = r - thickness;

        const startOuter = {
            x: center + r * Math.cos(startAngle),
            y: center + r * Math.sin(startAngle),
        };
        const endOuter = {
            x: center + r * Math.cos(endAngle),
            y: center + r * Math.sin(endAngle),
        };
        const startInner = {
            x: center + innerR * Math.cos(endAngle),
            y: center + innerR * Math.sin(endAngle),
        };
        const endInner = {
            x: center + innerR * Math.cos(startAngle),
            y: center + innerR * Math.sin(startAngle),
        };

        const largeArc = endAngle - startAngle > Math.PI ? 1 : 0;

        return [
            `M ${startOuter.x} ${startOuter.y}`,
            `A ${r} ${r} 0 ${largeArc} 1 ${endOuter.x} ${endOuter.y}`,
            `L ${startInner.x} ${startInner.y}`,
            `A ${innerR} ${innerR} 0 ${largeArc} 0 ${endInner.x} ${endInner.y}`,
            'Z',
        ].join(' ');
    };

    let currentAngle = -Math.PI / 2;

    return (
        <div className="flex items-center justify-center gap-8">
            <div className="relative flex items-center">
                <svg width={size} height={size}>
                    {data.map((item, idx) => {
                        const portion = item.value / total;
                        const endAngle = currentAngle + portion * 2 * Math.PI;
                        const path = getArcPath(currentAngle, endAngle);
                        const result = (
                            <path
                                key={idx}
                                d={path}
                                fill={item.color}
                                style={{
                                    transform: `scale(${progress})`,
                                    transformOrigin: 'center',
                                    opacity: progress,
                                    transition: 'transform 0.6s ease, opacity 0.6s ease',
                                }}
                            />
                        );
                        currentAngle = endAngle;
                        return result;
                    })}
                    {/* 중앙 원 (빈 공간) */}
                    <circle
                        cx={center}
                        cy={center}
                        r={radius - thickness - 8}
                        fill="white"
                    />
                </svg>

                {/* 중앙 텍스트 */}
                <div className="absolute inset-0 flex items-center justify-center">
                    <div className="text-center">
                        <div className="text-3xl font-extrabold bg-gradient-to-r from-indigo-500 to-pink-500 bg-clip-text text-transparent">
                            {total.toLocaleString()}
                        </div>
                        <div className="text-sm text-gray-500">Total Logs</div>
                    </div>
                </div>
            </div>

            <div className="flex flex-col gap-3 text-sm self-center">
                {data.map((item, idx) => (
                    <div key={idx} className="flex items-start gap-2">
                        <div
                            className="mt-1 w-3 h-3 rounded-full shrink-0"
                            style={{ backgroundColor: item.color }}
                        />
                        <div className="flex flex-col text-left">
                            <span className="font-medium leading-tight">{item.name}</span>
                            <span className="text-xs text-gray-500 leading-snug">
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
