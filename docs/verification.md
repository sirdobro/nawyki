# Weryfikacja fundamentu 2026-10-31

To wyniki pierwszego szkieletu, nie pełnego MVP ani wszystkich wymagań zajęć.

Sprawdzono w kontenerach Java 21 i Node 24, z osobnym tymczasowym PostgreSQL 18.6. Baza testowa używała oddzielnej sieci i tmpfs; po testach usunięto kontener i sieć. Nie używano bazy wdrożonej aplikacji.

| Sprawdzenie | Wynik |
|---|---|
| Frontend: instalacja z lockfile i build Vite | Poprawny |
| Jednostkowe i właściwości DailyStreakTest | 7 testów, 0 błędów, 0 pominiętych |
| W tym testy właściwości | 3: duplikaty, kolejność historii, przyszłe daty |
| FoundationApiIT z prawdziwym PostgreSQL | 4 testy, 0 błędów, 0 pominiętych |
| Pokrycie obecnego domain | 9/9 linii, 100%; 6/6 gałęzi |
| Gate 70% linii domain | Spełniony dla obecnego małego rdzenia |

Testy integracyjne sprawdzają gotowość bazy, JSON statusu, brak aktywnej rejestracji, odpowiedź 404, wykonanie migracji i rollback transakcji. Początkowo wykryto podwójny nagłówek Cache-Control; poprawiono źródło nagłówka i powtórzono cały zestaw. Zaktualizowano Flyway do 11.20.3, aby obsługiwał PostgreSQL 18.

GitHub Actions niezależnie powtarza build oraz testy i zachowuje raporty jako verification-reports przez 14 dni. Publikacja obrazu jest zależna od poprawnego zakończenia verify. Wynik biegu GitHub i publiczne wdrożenie należy potwierdzać osobno; sam ten dokument ich nie deklaruje.

Do wykonania w dalszych etapach: pozostałe harmonogramy, konto i izolacja użytkowników, nawyki, treningi, dwa SSE, raporty współbieżne oraz ich testy. Obecne 100% nie opisuje pokrycia przyszłego MVP.
