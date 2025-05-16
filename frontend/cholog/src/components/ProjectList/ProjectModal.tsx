interface ProjectModalProps {
  showModal: boolean;
  modalType: "add" | "join" | null;
  inputValue: string;
  setInputValue: (value: string) => void;
  onClose: () => void;
  onSubmit: () => void;
  isSubmitting?: boolean;
}

const ProjectModal = ({
  showModal,
  modalType,
  inputValue,
  setInputValue,
  onClose,
  onSubmit,
  isSubmitting = false,
}: ProjectModalProps) => {
  if (!showModal) return null;

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl p-7 w-[90%] max-w-md shadow-lg">
        <h2 className="text-xl font-semibold mb-6">
          {modalType === "add" ? "새 프로젝트 생성" : "프로젝트 참가"}
        </h2>
        <input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          placeholder={modalType === "add" ? "프로젝트명" : "프로젝트 ID"}
          className="w-full border border-gray-300 rounded-lg px-4 py-2 mb-6 focus:outline-none focus:border-[#5EA500] focus:ring-1 focus:ring-[#5EA500]"
        />
        <div className="flex justify-between">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
            disabled={isSubmitting}
          >
            취소
          </button>
          <button
            onClick={onSubmit}
            className={`px-4 py-2 bg-[#5EA500] text-white rounded-lg ${
              isSubmitting 
                ? "opacity-50 cursor-not-allowed" 
                : "hover:bg-[#4A8400] transition-colors"
            }`}
            disabled={isSubmitting}
          >
            {isSubmitting ? "처리 중..." : modalType === "add" ? "생성" : "참가"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProjectModal;