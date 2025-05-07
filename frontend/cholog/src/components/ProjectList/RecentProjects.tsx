import ProjectCard from "../ProjectCard";

interface Project {
  id: number;
  name: string;
  status: string;
  lastLog: string;
}

interface RecentProjectsProps {
  projects: Project[];
}

const RecentProjects = ({ projects }: RecentProjectsProps) => {
  return (
    <section className="mb-12">
      <p className="text-left text-[28px] leading-tight tracking-wide font-paperlogy7 mb-6">
        최근 프로젝트
      </p>
      <div className="grid grid-cols-4 gap-4">
        {projects.map((project) => (
          <ProjectCard
            key={project.id}
            id={project.id}
            name={project.name}
            status={project.status}
            lastLog={project.lastLog}
          />
        ))}
      </div>
    </section>
  );
};

export default RecentProjects;