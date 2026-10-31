# Konta bez e-maila i hasła

## Zatwierdzony zakres

Wiele pseudonimowych kont. Bez imienia, e-maila, hasła użytkownika, zewnętrznego dostawcy tożsamości i odzyskiwania przez e-mail. Konto identyfikuje losowy wewnętrzny identyfikator. Nie deklarujemy pełnej anonimowości: treść dziennika i metadane infrastruktury mogą identyfikować człowieka.

## Rejestracja i logowanie

Użytkownik tworzy prywatny dziennik i otrzymuje losowy klucz dostępu generowany na brzegu aplikacji przez SecureRandom (docelowo 256 bitów losowości). Klucz jest poświadczeniem, nie kopią danych i nie seedem odtwarzającym bazę. Serwer przechowuje wyłącznie jednokierunkowy weryfikator/hash, nigdy surowy klucz. Dla takiego losowego sekretu odpowiedni jest SHA-256; nie stosujemy tego jako schematu przechowywania haseł wybieranych przez człowieka.

Podstawą jest wklejenie klucza do formularza. Opcjonalna prywatna zakładka ma postać https://nawyki.intmain.cc/#access=SECRET. React czyta fragment, usuwa go z adresu przez history.replaceState i przekazuje sekret w treści POST po HTTPS, aby otrzymać niezależną sesję. Fragment nie trafia w początkowym żądaniu HTTP, ale jest dostępny skryptom przeglądarki, rozszerzeniom i synchronizacji zakładek. Nie przedstawiamy tej opcji jako odpornej na phishing czy przypadkowe udostępnienie.

Nie używamy sekretu w ścieżce, query string, analytics ani logach. Strona wejścia nie ładuje skryptów zewnętrznych/analytics. Klucz nie jest przechowywany w localStorage ani używany jako identyfikator sesji przy każdym żądaniu. Odpowiedzi uwierzytelniania i dane prywatne: Cache-Control: no-store.

Otwarcie prywatnej zakładki przy aktywnej sesji innego konta nie może po cichu przepiąć zapisywanych danych. Potrzebne jest jawne potwierdzenie zmiany konta; login wymaga kontroli pochodzenia żądania i ochrony przed login CSRF.

## Sesje i operacje konta

- Serwerowa sesja z niezależnym losowym tokenem; cookie __Host- z Secure, HttpOnly, Path=/ oraz jawnie dobranym SameSite. Kontrola CSRF dla operacji zmieniających stan.
- Jawne terminy ważności i unieważnianie po stronie serwera, nie wyłącznie w przeglądarce. Dokładne czasy pozostają do dobrania.
- Wylogowanie, unieważnienie wszystkich sesji, zastąpienie klucza (stary przestaje działać; istniejące sesje są unieważniane).
- Eksport własnych danych i usunięcie własnego konta. Operacje destrukcyjne wymagają potwierdzenia i ponownego okazania klucza.
- Utrata klucza i wszystkich sesji oznacza brak możliwości odzyskania konta. Wyjaśniamy to przy tworzeniu; zalecamy bezpieczne zapisanie klucza.
- Limity prób logowania i tworzenia kont; brak ujawniania istnienia konkretnego konta przez szczegóły błędów.
- Passkeys są możliwym dalszym rozwojem, nie zakresem MVP.

## Izolacja i testy

Każdy nawyk, wykonanie i trening należy do konta. Repozytoria ograniczają operacje przez identyfikator konta z sesji, nigdy na podstawie zaufania do accountId z klienta. Weryfikujemy także zasoby podrzędne i zbiorcze raporty. Oba SSE przesyłają jedynie zdarzenia aktualnego konta; utrata ważności sesji zamyka strumień.

Testy obejmą dwa konta, próby odczytu/zmiany cudzych identyfikatorów, izolację SSE, rotację klucza, wygasanie/wylogowanie sesji, CSRF i brak surowych kluczy w bazie. Migracje muszą uwzględnić właściciela danych od pierwszej wersji.
