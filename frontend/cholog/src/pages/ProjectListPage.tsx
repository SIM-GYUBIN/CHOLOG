import { useState } from "react";
import logo from '@/assets/logo.svg';
import RecentProjects from "../components/ProjectList/RecentProjects";
import ProjectActions from "../components/ProjectList/ProjectActions";
import ProjectTable from "../components/ProjectList/ProjectTable";
import ProjectModal from "../components/ProjectList/ProjectModal";

const ProjectListPage = () => {
  const [showModal, setShowModal] = useState(false);
  const [modalType, setModalType] = useState<"add" | "join" | null>(null);
  const [inputValue, setInputValue] = useState("");

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
      <div className="text-center">
        <img src={logo} alt="Cholog logo" className="h-36 mx-auto" />
      </div>

      <RecentProjects projects={recentProjects} />
      <ProjectActions onAdd={() => openModal("add")} onJoin={() => openModal("join")} />
      <section className="mt-8">
        <ProjectTable projects={projectList} onCopy={handleCopy} />
      </section>
      <ProjectModal
        showModal={showModal}
        modalType={modalType}
        inputValue={inputValue}
        setInputValue={setInputValue}
        onClose={closeModal}
        onSubmit={handleSubmit}
      />
    </div>
  );
};

export default ProjectListPage;
