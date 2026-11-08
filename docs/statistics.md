# Podsumowania i serie

## Sposób liczenia

- Codziennie / wybrane dni: kolejne wykonane terminy harmonogramu. Dni wolne nie przerywają serii. Dzisiejszy brak wykonania jest w toku; pominięty wcześniejszy termin przerywa serię.
- N dni na tydzień: stałe siedmiodniowe okresy od daty rozpoczęcia nawyku, np. środa–wtorek. Data startu jest niezmiennym początkiem wszystkich kolejnych okresów. Nie używamy ruchomego okna ani tygodnia poniedziałek–niedziela do oceny tego celu.
- Jedna data liczy się raz. Nadmiarowe, przyszłe, niezaplanowane daty oraz daty poza aktywnością nawyku są odfiltrowywane przez czyste obliczenia.
- Osiągnięty cel tygodniowy przedłuża serię od razu; otwarty nieosiągnięty okres nie zeruje poprzedniej. Pełny zamknięty okres bez celu zeruje serię.
- Archiwizacja zamyka aktywność dzień przed archivedOn. Nieosiągnięty skrócony ostatni okres jest PARTIAL i poza oceną; osiągnięty jest MET. Seria jest potem zamrożona. Najlepsza seria pozostaje widoczna.
- Wszystko wynika z historii; nie dodajemy liczników do bazy ani migracji. Zmiana reguły tygodnia dotyczy też istniejących nawyków i przelicza ich historię od ich pierwotnej daty startu.

## Interfejs i zakres raportu

Zakładka Podsumowanie wybiera tydzień kalendarzowy strzałkami (z widocznym zakresem dat) lub bezpośrednio miesiąc w polu MM-RRRR. Nie pokazujemy pomocniczego dnia wewnątrz okresu. Wykonania i liczba treningów obejmują tylko wybrany zakres do dzisiaj. Codzienne/wybrane terminy mają licznik wykonane/zaplanowane do dzisiaj, bez przyszłych dat. Dzisiejszy termin może być w toku; procent jest bieżącym postępem, a nie oceną zamkniętego okresu.

Tygodniowe cele mają osobne wiersze pełnych okresów przecinających filtr raportu. Ich liczniki obejmują cały własny tydzień nawyku, także daty poza wybranym miesiącem. Wiersze pokazują daty, wykonania/cel, procent ograniczony do 100% i MET/MISSED/IN_PROGRESS/PARTIAL z polskimi etykietami. Nie sumujemy procentów różnych harmonogramów w jeden mylący wynik.

Bieżąca i najlepsza seria obejmuje całą historię do dzisiaj (lub archiwizacji), niezależnie od wybranego raportu. Serie są też w szczegółach historii nawyku. Ekran Dzisiaj pokazuje licznik i daty własnego okresu celu tygodniowego. Trening pozostaje odrębnym wpisem, nie wykonuje automatycznie nawyku.

## Kod i współbieżność

HabitProgress to czysta Java: niemutowalne rekordy Progress, Period, Streak; jawne wejścia i data dzisiejsza, bez zegara/SQL/Springa. Kopiuje listę wyników. Oblicza różne daty, filtruje harmonogram i granice, grupuje tygodnie względem startu i wyznacza serie. Lokalne liczniki nie wychodzą poza funkcję.

ReportService uruchamia przez ParallelReport dwa niezależne zadania: podsumowanie nawyków i SQL count treningów. Używa standardowego ExecutorService Java 21 (bez nowej biblioteki), czterech pracowników i kolejki 32 zadań. Każdy raport zgłasza dwa zadania; przeciążenie daje 503. Wyniki to niemutowalne wartości składane dopiero po zakończeniu obu zadań. Błędy lub timeout nie dają częściowego sukcesu; oczekiwanie ma limit 20 sekund na zadanie, nieukończone zadania są anulowane, przerwanie zachowuje flagę wątku. Executor jest zamykany przy końcu aplikacji.

Nie przekazujemy sesji HTTP, JDBC Connection ani kontekstu transakcji między wątkami. Zadanie nawyków tworzy własną transakcję odczytu REPEATABLE_READ, aby lista i historia stanowiły jeden snapshot. Licznik treningów to jedno niezależne zapytanie z account_id. Raport nie gwarantuje wspólnego snapshotu obu części przy jednoczesnej edycji; ponowne pobranie odświeża wynik. Nie ma współdzielonego mutowalnego akumulatora wyników.

ParallelReportTest używa bariery: oba zadania muszą dotrzeć do niej, zanim którekolwiek się zakończy. Sekwencyjne wykonanie nie przejdzie tego testu. Testy nie polegają na porównywaniu szybkości zegara. Pozostałe testy obejmują awarię, odrzucenie zadania i przerwanie.

Podsumowanie jest podłączone do obu kanałów SSE. Wiadomość lub ponowne połączenie powoduje pobranie aktualnego REST, z zachowaniem widoku podczas odświeżania tego samego zakresu. Szczegóły: sse.md.
