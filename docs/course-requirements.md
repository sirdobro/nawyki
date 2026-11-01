# Wymagania zajęć i weryfikacja

Podstawa: dostarczona instrukcja lab-script-pl.pdf, punkty 5 i 7.2, oraz szablon dokumentacji. Nie kopiujemy prywatnych materiałów do repozytorium. Temat 15 obejmuje nawyki i treningi z ćwiczeniami oraz seriami powtórzeń.

## Macierz realizacji

| Nr | Wymaganie | Planowany dowód | Stan |
|---|---|---|---|
| 1 | Niemutowalne typy produktowe/sumacyjne, bez null w dziedzinie | HabitRules, Schedule, Result; kopie kolekcji i Optional | Wdrożone w kodzie nawyków; treningi pozostały |
| 2 | Czysty rdzeń, efekty na brzegach, jawne zależności | domain: czysta walidacja, DailyStreak i testy bez Springa | Zaimplementowane dla obecnego rdzenia |
| 3 | Błędy jako wartości, kumulacja błędów | Result i test trzech niezależnych błędów żądania | Zaimplementowane |
| 4 | Min. 8 REST endpointów JSON, składane trasy/filtry | docs/api.md, JournalRoutes i JournalApiIT | Ponad 8 tras JSON; treningi/raporty pozostały |
| 5 | Baza, niemutowalne odwzorowanie, transakcja | PostgreSQL/JDBC, transakcje kont i wykonań, test rollback fundamentu | Częściowo; rollback całego treningu pozostał |
| 6 | Min. 2 endpointy reaktywne/strumieniowe | Dwa SSE: nawyki i treningi, test dostarczania zmian | Planowane |
| 7 | Operacja współbieżna bez wspólnego mutowalnego stanu | Niezależne części raportu, dowód współbieżnego wykonania | Planowane |
| 8 | Testy jednostkowe, min. 3 właściwości, API, min. 70% linii rdzenia | 25 testów, w tym 3 właściwości; 55/55 linii obecnego domain | Spełnione dla obecnego kodu; kolejne moduły wymagają testów |
| 9 | Instrukcja budowy, uruchomienia i testów | README, Dockerfile, CI, deploy/ i docs/verification.md | Zaimplementowane; kompletna instrukcja oddania pozostała |

## Plan testów

- Jednostkowe: trzy harmonogramy, daty graniczne, strefa czasowa, serie dzienne i tygodniowe, archiwizacja, walidacja treningu i kumulacja błędów.
- Właściwości: przestawienie kolejności historii nie zmienia serii; duplikaty dat nie zmieniają wyniku; dodanie dat poza harmonogramem do surowego wejścia obliczeń nie zmienia wyniku serii. Nie generujemy wyłącznie trywialnych przykładów; kontrakt surowego wejścia i oczyszczania określimy przed testem.
- Integracyjne z prawdziwym PostgreSQL: CRUD, kody i JSON, dwa równoczesne PUT tego samego wykonania, poprawne dane po restarcie aplikacji, rollback treningu, dwa strumienie SSE i rozłączenia.
- Frontend: główny przepływ, zachowanie danych po błędzie, klawiatura, mały ekran i komunikaty stanu.
- Pokrycie: co najmniej 70% linii samego rdzenia; nie maskujemy wyniku pokryciem konfiguracji lub kontrolerów. Narzędzia i raport dobierzemy w szkielecie.

## Dokumentacja końcowa

Około 10–20 stron PDF: tytuł i dane zespołu, macierz realizacji, udział autorów, problem i użytkownicy, historyjki i kryteria, technologie z wersjami, diagram architektury, projekt funkcyjny z rzeczywistym kodem, API, diagram bazy i transakcje, testy i pokrycie, uruchamianie, decyzje i ograniczenia, użycie AI.

Nie wypełniamy statusów realizacji, udziałów autorów ani wyników testów na podstawie planu. Numer zespołu, rok akademicki, termin i dane autorów potwierdza użytkownik.

## Użycie AI

Dotychczas: Codex pomagał przeanalizować materiały, porównać technologie i hosting, ustalić zakres i interfejs oraz przygotować początkowe dokumenty repozytorium. AI przygotował też szkielet, interfejs React, trasy HTTP, model i walidację nawyków, SQL/JDBC, testy, dokumentację i automatyzację wdrożenia. Nie oznacza to samodzielnego wykonania tych elementów przez człowieka. Rozdział końcowy należy aktualizować w miarę implementacji, wskazując rzeczywiste pliki i zastosowania.
