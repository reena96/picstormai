/**
 * WebSocket Service - Real-time Progress Updates
 * Story 2.8: WebSocket Client Integration
 */

import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export class WebSocketService {
  private client: Client | null = null;
  private connected = false;

  connect(sessionId: string, onMessage: (data: any) => void) {
    const socket = new SockJS(`${process.env.REACT_APP_API_URL}/ws`);
    
    this.client = new Client({
      webSocketFactory: () => socket as any,
      onConnect: () => {
        this.connected = true;
        this.client?.subscribe(`/topic/upload-sessions/${sessionId}`, (message: IMessage) => {
          const data = JSON.parse(message.body);
          onMessage(data);
        });
      },
      onDisconnect: () => {
        this.connected = false;
      },
    });

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
    }
  }

  isConnected(): boolean {
    return this.connected;
  }
}

export const websocketService = new WebSocketService();
