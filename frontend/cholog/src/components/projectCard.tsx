interface ProjectCardProps {
  id: number;
  name: string;
  status: string;
  lastLog: string;
}

import { useNavigate } from 'react-router-dom';

const ProjectCard = ({ id, name, status, lastLog }: ProjectCardProps) => {
  const navigate = useNavigate();

  return (
    <div
      className="p-4 border border-gray-200 rounded-2xl hover:shadow-md transition-shadow cursor-pointer"
      onClick={() => navigate(`/project/${id}`)}
    >
      <div className="flex justify-between items-center mb-4">
        <h3 className="font-paperlogy5 text-18px">{name}</h3>
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-4 w-4 text-gray-400"
          viewBox="0 0 20 20"
          fill="currentColor"
        >
          <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
          <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
        </svg>
      </div>
      <div className="space-y-2">
        <div className="flex items-center gap-2 text-gray-600 text-14px">
          <span>프로젝트 ID:</span>
          <div className="flex items-center gap-1">
            <span>{id}</span>
          </div>
        </div>
        <div className="flex items-center gap-2 text-gray-600 text-14px">
          <span>최근로그:</span>
          <div className="flex items-center gap-1">
            <span>{lastLog}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProjectCard;
