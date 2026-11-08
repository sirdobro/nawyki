import { useEffect, useRef, useState } from 'react';
import { api } from './api';

export default function useJournalEvents(account, onHabits, onWorkouts, onIdentity) {
  const callbacks = useRef({ onHabits, onWorkouts, onIdentity });
  callbacks.current = { onHabits, onWorkouts, onIdentity };
  const [status, setStatus] = useState('connecting');
  useEffect(() => {
    if (!account) return;
    let stopped = false, checking = false, restartScheduled = false;
    const sources = new Map(), ready = new Set(), timers = new Set(), refreshTimers = new Map();
    setStatus('connecting');
    function later(action, delay) {
      const timer = setTimeout(() => { timers.delete(timer); if (!stopped) action(); }, delay);
      timers.add(timer); return timer;
    }
    function refresh(channel) {
      if (refreshTimers.has(channel)) return;
      refreshTimers.set(channel, later(() => {
        refreshTimers.delete(channel);
        callbacks.current[channel === 'habits' ? 'onHabits' : 'onWorkouts']();
      }, 150));
    }
    function restartBoth() {
      if (restartScheduled) return;
      restartScheduled = true;
      later(() => { restartScheduled = false; connect('habits'); connect('workouts'); }, 3000);
    }
    async function checkIdentity(ended = false) {
      if (checking || stopped) return;
      checking = true;
      try {
        const me = await api('/api/auth/me');
        if (!stopped && (ended || !me.authenticated || me.accountId !== account.accountId)) {
          sources.forEach(source => source.close());
          callbacks.current.onIdentity(me);
          restartBoth();
        }
      } catch { /* Connection recovery remains with EventSource, without clearing saved views. */ }
      finally { checking = false; }
    }
    function connect(channel) {
      if (stopped) return;
      sources.get(channel)?.close();
      const source = new EventSource(`/api/events/${channel}`);
      sources.set(channel, source);
      source.addEventListener('ready', () => {
        if (stopped || sources.get(channel) !== source) return;
        ready.add(channel); setStatus(ready.size === 2 ? 'live' : 'connecting');
        refresh(channel); // Also covers every reconnect and changes missed while offline.
      });
      source.addEventListener('changed', () => refresh(channel));
      source.addEventListener('session-ended', () => {
        sources.forEach(s => s.close()); ready.clear(); setStatus('reconnecting');
        checkIdentity(true);
        // Retry if the identity request itself was interrupted by a network failure.
        restartBoth();
      });
      source.addEventListener('limit', () => { source.close(); ready.delete(channel); setStatus('limited'); });
      source.onerror = () => {
        if (stopped || sources.get(channel) !== source) return;
        ready.delete(channel); setStatus('reconnecting'); checkIdentity();
        // Native EventSource retries interrupted streams. A rejected handshake (e.g. 429)
        // can be CLOSED permanently, so also retry that case with a longer delay.
        if (source.readyState === EventSource.CLOSED) later(() => connect(channel), 10000);
      };
    }
    connect('habits'); connect('workouts');
    return () => {
      stopped = true; sources.forEach(source => source.close()); timers.forEach(clearTimeout);
    };
  }, [account]);
  return status;
}
