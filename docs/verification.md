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

## Kontrola publicznej wersji i poprawka wejścia

GitHub run https://github.com/sirdobro/nawyki/actions/runs/41349146520 zakończył verify/publish dla 4b642a3; VPS wdrożył obraz i przeszedł healthcheck. Publiczny test wykrył brak dostępu anonimowego do wewnętrznego forwardu /index.html. Dodano tę konkretną publiczną trasę oraz test wejścia na /, który sprawdza HTML z root Reacta. Nie rozszerzano dostępu do prywatnego API.

Publiczny klient testowy otrzymał też CSRF_INVALID po zmianie stanu uwierzytelnienia. Pobranie świeżego tokenu pozwoliło wykonać pełny przepływ konta/nawyku/wykonania/eksportu/wylogowania i ponownego logowania. React już ponawiał raz żądanie po odświeżeniu CSRF; dodatkowo resetuje zapamiętany token po operacjach uwierzytelniania. Nie wyłączano ochrony CSRF ani Cloudflare.

Powtórzono cały zestaw z nowym testem strony głównej: 25 testów (12 jednostkowych/właściwości i 13 integracyjnych), brak błędów/pominięć, gate pokrycia spełniony. Weryfikacja publicznego HTML i interfejsu poprawionej wersji pozostaje do potwierdzenia po publikacji poprawki.

## Ostateczne potwierdzenie etapu kont/nawyków

Poprawka wejścia 4c1d5ad przeszła run https://github.com/sirdobro/nawyki/actions/runs/41349668394 i została wdrożona. Przeglądarka potwierdziła anonimową stronę React, utworzenie pustego dziennika, ukryty klucz z obowiązkowym potwierdzeniem zapisania, formularz nawyku, zapis, odznaczenie i historię. Nie publikowano wartości klucza ani cookie.

Dodatkowy test pola daty ujawnił, że natywne wprowadzenie daty nie synchronizowało stanu formularza. Dodano obsługę onInput obok onChange dla dat rozpoczęcia i planu dnia. Commit f2ab4c9 przeszedł verify i publish w https://github.com/sirdobro/nawyki/actions/runs/41350098088. VPS wdrożył dokładnie f2ab4c974715424687b24a2e8b2c77401464904b; publiczne /api/status potwierdziło release oraz accounts-and-habits. App i db były healthy.

Na tej wersji przeglądarka potwierdziła zapis daty rozpoczęcia 2026-10-29, korektę wykonania w historii dla 2026-10-31 i widoczny zaznaczony nawyk po bezpośrednim wybraniu tego dnia. Sesja przetrwała wdrożenie. Sprawdzono pola wyboru dni tygodnia i układ przy szerokości 390 px: document.scrollWidth <= innerWidth, bez przewijania poziomego; po teście przywrócono domyślny viewport. Konsola nie zawierała błędów ani ostrzeżeń w końcowym przepływie. Nie testowano ręcznie każdej kombinacji wszystkich kontrolek; reguły pokrywają wskazane testy Java/API.

Ponowny publiczny test API potwierdził utworzenie konta/nawyku, idempotentne wykonanie, historię, eksport, wylogowanie/logowanie i zachowane dane. Testowe dzienniki usunięto (API oraz ściśle wskazany rekord testowy przy sprzątaniu po przeglądarce). Nie używano istniejących danych użytkownika, nie przekazano klucza SSH do GitHub i nie zmieniano Cloudflare. Stan końcowy: 25 poprawnych testów, 55/55 linii obecnego rdzenia; aplikacja kont/nawyków online. Treningi, pełne serie/statystyki, SSE i końcowe PDF nadal są otwarte.

## Link z kluczem w otwartej karcie — 2026-11-01

Odtworzono zgłoszenie użytkownika: przejście z / do /#access=... w tej samej karcie pozostawiało nieobsłużony fragment i nie rozpoczynało logowania. Poprawka c345c93 dodaje jednorazowe automatyczne przeładowanie przy hashchange z niepustym access, korzystając z dotychczasowego procesu logowania i usuwania fragmentu. Bez nowych zależności i zmian w bazie.

