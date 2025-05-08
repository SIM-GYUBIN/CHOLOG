import { useParams } from 'react-router-dom';
import LogList from '../components/logList';
import copy from '@/assets/copy.svg';
import report from '@/assets/report.svg';
import setting from '@/assets/setting.svg';
import ErrorChart from '../components/charts/ErrorChart';
import LogSummary from '../components/LogSummary';

const ProjectPage = () => {
  const { projectId } = useParams();
  const projectName = "프로젝트이름이다";
  const handleCopyClipBoard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      alert("프로젝트ID 복사 완료");
    } catch (e) {
      alert("프로젝트ID 복사 실패");
    }
  };

  return (
    <div className='flex flex-col max-w-[70vw] mx-auto '>
      <div className='flex flex-row justify-between '>

        <div className='flex flex-row items-center gap-2 font-[paperlogy5]'>
          <div className=' text-[24px] text-slate-500'>{projectName}</div>
          <div className='text-[20px] text-slate-300'>{projectId}</div>
          <div className='rounded-sm p-1 cursor-pointer hover:bg-gray-200' onClick={() => handleCopyClipBoard(projectId || '')}>
            <img src={copy} alt="복사" className='w-5 h-5' />
          </div>
        </div>

        <div className='flex flex-row self-center'>
          <div className='rounded-sm p-1 cursor-pointer hover:bg-gray-200'>
            <img src={report} alt="리포트" className='w-5 h-5' />
          </div>
          <div className='rounded-sm p-1 cursor-pointer hover:bg-gray-200'>
            <img src={setting} alt="세팅" className='w-5 h-5' />
          </div>

        </div>

      </div>
      <div className='grid grid-cols-7 py-5 gap-10'>
        <div className='col-span-3'>
        <LogSummary />
        </div>
        <div className='col-span-4'>
        <ErrorChart />
        </div>
      </div>
      <LogList />
    </div>
  );
};

export default ProjectPage;
