import type { RelayDevice, RelayTelemetryEvent, RelayCommand } from '../types';

const BASE_URL = '/api/v1/relay';

// Local storage keys for credentials
const TOKEN_KEY = 'contextguard_dashboard_token';
const DEVICE_KEY = 'contextguard_paired_device_id';

export function getStoredDashboardToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function saveDashboardToken(token: string, deviceId?: string): void {
  localStorage.setItem(TOKEN_KEY, token);
  if (deviceId) localStorage.setItem(DEVICE_KEY, deviceId);
}

export function clearDashboardCredentials(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(DEVICE_KEY);
}

async function handleRelayResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const errorText = await res.text();
    let detail = errorText;
    try {
      const parsed = JSON.parse(errorText);
      detail = parsed.detail || errorText;
    } catch (_) {}
    throw new Error(detail);
  }
  return res.json();
}

// =============================================================================
// REST ENDPOINTS
// =============================================================================

export async function claimPairingSession(
  pairingCode: string,
  supervisorLabel = 'Supervisor Web Console'
): Promise<{ session_id: string; status: string; device_name: string; device_model: string }> {
  const res = await fetch(`${BASE_URL}/pairing/claim`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ pairing_code: pairingCode.trim().toUpperCase(), supervisor_label: supervisorLabel }),
  });
  return handleRelayResponse(res);
}

export async function getPairingStatus(sessionId: string): Promise<{
  session_id: string;
  status: string;
  device_id?: string;
  device_name?: string;
  token?: string;
}> {
  const res = await fetch(`${BASE_URL}/pairing/status/${sessionId}?client_type=dashboard`);
  return handleRelayResponse(res);
}

export async function fetchPrimaryDevice(): Promise<{
  status: string;
  device: RelayDevice | null;
  message?: string;
}> {
  const res = await fetch(`${BASE_URL}/device/primary`);
  return handleRelayResponse(res);
}

export async function fetchDeviceEvents(deviceId: string, limit = 50): Promise<RelayTelemetryEvent[]> {
  const token = getStoredDashboardToken();
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}/events/${deviceId}?limit=${limit}`, { headers });
  return handleRelayResponse<RelayTelemetryEvent[]>(res);
}

export async function queueDeviceCommand(
  deviceId: string,
  commandType: string,
  payload: Record<string, any> = {}
): Promise<RelayCommand> {
  const token = getStoredDashboardToken();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}/commands/queue`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ device_id: deviceId, command_type: commandType, payload }),
  });
  return handleRelayResponse<RelayCommand>(res);
}

export async function fetchRecentCommands(deviceId: string): Promise<RelayCommand[]> {
  const token = getStoredDashboardToken();
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}/commands/recent/${deviceId}`, { headers });
  return handleRelayResponse<RelayCommand[]>(res);
}

export async function revokePairedDevice(deviceId: string): Promise<void> {
  const token = getStoredDashboardToken();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}/pairing/revoke`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ device_id: deviceId }),
  });
  await handleRelayResponse(res);
  clearDashboardCredentials();
}

// =============================================================================
// WEBSOCKET CLIENT
// =============================================================================

export type RelayConnectionStatus = 'CONNECTING' | 'CONNECTED' | 'DEGRADED' | 'OFFLINE';

export class RelayWebSocketClient {
  private socket: WebSocket | null = null;
  private reconnectTimeout: any = null;
  private reconnectAttempts = 0;
  private isExplicitlyClosed = false;

  public status: RelayConnectionStatus = 'OFFLINE';
  public onStatusChange?: (status: RelayConnectionStatus) => void;
  public onPresenceUpdate?: (deviceInfo: any) => void;
  public onTelemetryEvent?: (event: RelayTelemetryEvent) => void;
  public onCommandUpdate?: (commandUpdate: any) => void;
  public onPairingSuccess?: (data: any) => void;

  public connect(customDeviceId?: string) {
    this.isExplicitlyClosed = false;
    this.setStatus('CONNECTING');

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const wsUrl = `${protocol}//${host}/api/v1/relay/ws/dashboard`;

    try {
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        this.reconnectAttempts = 0;
        const token = getStoredDashboardToken() || '';
        const devId = customDeviceId || localStorage.getItem(DEVICE_KEY) || '*';

        // Send initial auth
        this.socket?.send(
          JSON.stringify({
            token,
            device_id: devId,
          })
        );
      };

      this.socket.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);
          if (msg.type === 'AUTH_SUCCESS') {
            this.setStatus('CONNECTED');
          } else if (msg.type === 'DEVICE_PRESENCE') {
            if (this.onPresenceUpdate) this.onPresenceUpdate(msg.payload);
          } else if (msg.type === 'TELEMETRY_EVENT') {
            if (this.onTelemetryEvent) this.onTelemetryEvent(msg.payload as RelayTelemetryEvent);
          } else if (msg.type === 'COMMAND_STATUS_UPDATE') {
            if (this.onCommandUpdate) this.onCommandUpdate(msg.payload);
          } else if (msg.type === 'PAIRING_SUCCESS') {
            if (this.onPairingSuccess) this.onPairingSuccess(msg.payload);
          }
        } catch (e) {
          console.error('Error parsing relay message', e);
        }
      };

      this.socket.onerror = () => {
        this.setStatus('DEGRADED');
      };

      this.socket.onclose = () => {
        this.setStatus('OFFLINE');
        if (!this.isExplicitlyClosed) {
          this.scheduleReconnect(customDeviceId);
        }
      };
    } catch (e) {
      this.setStatus('OFFLINE');
      this.scheduleReconnect(customDeviceId);
    }
  }

  private setStatus(newStatus: RelayConnectionStatus) {
    this.status = newStatus;
    if (this.onStatusChange) this.onStatusChange(newStatus);
  }

  private scheduleReconnect(customDeviceId?: string) {
    clearTimeout(this.reconnectTimeout);
    this.reconnectAttempts++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 15000) + Math.random() * 1000;
    this.reconnectTimeout = setTimeout(() => {
      this.connect(customDeviceId);
    }, delay);
  }

  public disconnect() {
    this.isExplicitlyClosed = true;
    clearTimeout(this.reconnectTimeout);
    if (this.socket) {
      try {
        this.socket.send(JSON.stringify({ type: 'CLOSE' }));
        this.socket.close();
      } catch (_) {}
      this.socket = null;
    }
    this.setStatus('OFFLINE');
  }
}

export const relaySocketClient = new RelayWebSocketClient();
