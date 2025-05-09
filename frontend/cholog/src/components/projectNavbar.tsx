import { useNavigate, useParams } from 'react-router-dom';
import dashboardIcon from '../assets/navicon/dashboard.svg';
import archiveIcon from '../assets/navicon/archive.svg';
import reportIcon from '../assets/navicon/report.svg';
import settingIcon from '../assets/navicon/setting.svg';

export default function ProjectNavBar() {
  const navigate = useNavigate();
  const { projectId } = useParams();

  const isActive = (path: string) => {
    return window.location.pathname.includes(path);
  };

  return (
    <nav className="w-full inline-flex justify-end items-center gap-4 py-4">
      <button 
        onClick={() => navigate(`/project/${projectId}`)}
        className={`flex justify-start items-center gap-2.5 ${
          isActive('/project/') && !isActive('/archives') && !isActive('/report') && !isActive('/setting')
            ? 'text-green-600 font-bold' 
            : 'text-slate-400'
        }`}
      >
        <img 
          src={dashboardIcon} 
          alt="dashboard" 
          className={`w-4 h-4 ${
            isActive('/project/') && !isActive('/archives') && !isActive('/report') && !isActive('/setting')
              ? 'font-green-600 font-bold': ''
          }`}
        />
        <span className="text-[12px]">Dashboard</span>
      </button>

      <button 
        onClick={() => navigate(`/project/${projectId}/archives`)}
        className={`flex justify-start items-center gap-2.5 ${
          isActive('/archives')
            ? 'text-green-600 font-bold' 
            : 'text-slate-400'
        }`}
      >
        <img 
          src={archiveIcon} 
          alt="archive" 
          className={`w-4 h-4 ${
            isActive('/archives')
              ? 'brightness-0 saturate-100 invert-[0.39] sepia-[0.91] saturate-[17.7] hue-rotate-[89deg] brightness-[0.97] contrast-[0.91]'
              : ''
          }`}
        />
        <span className="text-[12px]">Archive</span>
      </button>

      <button 
        onClick={() => navigate(`/report/${projectId}`)}
        className={`flex justify-start items-center gap-2.5 ${
          isActive('/report')
            ? 'text-green-600 font-bold' 
            : 'text-slate-400'
        }`}
      >
        <img 
          src={reportIcon} 
          alt="report" 
          className={`w-4 h-4 ${
            isActive('/report')
              ? 'brightness-0 saturate-100 invert-[0.39] sepia-[0.91] saturate-[17.7] hue-rotate-[89deg] brightness-[0.97] contrast-[0.91]'
              : ''
          }`}
        />
        <span className="text-[12px]">Report</span>
      </button>

      <button 
        onClick={() => navigate(`/projectsetting/${projectId}`)}
        className={`flex justify-start items-center gap-2.5 ${
          isActive('/setting')
            ? 'text-green-600 font-bold' 
            : 'text-slate-400'
        }`}
      >
        <img 
          src={settingIcon} 
          alt="setting" 
          className={`w-4 h-4 ${
            isActive('/setting')
              ? 'brightness-0 saturate-100 invert-[0.39] sepia-[0.91] saturate-[17.7] hue-rotate-[89deg] brightness-[0.97] contrast-[0.91]'
              : ''
          }`}
        />
        <span className="text-[12px]">Setting</span>
      </button>
    </nav>
  );
}
