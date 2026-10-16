# Registration/Login Flow con JWT - Airport App

## Stato della verifica — aggiornamento del 16 ottobre 2026

L’analisi iniziale del 14 ottobre era basata sui sorgenti. Il 16 ottobre sono stati completati i controlli del converter e sono stati eseguiti con JDK 17 i suoi 20 casi automatici: tutti superati, zero errori, zero fallimenti e zero casi saltati. È stato inoltre implementato l’incremento atomico di authVersion nel database. Lo sviluppatore ha confermato il successo dei tre test di integrazione PostgreSQL con Testcontainers e della prova Postman di revoca di due token. Durante questo aggiornamento l’assistente ha controllato i sorgenti senza rieseguire i test di integrazione; le diverse verifiche sono registrate separatamente.

- **FATTO**: implementazione presente nel codice; le verifiche svolte sono indicate esplicitamente nella sezione Test.
- **PARZIALE**: presente con aspetti da completare.
- **NON IMPLEMENTATO**: assente nei sorgenti esaminati.
- **DA VERIFICARE**: comportamento o scenario non ancora confermato dalle prove registrate.

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

### Test automatici

- `backend/src/test/java/it/raffo/backend/security/AirportJwtAuthenticationConverterTest.java`
- `backend/src/test/java/it/raffo/backend/users/AppUserRepositoryIntegrationTest.java`
- `backend/src/test/java/it/raffo/backend/BackendApplicationTests.java` (presente; non eseguito nella verifica mirata del 16 ottobre)

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

## Errori di sicurezza e controlli dei claim — FATTO

`CustomAuthenticationEntryPoint` e `CustomAccessDeniedHandler` sono collegati sia a `exceptionHandling` sia a `oauth2ResourceServer` in `SecurityConfig`.

- Autenticazione assente o non valida: `401`, body `ApiErrorResponse`, `Content-Type: application/json` e header `WWW-Authenticate: Bearer`.
- Utente autenticato senza il ruolo richiesto: `403`, body `ApiErrorResponse` con messaggio `Access denied`.
- Il messaggio pubblico del 401 resta generico: `Authentication required or invalid token`.

Il converter usa due metodi dedicati:

- `extractUserId()` richiede un claim `sub` di tipo String, non vuoto o composto da soli spazi, convertibile in UUID; un subject non valido viene rifiutato prima di interrogare il repository.
- `extractAuthVersion()` accetta soltanto Integer e Long non negativi. Claim assenti, stringhe, valori negativi, decimali e booleani causano `BadCredentialsException`.

Dopo la lettura dell’utente, il converter verifica account attivo e coincidenza della versione. L’authority viene dal ruolo corrente nel database, anche se il claim `role` dichiara un ruolo diverso.

I 20 test automatici del converter verificano la logica in memoria; le risposte HTTP 401/403 sono state provate manualmente con Postman negli scenari elencati nella sezione Test. Firma, scadenza, filtri HTTP e accesso reale al database non sono coperti da questi test unitari.

## Logout e revoca — FATTO

`POST /api/v1/auth/logout` richiede un bearer token valido e restituisce `204 No Content`. Il controller estrae il subject e delega ad `AuthService.logout()`, che chiama `AppUserRepository.incrementAuthVersion(userId)` dentro una transazione.

Il repository esegue questo UPDATE JPQL:

```text
update AppUser u
set u.authVersion = u.authVersion + 1
where u.id = :userId
```

L’incremento è calcolato direttamente nel database. PostgreSQL coordina gli aggiornamenti concorrenti sulla stessa riga, evitando la precedente sequenza di lettura, incremento in memoria e salvataggio. Il risultato del repository è il numero di righe aggiornate: `1` per un utente esistente, `0` per un UUID non trovato. Il service trasforma `0` in `InvalidCredentialsException`; il successo viene confermato dal commit della transazione.

La query usa `@Modifying(flushAutomatically = true, clearAutomatically = true)`: invia prima le modifiche JPA pendenti e poi svuota il contesto di persistenza per evitare successive letture dello stato precedente. Il vecchio metodo di incremento in memoria è stato rimosso da `AppUser`.

Alla richiesta successiva il converter rifiuta i token con versione precedente: vengono revocati anche quelli delle altre sessioni dello stesso utente. Un nuovo login emette un token con la versione aggiornata.

**FATTO — incremento atomico:** i tre test di integrazione del repository, confermati come superati dallo sviluppatore, verificano incremento singolo, due transazioni concorrenti con risultato `5 -> 7` e utente inesistente. La prova concorrente attende entrambi i commit prima della lettura finale; gli scenari HTTP restano verifiche distinte.

## Test manuali con PowerShell

