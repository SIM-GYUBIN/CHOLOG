import { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "../store/store";
import { fetchProjects, createProject, joinProject } from "../store/slices/projectSlice";
import logo from "@/assets/logo2.svg";
import ProjectActions from "../components/ProjectList/ProjectActions";
import ProjectTable from "../components/ProjectList/ProjectTable";
import ProjectModal from "../components/ProjectList/ProjectModal";

const ProjectListPage = () => {
  const dispatch = useDispatch<AppDispatch>();
  const { projects, isLoading, error } = useSelector((state: RootState) => state.project);
  const [showModal, setShowModal] = useState(false);
  const [modalType, setModalType] = useState<"add" | "join" | null>(null);
  const [inputValue, setInputValue] = useState("");

  useEffect(() => {
    dispatch(fetchProjects());
  }, [dispatch]);

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

  const handleSubmit = async () => {
    if (modalType === "add") {
      try {
        const result = await dispatch(createProject({ name: inputValue, token: "" })).unwrap();
        if (result.success) {
          dispatch(fetchProjects());
          alert("프로젝트가 성공적으로 생성되었습니다.");
        }
      } catch (error: any) {
        alert(error.message || "프로젝트 생성 중 오류가 발생했습니다.");
      }
    } else if (modalType === "join") {
      try {
        const result = await dispatch(joinProject({ projectToken: inputValue })).unwrap();
        if (result.success) {
          dispatch(fetchProjects());
          alert("프로젝트에 성공적으로 참가했습니다.");
        }
      } catch (error: any) {
        alert(error.message || "프로젝트 참가 중 오류가 발생했습니다.");
      }
    }
    closeModal();
  };

  if (isLoading) {
    return <div className="text-center">로딩 중...</div>;
  }

  if (error) {
    return <div className="text-center text-red-500">{error.message}</div>;
  }

  return (
    <div className="max-w-[60vw] mx-auto">
      <div className="text-center my-18">
        <img src={logo} alt="Cholog logo" className="h-12 mx-auto" />
      </div>

      <ProjectActions
        onAdd={() => openModal("add")}
        onJoin={() => openModal("join")}
      />
      <section className="mt-8">
        <ProjectTable projects={projects} onCopy={handleCopy} />
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
