interface ProjectModalProps {
  showModal: boolean;
  modalType: "add" | "join" | null;
  inputValue: string;
  setInputValue: (value: string) => void;
  onClose: () => void;
  onSubmit: () => void;
}

const ProjectModal = ({
  showModal,
  modalType,
  inputValue,
  setInputValue,
  onClose,
  onSubmit,
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
          className="w-full border border-gray-300 rounded-lg px-4 py-2 mb-6"
        />
        <div className="flex justify-between">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300"
          >
            취소
          </button>
          <button
            onClick={onSubmit}
            className="px-4 py-2 bg-black text-white rounded-lg hover:bg-gray-800"
          >
            {modalType === "add" ? "생성" : "참가"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProjectModal;