import { useParams } from "react-router-dom";
import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import LogList from "../components/logList";
import copy from "@/assets/copy.svg";
import ErrorChart from "../components/charts/ErrorChart";
import LogSummary from "../components/LogSummary";
import { fetchLogStats, fetchLogs } from "../store/slices/logSlice";
import { AppDispatch, RootState } from "../store/store";
import ProjectNavBar from "../components/projectNavbar";
import { fetchProjectDetail } from "../store/slices/projectSlice";
import { motion } from "framer-motion";

const ProjectPage = () => {
  const { projectId } = useParams();
  const dispatch = useDispatch<AppDispatch>();
  const {
    logs,
    isLoading: logsLoading,
    error: logsError,
    pagination,
  } = useSelector((state: RootState) => state.log);
  const { projects, isLoading: projectLoading } = useSelector(
    (state: RootState) => state.project
  );

  useEffect(() => {
    if (projectId) {
      dispatch(fetchProjectDetail(Number(projectId)));
      dispatch(fetchLogs({ projectId: Number(projectId) }));
      dispatch(fetchLogStats(Number(projectId)));
    }
  }, [dispatch, projectId]);

  const currentProject = projects.find((p) => p.id === Number(projectId));

  const handleCopyClipBoard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      alert("프로젝트ID 복사 완료");
    } catch (e) {
      alert("프로젝트ID 복사 실패");
    }
  };

  if (projectLoading || logsLoading) {
    return (
      <div className="max-w-[60vw] mx-auto">
        <ProjectNavBar />
        <div className="flex flex-row justify-between">
          <div className="flex flex-row items-center gap-2">
            <div className="h-8 w-48 bg-slate-200 rounded"></div>
            <div className="h-6 w-24 bg-slate-200 rounded"></div>
          </div>
        </div>
        <div className="grid grid-cols-7 py-5 gap-10">
          <div className="col-span-3">
            <div className="h-40 bg-slate-200 rounded-2xl"></div>
          </div>
          <div className="col-span-4">
            <div className="h-40 bg-slate-200 rounded-2xl"></div>
          </div>
        </div>
        <div className="h-96 bg-slate-200 rounded-2xl mt-4"></div>
      </div>
    );
  }

  if (logsError) {
    return (
      <div className="max-w-[60vw] mx-auto">
        <ProjectNavBar />
        <div className="flex flex-col items-center justify-center h-[60vh]">
          <div className="text-xl text-red-500 mb-2">오류가 발생했습니다</div>
          <div className="text-gray-500">{logsError.message}</div>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      className="w-full min-w-[320px] max-w-[65vw] mx-auto px-4 lg:px-0"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
    >
      <ProjectNavBar />

      <motion.div
        className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 sm:gap-0"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
      >
        <div className="flex flex-row items-center gap-2 font-[paperlogy5] flex-wrap">
          <div className="text-[20px] sm:text-[24px] text-slate-500">
            {currentProject?.name || "프로젝트를 찾을 수 없습니다"}
          </div>
          {/* <div className="text-[16px] sm:text-[20px] text-slate-300">
            {currentProject?.projectToken}
          </div> */}
          <div
            className="rounded-sm p-1 cursor-pointer hover:bg-gray-200"
            onClick={() =>
              handleCopyClipBoard(currentProject?.projectToken || "")
            }
          >
            <img src={copy} alt="복사" className="w-4 sm:w-5 h-4 sm:h-5" />
          </div>
        </div>
      </motion.div>

      <motion.div
        className="grid grid-cols-1 lg:grid-cols-7 py-3 sm:py-5 gap-4 lg:gap-10"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
      >
        <div className="lg:col-span-3">
          <LogSummary />
        </div>
        <div className="lg:col-span-4">
          <ErrorChart projectId={Number(projectId)} />
        </div>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.35 }}
      >
        <LogList logs={logs} pagination={pagination} />
      </motion.div>
    </motion.div>
  );
};

export default ProjectPage;
