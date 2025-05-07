import { BrowserRouter, Routes, Route } from 'react-router-dom';
import './App.css';
import NavigationBar from './components/NavigationBar';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import ProjectListPage from './pages/ProjectListPage';
import ProjectPage from './pages/ProjectPage';
import LogPage from './pages/LogPage';
import ReportPage from './pages/ReportPage';
import ArchivePage from './pages/ArchivePage';

function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <NavigationBar />
        <div className="pt-16"> {/* Add padding to account for fixed navbar */}
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/projectList" element={<ProjectListPage />} />
            <Route path="/project/:projectId" element={<ProjectPage />} />
            <Route path="/log/:logId" element={<LogPage />} />
            <Route path="/report/:reportId" element={<ReportPage />} />
            <Route path="/archive/:archiveId" element={<ArchivePage />} />
          </Routes>
        </div>
      </div>
    </BrowserRouter>
  );
}

export default App;
