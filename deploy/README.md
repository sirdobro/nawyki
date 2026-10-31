# Środowisko serwerowe

Pliki tworzą fundament hostingu, nie działającą aplikację. compose.yaml uruchamia prywatny PostgreSQL 18 oraz Caddy. Tymczasowa odpowiedź HTTP nie przyjmuje żadnych danych i wyraźnie wskazuje brak aplikacji.

Na VPS pliki znajdują się w /opt/nawyki. Od 2026-10-31 aktywny Caddyfile odpowiada Caddyfile.https-placeholder: DNS i publiczny HTTPS zostały sprawdzone. Caddyfile.bootstrap zachowano jako wariant początkowy. .env zawiera przypięte digesty obrazów. Sekrety powstają wyłącznie na serwerze, w plikach secrets/db_admin_password i secrets/db_app_password. Katalog secrets ma właściciela root i uprawnienia 700, pliki 600. Plik db_app_password ma numeryczny UID/GID użytkownika postgres z użytego obrazu (obecnie 999:999), aby inicjalizator działający bez roota mógł go odczytać; plik administracyjny należy do roota. Przed nową inicjalizacją sprawdzić UID/GID w konkretnym obrazie. Nie kopiować sekretów do Git ani czatu. Compose secrets w tym trybie to pliki montowane do kontenera, nie szyfrowany magazyn sekretów.

Hasła administracyjne i aplikacyjne są różne. Konto nawyki może zarządzać swoją bazą i schematem, lecz nie jest superuserem PostgreSQL i nie tworzy innych baz ani ról. PostgreSQL nie publikuje żadnego portu hosta; działa na wewnętrznej sieci Compose. Aplikacja dołączy do tej sieci w kolejnym etapie.

Administrator używa sudo docker compose z katalogu /opt/nawyki. Nie dodajemy kont do grupy docker, ponieważ daje ona dostęp porównywalny z rootem. Przyszły użytkownik deploy będzie miał oddzielnie zaprojektowany mechanizm wdrożeń.

## Utrzymanie

- sudo docker compose ps: stan usług; sudo docker compose logs --tail=50: logi bez wypisywania sekretów.
- Nie uruchamiać down -v; usuwa wolumeny, w tym dane bazy.
- Skrypt postgres-init.sh działa wyłącznie przy pierwszym tworzeniu wolumenu. Zmiana pliku hasła nie zmienia automatycznie hasła istniejącej roli.
- Major PostgreSQL nie zmieniać przez prostą podmianę tagu; wymaga świadomej migracji. Digesty aktualizować po przeglądzie i sprawdzeniu obrazów.
- Logi kontenerów są ograniczone do 3 plików po 10 MB na usługę.
- Dla MVP korzystamy z istniejących kopii OVH; nie instalujemy harmonogramu eksportów, dodatkowej usługi backupu ani monitoringu.

## Następne kroki

Po wskazaniu DNS przez użytkownika włączyć Caddyfile.https-placeholder, zweryfikować HTTPS, a następnie skonfigurować Cloudflare proxy/Full (strict) bez naruszania pozostałych stron domeny. Podczas działania aplikacji ustawienia cookie i dostępu wymagają HTTPS.

Gdy powstanie przetestowany obraz Java 21, rozszerzyć Compose o app i healthcheck. Zbudowany React będzie wewnątrz aplikacji Spring. Nie ma potrzeby instalować hostowego Node, Mavena ani JDK na produkcyjnym VPS tylko do uruchomienia obrazów.

## Wdrożenie aplikacji przez pobieranie wydań

Zatwierdzono pobieranie wydań przez VPS zamiast SSH z GitHub Actions. Serwer używa root-owned /usr/local/sbin/nawyki-pull-release i /usr/local/sbin/nawyki-deploy, usługi nawyki-update.service i timera nawyki-update.timer. Nie utworzono sekretu GitHub z kluczem SSH; konto deploy pozostaje zablokowane bez dostępu.

Actions po przejściu testów publikuje deploy-<pełny SHA> z nawyki-image.tar.gz i nawyki-image.sha256. Puller sprawdza znaną ścieżkę repozytorium, format SHA, rozmiar i sumę archiwum. Suma wykrywa uszkodzenie, nie jest niezależnym podpisem: uprawnienia do publikacji w repozytorium pozostają granicą zaufania.

compose.app.yaml dodaje aplikację jako UID/GID 10001 z read-only root, tmpfs /tmp, bez capabilities, bez portów hosta i z limitem 512 MB. Sekret spring.datasource.password pochodzi z osobnego pliku serwerowego należącego do 10001:10001. Caddy proxy wskazuje app:8080 i wyłącza buforowanie dla przyszłych SSE. Brak healthcheck powoduje wycofanie obrazu do poprzedniego wydania, jeśli istnieje. Nie cofamy automatycznie migracji bazy; nowe SQL muszą być kompatybilne z planem powrotu.

Polecenia administratora: sudo systemctl start nawyki-update.service (sprawdź teraz), sudo journalctl -u nawyki-update.service -n 50 --no-pager (wynik), sudo systemctl stop nawyki-update.timer (wstrzymaj automatyczne sprawdzanie). Z katalogu /opt/nawyki używać obu --env-file .env i --env-file release.env oraz -f compose.yaml -f compose.app.yaml do obsługi pełnej aplikacji.
