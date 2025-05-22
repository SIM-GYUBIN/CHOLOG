interface ModifyProjectModalProps {
  showModal: boolean;
  projectName: string;
  projectId?: number;
  setProjectName: (name: string) => void;
  onClose: () => void;
  onSubmit: () => void;
}

const ModifyProjectModal = ({
  showModal,
  projectName,
  projectId,
  setProjectName,
  onClose,
  onSubmit,
}: ModifyProjectModalProps) => {
  if (!showModal) return null;

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl p-7 w-[90%] max-w-md shadow-lg">
        <h2 className="text-xl font-semibold mb-6">
          프로젝트명 수정
        </h2>
        <input
          type="text"
          value={projectName}
          onChange={(e) => setProjectName(e.target.value)}
          placeholder="새로운 프로젝트명"
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
            수정
          </button>
        </div>
      </div>
    </div>
  );
};

export default ModifyProjectModal;