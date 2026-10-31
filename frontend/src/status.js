export function describeStatus(state) {
  if (state === 'ready') return 'Serwer i baza danych działają';
  if (state === 'unavailable') return 'Nie udało się połączyć. Spróbuj ponownie.';
  return 'Sprawdzanie połączenia…';
}
