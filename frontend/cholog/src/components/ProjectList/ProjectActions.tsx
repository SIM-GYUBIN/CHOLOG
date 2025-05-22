import searchIcon from "@/assets/search.svg";

interface ProjectActionsProps {
  onAdd: () => void;
  onJoin: () => void;
  onSearch: (term: string) => void;
}

const ProjectActions = ({ onAdd, onJoin, onSearch }: ProjectActionsProps) => {
  return (
    <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 sm:gap-4 mb-6 sm:mb-8">
      <div className="relative flex-1">
        <img
          src={searchIcon}
          alt="search"
          className="absolute left-3 sm:left-5 top-1/2 -translate-y-1/2 w-4 h-4 opacity-60"
        />
        <input
          type="text"
          placeholder="프로젝트를 검색해보세요."
          className="w-full pl-10 sm:pl-12 pr-4 py-2.5 sm:py-3 text-sm text-[var(--helpertext)] bg-[var(--bg)] border border-[var(--line)] rounded-full focus:outline-none focus:ring-2 focus:ring-lime-500"
          onChange={(e) => onSearch(e.target.value)}
        />
      </div>
      <div className="flex gap-2 sm:gap-4">
        <button
          onClick={onAdd}
          className="flex-1 sm:flex-initial px-4 sm:px-6 py-2.5 sm:py-2 bg-[var(--bg)] text-[var(--helpertext)] border border-[var(--line)] rounded-2xl hover:bg-gray-50 transition-colors font-paperlogy5 cursor-pointer text-sm sm:text-base whitespace-nowrap"
        >
          ADD
        </button>
        <button
          onClick={onJoin}
          className="flex-1 sm:flex-initial px-4 sm:px-6 py-2.5 sm:py-2 bg-[var(--bg)] text-[var(--helpertext)] border border-[var(--line)] rounded-2xl hover:bg-gray-50 transition-colors font-paperlogy5 cursor-pointer text-sm sm:text-base whitespace-nowrap"
        >
          JOIN
        </button>
      </div>
    </div>
  );
};

export default ProjectActions;
