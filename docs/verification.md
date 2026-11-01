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

## Potwierdzenie GitHub i wdrożenia

Run https://github.com/sirdobro/nawyki/actions/runs/41192604750 zakończył się sukcesem dla df4b2f262dde22a4c6453879f3928a5c14a0a362. Oba joby verify i publish zakończyły się poprawnie. VPS pobrał wydanie, sprawdził sumę, uruchomił aplikację i przełączył proxy po healthcheck.

Publiczne /api/health/ready zwróciło 200 i UP, a /api/status podało dokładny commit wydania oraz accountCreationEnabled=false. Przeglądarka wyświetliła zbudowanego Reacta, poprawny status połączenia i identyfikator wydania. Sprawdzono układ desktop i mobilny przy zadanej szerokości 390 px (obszar dokumentu 375 px); brak przewijania poziomego i brak błędów/warningów konsoli.

Kontenery app i db były healthy, usługi systemowe bez błędów. Podczas kontroli aplikacja używała około 128 MB pamięci przy limicie 512 MB; to obserwacja fundamentu bez ruchu użytkowników, nie benchmark MVP. Skorygowano powielanie Cache-Control/nosniff na granicy Spring–Caddy i sprawdzono pojedyncze nagłówki przez publiczne HTTPS. Domyślny klient Python urllib otrzymał 403 od warstwy Cloudflare; standardowy curl i przeglądarka działały poprawnie, nie obniżano ochrony Cloudflare.

Github Actions secrets zwróciły pustą listę. Nieużywana para kluczy CI została usunięta lokalnie; klucz administratora pozostał bez zmian. Konto deploy nie otrzymało dostępu SSH ani sudo.

## Konta i nawyki — 2026-11-01

Weryfikacja przed publikacją: Java 21/Maven w kontenerze i osobny PostgreSQL w usuwalnym tmpfs, odrębna sieć od bazy online. Kontener i sieć testowe usunięto po zakończeniu. Frontend: npm ci oraz npm run build zakończone poprawnie.

- DailyStreakTest: 7 testów, w tym 3 właściwości.
- HabitRulesTest: 5 testów (trzy harmonogramy, niezmienność kolekcji, kumulacja błędów, granice dat i archiwizacji).
- FoundationApiIT: 4 testy, dostosowane do dwóch migracji, aktywnych kont i odpowiedzi 401 dla prywatnych tras.
- JournalApiIT: 8 testów integracyjnych z PostgreSQL: CSRF i obcy Origin, ciasteczka, hash klucza, logowanie i świadoma zmiana konta, wylogowanie, izolacja wszystkich tras nawyków/eksportu, harmonogramy/walidacja/historia/archiwum, osiem równoczesnych PUT, rotacja, kasowanie kaskadowe, wygaśnięcie sesji i limit rejestracji.
- Łącznie 24 testy, bez błędów i pominięć. JaCoCo: 55/55 linii obecnego domain (100%); gate 70% spełniony. To pokrycie istniejących klas, nie miara ukończenia MVP.

Pierwsza próba wykryła niejednoznaczne przeciążenie asercji w nowym teście; poprawiono typ wyniku i powtórzono pełną weryfikację. Wyłączono nieużywaną autokonfigurację konta z generowanym hasłem Springa. Uwierzytelnianie jest wyłącznie naszym kluczem i sesją.

CI, wdrożenie tej wersji i przegląd przeglądarkowy wymagają osobnego potwierdzenia. Pozostały treningi, obliczenia serii tygodniowych/wybranych dni, statystyki, dwa SSE, ich izolacja i testy współbieżnego raportu. Interfejs historii pokazuje 30 dni; API obsługuje zakresy do pięciu lat.
