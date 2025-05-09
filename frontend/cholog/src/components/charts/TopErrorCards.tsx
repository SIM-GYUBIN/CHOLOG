import React from 'react';

interface ErrorData {
    name: string;
    count: number;
}

interface TopErrorCardsProps {
    errors: ErrorData[];
}

const TopErrorCards: React.FC<TopErrorCardsProps> = ({ errors }) => {
    const getAccentColor = (index: number) => {
        switch (index) {
            case 0:
                return {
                    border: 'border-rose-500',
                    badgeBg: 'bg-rose-100',
                    badgeText: 'text-rose-800',
                };
            case 1:
                return {
                    border: 'border-amber-500',
                    badgeBg: 'bg-amber-100',
                    badgeText: 'text-amber-800',
                };
            case 2:
                return {
                    border: 'border-emerald-500',
                    badgeBg: 'bg-emerald-100',
                    badgeText: 'text-emerald-800',
                };
            default:
                return {
                    border: 'border-gray-300',
                    badgeBg: 'bg-gray-100',
                    badgeText: 'text-gray-700',
                };
        }
    };

    return (
        <div className="flex flex-col gap-4">
            {errors.map((error, index) => {
                const { border, badgeBg, badgeText } = getAccentColor(index);

                return (
                    <div
                        key={index}
                        className={`bg-white border-l-4 ${border} rounded-xl ring-1 ring-gray-100 hover:shadow-md transition-shadow duration-200`}
                    >
                        <div className="p-4 sm:p-5">
                            <div className="flex items-center justify-between mb-2">
                                <span
                                    className={`text-xs sm:text-sm font-semibold px-2 py-0.5 rounded-full ${badgeBg} ${badgeText}`}
                                >
                                    #{index + 1} 순위
                                </span>
                                <span className="text-base sm:text-lg font-bold text-gray-900">
                                    {error.count.toLocaleString()}회
                                </span>
                            </div>
                            <div className="text-sm sm:text-base text-gray-600 font-medium truncate">
                                {error.name}
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
};

export default TopErrorCards;
