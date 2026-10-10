"""
Pydantic contracts and DTO schemas for ContextGuard Real-Time Mobile <-> Web Relay.
"""

from datetime import datetime, timezone
from enum import Enum
from typing import Any, Dict, List, Optional
import uuid
from pydantic import BaseModel, ConfigDict, Field


class DeviceStatus(str, Enum):
    ONLINE = "ONLINE"
    OFFLINE = "OFFLINE"
    DEGRADED = "DEGRADED"
    UNPAIRED = "UNPAIRED"


class PairingStatus(str, Enum):
    PENDING = "PENDING"
    CLAIMED = "CLAIMED"
    APPROVED = "APPROVED"
    DENIED = "DENIED"
    EXPIRED = "EXPIRED"
    REVOKED = "REVOKED"


class CommandType(str, Enum):
    REQUEST_STATUS = "REQUEST_STATUS"
    REQUEST_DIAGNOSTICS = "REQUEST_DIAGNOSTICS"
    RUN_SYNTHETIC_DEMO = "RUN_SYNTHETIC_DEMO"
    REQUEST_CONFIG_REFRESH = "REQUEST_CONFIG_REFRESH"


class CommandStatus(str, Enum):
    QUEUED = "QUEUED"
    DELIVERED = "DELIVERED"
    ACKNOWLEDGED = "ACKNOWLEDGED"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"
    EXPIRED = "EXPIRED"
    CANCELLED = "CANCELLED"


class RelayEventType(str, Enum):
    ACCESSIBILITY_TRIGGER = "ACCESSIBILITY_TRIGGER"
    NOTIFICATION_TRIAGE = "NOTIFICATION_TRIAGE"
    URL_RISK_EVALUATION = "URL_RISK_EVALUATION"
    PRE_ACTION_INTERVENTION = "PRE_ACTION_INTERVENTION"
    SERVICE_STATUS_CHANGE = "SERVICE_STATUS_CHANGE"
    MODEL_HEALTH = "MODEL_HEALTH"
    SYNTHETIC_DEMO_RESULT = "SYNTHETIC_DEMO_RESULT"


# Documented WebSocket Message / Event Envelope
class RelayEnvelope(BaseModel):
    schema_version: str = Field("1.0.0", description="Strict schema specification version")
    message_id: str = Field(default_factory=lambda: str(uuid.uuid4()), description="Unique message UUID")
    event_id: Optional[str] = Field(None, description="Persisted event UUID if applicable")
    type: str = Field(..., description="Message classification type")
    device_id: str = Field(..., description="Associated paired device identifier")
    timestamp: str = Field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat(),
        description="ISO-8601 UTC timestamp"
    )
    payload: Dict[str, Any] = Field(default_factory=dict, description="Typed, bounded payload content")
    correlation_id: Optional[str] = Field(None, description="End-to-end cryptographic tracing hash")


# Pairing DTOs
class PairingStartRequest(BaseModel):
    device_name: str = Field("Android Device", max_length=64)
    platform: str = Field("Android", max_length=32)
    model: str = Field("Pixel", max_length=64)
    app_version: str = Field("1.0.0", max_length=32)
    capabilities: List[str] = Field(default_factory=lambda: ["accessibility", "notification", "local_ml"])


class PairingStartResponse(BaseModel):
    session_id: str
    pairing_code: str
    expires_at: str
    expires_in_seconds: int
    qr_payload: str


class PairingClaimRequest(BaseModel):
    pairing_code: str = Field(..., min_length=4, max_length=12)
    supervisor_label: str = Field("Supervisor Web Console", max_length=64)


class PairingClaimResponse(BaseModel):
    session_id: str
    status: PairingStatus
    device_name: str
    device_model: str


class PairingDecisionRequest(BaseModel):
    session_id: str
    approved: bool


class PairingDecisionResponse(BaseModel):
    session_id: str
    status: PairingStatus
    device_token: Optional[str] = None
    dashboard_token: Optional[str] = None


