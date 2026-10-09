import type {
  SystemOverviewData,
  EarbData,
  BaseArtifact,
  EvaluationResults,
  FailureAnalysisData,
  NetworkAuditEntry,
  AnalysisRequest,
  AnalysisResponse,
  ModelHealthData,
} from '../types';

const BASE_URL = '/api/v1';

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(`API Error [${res.status}]: ${errorText || res.statusText}`);
  }
  return res.json();
}

export async function fetchSystemOverview(): Promise<SystemOverviewData> {
  const res = await fetch(`${BASE_URL}/supervisor/overview`);
  return handleResponse<SystemOverviewData>(res);
}

export async function fetchEarbData(): Promise<EarbData> {
  const res = await fetch(`${BASE_URL}/supervisor/earb`);
  return handleResponse<EarbData>(res);
}

export async function fetchArtifacts(): Promise<BaseArtifact[]> {
  const res = await fetch(`${BASE_URL}/supervisor/artifacts`);
  return handleResponse<BaseArtifact[]>(res);
}

export async function fetchEvaluationResults(): Promise<EvaluationResults> {
  const res = await fetch(`${BASE_URL}/supervisor/results`);
  return handleResponse<EvaluationResults>(res);
}

export async function fetchFailureAnalysis(): Promise<FailureAnalysisData> {
  const res = await fetch(`${BASE_URL}/supervisor/failure-analysis`);
  return handleResponse<FailureAnalysisData>(res);
}

export async function fetchNetworkActivity(): Promise<{
  total_events: number;
  audit_enabled: boolean;
  entries: NetworkAuditEntry[];
}> {
  const res = await fetch(`${BASE_URL}/supervisor/network-activity`);
  return handleResponse<{
    total_events: number;
    audit_enabled: boolean;
    entries: NetworkAuditEntry[];
  }>(res);
}

export async function fetchModelHealth(): Promise<ModelHealthData> {
  const res = await fetch(`${BASE_URL}/supervisor/model-health`);
  return handleResponse<ModelHealthData>(res);
}

export async function runLiveAnalysis(payload: AnalysisRequest): Promise<AnalysisResponse> {
  const res = await fetch(`${BASE_URL}/supervisor/analyze`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
  return handleResponse<AnalysisResponse>(res);
}
