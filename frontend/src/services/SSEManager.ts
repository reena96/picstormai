/**
 * SSEManager - Server-Sent Events Connection Manager
 * Story 2.8: SSE Client Integration
 *
 * Uses @microsoft/fetch-event-source for proper JWT authentication in headers
 */

import { fetchEventSource } from '@microsoft/fetch-event-source';
import { UploadProgressMessage, ConnectionStatus } from '../types/sse';

export type MessageHandler = (message: UploadProgressMessage) => void;
export type ConnectionStatusHandler = (status: ConnectionStatus) => void;
export type StateResyncHandler = () => void;

class FatalError extends Error {}

export class SSEManager {
  private abortController: AbortController | null = null;
  private connectionStatus: ConnectionStatus = 'disconnected';
  private statusHandlers: Set<ConnectionStatusHandler> = new Set();
  private url: string;
  private authToken: string;
  private messageHandler: MessageHandler | null = null;
  private resyncHandler: StateResyncHandler | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;

  constructor(
    url: string,
    authToken: string
  ) {
    this.url = url;
    this.authToken = authToken;
  }

  /**
   * Connect to SSE stream with message handler
   */
  async connect(messageHandler: MessageHandler, resyncHandler?: StateResyncHandler): Promise<void> {
    if (this.abortController) {
      console.warn('[SSE] Already connected, closing existing connection');
      this.disconnect();
    }

    this.messageHandler = messageHandler;
    this.resyncHandler = resyncHandler || null;
    this.abortController = new AbortController();
    this.setConnectionStatus('connecting');
    this.reconnectAttempts = 0;

    try {
      await fetchEventSource(this.url, {
        headers: {
          'Authorization': `Bearer ${this.authToken}`,
        },
        signal: this.abortController.signal,

        onopen: async (response) => {
          if (response.ok && response.headers.get('content-type') === 'text/event-stream') {
            console.log('[SSE] Connection established');
            this.setConnectionStatus('connected');
            this.reconnectAttempts = 0;

            // Trigger state resync on reconnect (AC5)
            if (this.resyncHandler) {
              console.log('[SSE] Triggering state resync on connect');
              this.resyncHandler();
            }
          } else if (response.status >= 400 && response.status < 500 && response.status !== 429) {
            // Client error (except rate limit) - don't retry
            throw new FatalError(`SSE connection failed: ${response.status} ${response.statusText}`);
          } else if (response.ok && response.headers.get('content-type') !== 'text/event-stream') {
            // 200 OK but wrong content-type - likely session completed or doesn't exist
            // Don't retry as this indicates the session is no longer active
            console.warn('[SSE] Received 200 OK but wrong content-type. Session may be completed.');
            throw new FatalError(`SSE connection failed: ${response.status} ${response.statusText} (wrong content-type)`);
          } else {
            // Server error or rate limit - let library retry
            throw new Error(`SSE connection failed: ${response.status} ${response.statusText}`);
          }
        },

        onmessage: (event) => {
          // Parse event type and data
          if (event.event === 'PHOTO_UPLOADED' ||
              event.event === 'PHOTO_FAILED' ||
              event.event === 'SESSION_COMPLETED') {
            try {
              const message = JSON.parse(event.data) as UploadProgressMessage;
              this.messageHandler?.(message);
            } catch (error) {
              console.error(`[SSE] Failed to parse ${event.event}:`, error);
            }
          }
        },

        onerror: (error) => {
          console.error('[SSE] Connection error:', error);
          this.setConnectionStatus('disconnected');

          this.reconnectAttempts++;

          // If fatal error, stop retrying
          if (error instanceof FatalError) {
            console.error('[SSE] Fatal error, stopping reconnection attempts');
            throw error;
          }

          // If max retries exceeded, stop
          if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('[SSE] Max reconnection attempts exceeded');
            throw new FatalError('Max reconnection attempts exceeded');
          }

          // Otherwise, let fetch-event-source retry automatically
          console.log(`[SSE] Will retry connection (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
        },

        onclose: () => {
          console.log('[SSE] Connection closed by server');
          this.setConnectionStatus('disconnected');
        },

        // Exponential backoff for retries (in ms)
        openWhenHidden: true, // Keep connection open when tab is hidden
      });
    } catch (error) {
      console.error('[SSE] Failed to connect:', error);
      this.setConnectionStatus('disconnected');
      if (!(error instanceof FatalError)) {
        throw error;
      }
    }
  }

  /**
   * Disconnect from SSE stream
   */
  disconnect(): void {
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
      this.setConnectionStatus('disconnected');
      console.log('[SSE] Connection closed');
    }
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.connectionStatus === 'connected' && this.abortController !== null;
  }

  /**
   * Get current connection status
   */
  getConnectionStatus(): ConnectionStatus {
    return this.connectionStatus;
  }

  /**
   * Subscribe to connection status changes
   */
  onConnectionStatusChange(handler: ConnectionStatusHandler): () => void {
    this.statusHandlers.add(handler);
    // Return unsubscribe function
    return () => {
      this.statusHandlers.delete(handler);
    };
  }

  /**
   * Update connection status and notify handlers
   */
  private setConnectionStatus(status: ConnectionStatus): void {
    if (this.connectionStatus !== status) {
      this.connectionStatus = status;
      this.statusHandlers.forEach(handler => handler(status));
    }
  }

  /**
   * Update authentication token (for token refresh scenarios)
   */
  updateAuthToken(newToken: string): void {
    this.authToken = newToken;
  }
}
