export interface ServerMetrics {
  id: string;
  url: string;
  healthy: boolean;
  activeConnections: number;
  totalRequests: number;
  failedRequests: number;
  successRate: number;
  lastError: string | null;
}

export interface MetricsSnapshot {
  timestamp: number;
  totalServers: number;
  healthyServers: number;
  unhealthyServers: number;
  totalRequests: number;
  totalFailedRequests: number;
  totalActiveConnections: number;
  servers: ServerMetrics[];
}

export interface MetricsSummary {
  totalServers: number;
  healthyServers: number;
  unhealthyServers: number;
  totalRequests: number;
  activeConnections: number;
  activeStrategy: string;
  timestamp: number;
}

export interface BackendServer {
  id: string;
  url: string;
  weight: number;
  healthy: boolean;
  lastHealthCheck: string | null;
  consecutiveFailures: number;
  totalRequests: number;
  failedRequests: number;
  createdAt: string;
  lastError: string | null;
  description: string | null;
}

export interface StrategyConfig {
  active: string;
  available: string[];
}

/** A single data point for the requests-over-time chart */
export interface ChartDataPoint {
  time: string;
  [serverUrl: string]: number | string;
}
