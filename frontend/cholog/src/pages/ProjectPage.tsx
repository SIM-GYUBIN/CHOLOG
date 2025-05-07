import { useParams } from 'react-router-dom';
import LogList from '../components/logList';

const ProjectPage = () => {
  const { projectId } = useParams();

  return (
    <div>
      Project Page
      <p>Project ID: {projectId}</p>
      <LogList />
    </div>
  );
};

export default ProjectPage;
