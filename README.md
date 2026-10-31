# Nawyki

Projekt semestralny na przedmiot „Programowanie aplikacji internetowych w językach funkcyjnych”. Aplikacja pomaga planować nawyki, oznaczać ich wykonanie i śledzić postęp. Zawiera też minimalny dziennik treningu siłowego wymagany przez opis tematu 15.

## Stan projektu

Pierwszy szkielet: React serwowany przez Spring MVC, trzy endpointy stanu, PostgreSQL, migracja pustego schematu kont oraz czysta funkcja serii codziennych nawyków. Tworzenie kont, nawyki i treningi nie są jeszcze dostępne. Strona jawnie pokazuje etap rozwojowy i sprawdza gotowość API/bazy.

## Ustalony stack

- Java 21, Spring Boot i Spring MVC z funkcyjnymi trasami.
- PostgreSQL oraz JDBC ze wsparciem Springa; jawny SQL.
- React, JavaScript, JSX i CSS; Vite jako narzędzie budowania interfejsu.
- Maven Wrapper jako docelowy sposób budowania części Java.
- Czysty rdzeń w Javie: rekordy, typy sealed, niemutowalne kolekcje, Optional i własny Result.
- SSE dla dwóch strumieni zmian. Bez WebFlux, Reactor, R2DBC, Vavr, Hibernate/JPA i Next.js.

Wersje: Spring Boot 3.5.16, Flyway 11.20.3, Maven 3.9.11, React 19.3.1, Vite 8.3.5, Node 24, QuickTheories 0.26 i JaCoCo 0.8.14. Obrazy kontenerów i Actions przypięto do digestów/SHA. package-lock.json utrwala zależności frontendu.

## Dokumenty

- [Narzędzia i ich proste wyjaśnienie](docs/tools.md)
- [Zakres i reguły produktu](docs/product.md)
- [Architektura i decyzje](docs/architecture.md)
- [Plan API](docs/api.md)
- [Wymagania zajęć i plan testów](docs/course-requirements.md)
- [Rozwój i wdrażanie](docs/development.md)
- [Konta bez e-maila i hasła](docs/authentication.md)
- [Konfiguracja VPS i Cloudflare](docs/hosting-setup.md)
- [Rejestr zabezpieczeń VPS i dalsze kroki](docs/vps-security.md)
- [Zainstalowane środowisko serwerowe](docs/runtime-installation.md)
- [Pliki Docker Compose](deploy/README.md)
- [Bezpieczeństwo](SECURITY.md)
- [Instrukcje dla agentów](AGENTS.md)

## Budowanie i testy

Wymagania do budowania: JDK 21, Node 24 i tymczasowy PostgreSQL. Na serwerze produkcyjnym wystarcza Docker/Compose; narzędzia budowania działają w GitHub Actions.

```sh
cd frontend
npm ci
npm run build
cd ..
mkdir -p src/main/resources/static
cp -R frontend/dist/. src/main/resources/static/
# Ustaw SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME i SPRING_DATASOURCE_PASSWORD
# na osobną bazę TESTOWĄ, nigdy bazę wdrożonej aplikacji.
./mvnw -B -ntp verify
```

Windows: użyj mvnw.cmd zamiast ./mvnw i odpowiedników kopiowania plików. Raporty: target/surefire-reports, target/failsafe-reports i target/site/jacoco. Weryfikacja wymaga dostępnej bazy; testy integracyjne nie są pomijane.

Budowanie obrazu: docker build -t nawyki-app:local . Uruchomienie serwerowe opisano w deploy/README.md. Ustawienia bazy są wymagane; brak konfiguracji powoduje błąd startu zamiast cichego uruchomienia z inną bazą.

## Publikacja

Workflow Verify and deploy sprawdza PR i main. Po poprawnej weryfikacji aktualnego main buduje obraz i publikuje publiczne wydanie deploy-<SHA> z archiwum obrazu i SHA-256. VPS sprawdza nowe wydania co około trzy minuty i wdraża dopiero po weryfikacji sumy i gotowości aplikacji. Nie przekazujemy prywatnego klucza SSH do GitHub; workflow używa tylko krótkotrwałego GITHUB_TOKEN do publikacji wydania.

Produkcja: https://nawyki.intmain.cc. Publikacja wydania nie jest jeszcze dowodem jego wdrożenia: aktualny commit sprawdzaj w /api/status. Bez sekretów, danych kont i bazy w obrazach ani repozytorium.

## Oddanie

Docelowo kod źródłowy w ZIP oraz dokumentacja PDF około 10–20 stron, zgodnie z instrukcją zajęć. Pliki źródłowe dokumentacji utrzymujemy razem z projektem. Termin, numer zespołu i dane autorów wymagają uzupełnienia.
