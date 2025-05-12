import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { AppDispatch, RootState } from '../store/store';
import { fetchReportList } from '../store/slices/reportSlice';
import { MOCK_REPORT_LIST } from '../constants/mockData';

const ReportListPage = () => {
    const { projectId } = useParams();
    const navigate = useNavigate();
    const dispatch = useDispatch<AppDispatch>();
    const { reportList, isLoading, error } = useSelector((state: RootState) => state.report);

    console.log(projectId); // Check if id is being passed correctly t

    useEffect(() => {
        if (projectId) {
            dispatch(fetchReportList({ projectId: Number(projectId) }));
        }
    }, [dispatch, projectId]);

    const displayData = error ? MOCK_REPORT_LIST : reportList;

    const handleReportClick = (reportId: string) => {
        navigate(`/report/${projectId}/${reportId}`);
    };

    if (isLoading) {
        return <div>로딩 중...</div>;
    }

    return (
        <div className="w-full lg:w-[70vw] mx-auto">
            <div className="mt-8">
                <h1 className="text-2xl font-bold mb-6 text-left">리포트 목록</h1>

                {error && (
                    <div className="bg-yellow-100 border-l-4 border-yellow-500 text-yellow-700 p-4 mb-4 text-left">
                        API 오류가 발생하여 목데이터를 표시합니다.
                    </div>
                )}

                {(!displayData?.content || displayData.content.length === 0) ? (
                    <div className="text-left text-gray-500 p-4">
                        생성된 리포트가 없습니다.
                    </div>
                ) : (
                    <div className="grid gap-6">
                        {displayData.content.map((report) => (
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

                {displayData && (
                    <div className="mt-6 text-sm text-gray-500 bg-gray-50 p-4 rounded-lg flex justify-between items-center">
                        <span className="text-left">
                            총 <span className="font-semibold">{displayData.totalElements}</span>개의 리포트
                        </span>
                        <span>
                            페이지 <span className="font-semibold">{displayData.pageNumber}</span> / 
                            <span className="font-semibold ml-1">{displayData.totalPages}</span>
                        </span>
                    </div>
                )}
            </div>
        </div>
    );
};

export default ReportListPage;
