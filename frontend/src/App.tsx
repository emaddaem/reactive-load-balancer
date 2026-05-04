import { useMetrics } from './hooks/useMetrics';
import { SummaryBar } from './components/SummaryBar';
import { ServerList } from './components/ServerList';
import { MetricsChart } from './components/MetricsChart';
import './index.css';

export default function App() {
  const { snapshot, history, activeStrategy, error, loading } = useMetrics();

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Reactive Load Balancer</h1>
          <p className="text-xs text-gray-400 mt-0.5">Dashboard · auto-refresh every 5 s</p>
        </div>
        {error && (
          <span className="text-xs text-red-500 bg-red-50 border border-red-200 rounded-lg px-3 py-1">
            ⚠ {error}
          </span>
        )}
        {loading && !error && (
          <span className="text-xs text-gray-400 animate-pulse">Connecting…</span>
        )}
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8 space-y-8">
        {/* Summary cards */}
        <section>
          <SummaryBar
            snapshot={snapshot}
            activeStrategy={activeStrategy}
          />
        </section>

        {/* Requests-over-time chart */}
        <section className="bg-white rounded-xl border border-gray-100 shadow-sm p-6">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">
            Requests per Server (last 20 intervals)
          </h2>
          <MetricsChart
            history={history}
            servers={snapshot?.servers ?? []}
          />
        </section>

        {/* Per-server cards */}
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Backend Servers</h2>
          <ServerList servers={snapshot?.servers ?? []} />
        </section>
      </main>
    </div>
  );
}