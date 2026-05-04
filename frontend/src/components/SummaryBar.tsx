import type { MetricsSnapshot } from '../types';

interface Props {
  snapshot: MetricsSnapshot | null;
  activeStrategy: string;
}

export function SummaryBar({ snapshot, activeStrategy }: Props) {
  const stats = [
    {
      label: 'Healthy',
      value: snapshot?.healthyServers ?? '—',
      color: 'text-green-600',
    },
    {
      label: 'Down',
      value: snapshot?.unhealthyServers ?? '—',
      color: 'text-red-500',
    },
    {
      label: 'Total Requests',
      value: snapshot ? snapshot.totalRequests.toLocaleString() : '—',
      color: 'text-indigo-600',
    },
    {
      label: 'Active Connections',
      value: snapshot?.totalActiveConnections ?? '—',
      color: 'text-amber-600',
    },
    {
      label: 'Strategy',
      value: activeStrategy || '—',
      color: 'text-purple-600',
    },
  ];

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
      {stats.map((s) => (
        <div
          key={s.label}
          className="rounded-xl bg-white border border-gray-100 shadow-sm p-4 text-center"
        >
          <p className="text-xs text-gray-400 uppercase tracking-wide mb-1">{s.label}</p>
          <p className={`text-2xl font-bold ${s.color}`}>{s.value}</p>
        </div>
      ))}
    </div>
  );
}
