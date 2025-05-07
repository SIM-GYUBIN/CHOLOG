import { useState } from "react";
import ProjectCard from "../components/projectCard";
import logo from '@/assets/logo.svg';
import { useNavigate } from 'react-router-dom';
import exitIcon from '@/assets/exit.svg';
import deleteIcon from '@/assets/delete.svg';

const ProjectListPage = () => {
  const navigate = useNavigate(); // Add this line
  const [showModal, setShowModal] = useState(false);
  const [modalType, setModalType] = useState<"add" | "join" | null>(null);
  const [inputValue, setInputValue] = useState("");

  // 임시 프로젝트 데이터
  const recentProjects = [
    { id: 1, name: "Project name", status: "정상", lastLog: "2025.04.28" },
    { id: 2, name: "Project name", status: "비정상", lastLog: "2025.04.28" },
    { id: 3, name: "Project name", status: "정상", lastLog: "2025.04.28" },
    { id: 4, name: "Project name", status: "비정상", lastLog: "2025.04.28" },
  ];

  const projectList = [
    { id: 1, name: "프로젝트명", projectId: "prj-5f3a8b7e", date: "2025.04.28" },
    { id: 2, name: "프로젝트명", projectId: "prj-5f3a8b7e", date: "2025.04.28" },
    { id: 3, name: "프로젝트명", projectId: "prj-5f3a8b7e", date: "2025.04.28" },
    { id: 4, name: "프로젝트명", projectId: "prj-5f3a8b7e", date: "2025.04.28" },
    { id: 5, name: "프로젝트명", projectId: "prj-5f3a8b7e", date: "2025.04.28" },
  ];

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  const openModal = (type: "add" | "join") => {
    setModalType(type);
    setShowModal(true);
    setInputValue("");
  };

  const closeModal = () => {
    setShowModal(false);
    setModalType(null);
  };

  const handleSubmit = () => {
    if (modalType === "add") {
      console.log("프로젝트 생성:", inputValue);
    } else if (modalType === "join") {
      console.log("프로젝트 참가:", inputValue);
    }
    closeModal();
  };

  return (
    <div className="max-w-7xl mx-auto">
      {/* 로고 섹션 */}
      <div className="text-center">
        <img src={logo} alt="Cholog logo" className="h-36 mx-auto" />
      </div>

      {/* 최근 프로젝트 섹션 */}
      <section className="mb-12">
        <p className="text-left text-[28px] leading-tight tracking-wide font-paperlogy7 mb-6">
          최근 프로젝트
        </p>
        <div className="grid grid-cols-4 gap-4">
          {recentProjects.map((project, index) => (
            <ProjectCard
              key={index}
              id={project.id}
              name={project.name}
              status={project.status}
              lastLog={project.lastLog}
            />
          ))}
        </div>
      </section>

      {/* ADD, JOIN 버튼 섹션 */}
      <div className="flex justify-start gap-4 mb-8">
        <button
          onClick={() => openModal("add")}
          className="px-6 py-2 bg-white text-black border border-gray-200 rounded-xl hover:bg-gray-50 transition-colors font-paperlogy5 cursor-pointer"
        >
          ADD
        </button>
        <button
          onClick={() => openModal("join")}
          className="px-6 py-2 bg-white text-black border border-gray-200 rounded-xl hover:bg-gray-50 transition-colors font-paperlogy5 cursor-pointer"
        >
          JOIN
        </button>
      </div>

      {/* 프로젝트 리스트 섹션 */}
      <section className="mt-8">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-200">
              <th className="w-1/3 p-4 font-paperlogy5 text-left">
                프로젝트명
              </th>
              <th className="w-1/3 p-4 font-paperlogy5 text-left">
                프로젝트 ID
              </th>
              <th className="w-1/4 p-4 font-paperlogy5 text-left">생성 시간</th>
              <th className="w-12 p-4"></th>
            </tr>
          </thead>
          <tbody>
            {projectList.map((project, index) => (
              <tr key={index} className="border-b border-gray-200">
                <td className="w-1/3 p-4">
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 bg-gray-200 rounded-full flex-shrink-0"></span>
                    <button
                      onClick={() => navigate(`/project/${project.id}`)}
                      className="text-left hover:text-gray-900 truncate cursor-pointer"
                    >
                      {project.name}
                    </button>
                  </div>
                </td>
                <td className="w-1/3 p-4 text-gray-600 text-left">
                  <div className="flex items-center gap-2">
                    <span>{project.projectId}</span>
                    <button
                      onClick={() => handleCopy(project.projectId)}
                      className="p-1 hover:bg-gray-100 rounded-md transition-colors"
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-4 w-4 text-gray-400"
                        viewBox="0 0 20 20"
                        fill="currentColor"
                      >
                        <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                        <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                      </svg>
                    </button>
                  </div>
                </td>
                <td className="w-1/4 p-4 text-gray-600 text-left">
                  {project.date}
                </td>
                <td className="w-12 p-4">
                  <button
                    className="focus:outline-none cursor-pointer"
                  >
                    <img 
                      src={project.id === 1 ? deleteIcon : exitIcon} 
                      alt={project.id === 1 ? "DELETE" : "EXIT"} 
                      className="h-5 mt-2" 
                    />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      {/* 모달 */}
      {showModal && (
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
                onClick={closeModal}
                className="px-4 py-2 bg-gray-200 rounded-lg hover:bg-gray-300"
              >
                취소
              </button>
              <button
                onClick={handleSubmit}
                className="px-4 py-2 bg-black text-white rounded-lg hover:bg-gray-800"
              >
                {modalType === "add" ? "생성" : "참가"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProjectListPage;
