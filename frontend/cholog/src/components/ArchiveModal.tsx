import { useState, useEffect } from "react";
import { useDispatch } from "react-redux";
import { AppDispatch } from "../store/store";
import { archiveLog } from "../store/slices/logSlice";

interface ArchiveModalProps {
  logId: string;
  projectId: string | undefined; // projectId 추가
  isOpen: boolean;
  onClose: () => void;
  onArchive: (reason: string) => void;
}

export default function ArchiveModal({
  logId,
  projectId,
  isOpen,
  onClose,
  onArchive,
}: ArchiveModalProps) {
  const dispatch = useDispatch<AppDispatch>();
  const [reason, setReason] = useState("");
  const [error, setError] = useState("");

  // isOpen이 false로 변경될 때(모달이 닫힐 때) 입력값 초기화
  useEffect(() => {
    if (!isOpen) {
      setReason("");
      setError("");
    }
  }, [isOpen]);

  const handleSubmit = async () => {
    if (!reason.trim()) {
      setError("아카이브 사유를 입력해주세요.");
      return;
    }

    if (reason.length > 255) {
      setError("아카이브 사유는 255자를 초과할 수 없습니다.");
      return;
    }

    try {
      // projectId 파라미터 추가
      const result = await dispatch(
        archiveLog({
          logId,
          archiveReason: reason,
          projectId: projectId ? Number(projectId) : undefined,
        })
      ).unwrap();

      if (result.success) {
        onArchive(reason);
        onClose();
      } else {
        setError(
          result.error?.message || "아카이브 처리 중 오류가 발생했습니다."
        );
      }
    } catch (error: any) {
      setError(error.message || "아카이브 처리 중 오류가 발생했습니다.");
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
      <div className="bg-[var(--bg)] rounded-lg p-6 w-[500px] border border-[var(--line)]">
        <h2 className="text-[18px] font-[paperlogy6] mb-4">로그 아카이브</h2>

        <div className="mb-4">
          {/* <label className="block text-[14px] text-slate-600 mb-2">
            아카이브 사유
          </label> */}
          <textarea
            className="w-full h-32 p-3 border border-slate-200 rounded-lg text-[14px] font-[consolaNormal] resize-none focus:outline-none focus:ring-2 focus:ring-[rgba(101,218,94,1)]"
            placeholder="아카이브 사유를 입력해주세요. (최대 255자)"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
          {error && <p className="text-red-500 text-[12px] mt-1">{error}</p>}
          <p className="text-right text-[12px] text-slate-400 mt-1">
            {reason.length}/255
          </p>
        </div>

        <div className="flex justify-end gap-2">
          <button
            onClick={onClose}
            className="px-4 py-2 text-[14px] text-[var(--text)] hover:bg-slate-400/20 rounded-lg transition-colors"
          >
            취소
          </button>
          <button
            onClick={handleSubmit}
            className="px-4 py-2 text-[14px] text-white bg-lime-500 hover:bg-lime-600 rounded-lg transition-colors"
          >
            아카이브
          </button>
        </div>
      </div>
    </div>
  );
}
