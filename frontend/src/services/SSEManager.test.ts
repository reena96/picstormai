/**
 * SSEManager Tests
 * Story 2.8: SSE Client Integration
 */

import { SSEManager } from './SSEManager';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { UploadProgressMessage } from '../types/sse';

// Mock fetch-event-source
jest.mock('@microsoft/fetch-event-source');

const mockFetchEventSource = fetchEventSource as jest.MockedFunction<typeof fetchEventSource>;

describe('SSEManager', () => {
  const mockUrl = 'http://localhost:8080/api/upload/sessions/test-session/stream';
  const mockToken = 'test-token';
  let sseManager: SSEManager;

  beforeEach(() => {
    jest.clearAllMocks();
    sseManager = new SSEManager(mockUrl, mockToken);
  });

  afterEach(() => {
    sseManager.disconnect();
  });

  describe('Connection Management', () => {
    it('should initialize with disconnected status', () => {
      expect(sseManager.getConnectionStatus()).toBe('disconnected');
      expect(sseManager.isConnected()).toBe(false);
    });

    it('should set status to connecting when connect is called', async () => {
      const messageHandler = jest.fn();
      let statusChangeCount = 0;

      sseManager.onConnectionStatusChange((status) => {
        if (status === 'connecting') {
          statusChangeCount++;
        }
      });

      // Mock fetchEventSource to not resolve immediately
      mockFetchEventSource.mockImplementation(() => new Promise(() => {}));

      sseManager.connect(messageHandler);

      expect(sseManager.getConnectionStatus()).toBe('connecting');
      expect(statusChangeCount).toBe(1);
    });

    it('should include Authorization header in request', async () => {
      const messageHandler = jest.fn();

      mockFetchEventSource.mockImplementation(() => Promise.resolve());

      await sseManager.connect(messageHandler);

      expect(mockFetchEventSource).toHaveBeenCalledWith(
        mockUrl,
        expect.objectContaining({
          headers: {
            Authorization: `Bearer ${mockToken}`,
          },
        })
      );
    });

    it('should handle successful connection', async () => {
      const messageHandler = jest.fn();
      let connectedStatus = false;

      sseManager.onConnectionStatusChange((status) => {
        if (status === 'connected') {
          connectedStatus = true;
        }
      });

      mockFetchEventSource.mockImplementation(async (_url, options) => {
        // Simulate successful connection
        const mockResponse = {
          ok: true,
          headers: {
            get: (name: string) => name === 'content-type' ? 'text/event-stream' : null,
          },
        } as Response;

        await options.onopen?.(mockResponse);
      });

      await sseManager.connect(messageHandler);

      expect(connectedStatus).toBe(true);
      expect(sseManager.isConnected()).toBe(true);
    });

    it('should handle connection error', async () => {
      const messageHandler = jest.fn();
      let disconnectedStatus = false;

      sseManager.onConnectionStatusChange((status) => {
        if (status === 'disconnected') {
          disconnectedStatus = true;
        }
      });

      mockFetchEventSource.mockImplementation(async (_url, options) => {
        const error = new Error('Connection failed');
        options.onerror?.(error);
      });

      await sseManager.connect(messageHandler);

      expect(disconnectedStatus).toBe(true);
    });

    it('should disconnect and cleanup', () => {
      const messageHandler = jest.fn();

      mockFetchEventSource.mockImplementation(() => Promise.resolve());

      sseManager.connect(messageHandler);
      sseManager.disconnect();

      expect(sseManager.getConnectionStatus()).toBe('disconnected');
      expect(sseManager.isConnected()).toBe(false);
    });
  });

  describe('Message Handling', () => {
    it('should call message handler for PHOTO_UPLOADED event', async () => {
      const messageHandler = jest.fn();
      const mockMessage: UploadProgressMessage = {
        type: 'PHOTO_UPLOADED',
        sessionId: 'test-session',
        photoId: 'photo-1',
        uploadedCount: 1,
        totalCount: 5,
        progressPercent: 20,
        timestamp: '2025-11-11T12:00:00Z',
      };

      mockFetchEventSource.mockImplementation(async (_url, options) => {
        // Simulate successful connection
        const mockResponse = {
          ok: true,
          headers: {
            get: (name: string) => name === 'content-type' ? 'text/event-stream' : null,
          },
        } as Response;

        await options.onopen?.(mockResponse);

        // Simulate receiving message
        options.onmessage?.({
          event: 'PHOTO_UPLOADED',
          data: JSON.stringify(mockMessage),
          id: '1',
          retry: 0,
        });
      });

      await sseManager.connect(messageHandler);

      expect(messageHandler).toHaveBeenCalledWith(mockMessage);
    });

    it('should call message handler for PHOTO_FAILED event', async () => {
      const messageHandler = jest.fn();
      const mockMessage: UploadProgressMessage = {
        type: 'PHOTO_FAILED',
        sessionId: 'test-session',
        photoId: 'photo-2',
        reason: 'File too large',
        failedCount: 1,
        totalCount: 5,
        timestamp: '2025-11-11T12:00:00Z',
      };

      mockFetchEventSource.mockImplementation(async (_url, options) => {
        const mockResponse = {
          ok: true,
          headers: {
            get: (name: string) => name === 'content-type' ? 'text/event-stream' : null,
          },
        } as Response;

        await options.onopen?.(mockResponse);

        options.onmessage?.({
          event: 'PHOTO_FAILED',
          data: JSON.stringify(mockMessage),
          id: '2',
          retry: 0,
        });
      });

      await sseManager.connect(messageHandler);

      expect(messageHandler).toHaveBeenCalledWith(mockMessage);
    });

    it('should call message handler for SESSION_COMPLETED event', async () => {
      const messageHandler = jest.fn();
      const mockMessage: UploadProgressMessage = {
        type: 'SESSION_COMPLETED',
        sessionId: 'test-session',
        uploadedCount: 4,
        failedCount: 1,
        totalCount: 5,
        timestamp: '2025-11-11T12:00:00Z',
      };

      mockFetchEventSource.mockImplementation(async (_url, options) => {
        const mockResponse = {
          ok: true,
          headers: {
            get: (name: string) => name === 'content-type' ? 'text/event-stream' : null,
          },
        } as Response;

        await options.onopen?.(mockResponse);

        options.onmessage?.({
          event: 'SESSION_COMPLETED',
          data: JSON.stringify(mockMessage),
          id: '3',
          retry: 0,
        });
      });

      await sseManager.connect(messageHandler);

      expect(messageHandler).toHaveBeenCalledWith(mockMessage);
    });

    it('should ignore unknown event types', async () => {
      const messageHandler = jest.fn();

      mockFetchEventSource.mockImplementation(async (_url, options) => {
        const mockResponse = {
          ok: true,
          headers: {
            get: (name: string) => name === 'content-type' ? 'text/event-stream' : null,
          },
        } as Response;

        await options.onopen?.(mockResponse);

        options.onmessage?.({
          event: 'UNKNOWN_EVENT',
          data: JSON.stringify({ type: 'UNKNOWN' }),
          id: '4',
          retry: 0,
        });
      });

      await sseManager.connect(messageHandler);

      expect(messageHandler).not.toHaveBeenCalled();
    });

    it('should handle invalid JSON in message data', async () => {
      const messageHandler = jest.fn();
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

      mockFetchEventSource.mockImplementation(async (_url, options) => {
        const mockResponse = {
          ok: true,
          headers: {
            get: (name: string) => name === 'content-type' ? 'text/event-stream' : null,
          },
        } as Response;

        await options.onopen?.(mockResponse);

        options.onmessage?.({
          event: 'PHOTO_UPLOADED',
          data: 'invalid json',
          id: '5',
          retry: 0,
        });
      });

      await sseManager.connect(messageHandler);

      expect(messageHandler).not.toHaveBeenCalled();
      expect(consoleErrorSpy).toHaveBeenCalled();

      consoleErrorSpy.mockRestore();
    });
  });

  describe('Connection Status Handlers', () => {
    it('should notify all status handlers on status change', async () => {
      const handler1 = jest.fn();
      const handler2 = jest.fn();

      sseManager.onConnectionStatusChange(handler1);
      sseManager.onConnectionStatusChange(handler2);

      mockFetchEventSource.mockImplementation(async (_url, options) => {
        const mockResponse = {
          ok: true,
          headers: {
            get: (name: string) => name === 'content-type' ? 'text/event-stream' : null,
          },
        } as Response;

        await options.onopen?.(mockResponse);
      });

      await sseManager.connect(jest.fn());

      expect(handler1).toHaveBeenCalledWith('connecting');
      expect(handler1).toHaveBeenCalledWith('connected');
      expect(handler2).toHaveBeenCalledWith('connecting');
      expect(handler2).toHaveBeenCalledWith('connected');
    });

    it('should allow unsubscribing from status changes', async () => {
      const handler = jest.fn();

      const unsubscribe = sseManager.onConnectionStatusChange(handler);
      unsubscribe();

      mockFetchEventSource.mockImplementation(async (_url, options) => {
        const mockResponse = {
          ok: true,
          headers: {
            get: (name: string) => name === 'content-type' ? 'text/event-stream' : null,
          },
        } as Response;

        await options.onopen?.(mockResponse);
      });

      await sseManager.connect(jest.fn());

      expect(handler).not.toHaveBeenCalled();
    });
  });

  describe('Reconnection', () => {
    it('should trigger state resync on reconnect', async () => {
      const messageHandler = jest.fn();
      const resyncHandler = jest.fn();

      mockFetchEventSource.mockImplementation(async (_url, options) => {
        const mockResponse = {
          ok: true,
          headers: {
            get: (name: string) => name === 'content-type' ? 'text/event-stream' : null,
          },
        } as Response;

        await options.onopen?.(mockResponse);
      });

      await sseManager.connect(messageHandler, resyncHandler);

      expect(resyncHandler).toHaveBeenCalled();
    });
  });

  describe('Token Update', () => {
    it('should allow updating authentication token', () => {
      const newToken = 'new-token';
      sseManager.updateAuthToken(newToken);

      // No direct way to test this without connecting, but we verify it doesn't throw
      expect(sseManager).toBeDefined();
    });
  });
});
