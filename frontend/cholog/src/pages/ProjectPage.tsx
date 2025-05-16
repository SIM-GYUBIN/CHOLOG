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

const ProjectPage = () => {
  const { projectId } = useParams();
  const dispatch = useDispatch<AppDispatch>();
  const { logs, isLoading, error, pagination } = useSelector(
    (state: RootState) => state.log
  );

  const projectName = "프로젝트이름이다";

  useEffect(() => {
    if (projectId) {
      dispatch(fetchLogs({ projectId: Number(projectId) }));
      dispatch(fetchLogStats(Number(projectId)));
    }
  }, [dispatch, projectId]);

  const handleCopyClipBoard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      alert("프로젝트ID 복사 완료");
    } catch (e) {
      alert("프로젝트ID 복사 실패");
    }
  };

  return (
    <div className="flex flex-col w-full lg:w-[70vw] mx-auto ">
      <ProjectNavBar />
      <div className="flex flex-row justify-between ">
        <div className="flex flex-row items-center gap-2 font-[paperlogy5]">
          <div className=" text-[24px] text-slate-500">{projectName}</div>
          <div className="text-[20px] text-slate-300">{projectId}</div>
          <div
            className="rounded-sm p-1 cursor-pointer hover:bg-gray-200"
            onClick={() => handleCopyClipBoard(projectId || "")}
          >
            <img src={copy} alt="복사" className="w-5 h-5" />
          </div>
        </div>
      </div>
      <div className="grid grid-cols-7 py-5 gap-10">
        <div className="col-span-3">
          <LogSummary />
        </div>
        <div className="col-span-4">
          <ErrorChart projectId={Number(projectId)} />
        </div>
      </div>
      <LogList logs={logs} pagination={pagination} />
    </div>
  );
};

export default ProjectPage;
