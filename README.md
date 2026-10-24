# Nawyki

Projekt semestralny na przedmiot „Programowanie aplikacji internetowych w językach funkcyjnych”. Aplikacja pomaga planować nawyki, oznaczać ich wykonanie i śledzić postęp. Zawiera też minimalny dziennik treningu siłowego wymagany przez opis tematu 15.

## Stan projektu

Repozytorium zawiera uzgodnienia i zasady realizacji. Kod aplikacji, konfiguracja budowania, testy oraz wdrożenie nie zostały jeszcze utworzone. Nie deklarujemy zrealizowanych wymagań ani wyników testów przed ich weryfikacją.

## Ustalony stack

- Java 21, Spring Boot i Spring MVC z funkcyjnymi trasami.
- PostgreSQL oraz JDBC ze wsparciem Springa; jawny SQL.
- React, JavaScript, JSX i CSS; Vite jako narzędzie budowania interfejsu.
- Maven Wrapper jako docelowy sposób budowania części Java.
- Czysty rdzeń w Javie: rekordy, typy sealed, niemutowalne kolekcje, Optional i własny Result.
- SSE dla dwóch strumieni zmian. Bez WebFlux, Reactor, R2DBC, Vavr, Hibernate/JPA i Next.js.

Dokładne wersje bibliotek oraz narzędzia testów właściwości zostaną dobrane i przypięte przy tworzeniu szkieletu. Nie zmieniamy stacku bez konkretnego powodu i uzgodnienia.

## Dokumenty

- [Zakres i reguły produktu](docs/product.md)
- [Architektura i decyzje](docs/architecture.md)
- [Plan API](docs/api.md)
- [Wymagania zajęć i plan testów](docs/course-requirements.md)
- [Rozwój i wdrażanie](docs/development.md)
- [Bezpieczeństwo](SECURITY.md)
- [Instrukcje dla agentów](AGENTS.md)

## Uruchomienie

Instrukcja z rzeczywistymi poleceniami pojawi się wraz z działającym szkieletem. Plan: jedno wdrożenie aplikacji Spring serwującej także zbudowany interfejs React, osobny proces PostgreSQL na tym samym VPS oraz HTTPS. Nie są jeszcze wymagane zakup serwera ani dane dostępowe.

Plik `.env.example` jest wyłącznie przykładem nazw ustawień; obecnie żaden program go nie odczytuje. Nie umieszczaj w repozytorium prawdziwych haseł, tokenów, kluczy ani danych użytkowników.

## Oddanie

Docelowo kod źródłowy w ZIP oraz dokumentacja PDF około 10–20 stron, zgodnie z instrukcją zajęć. Pliki źródłowe dokumentacji utrzymujemy razem z projektem. Termin, numer zespołu i dane autorów wymagają uzupełnienia.
