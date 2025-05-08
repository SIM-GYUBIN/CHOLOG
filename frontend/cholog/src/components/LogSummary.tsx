import errorIcon from "@/assets/levelicon/error.png";
import warnIcon from "@/assets/levelicon/warn.png";
import infoIcon from "@/assets/levelicon/info.png";
import debugIcon from "@/assets/levelicon/debug.png";
import traceIcon from "@/assets/levelicon/trace.png";
import fatalIcon from "@/assets/levelicon/fatal.png";

const LogSummary = () => {
  const logsMockData = {
    total: 2183,
    logs: {
      error: 56,
      warn: 21,
      info: 1240,
      debug: 5,
      trace: 861,
      fatal: 0,
    },
  };

  const ICONS = {
    error: errorIcon,
    warn: warnIcon,
    info: infoIcon,
    debug: debugIcon,
    trace: traceIcon,
    fatal: fatalIcon,
  };

  const LABELS = ["error", "debug", "warn", "trace", "info", "fatal"] as const;

  return (
    <div className="rounded-2xl border border-gray-200 p-8 shadow-md w-full bg-white">
      <div className="text-left text-gray-800 mb-4 flex items-end gap-3">
        <span className="text-lg">Total</span>
        <span className="text-2xl font-bold">
          {logsMockData.total.toLocaleString()}
        </span>
      </div>

      <div className="grid grid-cols-2 gap-x-6 gap-y-6">
        {LABELS.map((label) => (
          <div key={label} className="flex items-center gap-3">
            <img
              src={ICONS[label]}
              alt={`${label} icon`}
              className="w-10 h-10"
            />
            <div className="flex flex-col text-left text-[var(--text)]">
              <span className="text-sm text-[var(--text)]">{label}</span>
              <span className="text-base font-medium">
                {logsMockData.logs[label]}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default LogSummary;
