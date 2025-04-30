import { BrowserRouter, Routes, Route } from 'react-router-dom';
import './App.css';
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
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element = {<LoginPage/>}></Route>
          <Route path="/projectList" element={<ProjectListPage/>}></Route>
          <Route path="/project/:projectId" element={<ProjectPage/>}></Route>
          <Route path="/log/:logId" element={<LogPage/>}></Route>
          <Route path="/report/:reportId" element={<ReportPage/>}></Route>
          <Route path="/archive/:archiveId" element={<ArchivePage/>}></Route>
        </Routes>
      </div>
    </BrowserRouter>
  )
}



export default App
