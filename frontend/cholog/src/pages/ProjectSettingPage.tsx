import { useParams, useNavigate } from "react-router-dom";
import { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import ProjectNavBar from "../components/projectNavbar";
import { AppDispatch, RootState } from "../store/store";
import { updateProject, leaveProject } from "../store/slices/projectSlice";

const ProjectSettingPage = () => {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch<AppDispatch>();
  const { projects, isLoading } = useSelector((state: RootState) => state.project);
  
  // 현재 프로젝트 찾기
  const currentProject = projects.find(p => p.id === Number(projectId));
  
  const [projectName, setProjectName] = useState(currentProject?.name || "");
  const [isEditing, setIsEditing] = useState(false);
  const [isWebhookModalOpen, setIsWebhookModalOpen] = useState(false);

  const handleUpdateName = async () => {
    if (!projectId || !projectName.trim()) return;

    try {
      const result = await dispatch(
        updateProject({ projectId: Number(projectId), name: projectName.trim() })
      ).unwrap();

      if (result.success) {
        setIsEditing(false);
        alert("프로젝트 이름이 성공적으로 수정되었습니다.");
      }
    } catch (error: any) {
      alert(error.message || "프로젝트 이름 수정 중 오류가 발생했습니다.");
    }
  };

  const handleLeaveProject = async () => {
    if (!projectId) return;

    if (window.confirm("프로젝트에서 탈퇴하면 더 이상 프로젝트의 로그를 볼 수 없습니다.\n정말로 프로젝트를 탈퇴하시겠습니까?")) {
      try {
        const result = await dispatch(
          leaveProject({ projectId: Number(projectId) })
        ).unwrap();

        if (result.success) {
          alert("프로젝트에서 탈퇴했습니다.");
          navigate("/projects");
        }
      } catch (error: any) {
        alert(error.message || "프로젝트 탈퇴 중 오류가 발생했습니다.");
      }
    }
  };

  if (isLoading) {
    return <div>로딩 중...</div>;
  }

  return (
    <div className="w-full lg:w-[70vw] mx-auto">
      <ProjectNavBar />
      <div className="text-[28px] font-[paperlogy6] my-6 text-slate-700">Project Setting</div>
      
      <div className="w-[60%] mx-auto p-6">
        <div className="mb-8">
          {/* <div className="flex justify-center gap-1">
            <input
              type="text"
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              className="text-[14px] px-3 py-2 rounded-lg border border-[var(--line)]"
              placeholder={currentProject?.name || "프로젝트 이름을 입력하세요"}
            />
            <button
              onClick={handleUpdateName}
              className="text-[12px] px-3 py-2 bg-lime-600 text-white rounded-lg hover:bg-lime-700"
            >
              저장
            </button>
          </div> */}
        </div>

        {/* <div className="border-slate-200  mb-8">
          <button
            onClick={() => setIsWebhookModalOpen(true)}
            className="flex items-center gap-1 text-[12px] px-3 py-2 text-lime-500 rounded-lg hover:border border-lime-500"
          >
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            웹훅 등록하기
          </button>
        </div> */}
      </div>

      {/* <WebhookModal
        isOpen={isWebhookModalOpen}
        onClose={() => setIsWebhookModalOpen(false)}
        webhookData={currentProject?.webhook}
        onSave={handleWebhookSave}
      /> */}
                <button
            onClick={handleLeaveProject}
            className="text-[12px] px-3 py-2 text-red-600 rounded-lg hover:border border-red-300"
            >
            프로젝트 탈퇴
          </button>
    </div>
    
  );
};

export default ProjectSettingPage;

const handleWebhookSave = async (webhookData: any) => {
  // 웹훅 설정 저장 로직 구현
  console.log('Webhook data:', webhookData);
};
