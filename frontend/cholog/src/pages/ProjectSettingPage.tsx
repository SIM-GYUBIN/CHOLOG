import { useParams } from "react-router-dom";

const ProjectSettingPage = () => {
  
  const { projectId } = useParams();

  console.log(
    '%c Project ID: %c' + projectId,
    'background: #3498db; color: white; padding: 2px 4px; border-radius: 4px;',
    'color: #e74c3c; font-weight: bold;'
  );

  return (
    <div>
      프로젝트 세팅 페이지
    </div>
  );
};

export default ProjectSettingPage;
