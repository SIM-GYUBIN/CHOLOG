import React from "react";

interface RankingItem {
  name: string;
  count: number;
  extra?: string;
  rank?: number;
}

interface RankingCardListProps {
  items: RankingItem[];
  renderItem?: (item: RankingItem, index: number) => React.ReactNode;
}

const RankingCardList: React.FC<RankingCardListProps> = ({ items }) => {
  return (
    <div className="flex flex-col gap-4">
      {items.map((item, index) => {
        const rank = item.rank ?? index + 1;

        // API 상세 카드 (extra 있는 경우)
        if (item.extra) {
          const parts = item.name.split(" ");
          const method = parts[0];
          const url = parts.slice(1).join(" ");

          return (
            <div
              key={index}
              className="bg-white rounded-lg border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200"
            >
              <div className="p-5 text-left text-gray-800">
                <div className="text-lg font-bold text-gray-500 mb-1">
                  #{rank}
                </div>
                <div className="text-sm font-semibold">{method}</div>
                <div className="text-sm break-all mb-2">{url}</div>
                <div className="text-sm text-gray-600 leading-snug space-y-1">
                  {item.extra.split("\n").map((line, i) => (
                    <div
                      key={i}
                      className="m-0 p-0"
                      style={{
                        margin: 0,
                        padding: 0,
                        textAlign: "left",
                        lineHeight: "1.25rem",
                      }}
                    >
                      {line}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          );
        }

        // 일반 에러 랭킹 카드
        return (
          <div
            key={index}
            className="bg-white rounded-lg border border-gray-100 shadow-sm hover:shadow-md transition-shadow duration-200"
          >
            <div className="p-5 flex items-center justify-between text-gray-800">
              <div className="flex items-center gap-3">
                <span className="text-lg font-bold text-gray-400">#{rank}</span>
                <h3 className="text-base font-bold">{item.name}</h3>
              </div>
              <span className="text-lg font-bold text-lime-500">
                {item.count.toLocaleString()}
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default RankingCardList;
