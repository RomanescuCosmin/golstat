import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export type LiveHandler = (payload: unknown) => void;

const handlers = new Map<number, Set<LiveHandler>>();
const activeSubs = new Map<number, StompSubscription>();
let client: Client | null = null;

/** Conecteaza (o singura data) clientul STOMP peste SockJS la endpoint-ul `/ws` al backend-ului. */
export function connectLive(): Client {
  if (client) {
    return client;
  }
  client = new Client({
    webSocketFactory: () => new SockJS('/ws') as unknown as WebSocket,
    reconnectDelay: 5000,
    // Backend oprit / conexiune pierduta: nu propagam nimic — clientul reincearca singur.
    onWebSocketError: () => {},
    onStompError: () => {},
  });
  client.onConnect = () => {
    // La (re)conectare abonamentele vechi sunt moarte; le refacem pentru toti ascultatorii.
    activeSubs.clear();
    for (const fixtureId of handlers.keys()) {
      subscribeRemote(fixtureId);
    }
  };
  client.activate();
  return client;
}

function subscribeRemote(fixtureId: number): void {
  if (!client?.connected || activeSubs.has(fixtureId)) {
    return;
  }
  const sub = client.subscribe(`/topic/live/${fixtureId}`, (message: IMessage) => {
    let payload: unknown = message.body;
    try {
      payload = JSON.parse(message.body);
    } catch {
      // pastram corpul brut daca nu e JSON
    }
    handlers.get(fixtureId)?.forEach((h) => h(payload));
  });
  activeSubs.set(fixtureId, sub);
}

/**
 * Aboneaza un callback la actualizarile live ale unui meci (`/topic/live/{fixtureId}`).
 * Returneaza functia de dezabonare.
 */
export function subscribeFixture(fixtureId: number, cb: LiveHandler): () => void {
  let set = handlers.get(fixtureId);
  if (!set) {
    set = new Set();
    handlers.set(fixtureId, set);
  }
  set.add(cb);

  connectLive();
  subscribeRemote(fixtureId);

  return () => {
    set.delete(cb);
    if (set.size === 0) {
      handlers.delete(fixtureId);
      activeSubs.get(fixtureId)?.unsubscribe();
      activeSubs.delete(fixtureId);
    }
  };
}

/** Inchide conexiunea live (ex. la parasirea zonei Live). */
export function disconnectLive(): void {
  handlers.clear();
  activeSubs.clear();
  void client?.deactivate();
  client = null;
}
