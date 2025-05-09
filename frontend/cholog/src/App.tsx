import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom';
import './App.css';
import NavigationBar from './components/NavigationBar';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import ProjectListPage from './pages/ProjectListPage';
import ProjectPage from './pages/ProjectPage';
import LogPage from './pages/LogPage';
import ReportPage from './pages/ReportPage';
import ArchivePage from './pages/ArchivePage';
import ProjectSettingPage from './pages/ProjectSettingPage';
import ArchiveListPage from './pages/ArchiveListPage';

function AppContent() {
  const location = useLocation();
  const hideNavbar = location.pathname.toLowerCase() === '/projectlist';

  return (
    <div className="app">
      {!hideNavbar && <NavigationBar />}
      <div className={!hideNavbar ? "pt-16" : ""}>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/projectlist" element={<ProjectListPage />} />
          <Route path="/project/:projectId" element={<ProjectPage />} />
          <Route path="/log/:logId" element={<LogPage />} />
          <Route path="/report/:projectId" element={<ReportPage />} />
          <Route path="/archive/:projectId" element={<ArchivePage />} />
          <Route path="/projectsetting/:projectId" element={<ProjectSettingPage />} />
          <Route path="/project/:projectId/archives" element={<ArchiveListPage />} />
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
