# Plan API

To projekt kontraktów, nie lista istniejących endpointów. Przed implementacją ustalimy schematy JSON i testy kontraktu. Daty: YYYY-MM-DD w strefie użytkownika. Zwykłe odpowiedzi i błędy: JSON; SSE używa text/event-stream z danymi JSON.

| Metoda | Ścieżka | Znaczenie | Planowane odpowiedzi |
|---|---|---|---|
| GET | /api/habits | Lista aktywnych lub archiwalnych nawyków | 200 |
| POST | /api/habits | Utworzenie nawyku | 201, 422 |
| GET | /api/habits/{id} | Szczegóły | 200, 404 |
| PATCH | /api/habits/{id} | Zmiana nazwy | 200, 404, 422 |
| POST | /api/habits/{id}/archive | Idempotentna archiwizacja | 200, 404 |
| PUT | /api/habits/{id}/completions/{date} | Idempotentne wykonanie | 200, 404, 422 |
| DELETE | /api/habits/{id}/completions/{date} | Idempotentne cofnięcie; JSON z wynikiem | 200, 404, 422 |
| GET | /api/habits/{id}/completions?from=&to= | Historia w zakresie | 200, 404, 422 |
| GET | /api/today?date= | Plan wybranego dnia i cele tygodnia | 200, 422 |
| GET | /api/workouts?from=&to= | Historia treningów | 200, 422 |
| POST | /api/workouts | Atomowy zapis treningu | 201, 422 |
| GET | /api/workouts/{id} | Szczegóły treningu | 200, 404 |
| PUT | /api/workouts/{id} | Atomowa aktualizacja | 200, 404, 422 |
| DELETE | /api/workouts/{id} | Usunięcie treningu; JSON z wynikiem | 200, 404 |
| GET | /api/statistics?period=&start= | Tydzień/miesiąc, nawyki i liczba treningów | 200, 422 |
| GET | /api/events/habits | SSE zmian definicji i wykonań nawyków | 200 |
| GET | /api/events/workouts | SSE zmian dziennika treningowego | 200 |

400 oznacza nieczytelny JSON lub niepoprawny format żądania, 422 poprawnie odczytane dane łamiące reguły. 409 rezerwujemy dla rzeczywistych konfliktów; duplikat idempotentnego PUT wykonania nie jest konfliktem. 500 nie ujawnia szczegółów infrastruktury. Po ustaleniu logowania dodamy spójne 401/403.

Walidacja zwraca listę błędów z kodem, polem i czytelnym komunikatem. SSE nie przekazuje sekretów ani pełnych zbędnych danych; zdarzenie wskazuje rodzaj zmiany i identyfikator zasobu. Potrzebne są heartbeat, sprzątanie po rozłączeniu i odświeżenie REST po wznowieniu połączenia.
