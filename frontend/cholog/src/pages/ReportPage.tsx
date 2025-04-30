import React from 'react';
import { useParams } from 'react-router-dom';

const ReportPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();

  return (
    <div>
      Report for ID: {id}
    </div>
  );
};

export default ReportPage;
