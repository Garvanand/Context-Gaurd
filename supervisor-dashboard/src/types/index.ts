export interface SystemOverviewData {
  timestamp: string;
  contextguard: {
    status: string;
    name: string;
    version: string;
    environment: string;
    security_invariant: string;
  };
  android_connection: {
    status: string;
    active_mode: string;
    device: string;
    last_handshake: string;
    sharesheet_integration: string;
  };
  backend: {
    status: string;
    uptime_seconds: number;
    host: string;
    workers: number;
    storage_mode: string;
  };
  ollama: {
    status: string;
    url: string;
    reachable: boolean;
  };
  qwen_vlm: {
    status: string;
    model_tag: string;
    installed: boolean;
    inference_status: string;
    fallback_active: boolean;
    latency_ms: number;
  };
  xgboost_url: {
    status: string;
    model_path: string;
    dataset: string;
    samples: number;
    accuracy: number;
    f1: number;
    features_extracted: number;
    inference_latency_ms: number;
  };
  ml_kit: {
    status: string;
    ocr_engine: string;
    face_detection: string;
    pii_parser: string;
    redaction_layer: string;
  };
  earb: {
    total_pairs: number;
    base_artifacts: number;
    categories: number;
    dataset_version: string;
  };
}

export interface EarbPair {
  pair_id: string;
  base_artifact_id: string;
  category: string;
  context: string;
  intended_action: string;
  destination: string;
  recipient: string;
  expected_intervention: 'STOP' | 'WARN' | 'ASK' | 'ACT';
  risk_type: string;
  is_ambiguous: boolean;
  is_negative_control: boolean;
  split: string;
}

export interface EarbData {
  total_pairs: number;
  base_artifacts: number;
  categories: Record<string, number>;
  intervention_distribution: Record<string, number>;
  ambiguity_distribution: Record<string, number>;
  negative_controls_count: number;
  pairs: EarbPair[];
}

export interface BaseArtifact {
  id: string;
  category: string;
  path: string;
  ocr_preview: string;
  detected_faces: number;
  detected_pii_count: number;
  detected_pii: string[];
  url?: string;
}

export interface MetricSummary {
  macro_f1?: number;
  accuracy?: number;
  stop_recall?: number;
  act_alarm_rate?: number;
  mean_latency_ms?: number;
  p90_latency_ms?: number;
  ece?: number;
  macro_f1_ci_95?: [number, number];
  samples?: number;
  per_class?: Record<string, { precision: number; recall: number; f1: number }>;
}

export interface EvaluationItem {
  name: string;
  status: 'COMPLETED' | 'Not evaluated' | string;
  metrics: MetricSummary | null;
}

export interface EvaluationResults {
  timestamp: string;
  baselines: Record<string, EvaluationItem>;
  ablations: Record<string, EvaluationItem>;
  privacy_utility: {
    status?: string;
    modes?: Record<string, {
      macro_f1?: number;
      stop_recall?: number;
      act_alarm_rate?: number;
      confidence?: number;
      mean_latency_ms?: number;
      sensitive_regions_transmitted?: number;
      redactions_performed?: number;
      avg_payload_bytes?: number;
    }>;
    privacy_metrics?: {
      redaction_efficiency?: number;
      payload_reduction_ratio?: number;
      zero_disk_leakage?: boolean;
    };
    raw_results?: any;
  } | null;
}

export interface FailureItem {
  pair_id: string;
  ground_truth: string;
  prediction: string;
  risk_score?: number;
  evidence?: string[];
  latency_ms?: number;
  model?: string;
}

export interface FailureAnalysisData {
  status: string;
  total_analyzed: number;
  summary: {
    false_stops_count: number;
    false_acts_count: number;
    false_warns_count: number;
    false_asks_count: number;
    latency_outliers_count: number;
    p90_latency_ms: number;
  };
  false_stops: FailureItem[];
  false_acts: FailureItem[];
  false_warns: FailureItem[];
  false_asks: FailureItem[];
  latency_outliers: FailureItem[];
}

export interface NetworkAuditEntry {
  id: string;
  timestamp: string;
  endpoint: string;
  method: string;
  status_code: number;
  network_mode: string;
  payload_bytes: number;
  is_redacted: boolean;
  masked_tokens_count: number;
  client_ip: string;
  sha256: string;
  duration_ms: number;
}

export interface AnalysisRequest {
  artifact?: string;
  action: string;
  recipient?: string;
  destination?: string;
  source_app?: string;
  ocr_text?: string;
  url?: string;
  detected_faces?: number;
  detected_pii?: string[];
}

export interface StageTrace {
  context: {
    extracted_features: {
      has_screenshot: boolean;
      detected_pii_count: number;
      detected_faces: number;
      url_present: boolean;
      url_risk_score?: number;
    };
    source_application: string;
  };
  intent: {
    intended_action: string;
    destination: string;
    recipient: string;
    is_inferred: boolean;
    inference_confidence: number;
  };
  evidence: {
    reasons: string[];
    pii_types_present: string[];
    risk_factors: string[];
  };
  consequence: {
    severity_level: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    reversibility: 'REVERSIBLE' | 'IRREVERSIBLE' | 'HARD_TO_REVERSE';
    reversibility_penalty: number;
  };
  uncertainty: {
    model_confidence: number;
    ambiguity_detected: boolean;
    threshold_gap: number;
  };
  intervention: {
    selected_intervention: 'STOP' | 'WARN' | 'ASK' | 'ACT';
    effective_risk_score: number;
    policy_trigger: string;
  };
}

export interface AnalysisResponse {
  intervention: 'STOP' | 'WARN' | 'ASK' | 'ACT';
  risk_score: number;
  severity?: number;
  reversibility?: number;
  confidence: number;
  evidence: string[] | any[];
  reason?: string;
  model_path?: string;
  network_mode?: string;
  latency_ms?: number;
  execution_timestamp?: string;
  stage_outputs?: StageTrace;
  stages?: any;
}

export interface ComponentHealth {
  name: string;
  status: 'HEALTHY' | 'DEGRADED' | 'OFFLINE' | 'ENFORCED' | 'ERROR';
  provider?: string;
  endpoint?: string;
  model_tag?: string;
  installed?: boolean;
  fallback_active?: boolean;
  model_path?: string;
  dataset?: string;
  features?: number;
  accuracy?: number;
  f1_score?: number;
  probe_result: string;
  sample_score?: number;
  latency_ms?: number;
  capabilities?: string[];
  detected_in_probe?: number;
  lambda_reversibility?: number;
  stop_threshold?: number;
  ask_threshold?: number;
  min_confidence_threshold?: number;
  formula?: string;
  storage_mode?: string;
  raw_disk_writes_allowed?: boolean;
  transient_ram_buffer?: string;
  error?: string;
}

export interface ModelHealthData {
  timestamp: string;
  overall_status: string;
  components: Record<string, ComponentHealth>;
}