I comandi seguenti restano esempi con risultati attesi. Le prove effettivamente confermate, svolte anche con Postman, sono registrate separatamente nella sezione Test; questo elenco non certifica che ogni comando sia stato eseguito.

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
- **FATTO** — PostgreSQL e Liquibase configurati; migrazioni e repository verificati nei tre test su PostgreSQL 17 temporaneo, con esito positivo confermato dallo sviluppatore. Gli altri ambienti restano da verificare.
- **FATTO** — Migrazione `app_users`, entity `AppUser`, enum `ADMIN`/`OPERATORI` e repository utenti.
- **FATTO** — DTO register/login/me e risposta autenticazione.
- **FATTO** — Registrazione con normalizzazione email, controllo duplicati e vincolo univoco nel database.
- **FATTO** — Hash password con `BCryptPasswordEncoder`.
- **FATTO** — Login con verifica password e account attivo; errore generico per credenziali non valide.
- **FATTO** — Emissione JWT HS256 dopo register/login, durata 900 secondi.
- **FATTO** — Resource Server per validazione JWT; controlli espliciti issuer/audience ancora assenti.
- **FATTO** — Controllo account attivo e `authVersion` a ogni richiesta autenticata; authority dal ruolo corrente nel database.
- **FATTO** — `/me`, ping ADMIN/OPERATORI e autorizzazione separata delle due aree.
- **FATTO** — Logout `204` e incremento atomico di `authVersion` tramite UPDATE; test concorrente del repository superato e revoca su `/me` di due token precedenti confermata dallo sviluppatore.
- **FATTO** — Errori applicativi per duplicati, credenziali errate e validazione DTO.
- **FATTO** — Gestori JSON 401/403 collegati a `exceptionHandling` e al Resource Server; 401 con header Bearer. Provati manualmente token assente, malformato e revocato, e accesso ADMIN negato a OPERATORI.
- **FATTO** — Controlli espliciti di subject e tipo/valore di `authVersion`, con rifiuto tramite `BadCredentialsException`.
- **FATTO** — 20 casi del converter superati con JDK 17; tre test di integrazione PostgreSQL con esito positivo confermato dallo sviluppatore. Esecuzioni e limiti distinti nella sezione Test.

## Cose da fare a breve

### Backend auth

- **NON IMPLEMENTATO** — `POST /api/v1/auth/change-password`.
- **NON IMPLEMENTATO** — Predisposizione riservata degli account: `/register` è pubblico e accetta anche `ADMIN`. Il pannello web utenti resta rinviato.

- **PARZIALE** — Garantire `409` anche per violazioni del vincolo univoco causate da registrazioni concorrenti.
- **DA VERIFICARE** — Automatizzare le risposte HTTP 401/403 già provate manualmente; completare la verifica di token scaduti e con firma alterata. I test del converter non attraversano la catena HTTP.
- **DA VERIFICARE** — `Cache-Control: no-store` su login e register: già osservato nelle risposte 401 riportate durante le prove, ma non ancora registrato per tutte le risposte di autenticazione.

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

### Test — stato al 16 ottobre 2026

#### Test automatici del converter — FATTO

`AirportJwtAuthenticationConverterTest` usa JUnit, test parametrizzati e Mockito, con repository simulato e utenti creati in memoria. Le verifiche precedenti su subject non UUID, versione decimale e ruolo dal database sono comprese nei casi parametrizzati.

| Gruppo | Casi | Risultato verificato |
| --- | --- | --- |
| Subject assente, vuoto, soli spazi, numerico, non UUID | 5 | `BadCredentialsException`, nessuna chiamata al repository |
| `authVersion` assente, stringa, Integer negativa, Long negativa, Double 0.5, Double 0.0, booleana | 7 | `BadCredentialsException`; nessun troncamento dei decimali |
| Versione Integer/Long valida, per entrambi i ruoli, con claim ruolo discordante | 4 | Autenticazione valida, identità corretta, unica authority dal database |
| Versione token minore o maggiore di quella nel database | 2 | `BadCredentialsException` |
| Utente inesistente e account disattivato | 2 | `BadCredentialsException` |

Esecuzione dalla cartella `backend`, con JDK 17:

```powershell
.\mvnw.cmd -Dtest=AirportJwtAuthenticationConverterTest test
```

**Esito effettivo: 20 casi, 0 fallimenti, 0 errori, 0 saltati; BUILD SUCCESS.**

Questa esecuzione mirata non avvia Spring o PostgreSQL e non esegue `BackendApplicationTests.contextLoads()`. I Jwt sono costruiti direttamente in memoria: questi test non verificano firma, issuer, audience, scadenza, status/header/body HTTP o persistenza reale. Una `BadCredentialsException` nei test unitari non equivale, da sola, a una verifica HTTP del 401.

#### Test di integrazione del repository su PostgreSQL — FATTO

`AppUserRepositoryIntegrationTest` usa `@DataJpaTest`, Testcontainers e l’immagine `postgres:17-alpine`. `@ServiceConnection` collega Spring al PostgreSQL temporaneo, separato dal database di sviluppo. Liquibase applica le migrazioni e Hibernate valida lo schema.

