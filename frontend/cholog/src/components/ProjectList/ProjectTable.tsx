import { useNavigate } from 'react-router-dom';

interface Project {
  id: number;
  name: string;
  projectId: string;
  date: string;
}

interface ProjectTableProps {
  projects: Project[];
  onCopy: (text: string) => void;
}

const ProjectTable = ({ projects, onCopy }: ProjectTableProps) => {
  const navigate = useNavigate();

  return (
    <table className="w-full">
      <thead>
        <tr className="border-b border-gray-200">
          <th className="w-1/3 p-4 font-paperlogy5 text-left">프로젝트명</th>
          <th className="w-1/3 p-4 font-paperlogy5 text-left">프로젝트 ID</th>
          <th className="w-1/4 p-4 font-paperlogy5 text-left">생성 시간</th>
          <th className="w-12 p-4"></th>
        </tr>
      </thead>
      <tbody>
        {projects.map((project) => (
          <tr key={project.id} className="border-b border-gray-200">
            <td className="w-1/3 p-4">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => navigate(`/project/${project.id}`)}
                  className="text-left hover:text-gray-900 truncate cursor-pointer"
                >
                  {project.name}
                </button>
              </div>
            </td>
            <td className="w-1/3 p-4 text-gray-600 text-left">
              <div className="flex items-center gap-2">
                <span>{project.projectId}</span>
                <button
                  onClick={() => onCopy(project.projectId)}
                  className="p-1 hover:bg-gray-100 rounded-md transition-colors"
                >
                  {/* Copy SVG icon */}
                </button>
              </div>
            </td>
            <td className="w-1/4 p-4 text-gray-600 text-left">
              {project.date}
            </td>
            <td className="w-12 p-4">
              <button className="text-gray-400 hover:text-gray-600">
                →
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};

export default ProjectTable;