# Registration/Login Flow con JWT - Airport App

## Stato della verifica — 14 ottobre 2026

Analisi statica dei sorgenti e della configurazione. Nessun test di esecuzione o accesso al database effettuato durante questa revisione.

- **FATTO**: presente nel codice; non certifica il funzionamento in esecuzione.
- **PARZIALE**: presente con aspetti da completare.
- **NON IMPLEMENTATO**: assente nei sorgenti esaminati.
- **DA VERIFICARE**: comportamento da confermare con test.

## Obiettivo

Questo documento descrive lo stato attuale del backend per autenticazione, registrazione tecnica, login con JWT e gestione base dei ruoli `ADMIN` e `OPERATORI`.

La priorita attuale e avere un flusso minimo funzionante:

1. registrazione tecnica di un utente;
2. login con email e password;
3. generazione di un access token JWT;
4. uso del token per chiamare API protette;
5. distinzione tra area ADMIN e area OPERATORI.

## Tecnologie usate nel backend

- Java 17
- Spring Boot 4.1.1 (versione dichiarata nel pom.xml)
- Spring Web MVC
- Spring Security
- Spring OAuth2 Resource Server
- Spring Data JPA
- PostgreSQL
- Liquibase
- Maven

Nota: nel documento di analisi iniziale era citato Flyway, ma il progetto attuale usa Liquibase. Per coerenza conviene continuare con Liquibase.

## File principali gia presenti

### Configurazione

