import { useParams } from 'react-router-dom';

const ArchivePage = () => {
  const { id } = useParams();
  
  return (
    <div>
      {id}
    </div>
  );
};

export default ArchivePage;
