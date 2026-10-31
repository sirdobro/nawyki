# Zabezpieczenia VPS i dalsza konfiguracja

## Rejestr zmian 2026-10-31

Środowisko: zakupiony OVHcloud VPS-1, Ubuntu 26.04 LTS, x86_64, około 4 GB RAM i 40 GB dysku. Użytkownik administracyjny ubuntu. Dostęp kluczem został sprawdzony przed zmianami i ponownie po restarcie. Adres IP i prywatne dane dostępowe nie są potrzebne w publicznej dokumentacji repozytorium.

Wykonano:

1. Audyt efektywnych ustawień SSH, uprawnień plików, usług, portów, zapory i aktualizacji. Początkowo UFW było nieaktywne, logowanie hasłem do SSH dozwolone; publicznie nasłuchiwało tylko SSH.
2. Potwierdzono właściciela ubuntu i uprawnienia: katalog domowy 750, .ssh 700, authorized_keys 600. Nie nadpisano istniejących kluczy.
3. Odświeżono indeksy i zainstalowano 175 aktualizacji, następnie pozostałe aktualizacje jądra wymagające nowych pakietów. Instalacja bez zgody na usuwanie pakietów, z zachowaniem istniejących plików konfiguracyjnych.
4. Przed zmianą dostępu wykonano kopie konfiguracji SSH i UFW do chronionego katalogu /root/nawyki-hardening-20261031T093224Z. Uzbrojono automatyczne wycofanie zmian po 10 minutach.
5. Dodano /etc/ssh/sshd_config.d/00-nawyki-hardening.conf: PubkeyAuthentication yes, AuthenticationMethods publickey, PasswordAuthentication no, KbdInteractiveAuthentication no, PermitRootLogin no, PermitEmptyPasswords no, X11Forwarding no, AllowAgentForwarding no, MaxAuthTries 3, LoginGraceTime 30. Sprawdzono sshd -t i efektywne ustawienia przed reloadem.
6. Dodano /etc/cloud/cloud.cfg.d/99-nawyki-ssh.cfg z ssh_pwauth: false, aby cloud-init nie przywracał logowania hasłem.
7. Włączono UFW: domyślnie blokowanie ruchu przychodzącego i zezwolenie na wychodzący, limit połączeń TCP 22, zezwolenie na TCP 80 i 443, zarówno dla IPv4 jak i IPv6, logowanie low. Brak reguł otwierających PostgreSQL 5432 i wewnętrzne 8080.
8. Zachowano domyślne źródła automatycznych aktualizacji Ubuntu. Jawnie ustawiono codzienne indeksy i unattended upgrades oraz Automatic-Reboot false w /etc/apt/apt.conf.d/52nawyki-security. Włączono timery apt-daily i apt-daily-upgrade.
9. Po sprawdzeniu nowego połączenia SSH kluczem i sudo anulowano timer rollback. Kopia i ręczny skrypt rollback pozostały wyłącznie na serwerze.
10. Wykonano restart nieużywanego jeszcze serwera. Po restarcie potwierdzono klucz, sudo, aktywne UFW, key-only SSH, brak root SSH i brak nieudanych usług. Działa jądro 7.0.0-42-generic; brak kolejnego wymaganego restartu.

Nie zainstalowano jeszcze Dockera, PostgreSQL, Caddy ani aplikacji. Nie zmieniono DNS, kont OVH/Cloudflare ani ustawień GitHub. Nie tworzono płatnych zasobów.

## Dostęp i utrzymanie

Na komputerze administratora używać dedykowanego klucza nawyki_admin_ed25519 z lokalnego .ssh, np. ssh -i <lokalna-sciezka-klucza> ubuntu@<IPv4-VPS>. Klucz prywatny nie jest na VPS ani w repozytorium; jest bez passphrase dla obecnej automatyzacji i wymaga ochrony konta oraz dysku komputera. Przyszłe CI otrzyma osobny klucz. W razie utraty klucza odzyskanie dostępu przez konsolę/rescue OVH, nie przez ponowne włączenie publicznego hasła bez potrzeby.

Nie ograniczono SSH do domowego IP: może się zmienić i docelowy runner CI wymaga osobnego dostępu. Ochronę stanowią key-only SSH i limit połączeń. Nie dodano Fail2ban. Hasło konta nie zostało zmienione ani zablokowane; wyłączono tylko uwierzytelnianie hasłem przez SSH. Uprawnienia sudo konta ubuntu zachowano.

UFW nie zastąpi poprawnej konfiguracji portów Dockera. Przy przyszłym wdrożeniu nie publikować portów bazy ani Springa na wszystkich interfejsach; zewnętrznie udostępniać tylko reverse proxy. Nie zakładać, że samo UFW blokuje port opublikowany przez Docker.

## Pozostała lista

| Czynność | Potrzebny udział użytkownika |
|---|---|
| Docker/Compose, katalogi wdrożenia i trwałe wolumeny | Nie, dostęp SSH jest gotowy; sprawdzić wsparcie Ubuntu 26.04 przed instalacją. |
| Szkielet Java/React i migracje PostgreSQL | Nie. |
| PostgreSQL i losowe hasło zapisane wyłącznie na serwerze | Nie. |
| Konto/klucz wdrożeniowy i ograniczony mechanizm publikacji | Nie na VPS; uprawnienia repozytorium do Actions mogą wymagać zalogowania. |
| GitHub Actions i bezpieczne sekrety wdrożeniowe | Dostęp konta/repozytorium; nie przesyłać kluczy w czacie. |
| DNS A nawyki.intmain.cc do istniejącego IPv4 | Dostęp do Cloudflare lub wykonanie wskazanej zmiany przez użytkownika. |
| Caddy, publiczny certyfikat i HTTPS | Nie po działającym DNS; sprawdzić CAA i ustawienia proxy. |
| Cloudflare proxy, Full (strict), wyjątki cache/SSE | Dostęp do Cloudflare; nie zmieniać globalnie ustawień istniejącej strony bez sprawdzenia. |
| Backup | Dla MVP pozostaje istniejący backup OVH; bez stałych eksportów i dodatkowej infrastruktury. |
| Monitoring i powiadomienia | Poza zakresem MVP; pozostają podstawowe logi i healthcheck. |

## Źródła zasad

- https://ubuntu.com/server/docs/openssh-server/
- https://ubuntu.com/server/docs/how-to/software/automatic-updates/

Rejestr dokumentuje wykonaną bazową konfigurację, nie gwarancję całkowitego bezpieczeństwa ani zakończenie zabezpieczeń aplikacji.
