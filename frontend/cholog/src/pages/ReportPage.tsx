import React from 'react';
import { useParams } from 'react-router-dom';
import DonutChart from '../components/charts/DonutChart';
import ErrorCountChart from '../components/charts/ErrorChart';
import RankingCardList from '../components/common/RankingCardList';

const ReportPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();

  const logData = [
    { name: 'ERROR', value: 400, color: '#EF4444' },
    { name: 'WARN', value: 300, color: '#F59E0B' },
    { name: 'INFO', value: 1200, color: '#3B82F6' },
    { name: 'DEBUG', value: 800, color: '#10B981' }
  ];

  const topErrors = [
    { name: 'NullPointerException', count: 156 },
    { name: 'InvalidArgumentException', count: 89 },
    { name: 'TimeoutException', count: 45 }
  ];

  return (
    <div className='flex flex-col max-w-[70vw] mx-auto'>
      {/* 프로젝트이름 섹션 */}
      <div className='flex flex-row justify-between mb-8'>
        <div className='flex flex-row items-center gap-2 font-[paperlogy5]'>
          <div className='text-[24px] text-slate-500'>프로젝트명</div>
          <div className='text-[20px] text-slate-300'>pjt-23sdfsdg234</div>
        </div>
      </div>

      <div className='grid grid-cols-2 gap-6'>
        {/* 로그 차트 섹션*/}
        <div className='bg-white rounded-xl p-6'>
          <h2 className='text-xl font-semibold mb-6 text-gray-700'>로그 레벨 분포</h2>
          <DonutChart data={logData} size={240} thickness={50} />
        </div>

        <div className='bg-white rounded-xl p-6'>
          <h2 className='text-xl font-semibold mb-6 text-gray-700'>로그 발생 추이</h2>
          <ErrorCountChart />
        </div>
      </div>

      <div className='grid grid-cols-2 gap-6 mt-6'>
        <div className='bg-white rounded-xl p-6'>
          <h2 className='text-xl font-semibold mb-6 text-gray-700'>자주 발생하는 에러 TOP 3</h2>
          <RankingCardList items={topErrors} />
        </div>
        <div className='bg-white rounded-xl p-6'>
          <h2 className='text-xl font-semibold mb-6 text-gray-700'>자주 호출되는 API TOP 3</h2>
          <RankingCardList items={[
            { name: '/api/user/login', count: 2345 },
            { name: '/api/project/list', count: 1890 },
            { name: '/api/logs/search', count: 1456 }
          ]} />
        </div>
      </div>

      <div>
        <div className="text-left p-4 text-[18px] font-[paperlogy6]">요약</div>
        <div className="text-left bg-[#F7FEE7] p-4 rounded-lg text-[14px] font-[consolaNormal] shadow-sm">
          하이하이
        </div>
      </div>
    </div>
  );
};

export default ReportPage;
