# Rozwój i wdrażanie

## Cel organizacyjny

Repozytorium: https://github.com/sirdobro/nawyki.git

Docelowy katalog lokalny: C:\Users\mikep\Documents\Prog\nawyki

Użytkownik chce możliwie wcześnie klikać kolejne wersje online, bez konieczności uruchamiania aplikacji i bazy lokalnie. Kod może być edytowany lokalnie; budowanie i testy mają działać w GitHub Actions. Hosting całej aplikacji i PostgreSQL u jednego dostawcy. Zakupiono OVHcloud VPS-1 z Ubuntu 26.04 LTS. Serwer jest dostępny przez SSH; pierwszy szkielet aplikacji wdrożono jako df4b2f2.

## Etapy

1. Uzgodnienia i instrukcje repozytorium — ten commit.
2. Budowalny szkielet Java/MVC, PostgreSQL i React; przypięte wersje, wrapper, lockfile, migracje i pierwsze testy.
3. Rzeczywisty workflow CI: budowanie obu części, jednostkowe/właściwości, integracyjne z tymczasowym PostgreSQL, kontrola pokrycia.
4. Przygotowanie VPS i HTTPS; decyzja o dostępie do dziennika. Automatyczne wdrażanie po pomyślnym CI.
5. Nawyki, wykonania i historia; następnie serie, raporty, minimalne treningi i SSE.
6. Weryfikacja wszystkich wymagań, dokumentacja PDF i czyste archiwum źródeł.

Hosting OVHcloud jest już zakupiony; nie zamawiamy następnego serwera. Nie tworzymy teraz pozornego CI bez kompilowalnej aplikacji.

## Docelowy przepływ

Branch → przegląd zmian/PR → testy → main → wdrożenie → sprawdzenie stałego adresu online.

Zatwierdzono: poprawna weryfikacja bieżącego main publikuje obraz jako publiczne wydanie GitHub, a VPS pobiera je co około trzy minuty. Prywatny klucz SSH nie jest przekazywany do GitHub. Zmiany w repozytorium nie powinny wykonywać dowolnego kodu z niezaufanego PR z dostępem do sekretów; nie używamy uprzywilejowanego pull_request_target do takiego budowania.

Jedno środowisko online służy rozwojowi i demonstracji. Testy CI mają własną, usuwaną bazę PostgreSQL i fikcyjne dane. Nie podłączamy testów do bazy online.

## Wdrożenie

Planowany pakiet: Spring Boot ze statycznym buildem Reacta; PostgreSQL jako osobny proces/kontener na tym samym VPS; reverse proxy i HTTPS. Docker Compose jest kandydatem do powtarzalnego uruchamiania, nie dodatkową usługą hostingową. Node nie musi działać na produkcji.

Przed pierwszym wdrożeniem potrzebne są: zatwierdzony zakup serwera, adres aplikacji nawyki.intmain.cc, dostęp wdrożeniowy, sekrety bazy, polityka dostępu i trwały wolumen PostgreSQL. Użytkownik wyłączył stałe eksporty i dodatkowy system backupu z zakresu MVP; dane są testowe, pozostaje backup OVH. Zdefiniujemy healthcheck, zachowanie podczas migracji i powrót do poprzedniego obrazu. Cofnięcie aplikacji nie cofa automatycznie zmian schematu bazy.

## Otwarte decyzje

- Konta zatwierdzone: wiele pseudonimowych kont z kluczem dostępu, bez e-maila i hasła; szczegóły w authentication.md.
- Cel N dni w pierwszym i ostatnim niepełnym tygodniu.
- Wersje narzędzi ustalono i przypięto; bieżąca lista znajduje się w README.
- VPS OVHcloud oraz domena nawyki.intmain.cc są wybrane; DNS, HTTPS i pierwsza automatyczna publikacja/wdrożenie są zweryfikowane. Bez dodatkowych backupów i monitoringu w MVP.

Nie są to powody do ponownego otwierania ustalonego wyboru języka, bazy ani frontendu.
