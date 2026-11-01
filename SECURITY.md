# Bezpieczeństwo

## Dane i sekrety

Nie zapisujemy w Git haseł, tokenów GitHub, danych dostępu do VPS, prywatnych kluczy SSH/TLS, rzeczywistych plików środowiska, kopii baz ani danych użytkowników. `.gitignore` wyklucza typowe lokalizacje, ale nie zastępuje przeglądu zmian przed zatwierdzeniem.

Zwykła konfiguracja bez sekretów, migracje SQL, instrukcje bezpieczeństwa oraz `.env.example` pozostają w repozytorium. Plików bezpieczeństwa jako takich nie ignorujemy: wykluczamy ich poufną zawartość i lokalne poświadczenia.

Sekrety CI/CD przechowujemy w GitHub Actions secrets, ustawienia aplikacji w środowisku serwera. Nie zapisujemy ich w workflow, Dockerfile, historii poleceń ani logach. Dane dostępowe do bazy nigdy nie trafiają do Reacta; wszystkie pliki frontendowe są publiczne. Nie umieszczamy sekretów w zmiennych `VITE_*`.

Nie commitujemy źródłowych materiałów zajęć ani danych autorów bez potrzeby i zgody. ZIP do oddania musi wykluczać lokalne sekrety niezależnie od .gitignore.

## Przed udostępnieniem online

- Zaimplementować zatwierdzone pseudonimowe konta z kluczem dostępu, sesjami i izolacją wszystkich danych oraz SSE; patrz docs/authentication.md.
- HTTPS; PostgreSQL dostępny tylko wewnętrznie. Konto bazy bez uprawnień administratora.
- Parametryzowane SQL; walidacja po stronie serwera; sensowne limity żądań i wielkości danych.
- Ochrona sesji i CSRF, jeśli wybrana zostanie autoryzacja oparta na ciasteczkach. Nie przechowywać haseł w postaci jawnej.
- SSE musi stosować te same zasady dostępu co REST, zamykać zasoby po rozłączeniu i poprawnie wznawiać/subskrybować stan po utracie połączenia.
- Aktualizacje systemu i zależności. Dla MVP dane są testowe i odtwarzalne; korzystamy z istniejącego backupu VPS OVH. Bez stałych eksportów bazy, dodatkowej usługi backupu i monitoringu.

## Testy i incydenty

Testy używają wyłącznie osobnej, jednorazowej bazy. Nie stosujemy rzeczywistych sekretów ani prywatnych danych w testach i przykładowych rekordach.

W przypadku ujawnienia sekretu należy go unieważnić lub zmienić. Usunięcie pliku w nowym commicie nie usuwa go z historii. Nie publikuj wartości sekretu w issue ani opisie błędu. Projekt nie ma jeszcze skonfigurowanego prywatnego kanału zgłoszeń; taki kanał należy ustalić przed publicznym uruchomieniem.

## Stan etapu kont i nawyków

Zaimplementowano losowe klucze, hashe w bazie, ograniczone sesje, CSRF/Origin, izolację kont dla nawyków i eksportu oraz ponowne okazanie klucza przy rotacji/usuwaniu. Szczegóły i ograniczenia globalnego limitera: docs/authentication.md. Ciasteczka wymagają HTTPS. Ochrony przyszłych treningów i SSE nie uznajemy za gotowe przed ich implementacją i testami. Publiczny interfejs jest wersją rozwojową z danymi testowymi.
