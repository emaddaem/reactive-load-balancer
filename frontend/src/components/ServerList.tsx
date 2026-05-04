import type { ServerMetrics } from '../types';

interface Props {
  servers: ServerMetrics[];
}

export function ServerList({ servers }: Props) {
  if (servers.length === 0) {
    return (
      <div className="text-center py-10 text-gray-500">
        No backend servers registered.
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {servers.map((s) => (
        <div
          key={s.id}
          className={`rounded-xl border p-4 shadow-sm transition-colors ${
            s.healthy
              ? 'border-green-200 bg-green-50'
              : 'border-red-200 bg-red-50'
          }`}
        >
          {/* Header row */}
          <div className="flex items-center justify-between mb-3">
            <span className="font-mono text-sm font-semibold text-gray-800 truncate max-w-[70%]">
              {s.url}
            </span>
            <span
              className={`text-xs font-bold px-2 py-0.5 rounded-full ${
                s.healthy
                  ? 'bg-green-100 text-green-700'
                  : 'bg-red-100 text-red-700'
              }`}
            >
              {s.healthy ? 'HEALTHY' : 'DOWN'}
            </span>
          </div>

          {/* Stats grid */}
          <div className="grid grid-cols-2 gap-2 text-xs text-gray-600">
            <Stat label="Total Requests" value={s.totalRequests.toLocaleString()} />
            <Stat label="Failed" value={s.failedRequests.toLocaleString()} />
            <Stat label="Active Connections" value={s.activeConnections} />
            <Stat label="Success Rate" value={`${s.successRate.toFixed(1)}%`} />
          </div>

          {/* Error message */}
          {s.lastError && (
            <p className="mt-2 text-xs text-red-600 truncate" title={s.lastError}>
              ⚠ {s.lastError}
            </p>
          )}
        </div>
      ))}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div>
      <p className="text-gray-400 uppercase tracking-wide text-[10px]">{label}</p>
      <p className="font-semibold text-gray-700">{value}</p>
    </div>
  );
}
