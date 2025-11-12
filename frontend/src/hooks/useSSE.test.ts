/**
 * useSSE Hook Tests
 * Story 2.8: SSE Client Integration
 */

import { renderHook, waitFor } from '@testing-library/react';
import { useSSE } from './useSSE';
import { SSEManager } from '../services/SSEManager';

// Mock SSEManager
jest.mock('../services/SSEManager');

const MockedSSEManager = SSEManager as jest.MockedClass<typeof SSEManager>;

describe('useSSE', () => {
  const mockUrl = 'http://localhost:8080/api/upload/sessions/test-session/stream';
  const mockToken = 'test-token';

  beforeEach(() => {
    jest.clearAllMocks();

    // Setup default mock implementation
    MockedSSEManager.mockImplementation(() => {
      const statusHandlers = new Set<(status: any) => void>();
      return {
        connect: jest.fn().mockResolvedValue(undefined),
        disconnect: jest.fn(),
        isConnected: jest.fn().mockReturnValue(false),
        getConnectionStatus: jest.fn().mockReturnValue('disconnected'),
        onConnectionStatusChange: jest.fn((handler) => {
          statusHandlers.add(handler);
          return () => statusHandlers.delete(handler);
        }),
        updateAuthToken: jest.fn(),
      } as any;
    });
  });

  describe('Initialization', () => {
    it('should initialize with disconnected state', () => {
      const { result } = renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: mockToken,
          enabled: true,
        })
      );

      expect(result.current.isConnected).toBe(false);
      expect(result.current.connectionStatus).toBe('disconnected');
    });

    it('should not create SSEManager when token is null', () => {
      renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: null,
          enabled: true,
        })
      );

      expect(MockedSSEManager).not.toHaveBeenCalled();
    });

    it('should not create SSEManager when enabled is false', () => {
      renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: mockToken,
          enabled: false,
        })
      );

      expect(MockedSSEManager).not.toHaveBeenCalled();
    });

    it('should not create SSEManager when url is empty', () => {
      renderHook(() =>
        useSSE({
          url: '',
          authToken: mockToken,
          enabled: true,
        })
      );

      expect(MockedSSEManager).not.toHaveBeenCalled();
    });

    it('should create SSEManager when all requirements met', () => {
      renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: mockToken,
          enabled: true,
        })
      );

      expect(MockedSSEManager).toHaveBeenCalledWith(mockUrl, mockToken);
    });
  });

  describe('Connection Management', () => {
    it('should call connect when subscribe is invoked', async () => {
      const mockConnect = jest.fn().mockResolvedValue(undefined);

      MockedSSEManager.mockImplementation(() => ({
        connect: mockConnect,
        disconnect: jest.fn(),
        isConnected: jest.fn().mockReturnValue(false),
        getConnectionStatus: jest.fn().mockReturnValue('disconnected'),
        onConnectionStatusChange: jest.fn(() => jest.fn()),
        updateAuthToken: jest.fn(),
      } as any));

      const { result } = renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: mockToken,
          enabled: true,
        })
      );

      const messageHandler = jest.fn();
      result.current.subscribe(messageHandler);

      await waitFor(() => {
        expect(mockConnect).toHaveBeenCalledWith(messageHandler, undefined);
      });
    });

    it('should pass onStateResync to connect when provided', async () => {
      const mockConnect = jest.fn().mockResolvedValue(undefined);
      const onStateResync = jest.fn();

      MockedSSEManager.mockImplementation(() => ({
        connect: mockConnect,
        disconnect: jest.fn(),
        isConnected: jest.fn().mockReturnValue(false),
        getConnectionStatus: jest.fn().mockReturnValue('disconnected'),
        onConnectionStatusChange: jest.fn(() => jest.fn()),
        updateAuthToken: jest.fn(),
      } as any));

      const { result } = renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: mockToken,
          enabled: true,
          onStateResync,
        })
      );

      const messageHandler = jest.fn();
      result.current.subscribe(messageHandler);

      await waitFor(() => {
        expect(mockConnect).toHaveBeenCalledWith(messageHandler, onStateResync);
      });
    });

    it('should call disconnect when unsubscribe is invoked', () => {
      const mockDisconnect = jest.fn();

      MockedSSEManager.mockImplementation(() => ({
        connect: jest.fn().mockResolvedValue(undefined),
        disconnect: mockDisconnect,
        isConnected: jest.fn().mockReturnValue(false),
        getConnectionStatus: jest.fn().mockReturnValue('disconnected'),
        onConnectionStatusChange: jest.fn(() => jest.fn()),
        updateAuthToken: jest.fn(),
      } as any));

      const { result } = renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: mockToken,
          enabled: true,
        })
      );

      result.current.unsubscribe();

      expect(mockDisconnect).toHaveBeenCalled();
    });

    it('should disconnect on unmount', () => {
      const mockDisconnect = jest.fn();

      MockedSSEManager.mockImplementation(() => ({
        connect: jest.fn().mockResolvedValue(undefined),
        disconnect: mockDisconnect,
        isConnected: jest.fn().mockReturnValue(false),
        getConnectionStatus: jest.fn().mockReturnValue('disconnected'),
        onConnectionStatusChange: jest.fn(() => jest.fn()),
        updateAuthToken: jest.fn(),
      } as any));

      const { unmount } = renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: mockToken,
          enabled: true,
        })
      );

      unmount();

      expect(mockDisconnect).toHaveBeenCalled();
    });
  });

  describe('Connection Status Updates', () => {
    it('should update connection status when SSE manager notifies', async () => {
      let statusHandler: ((status: any) => void) | undefined;

      MockedSSEManager.mockImplementation(() => ({
        connect: jest.fn().mockResolvedValue(undefined),
        disconnect: jest.fn(),
        isConnected: jest.fn().mockReturnValue(false),
        getConnectionStatus: jest.fn().mockReturnValue('disconnected'),
        onConnectionStatusChange: jest.fn((handler) => {
          statusHandler = handler;
          return () => {
            statusHandler = undefined;
          };
        }),
        updateAuthToken: jest.fn(),
      } as any));

      const { result } = renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: mockToken,
          enabled: true,
        })
      );

      // Simulate status change to 'connecting'
      if (statusHandler) {
        statusHandler('connecting');
      }

      await waitFor(() => {
        expect(result.current.connectionStatus).toBe('connecting');
        expect(result.current.isConnected).toBe(false);
      });

      // Simulate status change to 'connected'
      if (statusHandler) {
        statusHandler('connected');
      }

      await waitFor(() => {
        expect(result.current.connectionStatus).toBe('connected');
        expect(result.current.isConnected).toBe(true);
      });

      // Simulate status change to 'disconnected'
      if (statusHandler) {
        statusHandler('disconnected');
      }

      await waitFor(() => {
        expect(result.current.connectionStatus).toBe('disconnected');
        expect(result.current.isConnected).toBe(false);
      });
    });
  });

  describe('Reconnection', () => {
    it('should support manual reconnection', async () => {
      const mockConnect = jest.fn().mockResolvedValue(undefined);
      const mockDisconnect = jest.fn();

      MockedSSEManager.mockImplementation(() => ({
        connect: mockConnect,
        disconnect: mockDisconnect,
        isConnected: jest.fn().mockReturnValue(false),
        getConnectionStatus: jest.fn().mockReturnValue('disconnected'),
        onConnectionStatusChange: jest.fn(() => jest.fn()),
        updateAuthToken: jest.fn(),
      } as any));

      const { result } = renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: mockToken,
          enabled: true,
        })
      );

      const messageHandler = jest.fn();
      result.current.subscribe(messageHandler);

      await waitFor(() => {
        expect(mockConnect).toHaveBeenCalledTimes(1);
      });

      // Trigger manual reconnection
      result.current.reconnect();

      await waitFor(() => {
        expect(mockDisconnect).toHaveBeenCalled();
        expect(mockConnect).toHaveBeenCalledTimes(2);
      });
    });
  });

  describe('Dependencies Update', () => {
    it('should recreate SSEManager when url changes', () => {
      const { rerender } = renderHook(
        ({ url }) =>
          useSSE({
            url,
            authToken: mockToken,
            enabled: true,
          }),
        {
          initialProps: { url: mockUrl },
        }
      );

      expect(MockedSSEManager).toHaveBeenCalledTimes(1);

      // Change URL
      const newUrl = 'http://localhost:8080/api/upload/sessions/new-session/stream';
      rerender({ url: newUrl });

      expect(MockedSSEManager).toHaveBeenCalledTimes(2);
      expect(MockedSSEManager).toHaveBeenLastCalledWith(newUrl, mockToken);
    });

    it('should recreate SSEManager when authToken changes', () => {
      const { rerender } = renderHook(
        ({ authToken }) =>
          useSSE({
            url: mockUrl,
            authToken,
            enabled: true,
          }),
        {
          initialProps: { authToken: mockToken },
        }
      );

      expect(MockedSSEManager).toHaveBeenCalledTimes(1);

      // Change token
      const newToken = 'new-token';
      rerender({ authToken: newToken });

      expect(MockedSSEManager).toHaveBeenCalledTimes(2);
      expect(MockedSSEManager).toHaveBeenLastCalledWith(mockUrl, newToken);
    });

    it('should cleanup SSEManager when enabled changes to false', () => {
      const mockDisconnect = jest.fn();

      MockedSSEManager.mockImplementation(() => ({
        connect: jest.fn().mockResolvedValue(undefined),
        disconnect: mockDisconnect,
        isConnected: jest.fn().mockReturnValue(false),
        getConnectionStatus: jest.fn().mockReturnValue('disconnected'),
        onConnectionStatusChange: jest.fn(() => jest.fn()),
        updateAuthToken: jest.fn(),
      } as any));

      const { rerender } = renderHook(
        ({ enabled }) =>
          useSSE({
            url: mockUrl,
            authToken: mockToken,
            enabled,
          }),
        {
          initialProps: { enabled: true },
        }
      );

      expect(MockedSSEManager).toHaveBeenCalledTimes(1);

      // Disable
      rerender({ enabled: false });

      expect(mockDisconnect).toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('should handle error when subscribing without SSEManager', () => {
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

      const { result } = renderHook(() =>
        useSSE({
          url: mockUrl,
          authToken: null, // This prevents SSEManager creation
          enabled: true,
        })
      );

      const messageHandler = jest.fn();
      result.current.subscribe(messageHandler);

      expect(consoleErrorSpy).toHaveBeenCalledWith(
        '[useSSE] Cannot subscribe: SSE not initialized'
      );

      consoleErrorSpy.mockRestore();
    });
  });
});
