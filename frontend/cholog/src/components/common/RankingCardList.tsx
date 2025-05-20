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
              className="bg-[var(--sub)] rounded-lg border border-[var(--line)] shadow-sm hover:shadow-md transition-shadow duration-200"
            >
              <div className="p-5 text-left text-[var(--text)]">
                <div className="text-lg font-bold text-[var(--helpertext)] mb-1">
                  #{rank}
                </div>
                <div className="text-sm font-semibold">{method}</div>
                <div className="text-sm break-all mb-2">{url}</div>
                <div className="text-sm text-[var(--helpertext)] leading-snug space-y-1">
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
            className="bg-[var(--sub)] rounded-lg border border-[var(--line)] shadow-sm hover:shadow-md transition-shadow duration-200"
          >
            <div className="p-5 text-[var(--text)] relative min-h-[64px]">
              {/* 순위 */}
              <div className="text-lg font-bold text-gray-400 text-left">
                #{rank}
              </div>

              {/* 에러 메시지 */}
              <div className="mt-1 pr-9 text-base font-bold text-left break-words whitespace-pre-wrap leading-snug">
                {item.name}
              </div>

              {/* 오른쪽 고정 숫자 */}
              <div className="absolute top-5 right-5 text-lg font-bold text-lime-500 whitespace-nowrap">
                {item.count.toLocaleString()}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default RankingCardList;
