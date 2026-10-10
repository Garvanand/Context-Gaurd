import React, { useState, useEffect } from 'react';
import {
  Smartphone,
  Activity,
  Shield,
  Radio,
  RefreshCw,
  Terminal,
  AlertTriangle,
  CheckCircle,
  HelpCircle,
  XCircle,
} from 'lucide-react';
import type { RelayDevice, RelayTelemetryEvent, RelayCommand } from '../../types';
import {
  fetchPrimaryDevice,
  fetchDeviceEvents,
  fetchRecentCommands,
  queueDeviceCommand,
  relaySocketClient,
  type RelayConnectionStatus,
  revokePairedDevice,
} from '../../services/relay';
import { DevicePairingModal } from './DevicePairingModal';

interface LiveDeviceRelayPanelProps {
  onDeviceChange?: (device: RelayDevice | null) => void;
}

export const LiveDeviceRelayPanel: React.FC<LiveDeviceRelayPanelProps> = ({ onDeviceChange }) => {
  const [device, setDevice] = useState<RelayDevice | null>(null);
  const [events, setEvents] = useState<RelayTelemetryEvent[]>([]);
  const [commands, setCommands] = useState<RelayCommand[]>([]);
  const [socketStatus, setSocketStatus] = useState<RelayConnectionStatus>('OFFLINE');
  const [loading, setLoading] = useState(true);
  const [isPairingModalOpen, setIsPairingModalOpen] = useState(false);
  const [commandLoading, setCommandLoading] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<RelayTelemetryEvent | null>(null);

  const loadData = async () => {
    try {
      setLoading(true);
      const devRes = await fetchPrimaryDevice();
      setDevice(devRes.device);
      if (onDeviceChange) onDeviceChange(devRes.device);

      if (devRes.device?.device_id) {
        const [evList, cmdList] = await Promise.all([
          fetchDeviceEvents(devRes.device.device_id).catch(() => []),
          fetchRecentCommands(devRes.device.device_id).catch(() => []),
        ]);
        setEvents(evList);
        setCommands(cmdList);
      }
    } catch (e) {
      console.error('Failed to load relay data', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();

    // Connect WebSocket
    relaySocketClient.onStatusChange = (st) => setSocketStatus(st);
    relaySocketClient.onPresenceUpdate = (devInfo) => {
      setDevice((prev) => (prev ? { ...prev, ...devInfo } : devInfo));
    };
    relaySocketClient.onTelemetryEvent = (ev) => {
      setEvents((prev) => [ev, ...prev.filter((item) => item.event_id !== ev.event_id)]);
    };
    relaySocketClient.onCommandUpdate = (update) => {
      setCommands((prev) =>
        prev.map((c) =>
          c.command_id === update.command_id
            ? { ...c, status: update.status, result: update.result, error_message: update.error_message }
            : c
        )
      );
    };
    relaySocketClient.onPairingSuccess = () => {
      loadData();
    };

    relaySocketClient.connect();

    return () => {
      relaySocketClient.disconnect();
    };
  }, []);

  const handleSendCommand = async (cmdType: string, payload: Record<string, any> = {}) => {
    if (!device?.device_id) return;
    setCommandLoading(true);
    try {
      const newCmd = await queueDeviceCommand(device.device_id, cmdType, payload);
      setCommands((prev) => [newCmd, ...prev]);
    } catch (e) {
      console.error('Failed to issue command', e);
    } finally {
      setCommandLoading(false);
    }
  };

  const handleRevoke = async () => {
    if (!device?.device_id) return;
    if (window.confirm(`Revoke pairing for ${device.device_name}?`)) {
      await revokePairedDevice(device.device_id);
      setDevice(null);
      setEvents([]);
      setCommands([]);
      if (onDeviceChange) onDeviceChange(null);
    }
  };

  const getInterventionBadge = (intervention: string) => {
    switch (intervention) {
      case 'ACT':
        return { color: 'var(--act, #22c55e)', bg: 'rgba(34, 197, 94, 0.15)', icon: CheckCircle };
      case 'ASK':
        return { color: 'var(--ask, #eab308)', bg: 'rgba(234, 179, 8, 0.15)', icon: HelpCircle };
      case 'WARN':
        return { color: 'var(--warn, #f97316)', bg: 'rgba(249, 115, 22, 0.15)', icon: AlertTriangle };
      case 'STOP':
        return { color: 'var(--stop, #ef4444)', bg: 'rgba(239, 68, 68, 0.15)', icon: XCircle };
      default:
        return { color: 'var(--muted-text, #94a3b8)', bg: 'rgba(255, 255, 255, 0.1)', icon: Shield };
    }
  };

  return (
    <div id="section-relay" style={{ marginBottom: '32px' }}>
      {/* Section Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '20px',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h2 style={{ fontSize: '22px', fontWeight: 700, margin: 0 }}>
              Live Mobile Relay & Real-Time Telemetry
            </h2>
            <span
              style={{
                fontSize: '11px',
                fontWeight: 700,
                padding: '4px 8px',
                borderRadius: '6px',
                backgroundColor:
                  socketStatus === 'CONNECTED' ? 'rgba(34, 197, 94, 0.15)' : 'rgba(234, 179, 8, 0.15)',
                color: socketStatus === 'CONNECTED' ? 'var(--act, #22c55e)' : 'var(--ask, #eab308)',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '5px',
              }}
            >
              <Radio size={12} className={socketStatus === 'CONNECTED' ? 'animate-pulse' : ''} />
              WS {socketStatus}
            </span>
          </div>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--muted-text, #94a3b8)' }}>
            Authenticated bidirectional synchronization between Android client, FastAPI relay, and examiner console
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={loadData}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 14px',
              borderRadius: '8px',
              backgroundColor: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid var(--border-color, rgba(255, 255, 255, 0.12))',
              color: 'var(--text-main, #f8fafc)',
              cursor: 'pointer',
              fontSize: '13px',
              fontWeight: 500,
            }}
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            Refresh
          </button>

          {!device || device.status === 'UNPAIRED' ? (
            <button
              onClick={() => setIsPairingModalOpen(true)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '8px 16px',
                borderRadius: '8px',
                backgroundColor: 'var(--ion-cyan, #00e5ff)',
                color: '#0a0e17',
                border: 'none',
                fontWeight: 700,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              <Smartphone size={16} />
              Connect a Device
            </button>
          ) : (
            <button
              onClick={handleRevoke}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '8px 14px',
                borderRadius: '8px',
                backgroundColor: 'rgba(239, 68, 68, 0.15)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                color: '#ef4444',
                cursor: 'pointer',
                fontSize: '13px',
                fontWeight: 600,
              }}
            >
              Unpair Device
            </button>
          )}
        </div>
      </div>

      {/* Grid: Device Status + Command Center */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: '20px',
          marginBottom: '24px',
        }}
      >
        {/* Device Status Card */}
        <div
          style={{
            backgroundColor: 'var(--surface-dark, #121824)',
            border: '1px solid var(--border-color, rgba(255, 255, 255, 0.12))',
            borderRadius: '14px',
            padding: '20px',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div
                style={{
                  padding: '8px',
                  borderRadius: '10px',
                  backgroundColor: device?.status === 'ONLINE' ? 'rgba(34, 197, 94, 0.15)' : 'rgba(255, 255, 255, 0.05)',
                  color: device?.status === 'ONLINE' ? 'var(--act, #22c55e)' : 'var(--muted-text, #94a3b8)',
                }}
              >
                <Smartphone size={22} />
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>
                  {device ? `${device.device_name}` : 'No Device Paired'}
                </h3>
                <span style={{ fontSize: '12px', color: 'var(--muted-text, #94a3b8)' }}>
                  {device ? `${device.model} • ${device.platform} (App v${device.app_version})` : 'Waiting for device pairing'}
                </span>
              </div>
            </div>

            <span
              style={{
                fontSize: '11px',
                fontWeight: 700,
                padding: '4px 8px',
                borderRadius: '6px',
                backgroundColor:
                  device?.status === 'ONLINE'
                    ? 'rgba(34, 197, 94, 0.15)'
                    : device?.status === 'DEGRADED'
                    ? 'rgba(234, 179, 8, 0.15)'
                    : 'rgba(239, 68, 68, 0.15)',
                color:
                  device?.status === 'ONLINE'
                    ? 'var(--act, #22c55e)'
                    : device?.status === 'DEGRADED'
                    ? 'var(--ask, #eab308)'
                    : 'var(--stop, #ef4444)',
              }}
            >
              {device?.status || 'UNPAIRED'}
            </span>
          </div>

          {device ? (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', fontSize: '12px' }}>
              <div style={{ padding: '8px 12px', backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted-text, #94a3b8)' }}>Screen Interceptor:</span>
                <div style={{ fontWeight: 600, color: device.monitoring_active ? 'var(--act, #22c55e)' : 'var(--warn, #f97316)' }}>
                  {device.monitoring_active ? '● ACTIVE' : '○ DISABLED'}
                </div>
              </div>

              <div style={{ padding: '8px 12px', backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted-text, #94a3b8)' }}>Notification Sentinel:</span>
                <div style={{ fontWeight: 600, color: device.notification_active ? 'var(--act, #22c55e)' : 'var(--warn, #f97316)' }}>
                  {device.notification_active ? '● ACTIVE' : '○ STANDBY'}
                </div>
              </div>

              <div style={{ padding: '8px 12px', backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted-text, #94a3b8)' }}>Monitored Apps:</span>
                <div style={{ fontWeight: 600, color: 'var(--ion-cyan, #00e5ff)' }}>
                  {device.selected_apps_count} Allowlisted
                </div>
              </div>

              <div style={{ padding: '8px 12px', backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted-text, #94a3b8)' }}>Telemetry Sync:</span>
                <div style={{ fontWeight: 600, color: device.telemetry_enabled ? 'var(--act, #22c55e)' : 'var(--warn, #f97316)' }}>
                  {device.telemetry_enabled ? 'ENABLED' : 'OPTED-OUT'}
                </div>
              </div>

              <div style={{ gridColumn: 'span 2', padding: '8px 12px', backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted-text, #94a3b8)' }}>Last Heartbeat:</span>
                <div style={{ fontWeight: 500, fontFamily: 'monospace', fontSize: '11px', marginTop: '2px' }}>
                  {device.last_heartbeat ? new Date(device.last_heartbeat).toLocaleTimeString() : 'Never'}
                </div>
              </div>
            </div>
          ) : (
            <div style={{ padding: '20px', textAlign: 'center', color: 'var(--muted-text, #94a3b8)', fontSize: '13px' }}>
              No phone paired. Click <strong>"Connect a Device"</strong> to pair your Android device using a one-time code.
            </div>
          )}
        </div>

        {/* Command Center Card */}
        <div
          style={{
            backgroundColor: 'var(--surface-dark, #121824)',
            border: '1px solid var(--border-color, rgba(255, 255, 255, 0.12))',
            borderRadius: '14px',
            padding: '20px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
            <Terminal size={18} color="var(--ion-cyan, #00e5ff)" />
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>Command Center</h3>
          </div>
          <p style={{ margin: '0 0 14px 0', fontSize: '12px', color: 'var(--muted-text, #94a3b8)' }}>
            Issue authorized diagnostic and evaluation commands to the connected phone
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '14px' }}>
            <button
              onClick={() => handleSendCommand('REQUEST_STATUS')}
              disabled={!device || commandLoading}
              style={{
                padding: '10px',
                borderRadius: '8px',
                backgroundColor: 'rgba(0, 229, 255, 0.08)',
                border: '1px solid rgba(0, 229, 255, 0.3)',
                color: 'var(--ion-cyan, #00e5ff)',
                fontSize: '12px',
                fontWeight: 600,
                cursor: device ? 'pointer' : 'not-allowed',
                opacity: device ? 1 : 0.5,
              }}
            >
              Request Status
            </button>

            <button
              onClick={() => handleSendCommand('REQUEST_DIAGNOSTICS')}
              disabled={!device || commandLoading}
              style={{
                padding: '10px',
                borderRadius: '8px',
                backgroundColor: 'rgba(168, 85, 247, 0.08)',
                border: '1px solid rgba(168, 85, 247, 0.3)',
                color: '#c084fc',
                fontSize: '12px',
                fontWeight: 600,
                cursor: device ? 'pointer' : 'not-allowed',
                opacity: device ? 1 : 0.5,
              }}
            >
              Request Diagnostics
            </button>

            <button
              onClick={() => handleSendCommand('RUN_SYNTHETIC_DEMO')}
              disabled={!device || commandLoading}
              style={{
                padding: '10px',
                borderRadius: '8px',
                backgroundColor: 'rgba(34, 197, 94, 0.08)',
                border: '1px solid rgba(34, 197, 94, 0.3)',
                color: 'var(--act, #22c55e)',
                fontSize: '12px',
                fontWeight: 600,
                cursor: device ? 'pointer' : 'not-allowed',
                opacity: device ? 1 : 0.5,
              }}
            >
              Run Synthetic Demo
            </button>

            <button
              onClick={() => handleSendCommand('REQUEST_CONFIG_REFRESH')}
              disabled={!device || commandLoading}
              style={{
                padding: '10px',
                borderRadius: '8px',
                backgroundColor: 'rgba(234, 179, 8, 0.08)',
                border: '1px solid rgba(234, 179, 8, 0.3)',
                color: 'var(--ask, #eab308)',
                fontSize: '12px',
                fontWeight: 600,
                cursor: device ? 'pointer' : 'not-allowed',
                opacity: device ? 1 : 0.5,
              }}
            >
              Refresh Config
            </button>
          </div>

          {/* Recent Commands Feed */}
          <div style={{ borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: '10px' }}>
            <span style={{ fontSize: '11px', color: 'var(--muted-text, #94a3b8)', fontWeight: 600 }}>
              RECENT COMMAND DISPATCHES
            </span>
            <div style={{ maxHeight: '110px', overflowY: 'auto', marginTop: '6px' }}>
              {commands.length === 0 ? (
                <div style={{ fontSize: '12px', color: 'var(--muted-text, #94a3b8)', padding: '6px 0' }}>
                  No commands issued yet
                </div>
              ) : (
                commands.slice(0, 3).map((cmd) => (
                  <div
                    key={cmd.command_id}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      fontSize: '11px',
                      padding: '4px 0',
                      borderBottom: '1px solid rgba(255,255,255,0.04)',
                    }}
                  >
                    <span>{cmd.command_type}</span>
                    <span
                      style={{
                        color:
                          cmd.status === 'SUCCEEDED'
                            ? 'var(--act, #22c55e)'
                            : cmd.status === 'FAILED'
                            ? 'var(--stop, #ef4444)'
                            : 'var(--ask, #eab308)',
                        fontWeight: 600,
                      }}
                    >
                      {cmd.status}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Live Mobile Telemetry Stream */}
      <div
        style={{
          backgroundColor: 'var(--surface-dark, #121824)',
          border: '1px solid var(--border-color, rgba(255, 255, 255, 0.12))',
          borderRadius: '14px',
          padding: '20px',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Activity size={18} color="var(--ion-cyan, #00e5ff)" />
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>
              Real-Time Mobile Event Stream ({events.length})
            </h3>
          </div>
          <span style={{ fontSize: '12px', color: 'var(--muted-text, #94a3b8)' }}>
            Zero raw screens or secrets persisted • Cryptographically correlated
          </span>
        </div>

        {events.length === 0 ? (
          <div style={{ padding: '36px', textAlign: 'center', color: 'var(--muted-text, #94a3b8)', fontSize: '13px' }}>
            <Radio size={28} style={{ margin: '0 auto 10px', opacity: 0.5 }} />
            <div>Waiting for live mobile events from paired Android device...</div>
            <p style={{ margin: '6px 0 0', fontSize: '12px' }}>
              Trigger an analysis or notification triage on phone to observe live telemetry.
            </p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', maxHeight: '420px', overflowY: 'auto' }}>
            {events.map((ev) => {
              const badge = getInterventionBadge(ev.intervention);
              const BadgeIcon = badge.icon;
              return (
                <div
                  key={ev.event_id}
                  onClick={() => setSelectedEvent(ev)}
                  style={{
                    padding: '12px 16px',
                    borderRadius: '10px',
                    backgroundColor: 'rgba(255, 255, 255, 0.02)',
                    border: '1px solid rgba(255, 255, 255, 0.08)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    cursor: 'pointer',
                    transition: 'background 0.2s',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                    <div
                      style={{
                        padding: '8px',
                        borderRadius: '8px',
                        backgroundColor: badge.bg,
                        color: badge.color,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      <BadgeIcon size={18} />
                    </div>

                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontWeight: 600, fontSize: '14px' }}>
                          {ev.source_app || 'ContextGuard Engine'}
                        </span>
                        <span
                          style={{
                            fontSize: '11px',
                            fontWeight: 700,
                            padding: '2px 6px',
                            borderRadius: '4px',
                            backgroundColor: badge.bg,
                            color: badge.color,
                          }}
                        >
                          {ev.intervention}
                        </span>
                        <span style={{ fontSize: '11px', color: 'var(--muted-text, #94a3b8)' }}>
                          ρ = {ev.risk_score.toFixed(3)}
                        </span>
                      </div>

                      <div style={{ fontSize: '12px', color: 'var(--muted-text, #94a3b8)', marginTop: '2px' }}>
                        {ev.evidence_summary?.[0] || ev.risk_category}
                      </div>
                    </div>
                  </div>

                  <div style={{ textAlign: 'right', fontSize: '11px', color: 'var(--muted-text, #94a3b8)' }}>
                    <div>{new Date(ev.timestamp).toLocaleTimeString()}</div>
                    <div style={{ fontFamily: 'monospace', marginTop: '2px' }}>
                      {ev.latency_ms}ms • {ev.redaction_count} redacted
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <DevicePairingModal
        isOpen={isPairingModalOpen}
        onClose={() => setIsPairingModalOpen(false)}
        onPairingComplete={loadData}
      />

      {selectedEvent && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.75)',
            backdropFilter: 'blur(6px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '20px',
          }}
        >
          <div
            style={{
              backgroundColor: 'var(--surface-dark, #121824)',
              border: '1px solid var(--border-color, rgba(255, 255, 255, 0.12))',
              borderRadius: '16px',
              width: '100%',
              maxWidth: '560px',
              padding: '24px',
              boxShadow: '0 20px 40px rgba(0,0,0,0.6)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 600 }}>Event Telemetry Record</h3>
              <button
                onClick={() => setSelectedEvent(null)}
                style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', fontSize: '18px' }}
              >
                ✕
              </button>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', fontSize: '12px', marginBottom: '14px' }}>
              <div><strong>Event ID:</strong> {selectedEvent.event_id}</div>
              <div><strong>Source App:</strong> {selectedEvent.source_app || 'N/A'}</div>
              <div><strong>Intervention:</strong> {selectedEvent.intervention}</div>
              <div><strong>Risk Score (ρ):</strong> {selectedEvent.risk_score.toFixed(3)}</div>
              <div><strong>Severity (s):</strong> {selectedEvent.severity.toFixed(3)}</div>
              <div><strong>Reversibility (r):</strong> {selectedEvent.reversibility.toFixed(3)}</div>
              <div><strong>Confidence (c):</strong> {selectedEvent.confidence.toFixed(3)}</div>
              <div><strong>Latency:</strong> {selectedEvent.latency_ms} ms</div>
              <div><strong>Redacted Tokens:</strong> {selectedEvent.redaction_count}</div>
              <div><strong>Network Mode:</strong> {selectedEvent.network_mode}</div>
            </div>
            <div style={{ fontSize: '12px', marginTop: '10px' }}>
              <strong>Evidence Items:</strong>
              <ul style={{ margin: '6px 0 0', paddingLeft: '20px', color: '#cbd5e1' }}>
                {selectedEvent.evidence_summary.map((ev, i) => (
                  <li key={i}>{ev}</li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
