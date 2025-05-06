import ProjectCard from '../components/projectCard';

const ProjectListPage = () => {
  // 임시 프로젝트 데이터
  const recentProjects = [
    { name: "Project name", status: "정상", lastLog: "2025.04.28" },
    { name: "Project name", status: "정상", lastLog: "2025.04.28" },
    { name: "Project name", status: "정상", lastLog: "2025.04.28" },
    { name: "Project name", status: "정상", lastLog: "2025.04.28" },
  ];

  const projectList = [
    { name: "프로젝트명", projectId: "프로젝트ID", date: "방문 시간" },
    { name: "프로젝트명", projectId: "프로젝트ID", date: "방문 시간" },
    { name: "프로젝트명", projectId: "프로젝트ID", date: "방문 시간" },
    { name: "프로젝트명", projectId: "프로젝트ID", date: "방문 시간" },
    { name: "프로젝트명", projectId: "프로젝트ID", date: "방문 시간" },
  ];

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  return (
    <div className="p-8 max-w-7xl mx-auto">
      {/* 로고 섹션 */}
      <div className="text-center text-32px font-paperlogy7 mb-12">CHOLOG</div>

      {/* 최근 프로젝트 섹션 */}
      <section className="mb-12">
        <p className="text-left text-[28px] leading-tight tracking-wide font-paperlogy7 mb-6">
          최근 프로젝트
        </p>
        <div className="grid grid-cols-4 gap-4">
          {recentProjects.map((project, index) => (
            <ProjectCard
              key={index}
              name={project.name}
              status={project.status}
              lastLog={project.lastLog}
            />
          ))}
        </div>
      </section>

      {/* ADD, JOIN 버튼 섹션 */}
      <div className="flex justify-start gap-4 mb-8">
        <button className="px-6 py-2 bg-white text-black border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors font-paperlogy5">
          ADD
        </button>
        <button className="px-6 py-2 bg-white text-black border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors font-paperlogy5">
          JOIN
        </button>
      </div>

      {/* 프로젝트 리스트 섹션 */}
      <section className="mt-8">
        <table className="w-full">
          <thead>
            <tr className="border-b border-gray-200">
              <th className="w-1/3 p-4 font-paperlogy5 text-left">
                프로젝트명
              </th>
              <th className="w-1/3 p-4 font-paperlogy5 text-left">
                프로젝트 ID
              </th>
              <th className="w-1/4 p-4 font-paperlogy5 text-left">방문 시간</th>
              <th className="w-12 p-4"></th>
            </tr>
          </thead>
          <tbody>
            {projectList.map((project, index) => (
              <tr key={index} className="border-b border-gray-200">
                <td className="w-1/3 p-4">
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 bg-gray-200 rounded-full flex-shrink-0"></span>
                    <span className="truncate">{project.name}</span>
                  </div>
                </td>
                <td className="w-1/3 p-4 text-gray-600 text-left">
                  <div className="flex items-center gap-2">
                    <span>{project.projectId}</span>
                    <button
                      onClick={() => handleCopy(project.projectId)}
                      className="p-1 hover:bg-gray-100 rounded-md transition-colors"
                    >
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-4 w-4 text-gray-400"
                        viewBox="0 0 20 20"
                        fill="currentColor"
                      >
                        <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                        <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                      </svg>
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
      </section>
    </div>
  );
};

export default ProjectListPage;
