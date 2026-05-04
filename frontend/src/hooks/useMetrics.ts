import { useEffect, useRef, useState } from 'react';
import { getMetrics, getMetricsSummary } from '../api';
import type { ChartDataPoint, MetricsSnapshot } from '../types';

const POLL_INTERVAL_MS = 5000;
const MAX_HISTORY = 20;

interface UseMetricsResult {
  snapshot: MetricsSnapshot | null;
  history: ChartDataPoint[];
  activeStrategy: string;
  error: string | null;
  loading: boolean;
}

export function useMetrics(): UseMetricsResult {
  const [snapshot, setSnapshot] = useState<MetricsSnapshot | null>(null);
  const [history, setHistory] = useState<ChartDataPoint[]>([]);
  const [activeStrategy, setActiveStrategy] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const prevTotals = useRef<Record<string, number>>({});

  useEffect(() => {
    let cancelled = false;

    async function poll() {
      try {
        const [data, summary] = await Promise.all([getMetrics(), getMetricsSummary()]);
        if (cancelled) return;

        setSnapshot(data);
        setActiveStrategy(summary.activeStrategy);
        setError(null);
        setLoading(false);

        const time = new Date(data.timestamp).toLocaleTimeString();
        const point: ChartDataPoint = { time };

        data.servers.forEach((s) => {
          const prev = prevTotals.current[s.id] ?? s.totalRequests;
          point[s.url] = Math.max(0, s.totalRequests - prev);
          prevTotals.current[s.id] = s.totalRequests;
        });

        setHistory((h) => [...h.slice(-(MAX_HISTORY - 1)), point]);
      } catch (err) {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : 'Failed to fetch metrics');
        setLoading(false);
      }
    }

    poll();
    const timer = setInterval(poll, POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  return { snapshot, history, activeStrategy, error, loading };
}
