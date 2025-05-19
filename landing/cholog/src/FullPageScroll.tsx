import React, { useState, useEffect, useRef } from "react";

// 개별 섹션 컴포넌트
const Section = ({ children, className }) => {
  return (
    <div
      className={`h-screen w-full flex items-center justify-center ${className}`}
    >
      {children}
    </div>
  );
};

// FullpageScroll 컴포넌트
const FullpageScroll = ({ children }) => {
  const [currentSection, setCurrentSection] = useState(0);
  const containerRef = useRef(null);
  const isScrolling = useRef(false);
  const timeoutRef = useRef(null);
  const touchStartY = useRef(0);
  const touchEndY = useRef(0);

  const validChildren = React.Children.toArray(children).filter((child) =>
    React.isValidElement(child)
  );

  const sectionCount = validChildren.length;

  const handleSectionChange = (direction) => {
    if (isScrolling.current) return;
    isScrolling.current = true;

    if (direction === "down") {
      setCurrentSection((prev) => (prev < sectionCount - 1 ? prev + 1 : prev));
    } else {
      setCurrentSection((prev) => (prev > 0 ? prev - 1 : prev));
    }

    clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => {
      isScrolling.current = false;
    }, 800);
  };

  const handleWheel = (e) => {
    e.preventDefault();
    handleSectionChange(e.deltaY > 0 ? "down" : "up");
  };

  // Touch event handlers
  const handleTouchStart = (e) => {
    touchStartY.current = e.touches[0].clientY;
  };

  const handleTouchMove = (e) => {
    e.preventDefault();
    touchEndY.current = e.touches[0].clientY;
  };

  const handleTouchEnd = () => {
    const difference = touchStartY.current - touchEndY.current;
    const minSwipeDistance = 50; // Minimum distance for swipe

    if (Math.abs(difference) > minSwipeDistance) {
      handleSectionChange(difference > 0 ? "down" : "up");
    }
  };

  useEffect(() => {
    const container = containerRef.current;
    if (container) {
      container.addEventListener("wheel", handleWheel, { passive: false });
      container.addEventListener("touchstart", handleTouchStart, {
        passive: true,
      });
      container.addEventListener("touchmove", handleTouchMove, {
        passive: false,
      });
      container.addEventListener("touchend", handleTouchEnd, { passive: true });
    }

    return () => {
      if (container) {
        container.removeEventListener("wheel", handleWheel);
        container.removeEventListener("touchstart", handleTouchStart);
        container.removeEventListener("touchmove", handleTouchMove);
        container.removeEventListener("touchend", handleTouchEnd);
      }
      clearTimeout(timeoutRef.current);
    };
  }, [sectionCount]);

  return (
    <div
      ref={containerRef}
      className="h-screen w-full overflow-hidden relative touch-none"
    >
      <div
        className="transition-transform duration-700 ease-in-out h-full"
        style={{ transform: `translateY(-${currentSection * 100}%)` }}
      >
        {validChildren}
      </div>

      {/* 네비게이션 인디케이터 */}
      {sectionCount > 1 && (
        <div className="fixed right-8 top-1/2 transform -translate-y-1/2 z-10">
          {Array.from({ length: sectionCount }).map((_, index) => (
            <div
              key={index}
              className={`w-3 h-3 my-2 rounded-full cursor-pointer ${
                currentSection === index ? "bg-white scale-150" : "bg-gray-400"
              } transition-all duration-300`}
              onClick={() => setCurrentSection(index)}
            />
          ))}
        </div>
      )}
    </div>
  );
};

FullpageScroll.Section = Section;

export default FullpageScroll;
