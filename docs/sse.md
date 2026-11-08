# Dwa strumienie zmian SSE

## Działanie

GET /api/events/habits oraz GET /api/events/workouts to dwa prawdziwe, długotrwałe strumienie text/event-stream w Spring MVC functional routing (ServerResponse.sse). React używa natywnego EventSource. Bez WebFlux/Reactor, WebSocket, Redis i nowych bibliotek.

Po połączeniu serwer wysyła event ready z {"refresh":true}. Każda zatwierdzona zmiana odpowiedniego konta daje event changed z tym samym krótkim sygnałem. Nie wysyłamy identyfikatorów, nazw, dat, historii, ćwiczeń, kluczy ani cookie. Klient pobiera aktualny stan przez istniejący REST. Zmiany jednego konta nigdy nie są rozsyłane do innego konta ani niewłaściwego kanału.

Nawyki: utworzenie, nazwa, archiwizacja, dodanie/cofnięcie wykonania. Treningi: utworzenie, zastąpienie całego wpisu, usunięcie. Brak rzeczywistej zmiany przy powtórnym idempotentnym wykonaniu/usunięciu nie wymaga zdarzenia.

JournalChanges publikuje niemutowalne Change(account, channel). JournalStreams odbiera je przez @TransactionalEventListener w fazie AFTER_COMMIT; fallbackExecution obsługuje pojedyncze, już zakończone zapisy autocommit (np. nazwa). Wieloetapowe zapisy zachowują dotychczasowe transakcje. Rollback nie wysyła changed. Publikacja nie zapisuje na gnieździe i nie czeka na wolnego klienta.

## Sesje i zasoby

Początkowe żądanie SSE wymaga tej samej sesji co REST (401 bez niej), bez parametrów z kluczem w URL. Techniczny dispatch ASYNC kończący już otwartą odpowiedź jest dozwolony przez filtr; nie omija uwierzytelnienia początkowego REQUEST. Strumień zachowuje tylko hash identyfikatora sesji i konto. Przed każdym sygnałem oraz co 15 sekund sprawdza ważność tej konkretnej sesji (konto, absolutne wygaśnięcie i bezczynność), bez przesuwania last_seen_at. Cofnięcie sesji, rotacja klucza lub usunięcie konta powoduje session-ended i zamknięcie najpóźniej przy następnym sprawdzeniu. Frontend ponownie sprawdza auth/me; może wykryć również wspólne cookie zmienione w drugiej karcie. Nie ukrywa nowo wydanego klucza w trakcie własnego logowania/rotacji.

Maksymalnie 16 strumieni na konto (zwykle osiem kart po dwa kanały), 256 łącznie na instancję. Przekroczenie daje 429, a wyścig rejestracji po rozpoczęciu odpowiedzi — event limit i koniec. To limit zasobów MVP, nie wynik testu obciążeniowego. Każdy klient ma jeden lekki wątek wirtualny Java 21, semafor i jedną flagę oczekującej zmiany. Flaga scala serię sygnałów, nie tworzy nieograniczonej kolejki. Builder ma jednego autora. Completion, błąd, timeout i zamknięcie aplikacji usuwają rejestrację oraz przerywają pracownika. Rejestr i synchronizacja są efektownym adapterem poza czystą domeną.

Heartbeat to komentarz keepalive co około 15 sekund ciszy. Połączenie ma maksymalny czas 10 minut, potem przeglądarka je wznawia. retry=3000 ms dla przerwania strumienia; frontend ponawia także trwale zamknięte próby (np. błąd handshake) po 10 sekundach. Caddy ma już flush_interval -1, Cloudflare pozostaje bez zmian. Odpowiedzi SSE mają no-cache, a Caddy wymusza no-store; prywatne REST nadal no-store.

## Odtwarzanie stanu i interfejs

Nie ma magazynu/replay zdarzeń ani gwarancji dokładnie jednego dostarczenia. Każde ready, także po ponownym połączeniu/redeployu, powoduje odczyt bieżącego REST. Dzięki temu przegapiona zmiana nie zostaje trwale pominięta. Przeglądarka scala sygnały z 150 ms; formularze nie są źródłem stanu do podmiany. Efekty React zamykają oba EventSource i timery po wylogowaniu, zmianie konta lub odmontowaniu.