class PairingStatusResponse(BaseModel):
    session_id: str
    status: PairingStatus
    device_id: Optional[str] = None
    device_name: Optional[str] = None
    token: Optional[str] = None


class RevokePairingRequest(BaseModel):
    device_id: str


# Device DTOs
class DeviceHeartbeatRequest(BaseModel):
    device_id: str
    monitoring_active: bool = True
    notification_active: bool = True
    selected_apps_count: int = Field(0, ge=0)
    local_model_healthy: bool = True
    network_mode: str = Field("LOCAL_BACKEND", max_length=32)
    telemetry_enabled: bool = True
    battery_level: Optional[float] = Field(None, ge=0.0, le=1.0)


class DeviceHeartbeatResponse(BaseModel):
    status: str = "ACK"
    device_status: DeviceStatus
    server_timestamp: str
    pending_commands_count: int = 0


class DeviceStatusResponse(BaseModel):
    device_id: str
    device_name: str
    platform: str
    model: str
    app_version: str
    status: DeviceStatus
    last_heartbeat: Optional[str] = None
    monitoring_active: bool = False
    notification_active: bool = False
    selected_apps_count: int = 0
    local_model_healthy: bool = False
    network_mode: str = "UNKNOWN"
    telemetry_enabled: bool = False
    battery_level: Optional[float] = None
    paired_at: str


# Event DTOs
class EventIngestRequest(BaseModel):
    model_config = ConfigDict(extra="allow")

    event_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    device_id: str
    event_type: RelayEventType
    timestamp: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    source_app: Optional[str] = Field(None, max_length=128)
    risk_category: str = Field("UNKNOWN", max_length=64)
    intervention: str = Field("ASK", max_length=16)
    risk_score: float = Field(0.0, ge=0.0, le=2.0)
    severity: float = Field(0.0, ge=0.0, le=1.0)
    reversibility: float = Field(0.0, ge=0.0, le=1.0)
    confidence: float = Field(0.0, ge=0.0, le=1.0)
    evidence_summary: List[str] = Field(default_factory=list, max_length=20)
    model_version: str = Field("1.0.0", max_length=32)
    latency_ms: int = Field(0, ge=0)
    redaction_count: int = Field(0, ge=0)
    correlation_id: Optional[str] = Field(None, max_length=64)
    network_mode: str = Field("LOCAL_BACKEND", max_length=32)


class EventIngestResponse(BaseModel):
    event_id: str
    status: str = "INGESTED"
    server_timestamp: str


class EventSummary(BaseModel):
    event_id: str
    device_id: str
    event_type: str
    timestamp: str
    source_app: Optional[str] = None
    risk_category: str
    intervention: str
    risk_score: float
    severity: float
    reversibility: float
    confidence: float
    evidence_summary: List[str]
    model_version: str
    latency_ms: int
    redaction_count: int
    correlation_id: Optional[str] = None
    network_mode: str


# Command DTOs
class CommandQueueRequest(BaseModel):
    device_id: str
    command_type: CommandType
    payload: Dict[str, Any] = Field(default_factory=dict)
    timeout_seconds: int = Field(60, ge=5, le=300)


class CommandAckRequest(BaseModel):
    command_id: str
    device_id: str
    status: CommandStatus
    result: Dict[str, Any] = Field(default_factory=dict)
    error_message: Optional[str] = None


class CommandResponse(BaseModel):
    command_id: str
    device_id: str
    command_type: CommandType
    issuer_session: str
    payload: Dict[str, Any]
    status: CommandStatus
    created_at: str
    expires_at: str
    delivered_at: Optional[str] = None
    acknowledged_at: Optional[str] = None
    result: Dict[str, Any] = Field(default_factory=dict)
    error_message: Optional[str] = None


# WebSocket Auth Handshake
class WebSocketAuthPayload(BaseModel):
    token: str
    client_type: str = Field("device", description="'device' or 'dashboard'")
    device_id: Optional[str] = None
