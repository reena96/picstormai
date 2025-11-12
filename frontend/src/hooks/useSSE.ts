/**
 * useSSE - React Hook for SSE Connection Management
 * Story 2.8: SSE Client Integration
 */

import { useEffect, useRef, useState, useCallback } from 'react';
import { SSEManager, MessageHandler, StateResyncHandler } from '../services/SSEManager';
import { ConnectionStatus } from '../types/sse';

export interface UseSSEOptions {
  url: string;
  authToken: string | null;
  enabled?: boolean;
  onStateResync?: StateResyncHandler;
}

export interface UseSSEResult {
  isConnected: boolean;
  connectionStatus: ConnectionStatus;
  subscribe: (messageHandler: MessageHandler) => void;
  unsubscribe: () => void;
  reconnect: () => void;
}

/**
 * Hook for managing SSE connections with automatic lifecycle management
 *
 * @param options - Configuration options
 * @returns SSE connection state and control functions
 */
export function useSSE(options: UseSSEOptions): UseSSEResult {
  const { url, authToken, enabled = true, onStateResync } = options;

  const sseManagerRef = useRef<SSEManager | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('disconnected');
  const messageHandlerRef = useRef<MessageHandler | null>(null);

  // Initialize SSE manager when URL and token are available
  useEffect(() => {
    if (!authToken || !enabled || !url) {
      // Clean up if conditions not met
      if (sseManagerRef.current) {
        sseManagerRef.current.disconnect();
        sseManagerRef.current = null;
      }
      setIsConnected(false);
      setConnectionStatus('disconnected');
      return;
    }

    // Create SSE manager
    const sseManager = new SSEManager(url, authToken);
    sseManagerRef.current = sseManager;

    // Subscribe to connection status changes
    const unsubscribeStatus = sseManager.onConnectionStatusChange((status) => {
      setConnectionStatus(status);
      setIsConnected(status === 'connected');
    });

    // Cleanup on unmount or when dependencies change
    return () => {
      unsubscribeStatus();
      sseManager.disconnect();
      sseManagerRef.current = null;
      setIsConnected(false);
      setConnectionStatus('disconnected');
    };
  }, [url, authToken, enabled]);

  /**
   * Subscribe to SSE messages
   */
  const subscribe = useCallback((messageHandler: MessageHandler) => {
    if (!sseManagerRef.current) {
      console.error('[useSSE] Cannot subscribe: SSE not initialized');
      return;
    }

    messageHandlerRef.current = messageHandler;

    // Connect with message handler and optional resync handler
    sseManagerRef.current.connect(messageHandler, onStateResync).catch((error) => {
      console.error('[useSSE] Connection failed:', error);
    });
  }, [onStateResync]);

  /**
   * Unsubscribe from SSE messages
   */
  const unsubscribe = useCallback(() => {
    if (sseManagerRef.current) {
      sseManagerRef.current.disconnect();
      messageHandlerRef.current = null;
    }
  }, []);

  /**
   * Manually trigger reconnection
   */
  const reconnect = useCallback(() => {
    if (sseManagerRef.current && messageHandlerRef.current) {
      console.log('[useSSE] Manual reconnection triggered');
      sseManagerRef.current.disconnect();
      sseManagerRef.current.connect(messageHandlerRef.current, onStateResync).catch((error) => {
        console.error('[useSSE] Reconnection failed:', error);
      });
    }
  }, [onStateResync]);

  return {
    isConnected,
    connectionStatus,
    subscribe,
    unsubscribe,
    reconnect,
  };
}