Nawyki odświeżają plan, listę, metadane szczegółów, historię i serie. Oba kanały odświeżają podsumowanie. Treningi odświeżają listę i otwarty podgląd; usunięty zdalnie podgląd wraca do listy z informacją. Niezapisany formularz treningu pozostaje bez zmian. Przy sygnale podczas edycji istniejącego wpisu pokazujemy informację: zapis zastąpi wpis wersją z formularza, anulowanie pozwala zobaczyć aktualny zapis. Nie implementujemy automatycznego scalania ani wersjonowania konfliktów. Podczas odświeżania tego samego zakresu zachowujemy poprzedni poprawny widok, aby nie migał. Status synchronizacji jest pod widokiem.

Rozwiązanie jest dla obecnej jednej instancji aplikacji na VPS. Przy wielu instancjach potrzebny byłby osobny mechanizm rozsyłania zmian pomiędzy nimi. Nie dodajemy go do tego MVP.

## Automatyczna weryfikacja

JournalStreamsIT w GitHub Actions używa prawdziwego serwera HTTP, strumieniowego klienta i jednorazowego PostgreSQL. Sprawdza oba kanały, autoryzację, izolację kont/kanałów, mutacje REST, brak sygnałów po błędach i rollbacku, brak sygnału przed commit, zamknięcie po cofnięciu/wygaśnięciu sesji/rotacji/usunięciu konta, sprzątanie po rozłączeniu, ready i odczyt REST po powrocie oraz limit konta. Częstotliwość heartbeat w tych testach jest skrócona do 100 ms; w produkcji 15 s. Wyniki i liczby należy potwierdzić z CI w verification.md.

## Ręczny test w Firefoksie

1. Odśwież aplikację po wdrożeniu. Otwórz dwie karty tego samego dziennika; na dole powinno pojawić się „Synchronizacja aktywna”. Zwykłe karty współdzielą cookie — nie trzeba ponownie używać prywatnego linku.
2. W karcie A dodaj nawyk, zmień nazwę, zaznacz/cofnij wykonanie i zarchiwizuj. Karta B ma odzwierciedlić każdą zmianę bez przeładowania dokumentu. Sprawdź też otwartą historię/serię i zakładkę Podsumowanie.
3. W A dodaj, edytuj i usuń trening. W B sprawdź listę, otwarty podgląd i liczbę treningów w Podsumowaniu (wybierz odpowiedni miesiąc).
4. W B rozpocznij edycję istniejącego treningu, wpisz inną wartość i nie zapisuj. W A zmień ten sam trening. Formularz B ma zachować Twoje wartości i pokazać informację o możliwej zmianie. Anuluj B: podgląd powinien pokazać aktualny zapis. Nie używaj prawdziwych danych do próby nadpisywania.
5. Odłącz na chwilę internet na drugim urządzeniu albo włącz tryb offline w narzędziach deweloperskich tylko jednej karty. W działającej karcie zmień dane. Po przywróceniu połączenia druga karta ma samodzielnie nadrobić stan. Status może potrzebować kilku sekund na wykrycie utraty połączenia.
6. Osobne konto sprawdzaj w drugim profilu/prywatnym oknie albo na innym urządzeniu z innym kluczem. Zmiany A nie mogą pojawić się w jego dzienniku. Dwie zwykłe karty nie służą do izolacji różnych kont, bo mają wspólne cookie.
7. Wyloguj wszystkie urządzenia z A. B powinna wykryć koniec sesji w ciągu około 15 sekund i wrócić do logowania. Nie publikuj zrzutów z kluczem lub prywatnym linkiem.

Źródła interfejsów: [Spring MVC ServerResponse.sse](https://docs.spring.io/spring-framework/docs/6.2.16/javadoc-api/org/springframework/web/servlet/function/ServerResponse.html), [SseBuilder](https://docs.spring.io/spring-framework/docs/6.2.16/javadoc-api/org/springframework/web/servlet/function/ServerResponse.SseBuilder.html).
