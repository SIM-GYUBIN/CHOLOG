import searchIcon from "@/assets/search.svg";

interface ProjectActionsProps {
  onAdd: () => void;
  onJoin: () => void;
  onSearch: (term: string) => void;
}

const ProjectActions = ({ onAdd, onJoin, onSearch }: ProjectActionsProps) => {
  return (
    <div className="flex justify-start gap-4 mb-8">
      <div className="relative flex-1 mx-3">
        <img
          src={searchIcon}
          alt="search"
          className="absolute left-5 top-1/2 -translate-y-1/2 w-4 h-4 opacity-60"
        />
        <input
          type="text"
          placeholder="프로젝트를 검색해보세요."
          className="w-full pl-12 pr-4 py-3 text-sm text-[var(--helpertext)] bg-[var(--bg)] border border-[var(--line)] rounded-full focus:outline-none focus:ring-2 focus:ring-lime-500"
          onChange={(e) => onSearch(e.target.value)}
        />
      </div>
      <button
        onClick={onAdd}
        className="px-6 py-2 bg-[[var(--bg)]] text-[var(--helpertext)] border border-[var(--line)] rounded-2xl hover:bg-gray-50 transition-colors font-paperlogy5 cursor-pointer"
      >
        ADD
      </button>
      <button
        onClick={onJoin}
        className="px-6 py-2 bg-[[var(--bg)]] text-[var(--helpertext)] border border-[var(--line)] rounded-2xl hover:bg-gray-50 transition-colors font-paperlogy5 cursor-pointer"
      >
        JOIN
      </button>
    </div>
  );
};

export default ProjectActions;
