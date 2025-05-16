import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { AppDispatch, RootState } from '../store/store';
import { fetchReportList } from '../store/slices/reportSlice';
import ProjectNavBar from '../components/projectNavbar';

const ReportListPage = () => {
    const { projectId } = useParams();
    const navigate = useNavigate();
    const dispatch = useDispatch<AppDispatch>();
    const { reportList, isLoading, error } = useSelector((state: RootState) => state.report);

    useEffect(() => {
        if (projectId) {
            dispatch(fetchReportList({ projectId: Number(projectId) }));
        }
    }, [dispatch, projectId]);

    const handleReportClick = (reportId: string) => {
        navigate(`/report/${projectId}/${reportId}`);
    };

    if (isLoading) {
        return <div>로딩 중...</div>;
    }

    return (
        <div className="w-full lg:w-[70vw] mx-auto">
            <div className="mt-8">
                <ProjectNavBar />
                <h1 className="text-2xl font-bold mb-6 text-left">리포트 목록</h1>

                {error && (
                    <div className="bg-red-100 border-l-4 border-red-500 text-red-700 p-4 mb-4 text-left">
                        리포트 목록을 불러오는 중 오류가 발생했습니다.
                    </div>
                )}

                {(!reportList?.content || reportList.content.length === 0) ? (
                    <div className="text-left text-gray-500 p-4">
                        생성된 리포트가 없습니다.
                    </div>
                ) : (
                    <div className="grid gap-6">
                        {reportList.content.map((report) => (
                            <div
                                key={report.reportId}
                                className="bg-white p-6 rounded-xl shadow-md hover:shadow-lg transition-all duration-300 cursor-pointer border border-gray-100 hover:border-blue-200"
                                onClick={() => handleReportClick(report.reportId)}
                            >
                                <div className="flex justify-between items-start mb-4">
                                    <h2 className="text-xl font-bold text-gray-800 hover:text-blue-600 transition-colors text-left">
                                        {report.title}
                                    </h2>
                                    <span className="px-3 py-1 bg-blue-50 text-blue-600 rounded-full text-sm font-medium whitespace-nowrap ml-4">
                                        리포트
                                    </span>
                                </div>
                                
                                <p className="text-gray-600 mb-4 line-clamp-2 text-base text-left">
                                    {report.summary}
                                </p>
                                
                                <div className="flex items-center justify-between text-sm">
                                    <div className="flex items-center space-x-2 text-left">
                                        <svg className="w-4 h-4 text-gray-400 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                            <path fillRule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clipRule="evenodd" />
                                        </svg>
                                        <span className="text-gray-600 truncate">{report.createdBy}</span>
                                    </div>
                                    <div className="flex items-center space-x-2 text-right">
                                        <svg className="w-4 h-4 text-gray-400 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                            <path fillRule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clipRule="evenodd" />
                                        </svg>
                                        <span className="text-gray-600 whitespace-nowrap">
                                            {new Date(report.createdAt).toLocaleDateString('ko-KR', {
                                                year: 'numeric',
                                                month: 'long',
                                                day: 'numeric'
                                            })}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {reportList && (
                    <div className="mt-6 text-sm text-gray-500 bg-gray-50 p-4 rounded-lg flex justify-between items-center">
                        <span className="text-left">
                            총 <span className="font-semibold">{reportList.totalElements}</span>개의 리포트
                        </span>
                        <span>
                            페이지 <span className="font-semibold">{reportList.pageNumber}</span> / 
                            <span className="font-semibold ml-1">{reportList.totalPages}</span>
                        </span>
                    </div>
                )}
            </div>
        </div>
    );
};

export default ReportListPage;
