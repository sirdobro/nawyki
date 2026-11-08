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

- Harmonogramy: codziennie, niepusty zbiór dni tygodnia albo N różnych dni w kolejnych siedmiodniowych okresach od daty rozpoczęcia (1–7).
- Każdy nawyk ma datę rozpoczęcia. Nazwa jest niepusta po usunięciu zewnętrznych spacji.
- Jedno wykonanie na nawyk i lokalną datę. Idempotentny zapis; unikalność również w bazie.
- Nawyk jest binarny. Cele ilościowe pozostają w nazwie, bez pomiaru jednostek.
- Można uzupełniać historię od daty rozpoczęcia, nie można oznaczać przyszłości. Dla harmonogramu konkretnych dni zapis jest dozwolony tylko w dzień zaplanowany.
- Dni niezaplanowane nie przerywają serii. Seria codzienna / wybranych dni liczy kolejne wykonane wystąpienia harmonogramu.
- Dzisiejszy niewykonany termin nie przerywa jeszcze serii z wczoraj; pominięty zamknięty termin już tak.
- Cel N dni liczy różne daty w stałych okresach siedmiu dni od daty rozpoczęcia nawyku (np. środa–wtorek). Nie jest to ruchome okno ostatnich siedmiu dni. Nadwyżka jest dozwolona i widoczna jako liczba wykonań, ale realizację celu ograniczamy do 100%.
- Seria celu tygodniowego jest liczona w tygodniach, nie dniach. Nieukończony bieżący tydzień nie jest porażką. Osiągnięty cel bieżącego tygodnia może przedłużyć serię.
- Nieukończone bieżące okresy pokazujemy jako „w toku”, a nie jako zamknięty wynik. Przyszłe terminy nie wchodzą do mianownika realizacji do dzisiaj.
- Serie i statystyki wynikają z historii. Nie zapisujemy niezależnego, ręcznie aktualizowanego licznika serii.
- Strefa czasowa jest jawnym ustawieniem, początkowo Europe/Warsaw.
- Harmonogram nie jest edytowalny w pierwszej wersji. Archiwizacja i nowy nawyk zastępują zmianę reguł historycznych.
- Archiwizacja zachowuje historię i wyłącza kolejne terminy od lokalnej daty archiwizacji. Korekty wcześniejszych wykonań są dozwolone; nowe wykonania od tej daty nie.

Pierwszy tydzień zawsze trwa siedem dni od daty startu; cel nie jest proporcjonalnie obniżany. Przy archiwizacji nieosiągnięty skrócony ostatni okres ma status PARTIAL i nie przerywa serii; osiągnięty cel zalicza okres. Zamknięty pełny tydzień bez celu przerywa serię. Serie zarchiwizowanych nawyków zatrzymujemy na końcu ich aktywności. Raport kalendarzowego tygodnia (poniedziałek–niedziela) lub miesiąca pokazuje wykonania tylko z wybranego zakresu; osobne wiersze tygodniowych celów pokazują całe siedmiodniowe okresy przecinające zakres, z jawnymi datami. Serie są zawsze liczone z całej historii do dzisiaj, niezależnie od filtra raportu.

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

Stan etapu kont/nawyków: US-01, US-02 i US-05 zaimplementowane; US-03 pozwala poprawiać historię; serie w historii i raport są przeliczane z zapisanych wykonań. US-06 i US-07 są zaimplementowane w osobnej zakładce Treningi; US-04 i US-08 są zaimplementowane (podsumowania i serie), potwierdzone w CI 42473999791, oczekują ręcznej akceptacji; US-09 pozostaje do wykonania. Ekran Dzisiaj rozdziela zaplanowane terminy i elastyczne cele tygodniowe; pokazuje licznik wraz z datami własnego siedmiodniowego okresu.

## Interfejs

Polski język, responsywny układ, czytelne etykiety, obsługa klawiatury i widoczny fokus. Statusy nie mogą zależeć wyłącznie od koloru. Wygodne cele dotykowe około 44 px. Pokazujemy zapis, sukces i błąd z ponowieniem. Nie prezentujemy sukcesu bez potwierdzenia zapisu; ewentualne optymistyczne aktualizacje muszą obsługiwać wycofanie.

Puste widoki prowadzą do pierwszego wpisu. Formularze zachowują dane po błędzie. Usunięcie treningu i archiwizacja wymagają świadomej akcji. Zatwierdzono wiele pseudonimowych kont bez imienia, e-maila i hasła. Losowy klucz dostępu oraz opcjonalna prywatna zakładka służą do logowania; szczegóły w authentication.md.


Daty w interfejsie mają jawny polski zapis: DD-MM-RRRR, skróty historii DD-MM, filtr miesiąca MM-RRRR; nazwy miesięcy są polskie. Wspólny komponent DateField udostępnia wpisywanie oraz własny kalendarz z tygodniem od poniedziałku. Nie polegamy na formacie natywnych pól date/month zależnym od przeglądarki. Warstwa API i baza nadal korzystają z ISO YYYY-MM-DD. Ręczny zapis z kropką/ukośnikiem lub bez separatorów jest zawsze interpretowany dzień–miesiąc–rok i normalizowany na DD-MM-RRRR.

Usprawnienia formularzy: Podsumowanie wybiera miesiąc bezpośrednio, a tydzień strzałkami z jawnym zakresem dat. Dodaj serię kopiuje powtórzenia i opcjonalny ciężar z ostatniej serii tego samego ćwiczenia do nowego, niezależnie edytowalnego wiersza (również przy edycji treningu). Nowe ćwiczenie nadal zaczyna od pustej serii.
