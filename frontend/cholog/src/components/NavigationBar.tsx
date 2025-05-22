import { useNavigate } from "react-router-dom";
import logo from "@/assets/logo2.svg";
import ThemeToggle from "../components/ThemeToggle";

const NavigationBar = () => {
  const navigate = useNavigate();

  return (
    <nav className="fixed top-0 left-0 right-0 bg-[var(--nav)] border-b border-[var(--line)] z-50">
      <div className="max-w-9xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo on the left with navigation */}
          <div className="flex-shrink-0">
            <button
              onClick={() => navigate("/projectlist")}
              className="focus:outline-none cursor-pointer"
            >
              <img src={logo} alt="Cholog logo" className="h-8 mt-2" />
            </button>
          </div>

          {/* Buttons on the right */}
          <div className="flex space-x-4">
            {/* <button className="text-[#45556C] rounded-full text-[14px] bg-[#D1D5DB] font-medium w-12 h-12 flex items-center justify-center">
              이름
            </button> */}
            <ThemeToggle />
          </div>
        </div>
      </div>
    </nav>
  );
};

export default NavigationBar;
