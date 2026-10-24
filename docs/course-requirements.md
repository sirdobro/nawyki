# Wymagania zajęć i weryfikacja

Podstawa: dostarczona instrukcja lab-script-pl.pdf, punkty 5 i 7.2, oraz szablon dokumentacji. Nie kopiujemy prywatnych materiałów do repozytorium. Temat 15 obejmuje nawyki i treningi z ćwiczeniami oraz seriami powtórzeń.

## Macierz realizacji

| Nr | Wymaganie | Planowany dowód | Stan |
|---|---|---|---|
| 1 | Niemutowalne typy produktowe/sumacyjne, bez null w dziedzinie | Rekordy, sealed, defensywne kopie, Optional | Planowane |
| 2 | Czysty rdzeń, efekty na brzegach, jawne zależności | Moduł domain, przykłady i testy niezależne od Springa | Planowane |
| 3 | Błędy jako wartości, kumulacja błędów | Result i test wielu błędów jednego żądania | Planowane |
| 4 | Min. 8 REST endpointów JSON, składane trasy/filtry | Tabela API i testy integracyjne | Planowane |
| 5 | Baza, niemutowalne odwzorowanie, transakcja | PostgreSQL/JDBC, rollback zapisu treningu | Planowane |
| 6 | Min. 2 endpointy reaktywne/strumieniowe | Dwa SSE: nawyki i treningi, test dostarczania zmian | Planowane |
| 7 | Operacja współbieżna bez wspólnego mutowalnego stanu | Niezależne części raportu, dowód współbieżnego wykonania | Planowane |
| 8 | Testy jednostkowe, min. 3 właściwości, API, min. 70% linii rdzenia | CI i rzeczywisty raport pokrycia | Planowane |
| 9 | Instrukcja budowy, uruchomienia i testów | README i sprawdzony start na czystym środowisku | Planowane |

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

Dotychczas: Codex pomagał przeanalizować materiały, porównać technologie i hosting, ustalić zakres i interfejs oraz przygotować początkowe dokumenty repozytorium. Nie powstała jeszcze aplikacja. Rozdział końcowy należy aktualizować w miarę implementacji, wskazując rzeczywiste pliki i zastosowania.
