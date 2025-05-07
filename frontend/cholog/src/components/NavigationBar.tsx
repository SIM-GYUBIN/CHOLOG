import React from 'react';
import { useNavigate } from 'react-router-dom';
import logo from '@/assets/logo2.svg';

const NavigationBar = () => {
  const navigate = useNavigate();

  return (
    <nav className="fixed top-0 left-0 right-0 bg-white border-b border-gray-200 z-50">
      <div className="max-w-9xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo on the left with navigation */}
          <div className="flex-shrink-0">
            <button 
              onClick={() => navigate('/')}
              className="focus:outline-none"
            >
              <img src={logo} alt="Cholog logo" className="h-8 mt-2" />
            </button>
          </div>
          
          {/* Buttons on the right */}
          <div className="flex space-x-4">
            <button className="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium">
              Button 1
            </button>
            <button className="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium">
              Button 2
            </button>
            <button className="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium">
              Button 3
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default NavigationBar;