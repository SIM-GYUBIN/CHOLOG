import { useParams } from 'react-router-dom';

const ProjectPage = () => {
  const { projectId } = useParams();

  return (
    <div>
      Project Page
      <p>Project ID: {projectId}</p>
    </div>
  );
};

export default ProjectPage;
