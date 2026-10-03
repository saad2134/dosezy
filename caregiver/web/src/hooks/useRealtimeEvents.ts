import { useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

export function useRealtimeEvents() {
  const queryClient = useQueryClient();
  const [isConnected, setIsConnected] = useState(false);
  const [lastEvent, setLastEvent] = useState<{ event: string; data: any; time: string } | null>(null);

  useEffect(() => {
    let eventSource: EventSource | null = null;
    let reconnectTimeout: number | undefined;

    function connect() {
      try {
        eventSource = new EventSource('/v1/caregiver/events');

        eventSource.onopen = () => {
          setIsConnected(true);
        };

        eventSource.addEventListener('connected', () => {
          setIsConnected(true);
        });

        eventSource.addEventListener('dose_taken', (e) => {
          const data = JSON.parse(e.data);
          setLastEvent({ event: 'Dose Taken', data, time: new Date().toLocaleTimeString() });
          queryClient.invalidateQueries({ queryKey: ['schedules'] });
          queryClient.invalidateQueries({ queryKey: ['patients'] });
        });

        eventSource.addEventListener('dose_undo', (e) => {
          const data = JSON.parse(e.data);
          setLastEvent({ event: 'Dose Undone', data, time: new Date().toLocaleTimeString() });
          queryClient.invalidateQueries({ queryKey: ['schedules'] });
          queryClient.invalidateQueries({ queryKey: ['patients'] });
        });

        eventSource.addEventListener('schedules_updated', (e) => {
          const data = JSON.parse(e.data);
          setLastEvent({ event: 'Schedules Synced', data, time: new Date().toLocaleTimeString() });
          queryClient.invalidateQueries({ queryKey: ['schedules'] });
          queryClient.invalidateQueries({ queryKey: ['patients'] });
        });

        eventSource.addEventListener('pairing_confirmed', (e) => {
          const data = JSON.parse(e.data);
          setLastEvent({ event: 'New Patient Paired', data, time: new Date().toLocaleTimeString() });
          queryClient.invalidateQueries({ queryKey: ['patients'] });
        });

        eventSource.onerror = () => {
          setIsConnected(false);
          eventSource?.close();
          reconnectTimeout = window.setTimeout(connect, 4000);
        };
      } catch (err) {
        setIsConnected(false);
        reconnectTimeout = window.setTimeout(connect, 5000);
      }
    }

    connect();

    return () => {
      clearTimeout(reconnectTimeout);
      eventSource?.close();
    };
  }, [queryClient]);

  return { isConnected, lastEvent };
}
