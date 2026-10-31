# Zakres aplikacji

## Cel

Pomóc użytkownikowi zaplanować powtarzalne działania, szybko zaznaczać wykonanie i wiarygodnie oceniać regularność. Tracker nawyków jest głównym produktem. Minimalny dziennik treningu siłowego uzupełnia temat 15, nie zastępując aplikacji treningowej.

## Ekrany

1. Dzisiaj: nawyki zaplanowane na wybrany dzień, oznaczanie i cofanie wykonania, osobna lista elastycznych celów tygodniowych.
2. Nawyki: tworzenie, zmiana nazwy, szczegóły, historia, serie i archiwizacja.
3. Treningi: lista wpisów i formularz daty, ręcznych nazw ćwiczeń i poszczególnych serii z powtórzeniami oraz opcjonalnym ciężarem w kg. Odczyt, poprawianie i usuwanie wpisów.
4. Statystyki: tydzień/miesiąc, wykonania i cele nawyków, bieżące i najlepsze serie, liczba treningów.

Nie dodajemy katalogu ćwiczeń, planów, szablonów treningowych, timera, rekordów, szacowania maksymalnego ciężaru, filmów, integracji z zegarkiem ani automatycznego powiązania treningu z nawykiem. Brak czatu, rankingów, powiadomień push i offline sync.

## Reguły nawyków

- Harmonogramy: codziennie, niepusty zbiór dni tygodnia albo N różnych dni w tygodniu (1–7).
- Każdy nawyk ma datę rozpoczęcia. Nazwa jest niepusta po usunięciu zewnętrznych spacji.
- Jedno wykonanie na nawyk i lokalną datę. Idempotentny zapis; unikalność również w bazie.
- Nawyk jest binarny. Cele ilościowe pozostają w nazwie, bez pomiaru jednostek.
- Można uzupełniać historię od daty rozpoczęcia, nie można oznaczać przyszłości. Dla harmonogramu konkretnych dni zapis jest dozwolony tylko w dzień zaplanowany.
- Dni niezaplanowane nie przerywają serii. Seria codzienna / wybranych dni liczy kolejne wykonane wystąpienia harmonogramu.
- Dzisiejszy niewykonany termin nie przerywa jeszcze serii z wczoraj; pominięty zamknięty termin już tak.
- Cel N dni liczy różne daty w tygodniu poniedziałek–niedziela. Nadwyżka jest dozwolona i widoczna jako liczba wykonań, ale realizację celu ograniczamy do 100%.
- Seria celu tygodniowego jest liczona w tygodniach, nie dniach. Nieukończony bieżący tydzień nie jest porażką. Osiągnięty cel bieżącego tygodnia może przedłużyć serię.
- Nieukończone bieżące okresy pokazujemy jako „w toku”, a nie jako zamknięty wynik. Przyszłe terminy nie wchodzą do mianownika realizacji do dzisiaj.
- Serie i statystyki wynikają z historii. Nie zapisujemy niezależnego, ręcznie aktualizowanego licznika serii.
- Strefa czasowa jest jawnym ustawieniem, początkowo Europe/Warsaw.
- Harmonogram nie jest edytowalny w pierwszej wersji. Archiwizacja i nowy nawyk zastępują zmianę reguł historycznych.
- Archiwizacja zachowuje historię i wyłącza kolejne terminy od lokalnej daty archiwizacji. Korekty wcześniejszych wykonań są dozwolone; nowe wykonania od tej daty nie.

Szczegółowa zasada celu tygodniowego w pierwszym/ostatnim niepełnym tygodniu wymaga doprecyzowania przed implementacją: nie zgadujemy proporcji celu.

## Minimalny trening siłowy

Trening ma lokalną datę nie późniejszą niż dzisiaj, co najmniej jedno ćwiczenie i co najmniej jedną serię w każdym ćwiczeniu. Każda seria ma dodatnią całkowitą liczbę powtórzeń i opcjonalny nieujemny ciężar dziesiętny w kg. Brak ciężaru oznacza brak jego rejestrowania, np. ćwiczenie z masą ciała. Nie używamy float do zapisu ciężaru w domenie; stosujemy BigDecimal.

Kolejność ćwiczeń i serii jest zachowana. Zapis lub aktualizacja całego treningu jest atomowa. Przykład: przysiad 8 × 40 kg, 8 × 40 kg, 6 × 40 kg to trzy odrębne serie. Zapis treningu nie odznacza nawyku „Idź na trening”.

## Kryteria akceptacji

| ID | Potrzeba | Kryterium |
|---|---|---|
| US-01 | Zaplanować nawyk | Każdy z trzech harmonogramów ma poprawnie walidowany model; formularz pokazuje wiele niezależnych błędów. |
| US-02 | Zaznaczyć wykonanie | Powtórne lub jednoczesne żądanie nie tworzy duplikatu. |
| US-03 | Poprawić historię | Cofnięcie lub dodanie zaległego wykonania aktualizuje serię i raport. |
| US-04 | Śledzić regularność | Dni wolne i nieukończone okresy nie powodują fałszywej utraty serii. |
| US-05 | Zarchiwizować nawyk | Nawyk znika z kolejnych planów; przeszła historia pozostaje dostępna. |
| US-06 | Zapisać trening | Wszystkie ćwiczenia i serie zapisują się razem albo żadne. |
| US-07 | Poprawić trening | Użytkownik może otworzyć, zmienić i usunąć wpis. |
| US-08 | Zobaczyć postęp | Raporty tygodnia i miesiąca wskazują okres, sposób liczenia celu i liczbę treningów. |
| US-09 | Korzystać w dwóch kartach | Zmiany nawyków/wykonań i treningów aktualizują właściwe widoki przez dwa strumienie SSE. |

Wszystkie historyjki są obecnie planowane, nie zrealizowane.

## Interfejs

Polski język, responsywny układ, czytelne etykiety, obsługa klawiatury i widoczny fokus. Statusy nie mogą zależeć wyłącznie od koloru. Wygodne cele dotykowe około 44 px. Pokazujemy zapis, sukces i błąd z ponowieniem. Nie prezentujemy sukcesu bez potwierdzenia zapisu; ewentualne optymistyczne aktualizacje muszą obsługiwać wycofanie.

Puste widoki prowadzą do pierwszego wpisu. Formularze zachowują dane po błędzie. Usunięcie treningu i archiwizacja wymagają świadomej akcji. Zatwierdzono wiele pseudonimowych kont bez imienia, e-maila i hasła. Losowy klucz dostępu oraz opcjonalna prywatna zakładka służą do logowania; szczegóły w authentication.md.