| Test | Risultato verificato |
| --- | --- |
| `incrementsAuthVersionForExistingUser` | Versione `5 -> 6`; una riga aggiornata |
| `incrementsAuthVersionWithoutLosingConcurrentUpdates` | Due transazioni aggiornano la stessa riga e restituiscono entrambe `1`; dopo entrambi i commit la versione è `7` |
| `returnsZeroWhenUserDoesNotExist` | Nessuna riga aggiornata e risultato `0` per un UUID senza utente |

Nel test concorrente `Propagation.NOT_SUPPORTED` disabilita la transazione automatica del metodo. L’utente viene inserito e confermato prima dei lavoratori; due thread aprono transazioni separate con `TransactionTemplate` e si sincronizzano con `CyclicBarrier(2)` prima degli UPDATE. I `Future` vengono attesi prima della lettura finale. Il blocco `finally` termina i lavoratori e rimuove l’utente inserito con commit.

**Esito: tre test superati, confermato dallo sviluppatore durante il training.** L’assistente ha verificato i sorgenti senza rieseguire questi test durante l’aggiornamento del documento. La precedente esecuzione dei 20 casi del converter è distinta: non viene attestata un’unica esecuzione combinata.

Per eseguire la classe dalla cartella `backend`, con Docker Engine disponibile:

```powershell
.\mvnw.cmd -Dtest=AppUserRepositoryIntegrationTest test
```

Questi test verificano repository e database temporaneo reale. Non attraversano controller o filtri JWT e non provano due richieste HTTP di logout concorrenti. Il risultato `0` del repository non costituisce un test automatico della risposta HTTP del service.

#### Prove manuali locali confermate dallo sviluppatore — FATTO

Le prove seguenti sono state eseguite dallo sviluppatore con Postman e confermate durante il training del 16 ottobre; non sono test HTTP automatici.

- Registrazione OPERATORI: `201`, ruolo e token restituiti; login con le credenziali create: `200`.
- `/me` senza token e con bearer malformato: `401`, JSON coerente e header `WWW-Authenticate: Bearer`, dopo il collegamento dei gestori al Resource Server.
- Token OPERATORI valido: `/me` e `/operatori/ping` restituiscono `200`.
- Token OPERATORI su `/admin/ping`: `403`, JSON `ApiErrorResponse` con messaggio `Access denied`.
- Sequenza `/me` con token valido, logout, riuso dello stesso token su `/me`, nuovo login e `/me` con nuovo token: `200 -> 204 -> 401 con JSON -> 200`.
- Dopo il passaggio all’UPDATE atomico: due login dello stesso operatore producono i token A e B, entrambi accettati da `/me` (`200`); logout con A (`204`); A e B rifiutati da `/me` (`401` con JSON); un nuovo login permette nuovamente `/me` (`200`). Esito confermato dallo sviluppatore.

#### Scenari ancora DA VERIFICARE

- Test HTTP automatici di 401/403, content type, body JSON e header Bearer.
- Email duplicata: `409`, anche con registrazioni concorrenti.
- Login con password errata, email inesistente o account disattivato: `401` generico, nessun token.
- Token ADMIN: ping ADMIN `200`, ping OPERATORI `403`; non dedurre questa verifica HTTP dai test unitari dei ruoli.
- Token con firma alterata o scaduto: `401`; issuer/audience errati dopo i relativi controlli, ancora da implementare.
- Estensione della revoca dei due token ai ping e a richieste HTTP di logout concorrenti. La revoca su `/me` è confermata manualmente; la concorrenza dell’UPDATE è verificata nel test del repository.
- Disattivazione dell’account dopo emissione token verificata attraverso una richiesta HTTP e database reale.
- Header di cache di login/register e ulteriori test di integrazione dei flussi auth. I tre test PostgreSQL del repository sono completati; `contextLoads()` non è stato verificato in questa revisione.

## Ordine di ripresa

1. **FATTO per gli scenari registrati** — Errori JSON 401/403, controlli dei claim e 20 casi del converter; incremento atomico di `authVersion`, tre test PostgreSQL del repository e prova Postman di revoca di due token su `/me`.
2. Prossimo sviluppo consigliato: implementare il cambio password riutilizzando l’incremento atomico per revocare i token precedenti.
3. Completare le verifiche HTTP automatiche e gli scenari residui, la validazione JWT e la predisposizione riservata degli account.
4. Sviluppare login e landing Angular.
5. Sviluppare database, backend e frontend di modelli di aereo e tratte.

Il codice applicativo resta scritto dallo sviluppatore junior con guida passo per passo. Per questa revisione è stata autorizzata esplicitamente la scrittura assistita dei soli test e del documento.

## Nota didattica importante

Il flow attuale e volutamente semplice. Serve a capire bene il ciclo:

```text
credenziali -> verifica -> JWT -> Authorization header -> verifica JWT -> lettura utente dal DB -> controllo active/authVersion -> ruolo corrente -> autorizzazione -> controller protetto
```

Quando questo ciclo e chiaro e verificato, aggiungere le funzioni business diventa molto piu semplice, perche ogni nuova API dovra solo dichiarare quale ruolo puo usarla e applicare le proprie regole di dominio.
