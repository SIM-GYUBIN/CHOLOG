import { useParams } from "react-router-dom";
import ProjectNavBar from "../components/projectNavbar";
const ProjectSettingPage = () => {
  
  const { projectId } = useParams();

  console.log(
    '%c Project ID: %c' + projectId,
    'background: #3498db; color: white; padding: 2px 4px; border-radius: 4px;',
    'color: #e74c3c; font-weight: bold;'
  );

  return (
    <div className="w-full lg:w-[70vw] mx-auto">
          <div>

          <ProjectNavBar />
          </div>
          <div className="text-[28px] font-[paperlogy6] my-6">Project Setting</div>
    </div>
  );
};

export default ProjectSettingPage;
