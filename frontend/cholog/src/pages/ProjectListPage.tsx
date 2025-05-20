import { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { AppDispatch, RootState } from "../store/store";
import {
  fetchProjects,
  createProject,
  joinProject,
} from "../store/slices/projectSlice";
import logo from "@/assets/logo2.svg";
import ProjectActions from "../components/ProjectList/ProjectActions";
import ProjectTable from "../components/ProjectList/ProjectTable";
import ProjectModal from "../components/ProjectList/ProjectModal";
import { motion } from "framer-motion";

const ProjectListPage = () => {
  const dispatch = useDispatch<AppDispatch>();
  const { projects, isLoading, error } = useSelector(
    (state: RootState) => state.project
  );
  const [showModal, setShowModal] = useState(false);
  const [modalType, setModalType] = useState<"add" | "join" | null>(null);
  const [inputValue, setInputValue] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    dispatch(fetchProjects());
  }, [dispatch]);

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  const handleSearch = (term: string) => {
    setSearchTerm(term);
  };

  const filteredProjects =
    projects?.filter(
      (project) =>
        project?.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        project?.projectToken?.toLowerCase().includes(searchTerm.toLowerCase())
    ) || [];

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
    if (isSubmitting) return;
    setIsSubmitting(true);

    try {
      if (modalType === "add") {
        const result = await dispatch(
          createProject({ name: inputValue, token: "" })
        ).unwrap();
        if (result.success) {
          dispatch(fetchProjects());
          alert("프로젝트가 성공적으로 생성되었습니다.");
        }
      } else if (modalType === "join") {
        const result = await dispatch(
          joinProject({ projectToken: inputValue })
        ).unwrap();
        if (result.success) {
          dispatch(fetchProjects());
          alert("프로젝트에 성공적으로 참가했습니다.");
        }
      }
    } catch (error: any) {
      alert(error.message || "작업 중 오류가 발생했습니다.");
    }

    closeModal();
    setIsSubmitting(false);
  };

  return (
    <motion.div
      className="max-w-[65vw] mx-auto"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: "easeOut" }}
    >
      <motion.div
        className="text-center my-18"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.2 }}
      >
        <img src={logo} alt="Cholog logo" className="h-12 mx-auto" />
      </motion.div>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.3 }}
      >
        <ProjectActions
          onAdd={() => openModal("add")}
          onJoin={() => openModal("join")}
          onSearch={handleSearch}
        />
      </motion.div>

      <motion.section
        className="mt-8"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4 }}
      >
        <ProjectTable
          projects={filteredProjects}
          onCopy={handleCopy}
          isLoading={isLoading}
          error={error}
        />
      </motion.section>

      <ProjectModal
        showModal={showModal}
        modalType={modalType}
        inputValue={inputValue}
        setInputValue={setInputValue}
        onClose={closeModal}
        onSubmit={handleSubmit}
        isSubmitting={isSubmitting}
      />
    </motion.div>
  );
};

export default ProjectListPage;
