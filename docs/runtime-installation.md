# Instalacja środowiska VPS 2026-10-31

## Wykonane czynności

1. Sprawdzono zasoby, dostęp sudo, stan zapory i dostępność pakietów Ubuntu 26.04.1 LTS.
2. Zainstalowano docker.io 29.1.3 oraz docker-compose-v2 2.40.3 z podpisanych repozytoriów Ubuntu; Docker startuje automatycznie po restarcie systemu. Nie zainstalowano Docker Desktop ani hostowego Node/JDK/Mavena.
3. Utworzono /opt/nawyki z konfiguracją root-only, plikami Compose, inicjalizatorem roli PostgreSQL i konfiguracją Caddy. Utworzono również katalog transferowy /home/ubuntu/nawyki-runtime-stage zawierający wyłącznie jawne konfiguracje.
4. Pobrano oficjalne obrazy PostgreSQL 18.6 i Caddy 2.11.7; na serwerze przypięto digesty zgodne z deploy/.env.example.
5. Wygenerowano dwa niezależne losowe hasła wyłącznie na VPS. Nie wyświetlono ich i nie zapisano w repozytorium. Sekrety to pliki 600 w katalogu root:700. Dopasowano UID/GID pliku aplikacyjnego do postgres w kontenerze.
6. Zainicjalizowano bazę nawyki i rolę nawyki bez SUPERUSER, CREATEDB ani CREATEROLE. Rola ma uprawnienia do własnej bazy i schematu. Konto postgres pozostaje administracyjne.
7. Podłączono PostgreSQL do nazwanego wolumenu nawyki_postgres_data i prywatnej sieci nawyki_database (internal=true). Nie opublikowano portu 5432. Brak aplikacji oznacza brak portu 8080.
8. Uruchomiono Caddy z trwałymi wolumenami konfiguracji/certyfikatów. Na razie tylko neutralna odpowiedź HTTP bez logowania ani danych użytkowników. TCP 80/443 są przypisane proxy, ale certyfikat i obsługa TLS czekają na DNS.
9. Ograniczono logi każdej usługi do trzech plików po 10 MB. Ustawiono restart unless-stopped, limit pamięci PostgreSQL 768 MB i Caddy 128 MB oraz limity procesów.
10. Utworzono konto deploy bez hasła i bez przypisanego klucza, sudo czy grupy docker. Nie jest jeszcze gotowe do logowania lub CI; nie przyznano mu nieograniczonej administracji Dockerem.
11. Przygotowano alternatywny Caddyfile.https-placeholder dla nawyki.intmain.cc; nie aktywowano go przed DNS.

## Sprawdzenie

- Walidacja Compose oraz bootstrapowej konfiguracji Caddy: poprawna.
- Healthcheck PostgreSQL: poprawny, sprawdza gotowość i istnienie aplikacyjnej roli bez superusera.
- Uwierzytelniony odczyt/zapis przez TCP jako nawyki: poprawny.
- Testowy rekord pozostał po zastąpieniu kontenera bazy. Po sprawdzeniu usunięto testową tabelę; nie pozostawiono fikcyjnego schematu aplikacji.
- docker port dla PostgreSQL zwrócił pusty wynik, a prywatna sieć ma internal=true.
- Publiczny HTTP VPS zwrócił 200 i informację o niewdrożonej aplikacji; Cache-Control: no-store.
- UFW pozostało aktywne, brak nieudanych usług systemowych. Instalacja nie wymagała restartu VPS.

Pierwszy smoke test wykrył błąd uprawnień pliku sekretu odczytywanego przez nieuprzywilejowany inicjalizator. Poprawiono właściciela pliku i utworzono rolę, następnie przeprowadzono powyższe testy. Sam pg_isready nie wykrywał tego błędu, dlatego healthcheck rozszerzono o kontrolę roli.

## Granica tego etapu

Brak aplikacji Java/React, migracji domenowych, workflow GitHub Actions i klucza CI. Nie dodano monitoringu, harmonogramu eksportów ani zewnętrznej usługi backupowej. Uzgodniono użycie istniejącego backupu OVH i testowych danych MVP.

Do kontynuacji HTTPS potrzebny rekord A: nazwa nawyki, adres publiczny istniejącego VPS, DNS only i TTL Auto. Nie zmieniać głównej strony intmain.cc ani www; przed zastąpieniem istniejącego nawyki sprawdzić, co obsługuje. Następnie agent może aktywować publiczny certyfikat, sprawdzić HTTPS i przygotować dalszą konfigurację proxy Cloudflare.

Sekrety i klucze pozostają poza Git. Zmiany lokalnych plików wdrożenia i dokumentów nie oznaczają automatycznie commitu, push ani konfiguracji GitHub.
