# Plan API

Zatwierdzono wieloużytkownikowe konta z kluczem dostępu. Do kontraktów dojdą tworzenie konta, wymiana klucza na sesję, wylogowanie, unieważnienie sesji, rotacja klucza, eksport i usunięcie własnego konta. Wszystkie poniższe zasoby i SSE wymagają sesji oraz izolacji kont. 401/403 mają zastosowanie zgodnie z kontraktem; dla cudzego zasobu nie ujawniamy jego istnienia. Szczegóły: authentication.md.

Zaimplementowano endpointy kont i nawyków opisane poniżej. Endpointy /api/workouts są także zaimplementowane. Wiersze /api/statistics i /api/events pozostają planem. Daty: YYYY-MM-DD w strefie użytkownika. Zwykłe odpowiedzi i błędy: JSON; SSE używa text/event-stream z danymi JSON.

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

## Dostęp i konta — zaimplementowane

| Metoda | Ścieżka | Odpowiedź |
|---|---|---|
| GET | /api/auth/csrf | 200: token, headerName i cookie CSRF |
| GET | /api/auth/me | 200: authenticated; po zalogowaniu także accountId, today, timeZone |
| POST | /api/accounts | 201: accountId, accessKey (tylko przy utworzeniu), cookie sesji; 409 aktywna sesja; 429 limit |
| POST | /api/auth/login | JSON accessKey, opcjonalnie confirmSwitch; 200 accountId i cookie; 401 zły klucz, 409 wymaga potwierdzenia |
| POST | /api/auth/logout | 200 signedOut; usuwa bieżącą sesję i cookie |
| POST | /api/auth/logout-all | 200 signedOut; usuwa wszystkie sesje konta |
| POST | /api/auth/rotate | JSON accessKey; 200 nowy accessKey i cookie; stary klucz i sesje przestają działać |
| GET | /api/account/export | 200 JSON do pobrania: formatVersion, timeZone, habits wraz z completions |
| DELETE | /api/account | JSON accessKey; 200 signedOut; kaskadowo usuwa dane konta |

Poza tworzeniem konta, logowaniem, csrf/me i endpointami stanu trasy wymagają ważnej sesji (401). Każdy zapis wymaga tokenu CSRF (403). Rotacja/usuwanie także ponownie okazują klucz (401). Limity mogą zwracać 429.

POST /api/habits: {name, scheduleType, days, weeklyTarget, startDate}. scheduleType: DAILY, SELECTED_DAYS lub WEEKLY_GOAL. days to liczby 1–7 (poniedziałek–niedziela); weeklyTarget to 1–7 dla celu tygodniowego; pozostałe warianty ignorują nieużywane wartości. startDate jest wymagane i nie może być przyszłe. PATCH przyjmuje {name}. Zmiana harmonogramu nie jest obsługiwana. Obiekt nawyku zawiera id, name, scheduleType, days, weeklyTarget, startDate, archivedOn (pusty tekst oznacza brak archiwizacji). GET /api/habits zwraca {habits:[...]}, GET historii {dates:[YYYY-MM-DD,...]}, GET planu dnia {date,today,habits:[...]} z completed i weekCount. Historia przyjmuje zakres do pięciu lat, domyślnie ostatni miesiąc; interfejs pokazuje ostatnie 30 dni. PUT/DELETE wykonania nie wymagają treści JSON. Zapis dat przyszłych, sprzed rozpoczęcia, niezaplanowanych lub od daty archiwizacji zwraca 422.

Walidacja: {code:"VALIDATION",errors:[{field,message}]}. Pozostałe błędy mają code i opcjonalnie message. Maksymalna odczytywana treść JSON to 16 KiB (400 po przekroczeniu). Nie zwracamy klucza w szczegółach błędu. GET /api/status pokazuje stage=habits-and-workouts i accountCreationEnabled=true.

## Treningi — zaimplementowane

GET /api/workouts?from=YYYY-MM-DD&to=YYYY-MM-DD zwraca {workouts:[{id,date,exerciseNames,setCount}]}, najnowsze daty najpierw. Domyślny zakres: początek bieżącego miesiąca do dzisiaj; do pięciu lat na zapytanie. Interfejs wybiera jeden miesiąc.

POST /api/workouts tworzy cały trening (201); GET /api/workouts/{id} zwraca cały wpis (200); PUT pod tym samym adresem zastępuje go w całości (200); DELETE usuwa cały wpis (200 {deleted:true}). Nieistniejący lub cudzy wpis: 404. Wszystkie trasy wymagają sesji (401), zapisy także CSRF (403). Błędny kształt/JSON/UUID: 400; niepoprawne wartości: 422 z listą {field,message}. Limit JSON 16 KiB.

Treść POST/PUT: {date:"YYYY-MM-DD",exercises:[{name:"Przysiad",sets:[{repetitions:8,weightKg:"40.125"},{repetitions:6,weightKg:""}]}]}. Kolejność tablic jest zachowana. Odpowiedź szczegółowa dodaje id; weightKg jest tekstem dziesiętnym bez zbędnych końcowych zer, pusty tekst oznacza brak ciężaru. Wejście dopuszcza tekst lub liczbę dla powtórzeń/ciężaru; pusty/nieobecny/null ciężar oznacza brak zapisu, zero jest odrębną wartością. Przecinek dziesiętny jest akceptowany w tekście. Daty nie mogą być przyszłe; wymagane 1–30 ćwiczeń, nazwy 1–100 znaków, 1–30 serii na ćwiczenie, całkowite powtórzenia 1–10000 i opcjonalny ciężar 0–9999,999 kg z maksymalnie trzema miejscami dziesiętnymi. Te limity ograniczają pojedynczy wpis, nie opisują zaleceń treningowych.

Tworzenie/edycja są transakcyjne. Edycje tego samego wpisu są serializowane blokadą wiersza; ostatni zatwierdzony zapis zastępuje cały wpis (bez wykrywania konfliktu wersji). Eksport konta ma teraz formatVersion=2 i zawiera także workouts z ćwiczeniami i seriami. Usunięcie konta kaskadowo usuwa treningi. Trening nie zmienia wykonania żadnego nawyku.
