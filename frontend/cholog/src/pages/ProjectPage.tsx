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

const ProjectPage = () => {
  const { projectId } = useParams();
  const dispatch = useDispatch<AppDispatch>();
  const { logs, isLoading: logsLoading, error: logsError, pagination } = useSelector(
    (state: RootState) => state.log
  );
  const { projects, isLoading: projectLoading } = useSelector((state: RootState) => state.project);

  useEffect(() => {
    if (projectId) {
      dispatch(fetchProjectDetail(Number(projectId)));
      dispatch(fetchLogs({ projectId: Number(projectId) }));
      dispatch(fetchLogStats(Number(projectId)));
    }
  }, [dispatch, projectId]);

  // 현재 프로젝트 찾기
  const currentProject = projects.find(p => p.id === Number(projectId));

  const handleCopyClipBoard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      alert("프로젝트ID 복사 완료");
    } catch (e) {
      alert("프로젝트ID 복사 실패");
    }
  };

  // 로딩 중일 때 표시할 스켈레톤 UI
  if (projectLoading || logsLoading) {
    return (
      <div className="flex flex-col w-full lg:w-[70vw] mx-auto animate-pulse">
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

  // 에러 발생 시 표시할 UI
  if (logsError) {
    return (
      <div className="flex flex-col w-full lg:w-[70vw] mx-auto">
        <ProjectNavBar />
        <div className="flex flex-col items-center justify-center h-[60vh]">
          <div className="text-xl text-red-500 mb-2">오류가 발생했습니다</div>
          <div className="text-gray-500">{logsError.message}</div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col w-full lg:w-[70vw] mx-auto">
      <ProjectNavBar />
      <div className="flex flex-row justify-between">
        <div className="flex flex-row items-center gap-2 font-[paperlogy5]">
          <div className="text-[24px] text-slate-500">
            {currentProject?.name || "프로젝트를 찾을 수 없습니다"}
          </div>
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
      {logs.length > 0 ? (
        <LogList logs={logs} pagination={pagination} />
      ) : (
        <div className="flex flex-col items-center justify-center h-48 bg-white/5 rounded-2xl border border-[var(--line)]">
          <div className="text-xl text-[#5EA500] mb-2">로그가 없습니다</div>
          <div className="text-gray-500">아직 수집된 로그가 없습니다.</div>
        </div>
      )}
    </div>
  );
};

export default ProjectPage;
