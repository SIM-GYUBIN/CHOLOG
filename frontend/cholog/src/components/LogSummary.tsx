import errorIcon from "@/assets/levelicon/error.svg";
import warnIcon from "@/assets/levelicon/warn.svg";
import infoIcon from "@/assets/levelicon/info.svg";
import debugIcon from "@/assets/levelicon/debug.svg";
import traceIcon from "@/assets/levelicon/trace.svg";
import fatalIcon from "@/assets/levelicon/fatal.svg";

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
    <div className="rounded-2xl border border-[var(--line)] p-6 shadow-md w-full bg-white/5 h-full">
      <div className="text-left text-[var(--text)] mb-4 flex items-end gap-3">
        <span className="text-lg">Total</span>
        <span className="text-2xl font-bold">
          {logsMockData.total.toLocaleString()}
        </span>
      </div>

      <div className="grid grid-cols-2 gap-x-4 gap-y-4">
        {LABELS.map((label) => (
          <div key={label} className="flex items-center gap-2">
            <img
              src={ICONS[label]}
              alt={`${label} icon`}
              className="w-11 h-11"
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
