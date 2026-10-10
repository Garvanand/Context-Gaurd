"""
In-Process WebSocket Connection Manager and Pub/Sub Event Broker.

Coordinates real-time WebSocket connections between:
- Paired Android clients (live telemetry, command reception, acknowledgements)
- Supervisor Web Dashboards (live event stream, device presence, command dispatch)

EXTENSION POINT NOTE:
This in-process broker is optimized for single-server workstation deployment.
If running across multiple backend worker processes or horizontal replicas,
the broker should be backed by an external distributed Pub/Sub mechanism
(such as Redis Pub/Sub or RabbitMQ).
"""

import asyncio
from datetime import datetime, timezone
import json
from typing import Any, Dict, Optional, Set
from fastapi import WebSocket, WebSocketDisconnect

from backend.app.core.logging import logger
from backend.app.relay.models import (
    CommandStatus,
    DeviceStatus,
    RelayEnvelope,
)
from backend.app.relay.persistence import RelayDatabase


class RelayConnectionManager:
    """Manages active WebSockets for devices and dashboards with live fan-out."""

    def __init__(self, db: RelayDatabase):
        self.db = db
        # Active device connections: device_id -> WebSocket
        self._device_sockets: Dict[str, WebSocket] = {}
        # Active dashboard connections: socket_id -> (WebSocket, Set[device_id])
        self._dashboard_sockets: Dict[str, WebSocket] = {}
        self._dashboard_subscriptions: Dict[str, Set[str]] = {}  # device_id -> Set[socket_id]
        self._lock = asyncio.Lock()

    # =========================================================================
    # DEVICE SOCKET LIFECYCLE
    # =========================================================================

    async def connect_device(self, device_id: str, websocket: WebSocket) -> None:
        """Registers an authenticated device WebSocket connection."""
        async with self._lock:
            # If an existing socket is present for this device, gracefully close it
            if device_id in self._device_sockets:
                try:
                    await self._device_sockets[device_id].close(code=1000, reason="Replaced by new connection")
                except Exception:
                    pass
            self._device_sockets[device_id] = websocket
            logger.info(f"Relay: Device '{device_id}' connected via WebSocket")

        # Notify subscribed dashboards of ONLINE transition
        await self.notify_device_presence(device_id, DeviceStatus.ONLINE)

        # Automatically flush pending commands queued while device was offline
        await self.flush_pending_commands(device_id)

    async def disconnect_device(self, device_id: str, websocket: WebSocket) -> None:
        """Removes a device WebSocket upon disconnect."""
        async with self._lock:
            if self._device_sockets.get(device_id) == websocket:
                del self._device_sockets[device_id]
                logger.info(f"Relay: Device '{device_id}' disconnected")

        # Notify dashboards of offline state
        await self.notify_device_presence(device_id, DeviceStatus.OFFLINE)

    def is_device_connected(self, device_id: str) -> bool:
        """Returns True if device currently holds an active open WebSocket."""
        return device_id in self._device_sockets

    # =========================================================================
    # DASHBOARD SOCKET LIFECYCLE
    # =========================================================================

    async def connect_dashboard(
        self, socket_id: str, websocket: WebSocket, subscribed_device_id: Optional[str] = None
    ) -> None:
        """Registers an authenticated supervisor dashboard WebSocket connection."""
        async with self._lock:
            self._dashboard_sockets[socket_id] = websocket
            dev_id = subscribed_device_id or "*"
            if dev_id not in self._dashboard_subscriptions:
                self._dashboard_subscriptions[dev_id] = set()
            self._dashboard_subscriptions[dev_id].add(socket_id)
            logger.info(f"Relay: Dashboard '{socket_id}' connected (subscribed to '{dev_id}')")

    async def disconnect_dashboard(self, socket_id: str) -> None:
        """Removes a dashboard WebSocket upon disconnect."""
        async with self._lock:
            self._dashboard_sockets.pop(socket_id, None)
            for dev_id, socket_set in list(self._dashboard_subscriptions.items()):
                socket_set.discard(socket_id)
                if not socket_set and dev_id != "*":
                    del self._dashboard_subscriptions[dev_id]
            logger.info(f"Relay: Dashboard '{socket_id}' disconnected")

    # =========================================================================
    # BROADCAST & LIVE FAN-OUT
    # =========================================================================

    async def broadcast_to_dashboards(self, device_id: str, envelope: RelayEnvelope) -> int:
        """
        Broadcasts an envelope to all dashboards subscribed to device_id or wildcard '*'.
        Returns number of successful deliveries.
        """
        target_socket_ids: Set[str] = set()
        async with self._lock:
            target_socket_ids.update(self._dashboard_subscriptions.get(device_id, set()))
            target_socket_ids.update(self._dashboard_subscriptions.get("*", set()))

        if not target_socket_ids:
            return 0

        payload_json = envelope.model_dump_json()
        deliveries = 0
        dead_sockets = []

        for sid in target_socket_ids:
            ws = self._dashboard_sockets.get(sid)
            if ws:
                try:
                    await ws.send_text(payload_json)
                    deliveries += 1
                except Exception as e:
                    logger.warning(f"Failed to send to dashboard '{sid}': {e}")
                    dead_sockets.append(sid)

        for ds in dead_sockets:
            await self.disconnect_dashboard(ds)

        return deliveries

    async def notify_device_presence(self, device_id: str, status: DeviceStatus) -> None:
        """Broadcasts real-time presence change to supervisors."""
        dev_info = self.db.get_device_status(device_id) or {}
        env = RelayEnvelope(
            type="DEVICE_PRESENCE",
            device_id=device_id,
            payload={
                "status": status.value,
                "device_name": dev_info.get("device_name", "Android Device"),
                "model": dev_info.get("model", ""),
                "last_heartbeat": dev_info.get("last_heartbeat"),
                "monitoring_active": bool(dev_info.get("monitoring_active", 0)),
                "notification_active": bool(dev_info.get("notification_active", 0)),
                "selected_apps_count": dev_info.get("selected_apps_count", 0),
                "local_model_healthy": bool(dev_info.get("local_model_healthy", 0)),
                "network_mode": dev_info.get("network_mode", "UNKNOWN"),
                "telemetry_enabled": bool(dev_info.get("telemetry_enabled", 0)),
            }
        )
        await self.broadcast_to_dashboards(device_id, env)

    # =========================================================================
    # COMMAND DISPATCH
    # =========================================================================

    async def deliver_command(self, command_id: str, device_id: str, command_data: Dict[str, Any]) -> bool:
        """
        Attempts immediate WebSocket delivery of an enqueued command to the device.
        If device is connected, transmits command and transitions status to DELIVERED.
        """
        ws = None
        async with self._lock:
            ws = self._device_sockets.get(device_id)

        if not ws:
            logger.info(f"Relay: Device '{device_id}' offline; command '{command_id}' remains QUEUED")
            return False

        envelope = RelayEnvelope(
            type="COMMAND_DELIVER",
            device_id=device_id,
            payload={
                "command_id": command_id,
                "command_type": command_data.get("command_type"),
                "payload": command_data.get("payload", {}),
                "expires_at": command_data.get("expires_at"),
            }
        )

        try:
            await ws.send_text(envelope.model_dump_json())
            self.db.mark_command_delivered(command_id)
            logger.info(f"Relay: Command '{command_id}' delivered to device '{device_id}'")

            # Notify dashboard that command transitioned to DELIVERED
            ack_env = RelayEnvelope(
                type="COMMAND_STATUS_UPDATE",
                device_id=device_id,
                payload={"command_id": command_id, "status": CommandStatus.DELIVERED.value}
            )
            await self.broadcast_to_dashboards(device_id, ack_env)
            return True
        except Exception as e:
            logger.error(f"Failed to deliver command '{command_id}' to device '{device_id}': {e}")
            return False

    async def flush_pending_commands(self, device_id: str) -> int:
        """Delivers any pending commands that were queued while device was offline."""
        pending = self.db.get_pending_commands(device_id)
        count = 0
        for cmd in pending:
            sent = await self.deliver_command(cmd["command_id"], device_id, cmd)
            if sent:
                count += 1
        return count
