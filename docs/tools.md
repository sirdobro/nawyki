# Narzędzia projektu i jak je wyjaśnić

## Uzgodniony stack aplikacji

Java 21, Spring Boot i Spring MVC z funkcyjnymi trasami, PostgreSQL, JDBC z jawnym SQL, React z JavaScript/JSX i CSS. Logika dziedziny to czysta Java. Nie wprowadzono WebFlux, R2DBC, Vavr, Hibernate/JPA, TypeScriptu ani Next.js.

## Małe narzędzia pomocnicze

| Narzędzie | Rola | Wyjaśnienie |
|---|---|---|
| Maven Wrapper | Budowanie Javy | Pobiera ustaloną wersję Mavena i wykonuje polecenia z pom.xml. |
| Vite | Budowanie Reacta | Zamienia pliki źródłowe interfejsu na statyczne pliki serwowane przez Spring. |
| Flyway | Wersjonowanie schematu | Uruchamia nasze numerowane pliki SQL i zapisuje, które już wykonał. Nie jest ORM i nie zastępuje JDBC. |
| Spring Security | Ochrona żądań HTTP | Sprawdza dostęp do prywatnych tras i token CSRF dla zapisów. Nie wprowadza haseł ani osobnej usługi. Nasz AccountService generuje klucze i obsługuje sesje w PostgreSQL. Wersję dobiera przypięty Spring Boot. |
| JUnit | Testy | Uruchamia testy przykładów i integracji. |
| QuickTheories | Testy właściwości | Generuje wiele przypadków dla reguł, które mają być zawsze prawdziwe. |
| JaCoCo | Pokrycie testami | Mierzy wykonane linie kodu; gate dotyczy tylko pakietu domain. |

## Narzędzia serwera

| Narzędzie | Rola |
|---|---|
| Docker i Compose | Uruchamianie aplikacji, bazy i proxy w powtarzalnych kontenerach. |
| Caddy | Certyfikat HTTPS i przekazywanie żądań do Springa; bez logiki biznesowej. |
| systemd | Istniejący mechanizm Ubuntu uruchamiający sprawdzanie nowych wydań. |
| GitHub Actions | Budowanie, testy i publikacja obrazu. VPS pobiera publiczny obraz; GitHub nie otrzymuje klucza SSH. |

Flyway został dodany na etapie szkieletu, aby uniknąć ręcznego śledzenia zmian schematu. Jego cel i opcjonalność wyjaśniono użytkownikowi. Przed dodaniem kolejnych komponentów należy najpierw wyjaśnić potrzebę, koszt nauki i możliwość wykorzystania istniejących narzędzi.