- `backend/pom.xml`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/src/main/resources/db/changelog/001-create-app-users.yaml`

### Utenti

- `backend/src/main/java/it/raffo/backend/users/AppUser.java`
- `backend/src/main/java/it/raffo/backend/users/AppUserRepository.java`
- `backend/src/main/java/it/raffo/backend/users/Role.java`

### Autenticazione

- `backend/src/main/java/it/raffo/backend/auth/AuthController.java`
- `backend/src/main/java/it/raffo/backend/auth/AuthService.java`
- `backend/src/main/java/it/raffo/backend/auth/JwtService.java`
- `backend/src/main/java/it/raffo/backend/auth/RegisterRequest.java`
- `backend/src/main/java/it/raffo/backend/auth/LoginRequest.java`
- `backend/src/main/java/it/raffo/backend/auth/AuthResponse.java`
- `backend/src/main/java/it/raffo/backend/auth/MeResponse.java`

### Sicurezza

- `backend/src/main/java/it/raffo/backend/security/SecurityConfig.java`
- `backend/src/main/java/it/raffo/backend/security/AirportJwtAuthenticationConverter.java`
- `backend/src/main/java/it/raffo/backend/security/CustomAuthenticationEntryPoint.java`
- `backend/src/main/java/it/raffo/backend/security/CustomAccessDeniedHandler.java`

### Error handling

- `backend/src/main/java/it/raffo/backend/common/GlobalExceptionHandler.java`
- `backend/src/main/java/it/raffo/backend/common/ApiErrorResponse.java`
- `backend/src/main/java/it/raffo/backend/common/DuplicateResourceException.java`
- `backend/src/main/java/it/raffo/backend/common/InvalidCredentialsException.java`

### Endpoint di prova per ruoli

- `backend/src/main/java/it/raffo/backend/admin/AdminPingController.java`
- `backend/src/main/java/it/raffo/backend/operatori/OperatoriPingController.java`

## Modello utente attuale

La tabella `app_users` contiene questi campi principali:

- `id`: UUID dell'utente;
- `first_name`: nome;
- `last_name`: cognome;
- `email`: email normalizzata e univoca;
- `password_hash`: password salvata come hash, non in chiaro;
- `role`: ruolo applicativo, `ADMIN` oppure `OPERATORI`;
- `active`: indica se l'account e attivo;
- `auth_version`: contatore usato per revocare token precedenti al logout;
- `created_at`: data di creazione.

## Registration flow

Endpoint:

```http
POST /api/v1/auth/register
```

Per ora e pubblico per facilitare lo sviluppo. Nel prodotto finale non dovrebbe essere una registrazione libera: dovra diventare una procedura tecnica riservata o essere sostituita da gestione utenti/admin.

### Passaggi logici

1. Il client invia nome, cognome, email, password e ruolo.
2. `AuthController` riceve la richiesta e valida il DTO `RegisterRequest` con `@Valid`.
3. `AuthService.register()` normalizza l'email:
   - trim degli spazi esterni;
   - conversione in minuscolo.
4. Il service controlla se l'email esiste gia.
5. La password viene trasformata in hash tramite `PasswordEncoder`.
6. Viene creata una entity `AppUser`.
7. L'utente viene salvato nel database tramite `AppUserRepository`.
8. Il backend genera un JWT.
9. La risposta contiene dati utente essenziali e token.

### Esempio richiesta

```json
{
  "firstName": "Mario",
  "lastName": "Admin",
  "email": "admin@test.it",
  "password": "password123",
  "role": "ADMIN"
}
```

### Risposta attesa

Status:

```http
201 Created
```

Body indicativo:

```json
{
  "userId": "...",
  "firstName": "Mario",
  "lastName": "Admin",
  "email": "admin@test.it",
  "role": "ADMIN",
  "accessToken": "...",
  "expiresInSeconds": 900
}
```

## Login flow

Endpoint:

```http
POST /api/v1/auth/login
```

### Passaggi logici

1. Il client invia email e password.
2. `AuthController` riceve la richiesta e valida `LoginRequest`.
3. `AuthService.login()` normalizza l'email.
4. Il service cerca l'utente nel database.
5. Se l'utente non esiste, ritorna errore generico.
6. Se l'utente non e attivo, ritorna errore generico.
7. Se la password non corrisponde all'hash, ritorna errore generico.
8. Se le credenziali sono valide, genera un JWT.
9. Il client usera il token nelle chiamate successive.

### Perche l'errore credenziali e generico

Il backend non deve dire se e sbagliata l'email, la password o se l'account e disattivato. Questo evita di dare informazioni utili a chi prova a enumerare account validi.

## JWT attuale

Il token viene generato da `JwtService`.

Durata attuale:

```text
900 secondi = 15 minuti
```

Claim principali:

- `iss`: issuer, attualmente `airport-app`;
- `iat`: data emissione;
- `exp`: scadenza;
- `sub`: UUID utente;
- `email`: email utente;
- `role`: ruolo applicativo;
- `authVersion`: versione di autenticazione dell'utente.

Al momento il token e firmato con algoritmo simmetrico `HS256` usando `app.jwt.secret`.

La password del database e il segreto JWT sono forniti tramite le variabili d’ambiente AIRPORT_APP_DB_PASSWORD e AIRPORT_APP_JWT_SECRET, senza valori predefiniti in application.properties. In sviluppo locale sono impostati nella configurazione di avvio IntelliJ, non condivisa nel repository.

## Uso del token

Dopo login o register, il client riceve `accessToken`.

Per chiamare API protette deve inviarlo nell'header HTTP:

```http
Authorization: Bearer TOKEN_QUI
```

Esempio:

```powershell
curl.exe -i "http://localhost:8080/api/v1/auth/me" -H "Authorization: Bearer $adminToken"
```

## Endpoint attuali

### Pubblici

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Autenticati

```http
GET /api/v1/auth/me
POST /api/v1/auth/logout
```

### Solo ADMIN

```http
GET /api/v1/admin/ping
```

### Solo OPERATORI

```http
GET /api/v1/operatori/ping
```

## Gestione ruoli

I ruoli applicativi sono definiti nell'enum:

```java
Role.ADMIN
Role.OPERATORI
```

Nel JWT viene salvato il claim:

```json
"role": "ADMIN"
```

oppure:

```json
"role": "OPERATORI"
```

In `SecurityConfig` è collegato `AirportJwtAuthenticationConverter`. Per ogni richiesta autenticata legge dal database lo stato utente identificato dal subject del token, verifica account attivo e coincidenza di `authVersion`. Il ruolo corrente nel database, anziché il claim `role`, determina la authority Spring Security:

```text
ADMIN -> ROLE_ADMIN
OPERATORI -> ROLE_OPERATORI
```

Questo permette di usare regole come:

```java
.hasRole("ADMIN")
.hasRole("OPERATORI")
```

## Regole di sicurezza attuali

In sintesi:

- register e login sono pubblici;
- `/api/v1/auth/me` e `POST /api/v1/auth/logout` richiedono token valido, account attivo e versione coincidente;
- `/api/v1/admin/**` richiede ruolo ADMIN;
- `/api/v1/operatori/**` richiede ruolo OPERATORI;
- tutte le altre API richiedono autenticazione.

Le sessioni HTTP sono disabilitate:

```text
SessionCreationPolicy.STATELESS
```

Questo significa che il backend non salva una sessione HTTP tradizionale: ogni richiesta protetta deve portare il token. Account, ruolo e revoca sono comunque verificati leggendo il database per ogni richiesta autenticata.

## Logout e revoca — FATTO

`POST /api/v1/auth/logout` richiede un bearer token valido e restituisce `204 No Content`. Il controller estrae il subject e delega ad `AuthService.logout()`, che carica l’utente e incrementa `authVersion` in una transazione. JPA persiste la modifica al commit.

Alla richiesta successiva il converter rifiuta i token con versione precedente: vengono revocati anche quelli delle altre sessioni dello stesso utente. Un nuovo login emette un token con la versione aggiornata.

**PARZIALE:** `authVersion++` non è protetto da locking o versione ottimistica; la transazione da sola non garantisce un incremento atomico con richieste concorrenti.

## Test manuali con PowerShell

**DA VERIFICARE:** i comandi seguenti indicano scenari e risultati attesi, non test superati.

### Registrare ADMIN

```powershell
curl.exe -i -X POST "http://localhost:8080/api/v1/auth/register" -H "Content-Type: application/json" -d '{"firstName":"Mario","lastName":"Admin","email":"admin@test.it","password":"password123","role":"ADMIN"}'
```

### Registrare OPERATORI

```powershell
curl.exe -i -X POST "http://localhost:8080/api/v1/auth/register" -H "Content-Type: application/json" -d '{"firstName":"Luca","lastName":"Operatore","email":"operatore@test.it","password":"password123","role":"OPERATORI"}'
```

### Login ADMIN

```powershell
curl.exe -i -X POST "http://localhost:8080/api/v1/auth/login" -H "Content-Type: application/json" -d '{"email":"admin@test.it","password":"password123"}'
```

Salvare il token:

```powershell
$adminToken = "INCOLLA_TOKEN_ADMIN"
```

### Verificare profilo ADMIN

```powershell
curl.exe -i "http://localhost:8080/api/v1/auth/me" -H "Authorization: Bearer $adminToken"
```

### Verificare accesso ADMIN

```powershell
curl.exe -i "http://localhost:8080/api/v1/admin/ping" -H "Authorization: Bearer $adminToken"
```

Atteso:

```http
200 OK
```

### Verificare blocco area OPERATORI con token ADMIN

```powershell
curl.exe -i "http://localhost:8080/api/v1/operatori/ping" -H "Authorization: Bearer $adminToken"
```

Atteso:

```http
403 Forbidden
```

### Login OPERATORI

```powershell
curl.exe -i -X POST "http://localhost:8080/api/v1/auth/login" -H "Content-Type: application/json" -d '{"email":"operatore@test.it","password":"password123"}'
```

Salvare il token:

```powershell
$operatoriToken = "INCOLLA_TOKEN_OPERATORI"
```

### Verificare accesso OPERATORI

```powershell
curl.exe -i "http://localhost:8080/api/v1/operatori/ping" -H "Authorization: Bearer $operatoriToken"
```

Atteso:

```http
200 OK
```

### Verificare blocco area ADMIN con token OPERATORI

```powershell
curl.exe -i "http://localhost:8080/api/v1/admin/ping" -H "Authorization: Bearer $operatoriToken"
```

Atteso:

```http
403 Forbidden
```

### Verificare API protetta senza token

```powershell
curl.exe -i "http://localhost:8080/api/v1/admin/ping"
```

Atteso:

```http
401 Unauthorized
```

## Errore comune incontrato

In PowerShell, se il JSON viene scritto con escape sbagliati, il backend puo ricevere un body non valido.

Errore visto nei log:

```text
JSON parse error: Unexpected character ('\')
```

Significa che al backend sta arrivando qualcosa tipo:

```json
{\"firstName\":\"Mario\"}
```

Invece deve arrivare:

```json
{"firstName":"Mario"}
```

In PowerShell conviene usare apici singoli attorno al JSON:

```powershell
-d '{"firstName":"Mario","lastName":"Admin"}'
```

## Cose già fatte

- **FATTO** — Backend Spring Boot, Maven e Java 17.
- **FATTO** — PostgreSQL e Liquibase configurati; connessione effettiva e migrazioni applicate **DA VERIFICARE**.
- **FATTO** — Migrazione `app_users`, entity `AppUser`, enum `ADMIN`/`OPERATORI` e repository utenti.
- **FATTO** — DTO register/login/me e risposta autenticazione.
- **FATTO** — Registrazione con normalizzazione email, controllo duplicati e vincolo univoco nel database.
- **FATTO** — Hash password con `BCryptPasswordEncoder`.
- **FATTO** — Login con verifica password e account attivo; errore generico per credenziali non valide.
- **FATTO** — Emissione JWT HS256 dopo register/login, durata 900 secondi.
- **FATTO** — Resource Server per validazione JWT; controlli espliciti issuer/audience ancora assenti.
- **FATTO** — Controllo account attivo e `authVersion` a ogni richiesta autenticata; authority dal ruolo corrente nel database.
- **FATTO** — `/me`, ping ADMIN/OPERATORI e autorizzazione separata delle due aree.
- **FATTO** — Logout `204` e revoca tramite incremento `authVersion`; concorrenza **PARZIALE**.
- **FATTO** — Errori applicativi per duplicati, credenziali errate e validazione DTO.
- **PARZIALE** — Gestori JSON 401/403 collegati a `exceptionHandling`; copertura degli errori del Resource Server **DA VERIFICARE**.

## Cose da fare a breve

### Backend auth

- **NON IMPLEMENTATO** — `POST /api/v1/auth/change-password`.
- **NON IMPLEMENTATO** — Predisposizione riservata degli account: `/register` è pubblico e accetta anche `ADMIN`. Il pannello web utenti resta rinviato.
- **PARZIALE** — Garantire incremento di `authVersion` sicuro con richieste concorrenti.
- **PARZIALE** — Gestire subject non UUID e tipo inatteso di `authVersion` come token non valido, evitando errori interni.
- **PARZIALE** — Garantire `409` anche per violazioni del vincolo univoco causate da registrazioni concorrenti.
- **DA VERIFICARE** — Risposte JSON coerenti per tutti gli errori di sicurezza, compresi quelli del Resource Server.
- **DA VERIFICARE** — `Cache-Control: no-store` sulle risposte di autenticazione: nessuna configurazione dedicata trovata; controllare gli header effettivi prima di aggiungerla.

### Differenze rispetto all’analisi iniziale

Queste differenze riguardano i requisiti dell’analisi funzionale e tecnica, oltre al flusso minimo già implementato.

- **FATTO** — Liquibase anziché Flyway: continuare con Liquibase e riallineare la documentazione in una revisione successiva.
- **NON IMPLEMENTATO** — RS256 con chiave privata esterna; attualmente firma HS256.
- **NON IMPLEMENTATO** — Claim `aud`, `jti` e controlli espliciti di issuer/audience; il token attuale contiene anche email.
- **PARZIALE** — BCrypt anziché Argon2id proposto; minimo password 8 caratteri anziché 15. Allineare la policy e verificare password lunghe senza troncamento.
- **NON IMPLEMENTATO** — Password temporanee, scadenza, cambio obbligatorio e blocco delle API business; campi `mustChangePassword`, `temporaryPasswordExpiresAt`, `updatedAt` assenti.
- **NON IMPLEMENTATO** — Limite ai tentativi di login e configurazione CORS dedicata.
- **NON IMPLEMENTATO** — Negazione dei percorsi non dichiarati: attualmente richiedono soltanto autenticazione.
- **DA VERIFICARE** — Configurazione di produzione per HTTPS e segreti esterni; ambiente distribuito non esaminato.

### Backend business

- **NON IMPLEMENTATO** — Modulo `aircraftmodels`, tabella `aircraft_models`, GET/POST `/api/v1/aircraft-models` solo ADMIN.
- **NON IMPLEMENTATO** — Modulo `routes`, tabella `routes`, GET/POST `/api/v1/routes` solo ADMIN.

### Frontend minimo

- **FATTO** — Progetto Angular predisposto, ancora con template iniziale e route vuote.
- **NON IMPLEMENTATO** — Pagina login, chiamate HTTP, token in memoria e invio bearer alle API del gestionale.
- **NON IMPLEMENTATO** — Lettura ruolo e navigazione a `/admin` o `/operatori`.
- **NON IMPLEMENTATO** — Landing ADMIN con menu e segnaposto immagine; benvenuto OPERATORI senza menu amministrativo.
- **NON IMPLEMENTATO** — Protezioni route, scadenza, logout e cambio password frontend.
- **NON IMPLEMENTATO** — Liste e form di modelli di aereo e tratte.

La registrazione temporanea citata precedentemente non è una funzionalità del prodotto previsto: l’analisi richiede una procedura tecnica riservata. La sicurezza frontend non sostituisce quella backend.

### Test — DA VERIFICARE

Nel backend è presente soltanto `contextLoads()`, senza test specifici auth. Questa revisione non esegue test e non certifica quelli eventualmente svolti in precedenza.

- Email duplicata: `409`, anche con registrazioni concorrenti.
- Password errata, email inesistente o account disattivato: `401` generico, nessun token.
- `/me` senza token: `401`.
- Ping del proprio ruolo: `200`; accesso incrociato ADMIN/OPERATORI: `403`.
- Logout valido: `204`; riuso del vecchio token su `/me` e ping: `401`.
- Anche altri token precedenti dello stesso utente devono essere rifiutati dopo logout; nuovo login e nuovo token devono consentire accesso.
- Account disattivato dopo emissione token: richieste protette rifiutate.
- Token alterato/scaduto: `401`; issuer/audience errati: scenario da completare dopo i relativi controlli.
- Subject non UUID e `authVersion` assente/di tipo inatteso: verificare assenza di errori interni.
- Verificare JSON dei 401/403 e header di cache delle risposte auth.

## Ordine di ripresa

1. Verificare login, ruoli, logout, riuso dei token e risposte di sicurezza.
2. Consolidare revoca concorrente, errori e validazione JWT.
3. Implementare cambio password e predisposizione riservata degli account, riallineando le protezioni all’analisi.
4. Sviluppare login e landing Angular.
5. Sviluppare database, backend e frontend di modelli di aereo e tratte.

Il codice sarà scritto esclusivamente dallo sviluppatore junior, con guida passo per passo su file, responsabilità e motivazioni.

## Nota didattica importante

Il flow attuale e volutamente semplice. Serve a capire bene il ciclo:

```text
credenziali -> verifica -> JWT -> Authorization header -> verifica JWT -> lettura utente dal DB -> controllo active/authVersion -> ruolo corrente -> autorizzazione -> controller protetto
```

Quando questo ciclo e chiaro e verificato, aggiungere le funzioni business diventa molto piu semplice, perche ogni nuova API dovra solo dichiarare quale ruolo puo usarla e applicare le proprie regole di dominio.
