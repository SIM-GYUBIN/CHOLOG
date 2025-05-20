import React, { useState } from "react";
import url1 from "@/assets/urlimg/url1.png";
import url2 from "@/assets/urlimg/url2.png";
import url3 from "@/assets/urlimg/url3.png";
import url4 from "@/assets/urlimg/url4.png";
import url5 from "@/assets/urlimg/url5.png";

interface URLGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const URLGuideModal: React.FC<URLGuideModalProps> = ({ isOpen, onClose }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const images = [url1, url2, url3, url4, url5];

  if (!isOpen) return null;

  const nextSlide = () => {
    setCurrentIndex((prev) => (prev === images.length - 1 ? 0 : prev + 1));
  };

  const prevSlide = () => {
    setCurrentIndex((prev) => (prev === 0 ? images.length - 1 : prev - 1));
  };

  return (
    <div
      className="fixed inset-0 bg-black/30 flex items-center justify-center z-[60]"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-lg p-4 w-[700px] shadow-lg"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="text-[16px] font-[paperlogy6] mb-3">
          Mattermost Webhook URL 설정 방법
        </div>
        <div className="relative">
          <div className="overflow-hidden rounded-lg">
            <div
              className="flex transition-transform duration-300 ease-in-out"
              style={{ transform: `translateX(-${currentIndex * 100}%)` }}
            >
              {images.map((img, index) => (
                <img
                  key={index}
                  src={img}
                  alt={`url${index + 1}`}
                  className="w-full flex-shrink-0"
                />
              ))}
            </div>
          </div>

          <button
            onClick={(e) => {
              e.stopPropagation();
              prevSlide();
            }}
            className="absolute left-2 top-1/2 -translate-y-1/2 bg-white/80 rounded-full p-1 hover:bg-white"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={2}
              stroke="currentColor"
              className="w-5 h-5"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M15.75 19.5L8.25 12l7.5-7.5"
              />
            </svg>
          </button>

          <button
            onClick={(e) => {
              e.stopPropagation();
              nextSlide();
            }}
            className="absolute right-2 top-1/2 -translate-y-1/2 bg-white/80 rounded-full p-1 hover:bg-white"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={2}
              stroke="currentColor"
              className="w-5 h-5"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M8.25 4.5l7.5 7.5-7.5 7.5"
              />
            </svg>
          </button>

          <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
            {images.map((_, index) => (
              <button
                key={index}
                onClick={(e) => {
                  e.stopPropagation();
                  setCurrentIndex(index);
                }}
                className={`w-2 h-2 rounded-full transition-colors ${
                  currentIndex === index ? "bg-slate-700" : "bg-slate-300"
                }`}
              />
            ))}
          </div>
        </div>
        <button
          onClick={onClose}
          className="mt-4 text-[12px] text-slate-500 hover:text-slate-700"
        >
          닫기
        </button>
      </div>
    </div>
  );
};

export default URLGuideModal;
