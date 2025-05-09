import React from 'react';

interface RankingItem {
  name: string;
  count: number;
}

interface RankingCardListProps {
  items: RankingItem[];
}

const RankingCardList: React.FC<RankingCardListProps> = ({ items }) => {
  return (
    <div className="flex flex-col gap-4">
      {items.map((item, index) => (
        <div
          key={index}
          className="bg-white rounded-lg border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200"
        >
          <div className="p-5">
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-3">
                <span className="text-lg font-bold text-gray-400">
                  #{index + 1}
                </span>
                <h3 className="text-base font-bold text-gray-800">
                  {item.name}
                </h3>
              </div>
              <span className="text-lg font-bold text-indigo-600">
                {item.count.toLocaleString()}
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};

export default RankingCardList;