Run https://github.com/sirdobro/nawyki/actions/runs/41352529245 zakończył verify/publish poprawnie; VPS oraz publiczne /api/status potwierdziły c345c931ddf2f530f22b22ed9172888e6218e95b. Na wdrożonej wersji sprawdzono w przeglądarce, bez ręcznego reload po wejściu w link: (1) wylogowana karta automatycznie pokazała nawyk testowego konta A, (2) link konta B przy aktywnej sesji od razu pokazał potwierdzenie i zachował A do decyzji, (3) zatwierdzenie pokazało B bez nawyku A, (4) błędny klucz pokazał czytelny błąd. Fragment znikał z adresu, nie wystąpiła pętla przeładowań. Dwa odrębne konta testowe oraz ich lokalne poświadczenia usunięto po sprawdzeniu; kont użytkownika nie odczytywano ani nie zmieniano.

## Dziennik treningów — 2026-11-03

Dodano WorkoutRulesTest (5 testów) i WorkoutApiIT (7 testów). Powtórzono weryfikację w kontenerach Java 21/Node 24 i osobnym, usuwalnym PostgreSQL: 17 testów jednostkowych/właściwości i 20 integracyjnych, łącznie 37; zero błędów i pominięć. JaCoCo: 86/86 linii obecnego domain, 100%; gate 70% spełniony. npm ci/build poprawne. Nie jest to pokrycie przyszłych statystyk/SSE.

Sprawdzono trzy migracje, kolejność wielu ćwiczeń i serii, dokładny ciężar dziesiętny z przecinkiem/kropką, odróżnienie pustego ciężaru od zera, kumulację błędów, daty/limity, pełny CRUD i eksport, cudze identyfikatory, CSRF, usunięcie konta oraz brak automatycznego wykonania nawyku. Wymuszony błąd ograniczenia SQL przy drugiej części treningu wycofał częściowy INSERT; przy edycji odtworzył pierwotną datę, ćwiczenia i serie. Dwa równoczesne PUT pozostawiły jedną kompletną wersję bez mieszania serii. Testy uruchomiono poza bazą online; kontener i sieć testowe zostały usunięte.

Brak nowych bibliotek lub usług. Odczyty/zapisy z jawnym SQL, transakcje/blokady w adapterze. Eksport formatVersion=2 zawiera workouts; istniejące habits nadal są eksportowane. Publikację, wdrożenie i kontrolę przeglądarkową należy potwierdzić osobno.

## Wdrożenie treningów i zakres kontroli interfejsu — 2026-11-03

GitHub run https://github.com/sirdobro/nawyki/actions/runs/41729055565 zakończył verify i publish poprawnie. VPS wdrożył 15eeba6fa74d5a10d6ce8fd0dfc6a2cb5f27e3b4; publiczne /api/status potwierdziło release i stage=habits-and-workouts. Przeprowadzono publiczny test API na dwóch własnych kontach: CRUD, lista, eksport, precyzja/blank kg, zachowanie danych po niepoprawnym PUT i odpowiedzi 404 dla cudzego konta. Wpis API usunięto po teście.

Wstępne próby interfejsu wykonano we wbudowanym podglądzie ChatGPT: dodanie dwóch ćwiczeń i trzech serii z datą 2026-10-31, automatyczny wybór października, ponowny odczyt, komunikat błędu ciężaru z zachowaniem danych, zapis edycji powtórzeń i widok mobilny bez przewijania poziomego. Natywne potwierdzenie porzucenia zmian zablokowało automatykę jednej karty; tej próby nie uznajemy za zakończoną. Kontynuowano odczyt/edycję w oddzielnej karcie podglądu, bez modyfikacji danych użytkownika.

Użytkownik określił następnie, że testy interfejsu mają odbywać się wyłącznie w osobnej, rzeczywistej przeglądarce, nie wewnątrz aplikacji ChatGPT. Zakończono próby w podglądzie i zapisano tę zasadę w AGENTS.md. Opisane próby nie zastępują akceptacji w zwykłej przeglądarce; ta pozostaje do wykonania. Dwa własne konta testowe i ich lokalne poświadczenia usunięto przez API. Nie odczytywano ani nie zmieniano kont użytkownika.


Doprecyzowanie zasad weryfikacji przez użytkownika: ręczne testy wykonuje sam użytkownik w Firefoksie. Agent ma polegać wyłącznie na automatycznych buildach i testach projektu w GitHub Actions; nie sprawdza dostępności przeglądarek, nie prowadzi doraźnych prób UI/API ani dodatkowych lokalnych/VPS sesji testowych bez osobnego polecenia. Zaktualizowano AGENTS.md. Wcześniejsze wpisy pozostają historycznym rejestrem wykonanych sprawdzeń.
