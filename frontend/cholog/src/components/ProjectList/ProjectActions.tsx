interface ProjectActionsProps {
  onAdd: () => void;
  onJoin: () => void;
}

const ProjectActions = ({ onAdd, onJoin }: ProjectActionsProps) => {
  return (
    <div className="flex justify-start gap-4 mb-8">
      <button
        onClick={onAdd}
        className="px-6 py-2 bg-[#F8FAFC] text-[#64748B] border border-[#CBD5E1] rounded-2xl hover:bg-gray-50 transition-colors font-paperlogy5 cursor-pointer"
      >
        ADD
      </button>
      <button
        onClick={onJoin}
        className="px-6 py-2 bg-[#F8FAFC] text-[#64748B] border border-[#CBD5E1] rounded-2xl hover:bg-gray-50 transition-colors font-paperlogy5 cursor-pointer"
      >
        JOIN
      </button>
    </div>
  );
};

export default ProjectActions;