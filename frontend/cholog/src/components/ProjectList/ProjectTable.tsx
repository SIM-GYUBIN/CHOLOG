// src/components/ProjectTable.tsx

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAppDispatch } from "../../hooks/useAppDispatch";
import exitIcon from "@/assets/exit.svg";
import deleteIcon from "@/assets/delete.svg";
import copyIcon from "@/assets/copy.svg";
import modifyIcon from "@/assets/modify.svg";
import ModifyProjectModal from "./ModifyProjectModal";
import {
  deleteProject,
  leaveProject,
  updateProject,
} from "../../store/slices/projectSlice";

interface Project {
  id: number;
  name: string;
  projectToken: string;
  createdAt: string;
  isCreator: boolean;
}

interface ProjectTableProps {
  projects: Project[];
  onCopy: (text: string) => void;
  isLoading: boolean;
  error: any;
}

const ProjectTable = ({
  projects,
  onCopy,
  isLoading,
  error,
}: ProjectTableProps) => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [showModifyModal, setShowModifyModal] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [newProjectName, setNewProjectName] = useState("");
  const [modalType, setModalType] = useState<"delete" | "leave" | null>(null);

  const handleCopy = (projectId: string) => {
    onCopy(projectId);
    alert("프로젝트 ID가 복사되었습니다.");
  };

  const handleModifyClick = (project: Project) => {
    setSelectedProject(project);
    setNewProjectName(project.name);
    setShowModifyModal(true);
  };

  const handleModifySubmit = async () => {
    if (!selectedProject?.id) return;

    try {
      const result = await dispatch(
        updateProject({
          projectId: selectedProject.id,
          name: newProjectName.trim(),
        })
      ).unwrap();

      if (result.success) {
        alert("프로젝트 이름이 수정되었습니다.");
        window.location.reload();
      }
    } catch (error: any) {
      alert("프로젝트 이름 수정에 실패했습니다.");
    } finally {
      setShowModifyModal(false);
      setSelectedProject(null);
      setNewProjectName("");
    }
  };

  const handleConfirm = async () => {
    if (!selectedProject?.id) return;

    try {
      let result;
      if (modalType === "delete") {
        result = await dispatch(
          deleteProject({ projectId: selectedProject.id })
        ).unwrap();
        if (result) alert("프로젝트가 삭제되었습니다.");
      } else if (modalType === "leave") {
        result = await dispatch(
          leaveProject({ projectId: selectedProject.id })
        ).unwrap();
        if (result) alert("프로젝트에서 탈퇴했습니다.");
      }

      if (result) window.location.reload();
    } catch (error: any) {
      alert(
        modalType === "delete"
          ? "프로젝트 삭제에 실패했습니다."
          : "프로젝트 탈퇴에 실패했습니다."
      );
    } finally {
      setShowConfirmModal(false);
      setSelectedProject(null);
      setModalType(null);
    }
  };

  if (isLoading) {
    return (
      <div className="w-full p-8 text-center">
        <div className="animate-pulse text-gray-500">로딩중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full p-8 text-center bg-white rounded-xl">
        <div className="text-red-500 font-medium mb-2">
          데이터를 불러올 수 없습니다
        </div>
        <button
          onClick={() => window.location.reload()}
          className="text-gray-500 hover:text-gray-700 underline"
        >
          다시 시도
        </button>
      </div>
    );
  }

  if (!projects?.length) {
    return (
      <div className="w-full p-8 text-center bg-white rounded-xl">
        <div className="text-[#5EA500] font-medium mb-2">
          프로젝트가 없습니다
        </div>
        <div className="text-gray-500">
          새 프로젝트를 생성하거나 참여해보세요
        </div>
      </div>
    );
  }

  const handleActionClick = (project: Project, type: "delete" | "leave") => {
    setSelectedProject(project);
    setModalType(type);
    setShowConfirmModal(true);
  };

  if (isLoading) {
    return (
      <div className="w-full">
        <table className="w-full">
          <thead>
            <tr className="border-b-2 border-[#5EA500]">
              <th className="w-1/3 p-4 font-paperlogy6 text-[var(--text)] text-left text-xl">
                프로젝트명
              </th>
              <th className="w-1/3 p-4 font-paperlogy6 text-[var(--text)] text-left text-xl">
                프로젝트 ID
              </th>
              <th className="w-1/4 p-4 font-paperlogy6 text-[var(--text)] text-left text-xl">
                생성일
              </th>
              <th className="w-12 p-4"></th>
            </tr>
          </thead>
          <tbody>
            {[1, 2, 3].map((i) => (
              <tr key={i} className="border-b border-[var(--line)]">
                <td className="w-1/3 p-4">
                  <div className="animate-pulse h-6 bg-gray-200 rounded w-3/4"></div>
                </td>
                <td className="w-1/3 p-4">
                  <div className="animate-pulse h-6 bg-gray-200 rounded w-1/2"></div>
                </td>
                <td className="w-1/4 p-4">
                  <div className="animate-pulse h-6 bg-gray-200 rounded w-2/3"></div>
                </td>
                <td className="w-12 p-4">
                  <div className="animate-pulse h-6 w-6 bg-gray-200 rounded-full"></div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full min-h-[200px] flex flex-col items-center justify-center p-8 bg-white rounded-xl">
        <div className="text-red-500 font-paperlogy6 text-xl mb-2">
          오류가 발생했습니다
        </div>
        <div className="text-gray-500 font-paperlogy4 text-base">
          {error.message || "데이터를 불러오는 중 문제가 발생했습니다"}
        </div>
      </div>
    );
  }

  if (!projects || projects.length === 0) {
    return (
      <div className="w-full min-h-[200px] flex flex-col items-center justify-center p-8 bg-white rounded-xl">
        <div className="text-[#5EA500] font-paperlogy6 text-xl mb-2">
          아직 프로젝트가 없습니다
        </div>
        <div className="text-gray-500 font-paperlogy4 text-base">
          새로운 프로젝트를 생성하거나 가입하세요.
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="w-full overflow-x-auto">
        <table className="w-full min-w-[800px]">
          {" "}
          {/* 최소 너비 설정 */}
          <thead>
            <tr className="border-b-2 border-[#5EA500] animate-fadeIn">
              <th className="w-1/3 p-2 sm:p-4 font-paperlogy6 text-[var(--text)] text-left text-base sm:text-xl">
                프로젝트명
              </th>
              <th className="w-1/3 p-2 sm:p-4 font-paperlogy6 text-[var(--text)] text-left text-base sm:text-xl">
                프로젝트 ID
              </th>
              <th className="w-1/4 p-2 sm:p-4 font-paperlogy6 text-[var(--text)] text-left text-base sm:text-xl">
                생성일
              </th>
              <th className="w-12 p-2 sm:p-4"></th>
            </tr>
          </thead>
          <tbody>
            {projects.map((project, index) => (
              <tr
                key={project.id}
                className="border-b border-[var(--line)] hover:bg-[#5EA50008] transition-colors animate-fadeIn"
                style={{ animationDelay: `${index * 100}ms` }}
              >
                <td className="w-1/3 p-2 sm:p-4">
                  <div className="flex items-center gap-1 sm:gap-2">
                    <button
                      onClick={() => navigate(`/project/${project.id}`)}
                      className="text-left text-sm sm:text-[16px] text-[var(--text)] font-paperlogy5 hover:text-[#5EA500] truncate cursor-pointer transition-colors max-w-[150px] sm:max-w-[200px]"
                    >
                      {project.name}
                    </button>
                    {project.isCreator && (
                      <button
                        className="p-1 hover:bg-[#5EA50015] rounded-md transition-colors flex-shrink-0"
                        onClick={() => handleModifyClick(project)}
                      >
                        <img
                          src={modifyIcon}
                          alt="Modify"
                          className="h-3 sm:h-3.5 w-3 sm:w-3.5"
                        />
                      </button>
                    )}
                  </div>
                </td>
                <td className="w-1/4 p-2 sm:p-4 font-paperlogy4 text-[var(--helpertext)] text-left text-sm sm:text-[16px]">
                  <div className="flex items-center gap-1 sm:gap-2">
                    <span className="truncate max-w-[100px] sm:max-w-[150px]">
                      {project.projectToken}
                    </span>
                    <button
                      onClick={() => handleCopy(project.projectToken)}
                      className="p-1 hover:bg-[#5EA50015] rounded-md transition-colors flex-shrink-0"
                    >
                      <img
                        src={copyIcon}
                        alt="Copy"
                        className="h-3.5 sm:h-4 w-3.5 sm:w-4"
                      />
                    </button>
                  </div>
                </td>
                <td className="w-1/4 p-2 sm:p-4 font-paperlogy4 text-[var(--text)] text-left text-sm sm:text-[16px]">
                  {new Date(project.createdAt).toLocaleString("ko-KR", {
                    year: "numeric",
                    month: "2-digit",
                    day: "2-digit",
                  })}
                </td>
                <td className="w-1/4">
                  <button
                    className="p-1 sm:p-1.5 hover:bg-[#5EA50015] rounded-md transition-colors focus:outline-none cursor-pointer"
                    onClick={() =>
                      handleActionClick(
                        project,
                        project.isCreator ? "delete" : "leave"
                      )
                    }
                  >
                    <img
                      src={project.isCreator ? deleteIcon : exitIcon}
                      alt={project.isCreator ? "Delete" : "Exit"}
                      className={`${project.isCreator ? "h-2.5 sm:h-3" : "h-4 sm:h-5"}`}
                    />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showModifyModal && selectedProject && (
        <ModifyProjectModal
          showModal={showModifyModal}
          projectName={newProjectName}
          projectId={selectedProject?.id}
          setProjectName={setNewProjectName}
          onClose={() => {
            setShowModifyModal(false);
            setSelectedProject(null);
            setNewProjectName("");
          }}
          onSubmit={handleModifySubmit}
        />
      )}

      {showConfirmModal && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-sm w-full">
            <h2 className="text-xl font-paperlogy6 mb-4">
              {modalType === "delete" ? "프로젝트 삭제" : "프로젝트 탈퇴"}
            </h2>
            <p className="text-gray-600 mb-6">
              {modalType === "delete"
                ? "정말로 이 프로젝트를 삭제하시겠습니까?"
                : "정말로 이 프로젝트에서 탈퇴하시겠습니까?"}
            </p>
            <div className="flex justify-end gap-2">
              <button
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-md transition-colors"
                onClick={() => {
                  setShowConfirmModal(false);
                  setSelectedProject(null);
                  setModalType(null);
                }}
              >
                취소
              </button>
              <button
                className="px-4 py-2 bg-red-500 text-white hover:bg-red-600 rounded-md transition-colors"
                onClick={handleConfirm}
              >
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default ProjectTable;
