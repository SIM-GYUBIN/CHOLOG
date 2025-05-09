import { useState } from "react";
import { useNavigate } from "react-router-dom";
import exitIcon from "@/assets/exit.svg";
import deleteIcon from "@/assets/delete.svg";
import copyIcon from "@/assets/copy.svg";
import modifyIcon from "@/assets/modify.svg";
import ModifyProjectModal from "./ModifyProjectModal";

interface Project {
  id: number;
  name: string;
  projectId: string;
  date: string;
  isCreator: boolean; // 생성자 여부 추가
}

interface ProjectTableProps {
  projects: Project[];
  onCopy: (text: string) => void;
}

const ProjectTable = ({ projects, onCopy }: ProjectTableProps) => {
  const navigate = useNavigate();
  const [showModifyModal, setShowModifyModal] = useState(false);
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [newProjectName, setNewProjectName] = useState("");

  const handleCopy = (projectId: string) => {
    onCopy(projectId);
    alert("프로젝트 ID가 복사되었습니다.");
  };

  const handleModifyClick = (project: Project) => {
    setSelectedProject(project);
    setNewProjectName(project.name);
    setShowModifyModal(true);
  };

  const handleModifySubmit = () => {
    // TODO: API 호출하여 프로젝트명 수정
    setShowModifyModal(false);
    setSelectedProject(null);
    setNewProjectName("");
  };

  return (
    <>
      <table className="w-full">
        <thead>
          <tr className="border-b border-[var(--line)]">
            <th className="w-1/3 p-4 font-medium text-[var(--text)] text-left text-xl">
              프로젝트명
            </th>
            <th className="w-1/3 p-4 font-medium text-[var(--text)] text-left text-xl">
              프로젝트 ID
            </th>
            <th className="w-1/4 p-4 font-medium text-[var(--text)] text-left text-xl">
              생성 시간
            </th>
            <th className="w-12 p-4"></th>
          </tr>
        </thead>
        <tbody>
          {projects.map((project) => (
            <tr key={project.id} className="border-b border-[var(--line)]">
              <td className="w-1/3 p-4">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => navigate(`/project/${project.id}`)}
                    className="text-left text-[16px] text-[var(--text)] hover:text-gray-900 truncate cursor-pointer"
                  >
                    {project.name}
                  </button>
                  {project.isCreator && (
                    <button
                      className="p-1 hover:bg-gray-100 rounded-md transition-colors"
                      onClick={() => handleModifyClick(project)}
                    >
                      <img
                        src={modifyIcon}
                        alt="Modify"
                        className="h-3.5 w-3.5"
                      />
                    </button>
                  )}
                </div>
              </td>
              <td className="w-1/3 p-4 text-[var(--helpertext)] text-left text-[16px]">
                <div className="flex items-center gap-2">
                  <span>{project.projectId}</span>
                  <button
                    onClick={() => handleCopy(project.projectId)}
                    className="p-1 hover:bg-gray-100 rounded-md transition-colors"
                  >
                    <img src={copyIcon} alt="Copy" className="h-4 w-4" />
                  </button>
                </div>
              </td>
              <td className="w-1/4 p-4 text-[var(--text)] text-left text-[16px]">
                {project.date}
              </td>
              <td className="w-12 p-4">
                <button className="focus:outline-none cursor-pointer">
                  <img
                    src={project.isCreator ? deleteIcon : exitIcon}
                    alt={project.isCreator ? "Delete" : "Exit"}
                    className={`mt-2 mx-1 ${project.isCreator ? "h-3" : "h-5"}`}
                  />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <ModifyProjectModal
        showModal={showModifyModal}
        projectName={newProjectName}
        setProjectName={setNewProjectName}
        onClose={() => {
          setShowModifyModal(false);
          setSelectedProject(null);
          setNewProjectName("");
        }}
        onSubmit={handleModifySubmit}
      />
    </>
  );
};

export default ProjectTable;
