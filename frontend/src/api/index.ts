import axios from 'axios';
import type { BackendServer, MetricsSnapshot, MetricsSummary, StrategyConfig } from '../types';

const api = axios.create({
  // In Docker: Nginx proxies /api to the backend, so baseURL is empty (relative).
  // In local dev: Vite proxy forwards /api to localhost:8080, so also relative.
  // VITE_API_URL can override for non-proxied setups.
  baseURL: import.meta.env.VITE_API_URL ?? '',
  timeout: 5000,
});

// ── Servers ────────────────────────────────────────────────────────────────

export const getServers = () =>
  api.get<BackendServer[]>('/api/servers').then((r) => r.data);

export const registerServer = (server: Pick<BackendServer, 'url' | 'weight' | 'description'>) =>
  api.post<BackendServer>('/api/servers', server).then((r) => r.data);

export const deregisterServer = (id: string) =>
  api.delete(`/api/servers/${id}`);

// ── Metrics ────────────────────────────────────────────────────────────────

export const getMetrics = () =>
  api.get<MetricsSnapshot>('/api/metrics').then((r) => r.data);

export const getMetricsSummary = () =>
  api.get<MetricsSummary>('/api/metrics/summary').then((r) => r.data);

// ── Strategy ───────────────────────────────────────────────────────────────

export const getStrategy = () =>
  api.get<StrategyConfig>('/api/config/strategy').then((r) => r.data);

export const setStrategy = (strategy: string) =>
  api.patch<{ active: string }>('/api/config/strategy', { strategy }).then((r) => r.data);
