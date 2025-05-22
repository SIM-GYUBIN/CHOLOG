import {
  BrowserRouter,
  Routes,
  Route,
  useLocation,
  Navigate,
} from "react-router-dom";
import "./App.css";
import NavigationBar from "./components/NavigationBar";
import LandingPage from "./pages/LandingPage";
import LoginPage from "./pages/LoginPage";
import ProjectListPage from "./pages/ProjectListPage";
import ProjectPage from "./pages/ProjectPage";
import LogPage from "./pages/LogPage";
import ReportPage from "./pages/ReportPage";
import ArchiveListPage from "./pages/ArchiveListPage";
import GuidePage from "./pages/GuidePage";
import PrivateRoute from "./components/common/PrivateRoute";

function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center h-[80vh]">
      <div className="text-4xl font-bold text-[#5EA500] mb-4">404</div>
      <div className="text-xl text-slate-600 mb-6">
        페이지를 찾을 수 없습니다
      </div>
      <div className="text-slate-500 mb-8">
        요청하신 페이지가 존재하지 않거나 삭제되었습니다
      </div>
      <button
        onClick={() => window.history.back()}
        className="px-6 py-2 bg-[#5EA500] text-white rounded-xl hover:bg-[#4A8400] transition-colors"
      >
        이전 페이지로 돌아가기
      </button>
    </div>
  );
}

function AppContent() {
  const location = useLocation();
  const hideNavbar =
    location.pathname.toLowerCase() === "/projectlist" ||
    location.pathname.toLowerCase() === "/login" ||
    location.pathname.toLowerCase() === "/" ||
    location.pathname.toLowerCase() === "/landing" ||
    location.pathname.toLowerCase() === "/guide";

  return (
    <div className="app">
      {!hideNavbar && <NavigationBar />}
      <div className={!hideNavbar ? "pt-16" : ""}>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/landing" element={<LandingPage />} />
          <Route
            path="/projectlist"
            element={
              <PrivateRoute>
                <ProjectListPage />
              </PrivateRoute>
            }
          />
          <Route path="/guide" element={<GuidePage />} />
          <Route
            path="/project/:projectId"
            element={
              <PrivateRoute>
                <ProjectPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/project/:projectId/log/:logId"
            element={
              <PrivateRoute>
                <LogPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/report/:projectId"
            element={
              <PrivateRoute>
                <ReportPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/archive/:projectId"
            element={
              <PrivateRoute>
                <ArchiveListPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/project/:projectId/archives"
            element={
              <PrivateRoute>
                <ArchiveListPage />
              </PrivateRoute>
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </div>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}

export default App;
