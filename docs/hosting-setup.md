# Konfiguracja hostingu nawyki.intmain.cc

## Stan i wybór

Wybrano OVHcloud VPS-1: Ubuntu 26.04 LTS, x86_64, około 4 GB RAM i 40 GB dysku. Konto administracyjne ubuntu; dostęp kluczem SSH. Docelowy adres aplikacji: nawyki.intmain.cc. Cloudflare zarządza DNS, a cała aplikacja i PostgreSQL będą na jednym VPS.

Bieżący rejestr zabezpieczeń: vps-security.md. Zbudowany React będzie serwowany przez Spring Boot. Docker, Compose, PostgreSQL i bootstrap Caddy zostały zainstalowane 2026-10-31. DNS i publiczny HTTPS zweryfikowano 2026-10-31; proxy Cloudflare jest włączone i zweryfikowane, pierwszy szkielet Java/React został wdrożony jako df4b2f2 (kontynuacja poniżej). Szczegóły i weryfikacja w runtime-installation.md. Nie zamawiamy kolejnego serwera.

Nie dodajemy prywatnego klucza administratora do GitHub Actions. CI otrzyma osobną tożsamość wdrożeniową. Hasło konta ubuntu pozostaje przydatne do konsoli/sudo w razie potrzeby; logowanie SSH hasłem jest wyłączone po sprawdzeniu dostępu kluczem.

## Cloudflare

1. W aktywnej strefie intmain.cc otworzyć DNS / Records / Add record.
2. Dodać A o nazwie nawyki, wskazujący publiczny IPv4 VPS. TTL Auto. Początkowo DNS only (szara chmurka), do uruchomienia i sprawdzenia certyfikatu HTTPS na VPS. Nie zmieniać rekordów głównej strony ani istniejącego www.
3. Nie dodawać AAAA bez działającego IPv6. Jeśli dokładnie nawyki ma już kolidujący rekord, ustalić jego rolę przed zmianą.
4. Po uruchomieniu Caddy i poprawnego publicznego certyfikatu dla nawyki.intmain.cc włączyć proxy (pomarańczowa chmurka).
5. Użyć Full (strict) dla tego hosta. Nie zmieniać globalnie trybu strefy bez sprawdzenia, czy obecna strona jest kompatybilna. Jeśli globalny tryb jest inny, sprawdzić możliwość reguły konfiguracji obejmującej tylko ten hostname; do tego czasu DNS only z poprawnym HTTPS jest działającym wariantem.
6. Nie ustawiać Cache Everything dla prywatnego API ani strumieni. Jeśli istnieją szerokie reguły cache, dodać wyjątek /api/* dla tego hosta. Aplikacja ustawia no-store dla uwierzytelniania i prywatnych odpowiedzi, proxy nie buforuje SSE, a strumienie wysyłają heartbeat.

## GitHub i dostęp

Przygotujemy workflow, konfigurację Compose i sekretne ustawienia dopiero z działającą aplikacją. Osobny klucz wdrożeniowy, znany/zweryfikowany klucz hosta SSH i sekrety w GitHub Actions. Nie wyłączać sprawdzania klucza hosta. Poświadczenia bazy pozostają na VPS; nie w obrazie Reacta i nie w Git.

Użytkownik może przekazać publiczny IP, wybraną lokalizację/plan i informację, że klucz publiczny został dodany. Prywatne klucze, hasła i tokeny nie należą do czatu. Zakup i zmiany kont muszą być jawnie autoryzowane.

## Źródła

- https://docs.hetzner.com/cloud/servers/getting-started/creating-a-server/
- https://www.hetzner.com/cloud/cost-optimized/
- https://developers.cloudflare.com/dns/manage-dns-records/how-to/create-dns-records/
- https://developers.cloudflare.com/ssl/origin-configuration/ssl-modes/full-strict/

## Weryfikacja DNS i HTTPS 2026-10-31

Użytkownik dodał rekord A nawyki do publicznego IPv4 VPS z TTL Auto i DNS only. Agent potwierdził odpowiedź publicznego resolvera 1.1.1.1 oraz brak rekordu AAAA dla tego hosta. Zweryfikowano i aktywowano przygotowaną konfigurację Caddy dla nawyki.intmain.cc, zachowując poprzednią konfigurację jako Caddyfile.before-https na serwerze.

Publiczne żądanie HTTPS bez wyłączania walidacji certyfikatu zwróciło 200 i neutralną odpowiedź o niewdrożonej aplikacji. HTTP zwraca 308 do HTTPS. Caddy obsługuje automatyczne odnawianie certyfikatu; dane certyfikatu pozostają w trwałym wolumenie. Nie zmieniono głównej strony intmain.cc ani globalnego trybu SSL Cloudflare.

Przed włączeniem pomarańczowej chmurki użytkownik powinien sprawdzić bieżący tryb SSL/TLS strefy. Full (strict) jest odpowiedni dla przygotowanego originu; jeśli strefa używa innego trybu, najpierw sprawdzić wpływ na pozostałe hosty lub zastosować regułę dla tego hosta. DNS only z HTTPS już działa i może pozostać aktywne do ustalenia tych ustawień.

Użytkownik potwierdził ustawienie Full (strict) w Cloudflare. Kolejny niezależny test HTTPS zwrócił 200; resolver 1.1.1.1 nadal zwracał IP VPS, a odpowiedź Server: Caddy nie zawierała nagłówków proxy Cloudflare. Włączenie proxy nie zostało potwierdzone; nie oznaczamy go jako wykonane.

## Proxy potwierdzone 2026-10-31

Użytkownik włączył Proxied dla nawyki.intmain.cc. Agent potwierdził adresy Cloudflare w publicznym DNS oraz odpowiedź HTTPS 200 z Server: cloudflare, CF-RAY, cf-cache-status: DYNAMIC i Cache-Control: no-store. Nadal wyświetlany jest placeholder; kod aplikacji nie został wdrożony. Wcześniejsze wpisy o DNS only dokumentują poprzedni etap.

## Pierwszy szkielet aplikacji

Wdrożono df4b2f2 przez pobieranie publicznego wydania GitHub po testach. Aktywny Caddyfile odpowiada teraz Caddyfile.app, a nie placeholderowi. Repozytorium nie przechowuje żadnych sekretów Actions; VPS używa publicznego API wydań. Publiczne API i strona React zweryfikowane. Pełna funkcjonalność kont i dziennika pozostaje kolejnym etapem.
