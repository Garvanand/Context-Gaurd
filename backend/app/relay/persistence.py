"""
SQLite Persistence Layer for ContextGuard Device Relay.
Provides durable storage for paired devices, short-lived pairing sessions,
privacy-safe event ledger, persistent command queues, and security audit logs.
"""

from datetime import datetime, timezone, timedelta
import hashlib
import json
import os
from pathlib import Path
import secrets
import sqlite3
import threading
from typing import Any, Dict, List, Optional, Tuple

from backend.app.core.config import settings
from backend.app.core.logging import logger
from backend.app.relay.models import (
    CommandStatus,
    CommandType,
    DeviceStatus,
    PairingStatus,
    RelayEventType,
)
from backend.app.relay.privacy_filter import enforce_privacy_or_raise


def hash_token(raw_token: str) -> str:
    """Cryptographic SHA-256 hash for stored token material."""
    return hashlib.sha256(raw_token.encode("utf-8")).hexdigest()


class RelayDatabase:
    """Thread-safe SQLite database manager for the ContextGuard relay."""

    _lock = threading.Lock()

    def __init__(self, db_path: Optional[str] = None):
        self.db_path = db_path or settings.relay_db_path
        if self.db_path != ":memory:":
            os.makedirs(os.path.dirname(os.path.abspath(self.db_path)), exist_ok=True)
        self.init_db()

    def _get_connection(self) -> sqlite3.Connection:
        conn = sqlite3.connect(
            self.db_path,
            check_same_thread=False,
            timeout=10.0
        )
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA foreign_keys = ON")
        conn.execute("PRAGMA journal_mode = WAL")
        return conn

    def init_db(self) -> None:
        """Initializes database schema if tables do not exist."""
        with self._lock, self._get_connection() as conn:
            cursor = conn.cursor()

            # 1. Devices Table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS devices (
                    device_id TEXT PRIMARY KEY,
                    device_name TEXT NOT NULL,
                    platform TEXT NOT NULL,
                    model TEXT NOT NULL,
                    app_version TEXT NOT NULL,
                    status TEXT NOT NULL,
                    last_heartbeat TEXT,
                    device_token_hash TEXT NOT NULL,
                    dashboard_token_hash TEXT NOT NULL,
                    paired_at TEXT NOT NULL,
                    revoked_at TEXT,
                    monitoring_active INTEGER DEFAULT 1,
                    notification_active INTEGER DEFAULT 1,
                    selected_apps_count INTEGER DEFAULT 0,
                    local_model_healthy INTEGER DEFAULT 1,
                    network_mode TEXT DEFAULT 'LOCAL_BACKEND',
                    telemetry_enabled INTEGER DEFAULT 1,
                    battery_level REAL
                )
            """)

            # 2. Pairing Sessions Table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS pairing_sessions (
                    session_id TEXT PRIMARY KEY,
                    pairing_code TEXT UNIQUE NOT NULL,
                    device_id TEXT NOT NULL,
                    device_name TEXT NOT NULL,
                    device_model TEXT NOT NULL,
                    app_version TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    expires_at TEXT NOT NULL,
                    claimant_label TEXT,
                    device_token_hash TEXT,
                    dashboard_token_hash TEXT,
                    raw_device_token TEXT,
                    raw_dashboard_token TEXT
                )
            """)

            # 3. Events Ledger Table (Durable privacy-safe telemetry)
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS events (
                    event_id TEXT PRIMARY KEY,
                    device_id TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    source_app TEXT,
                    risk_category TEXT NOT NULL,
                    intervention TEXT NOT NULL,
                    risk_score REAL NOT NULL,
                    severity REAL NOT NULL,
                    reversibility REAL NOT NULL,
                    confidence REAL NOT NULL,
                    evidence_summary_json TEXT NOT NULL,
                    model_version TEXT NOT NULL,
                    latency_ms INTEGER NOT NULL,
                    redaction_count INTEGER NOT NULL,
                    correlation_id TEXT,
                    network_mode TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (device_id) REFERENCES devices (device_id) ON DELETE CASCADE
                )
            """)

            # 4. Commands Table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS commands (
                    command_id TEXT PRIMARY KEY,
                    device_id TEXT NOT NULL,
                    command_type TEXT NOT NULL,
                    issuer_session TEXT NOT NULL,
                    payload_json TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    expires_at TEXT NOT NULL,
                    delivered_at TEXT,
                    acknowledged_at TEXT,
                    result_json TEXT,
                    error_message TEXT,
                    correlation_id TEXT,
                    FOREIGN KEY (device_id) REFERENCES devices (device_id) ON DELETE CASCADE
                )
            """)

            # 5. Audit Log Table
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    device_id TEXT,
                    action TEXT NOT NULL,
                    actor TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    details_json TEXT
                )
            """)

            conn.commit()
            logger.info(f"ContextGuard Relay SQLite schema initialized at {self.db_path}")

    # =========================================================================
    # AUDIT LOGGING
    # =========================================================================

    def record_audit(
        self, action: str, actor: str, device_id: Optional[str] = None, details: Optional[Dict[str, Any]] = None
    ) -> None:
        """Records security audit entry."""
        now_iso = datetime.now(timezone.utc).isoformat()
        details_str = json.dumps(details or {})
        try:
            with self._lock, self._get_connection() as conn:
                conn.execute(
                    "INSERT INTO audit_logs (device_id, action, actor, timestamp, details_json) VALUES (?, ?, ?, ?, ?)",
                    (device_id, action, actor, now_iso, details_str)
                )
                conn.commit()
        except Exception as e:
            logger.error(f"Failed to record audit log: {e}")

    # =========================================================================
    # PAIRING MANAGEMENT
    # =========================================================================

    def create_pairing_session(
        self,
        device_name: str,
        platform: str,
        model: str,
        app_version: str,
        device_id: Optional[str] = None
    ) -> Tuple[str, str, str, int]:
        """
        Creates a new short-lived pairing session on behalf of the phone.
        Returns: (session_id, pairing_code, expires_at_iso, expires_in_seconds)
        """
        session_id = secrets.token_hex(16)
        # Generate 6-character unambiguous alphanumeric code (no O/0, I/1 confusion)
        charset = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        pairing_code = "".join(secrets.choice(charset) for _ in range(6))
        dev_id = device_id or f"dev_{secrets.token_hex(8)}"

        now = datetime.now(timezone.utc)
        expires_at = now + timedelta(seconds=settings.relay_pairing_code_expiry_seconds)

        with self._lock, self._get_connection() as conn:
            conn.execute(
                """
                INSERT INTO pairing_sessions (
                    session_id, pairing_code, device_id, device_name, device_model, app_version,
                    status, created_at, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    session_id,
                    pairing_code,
                    dev_id,
                    device_name,
                    model,
                    app_version,
                    PairingStatus.PENDING.value,
                    now.isoformat(),
                    expires_at.isoformat(),
                )
            )
            conn.commit()

        self.record_audit("PAIRING_START", "device", dev_id, {"session_id": session_id})
        return session_id, pairing_code, expires_at.isoformat(), settings.relay_pairing_code_expiry_seconds

    def claim_pairing_session(self, pairing_code: str, supervisor_label: str) -> Optional[Dict[str, Any]]:
        """
        Dashboard claims a pairing session using the one-time code.
        Returns session info if successfully claimed, None if invalid or expired.
        """
        clean_code = pairing_code.strip().upper()
        now = datetime.now(timezone.utc)

        with self._lock, self._get_connection() as conn:
            row = conn.execute(
                "SELECT * FROM pairing_sessions WHERE pairing_code = ?",
                (clean_code,)
            ).fetchone()

            if not row:
                return None

            # Check expiration
            expires_at = datetime.fromisoformat(row["expires_at"])
            if now > expires_at:
                conn.execute(
                    "UPDATE pairing_sessions SET status = ? WHERE session_id = ?",
                    (PairingStatus.EXPIRED.value, row["session_id"])
                )
                conn.commit()
                return None

            if row["status"] != PairingStatus.PENDING.value:
                return None

            # Mark as CLAIMED
            conn.execute(
                "UPDATE pairing_sessions SET status = ?, claimant_label = ? WHERE session_id = ?",
                (PairingStatus.CLAIMED.value, supervisor_label, row["session_id"])
            )
            conn.commit()

        self.record_audit(
            "PAIRING_CLAIMED",
            "dashboard",
            row["device_id"],
            {"session_id": row["session_id"], "supervisor_label": supervisor_label}
        )

        return {
            "session_id": row["session_id"],
            "device_id": row["device_id"],
            "device_name": row["device_name"],
            "device_model": row["device_model"],
            "status": PairingStatus.CLAIMED.value,
        }

    def get_pairing_session(self, session_id: str) -> Optional[Dict[str, Any]]:
        """Retrieves pairing session by session_id."""
        with self._lock, self._get_connection() as conn:
            row = conn.execute(
                "SELECT * FROM pairing_sessions WHERE session_id = ?",
                (session_id,)
            ).fetchone()
            if not row:
                return None
            return dict(row)

    def approve_pairing_session(self, session_id: str) -> Tuple[bool, Optional[str], Optional[str]]:
        """
        Phone approves the claim.
        Generates scoped raw tokens, persists their SHA-256 hashes, registers device in devices table.
        Returns: (success, raw_device_token, raw_dashboard_token)
        """
        now = datetime.now(timezone.utc)
        with self._lock, self._get_connection() as conn:
            row = conn.execute(
                "SELECT * FROM pairing_sessions WHERE session_id = ?",
                (session_id,)
            ).fetchone()

            if not row or row["status"] != PairingStatus.CLAIMED.value:
                return False, None, None

            # Generate scoped credentials
            raw_device_token = f"devtok_{secrets.token_urlsafe(32)}"
            raw_dashboard_token = f"dshtok_{secrets.token_urlsafe(32)}"
            device_token_hash = hash_token(raw_device_token)
            dashboard_token_hash = hash_token(raw_dashboard_token)

            # Update session
            conn.execute(
                """
                UPDATE pairing_sessions SET
                    status = ?,
                    device_token_hash = ?,
                    dashboard_token_hash = ?,
                    raw_device_token = ?,
                    raw_dashboard_token = ?
                WHERE session_id = ?
                """,
                (
                    PairingStatus.APPROVED.value,
                    device_token_hash,
                    dashboard_token_hash,
                    raw_device_token,
                    raw_dashboard_token,
                    session_id,
                )
            )

            # Insert or replace active device
            conn.execute(
                """
                INSERT INTO devices (
                    device_id, device_name, platform, model, app_version, status,
                    last_heartbeat, device_token_hash, dashboard_token_hash, paired_at,
                    monitoring_active, notification_active, selected_apps_count,
                    local_model_healthy, network_mode, telemetry_enabled
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 0, 1, 'LOCAL_BACKEND', 1)
                ON CONFLICT(device_id) DO UPDATE SET
                    device_name = excluded.device_name,
                    status = 'ONLINE',
                    last_heartbeat = excluded.last_heartbeat,
                    device_token_hash = excluded.device_token_hash,
                    dashboard_token_hash = excluded.dashboard_token_hash,
                    paired_at = excluded.paired_at,
                    revoked_at = NULL
                """,
                (
                    row["device_id"],
                    row["device_name"],
                    "Android",
                    row["device_model"],
                    row["app_version"],
                    DeviceStatus.ONLINE.value,
                    now.isoformat(),
                    device_token_hash,
                    dashboard_token_hash,
                    now.isoformat(),
                )
            )
            conn.commit()

        self.record_audit("PAIRING_APPROVED", "device", row["device_id"], {"session_id": session_id})
        return True, raw_device_token, raw_dashboard_token

    def deny_pairing_session(self, session_id: str) -> bool:
        """Phone denies the connection claim."""
        with self._lock, self._get_connection() as conn:
            row = conn.execute(
                "SELECT * FROM pairing_sessions WHERE session_id = ?",
                (session_id,)
            ).fetchone()
            if not row:
                return False

            conn.execute(
                "UPDATE pairing_sessions SET status = ? WHERE session_id = ?",
                (PairingStatus.DENIED.value, session_id)
            )
            conn.commit()

        self.record_audit("PAIRING_DENIED", "device", row["device_id"], {"session_id": session_id})
        return True

    def revoke_device(self, device_id: str) -> bool:
        """Revokes an existing device pairing."""
        now = datetime.now(timezone.utc).isoformat()
        with self._lock, self._get_connection() as conn:
            cursor = conn.execute(
                "UPDATE devices SET status = ?, revoked_at = ? WHERE device_id = ?",
                (DeviceStatus.UNPAIRED.value, now, device_id)
            )
            conn.commit()
            if cursor.rowcount == 0:
                return False

        self.record_audit("DEVICE_REVOKED", "supervisor", device_id)
        return True

    # =========================================================================
    # AUTHENTICATION VERIFICATION
    # =========================================================================

    def authenticate_device(self, device_token: str) -> Optional[Dict[str, Any]]:
        """Validates raw device token against devices table. Returns device row if valid."""
        h = hash_token(device_token)
        with self._lock, self._get_connection() as conn:
            row = conn.execute(
                "SELECT * FROM devices WHERE device_token_hash = ? AND revoked_at IS NULL",
                (h,)
            ).fetchone()
            if not row:
                return None
            return dict(row)

    def authenticate_dashboard(self, dashboard_token: str) -> Optional[Dict[str, Any]]:
        """Validates raw dashboard token against devices table. Returns device row if authorized."""
        h = hash_token(dashboard_token)
        with self._lock, self._get_connection() as conn:
            row = conn.execute(
                "SELECT * FROM devices WHERE dashboard_token_hash = ? AND revoked_at IS NULL",
                (h,)
            ).fetchone()
            if not row:
                return None
            return dict(row)

    # =========================================================================
    # DEVICE TELEMETRY & HEARTBEAT
    # =========================================================================

    def record_heartbeat(
        self,
        device_id: str,
        monitoring_active: bool,
        notification_active: bool,
        selected_apps_count: int,
        local_model_healthy: bool,
        network_mode: str,
        telemetry_enabled: bool,
        battery_level: Optional[float] = None
    ) -> bool:
        """Updates device presence timestamp and current capability status."""
        now = datetime.now(timezone.utc).isoformat()
        status = DeviceStatus.ONLINE.value if (monitoring_active and local_model_healthy) else DeviceStatus.DEGRADED.value

        with self._lock, self._get_connection() as conn:
            cursor = conn.execute(
                """
                UPDATE devices SET
                    status = ?,
                    last_heartbeat = ?,
                    monitoring_active = ?,
                    notification_active = ?,
                    selected_apps_count = ?,
                    local_model_healthy = ?,
                    network_mode = ?,
                    telemetry_enabled = ?,
                    battery_level = ?
                WHERE device_id = ? AND revoked_at IS NULL
                """,
                (
                    status,
                    now,
                    int(monitoring_active),
                    int(notification_active),
                    selected_apps_count,
                    int(local_model_healthy),
                    network_mode,
                    int(telemetry_enabled),
                    battery_level,
                    device_id,
                )
            )
            conn.commit()
            return cursor.rowcount > 0

    def get_device_status(self, device_id: str) -> Optional[Dict[str, Any]]:
        """Retrieves current verified device status, evaluating heartbeat timeout."""
        with self._lock, self._get_connection() as conn:
            row = conn.execute("SELECT * FROM devices WHERE device_id = ?", (device_id,)).fetchone()
            if not row:
                return None
            d = dict(row)

            # Evaluate heartbeat expiration
            if d["last_heartbeat"] and d["status"] != DeviceStatus.UNPAIRED.value:
                last_dt = datetime.fromisoformat(d["last_heartbeat"])
                elapsed = (datetime.now(timezone.utc) - last_dt).total_seconds()
                if elapsed > settings.relay_device_offline_timeout_seconds:
                    d["status"] = DeviceStatus.OFFLINE.value

            return d

    def get_primary_device(self) -> Optional[Dict[str, Any]]:
        """Returns the most recently paired or active device."""
        with self._lock, self._get_connection() as conn:
            row = conn.execute(
                "SELECT * FROM devices WHERE revoked_at IS NULL ORDER BY last_heartbeat DESC, paired_at DESC LIMIT 1"
            ).fetchone()
            if not row:
                return None
            d = dict(row)
            if d["last_heartbeat"]:
                last_dt = datetime.fromisoformat(d["last_heartbeat"])
                elapsed = (datetime.now(timezone.utc) - last_dt).total_seconds()
                if elapsed > settings.relay_device_offline_timeout_seconds:
                    d["status"] = DeviceStatus.OFFLINE.value
            return d

    # =========================================================================
    # EVENT INGESTION & DURABLE LEDGER
    # =========================================================================

    def ingest_event(self, event_dict: Dict[str, Any]) -> Tuple[bool, str]:
        """
        Durable, idempotent insertion of privacy-safe mobile event.
        Raises PrivacyViolationError if payload contains prohibited sensitive artifacts.
        """
        enforce_privacy_or_raise(event_dict)

        event_id = event_dict.get("event_id")
        device_id = event_dict.get("device_id")
        now = datetime.now(timezone.utc).isoformat()
        evidence_json = json.dumps(event_dict.get("evidence_summary", []))

        with self._lock, self._get_connection() as conn:
            # Check for idempotent duplicate
            existing = conn.execute("SELECT event_id FROM events WHERE event_id = ?", (event_id,)).fetchone()
            if existing:
                return True, "DUPLICATE_IGNORED"

            conn.execute(
                """
                INSERT INTO events (
                    event_id, device_id, event_type, timestamp, source_app, risk_category,
                    intervention, risk_score, severity, reversibility, confidence,
                    evidence_summary_json, model_version, latency_ms, redaction_count,
                    correlation_id, network_mode, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    event_id,
                    device_id,
                    event_dict.get("event_type", RelayEventType.PRE_ACTION_INTERVENTION.value),
                    event_dict.get("timestamp", now),
                    event_dict.get("source_app"),
                    event_dict.get("risk_category", "UNKNOWN"),
                    event_dict.get("intervention", "ASK"),
                    float(event_dict.get("risk_score", 0.0)),
                    float(event_dict.get("severity", 0.0)),
                    float(event_dict.get("reversibility", 0.0)),
                    float(event_dict.get("confidence", 0.0)),
                    evidence_json,
                    event_dict.get("model_version", "1.0.0"),
                    int(event_dict.get("latency_ms", 0)),
                    int(event_dict.get("redaction_count", 0)),
                    event_dict.get("correlation_id"),
                    event_dict.get("network_mode", "LOCAL_BACKEND"),
                    now,
                )
            )
            conn.commit()

        return True, "INSERTED"

    def get_events(self, device_id: str, limit: int = 50, offset: int = 0) -> List[Dict[str, Any]]:
        """Retrieves recent events for authorized device."""
        with self._lock, self._get_connection() as conn:
            rows = conn.execute(
                """
                SELECT * FROM events WHERE device_id = ?
                ORDER BY timestamp DESC LIMIT ? OFFSET ?
                """,
                (device_id, limit, offset)
            ).fetchall()

            results = []
            for r in rows:
                item = dict(r)
                try:
                    item["evidence_summary"] = json.loads(item.pop("evidence_summary_json"))
                except Exception:
                    item["evidence_summary"] = []
                results.append(item)
            return results

    def get_event_by_id(self, event_id: str) -> Optional[Dict[str, Any]]:
        """Retrieves single event by ID."""
        with self._lock, self._get_connection() as conn:
            row = conn.execute("SELECT * FROM events WHERE event_id = ?", (event_id,)).fetchone()
            if not row:
                return None
            item = dict(row)
            try:
                item["evidence_summary"] = json.loads(item.pop("evidence_summary_json"))
            except Exception:
                item["evidence_summary"] = []
            return item

    # =========================================================================
    # COMMAND QUEUE & ACKNOWLEDGEMENT
    # =========================================================================

    def enqueue_command(
        self,
        device_id: str,
        command_type: CommandType,
        issuer_session: str,
        payload: Dict[str, Any],
        timeout_seconds: int = 60
    ) -> str:
        """Enqueues an authorized command targeting a paired device."""
        command_id = f"cmd_{secrets.token_hex(12)}"
        now = datetime.now(timezone.utc)
        expires_at = now + timedelta(seconds=timeout_seconds)

        with self._lock, self._get_connection() as conn:
            conn.execute(
                """
                INSERT INTO commands (
                    command_id, device_id, command_type, issuer_session, payload_json,
                    status, created_at, expires_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    command_id,
                    device_id,
                    command_type.value,
                    issuer_session,
                    json.dumps(payload),
                    CommandStatus.QUEUED.value,
                    now.isoformat(),
                    expires_at.isoformat(),
                )
            )
            conn.commit()

        self.record_audit("COMMAND_ENQUEUED", "supervisor", device_id, {"command_id": command_id, "type": command_type.value})
        return command_id

    def get_pending_commands(self, device_id: str) -> List[Dict[str, Any]]:
        """Retrieves queued commands eligible for delivery, expiring stale ones."""
        now = datetime.now(timezone.utc).isoformat()
        with self._lock, self._get_connection() as conn:
            # Expire stale commands first
            conn.execute(
                """
                UPDATE commands SET status = ?
                WHERE device_id = ? AND status = ? AND expires_at < ?
                """,
                (CommandStatus.EXPIRED.value, device_id, CommandStatus.QUEUED.value, now)
            )
            conn.commit()

            rows = conn.execute(
                """
                SELECT * FROM commands
                WHERE device_id = ? AND status = ?
                ORDER BY created_at ASC
                """,
                (device_id, CommandStatus.QUEUED.value)
            ).fetchall()

            res = []
            for r in rows:
                item = dict(r)
                item["payload"] = json.loads(item.pop("payload_json"))
                res.append(item)
            return res

    def mark_command_delivered(self, command_id: str) -> bool:
        """Marks a queued command as DELIVERED upon transmission."""
        now = datetime.now(timezone.utc).isoformat()
        with self._lock, self._get_connection() as conn:
            cursor = conn.execute(
                "UPDATE commands SET status = ?, delivered_at = ? WHERE command_id = ? AND status = ?",
                (CommandStatus.DELIVERED.value, now, command_id, CommandStatus.QUEUED.value)
            )
            conn.commit()
            return cursor.rowcount > 0

    def acknowledge_command(
        self,
        command_id: str,
        device_id: str,
        status: CommandStatus,
        result: Optional[Dict[str, Any]] = None,
        error_message: Optional[str] = None
    ) -> bool:
        """Records device acknowledgement and execution outcome."""
        now = datetime.now(timezone.utc).isoformat()
        res_str = json.dumps(result or {})
        with self._lock, self._get_connection() as conn:
            cursor = conn.execute(
                """
                UPDATE commands SET
                    status = ?,
                    acknowledged_at = ?,
                    result_json = ?,
                    error_message = ?
                WHERE command_id = ? AND device_id = ?
                """,
                (status.value, now, res_str, error_message, command_id, device_id)
            )
            conn.commit()
            success = cursor.rowcount > 0

        if success:
            self.record_audit(
                f"COMMAND_ACK_{status.value}",
                "device",
                device_id,
                {"command_id": command_id, "status": status.value, "error": error_message}
            )
        return success

    def get_command(self, command_id: str) -> Optional[Dict[str, Any]]:
        """Retrieves command details by ID."""
        with self._lock, self._get_connection() as conn:
            row = conn.execute("SELECT * FROM commands WHERE command_id = ?", (command_id,)).fetchone()
            if not row:
                return None
            item = dict(row)
            item["payload"] = json.loads(item.pop("payload_json"))
            if item["result_json"]:
                item["result"] = json.loads(item.pop("result_json"))
            else:
                item["result"] = {}
                item.pop("result_json", None)
            return item

    def get_recent_commands(self, device_id: str, limit: int = 20) -> List[Dict[str, Any]]:
        """Retrieves recent commands for dashboard view."""
        with self._lock, self._get_connection() as conn:
            rows = conn.execute(
                "SELECT * FROM commands WHERE device_id = ? ORDER BY created_at DESC LIMIT ?",
                (device_id, limit)
            ).fetchall()
            res = []
            for r in rows:
                item = dict(r)
                item["payload"] = json.loads(item.pop("payload_json"))
                item["result"] = json.loads(item.pop("result_json")) if item.get("result_json") else {}
                res.append(item)
            return res
