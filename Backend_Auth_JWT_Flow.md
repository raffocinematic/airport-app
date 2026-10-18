# Autenticazione con JWT - Airport App

## Stato della verifica — aggiornamento del 18 ottobre 2026

L’analisi iniziale del 14 ottobre era basata sui sorgenti. Il 16 ottobre sono stati completati i controlli del converter e sono stati eseguiti con JDK 17 i suoi 20 casi automatici: tutti superati, zero errori, zero fallimenti e zero casi saltati. È stato inoltre implementato l’incremento atomico di authVersion nel database. Lo sviluppatore ha confermato il successo dei tre test di integrazione PostgreSQL con Testcontainers e della prova Postman di revoca di due token. È stato inoltre implementato `POST /api/v1/auth/change-password`: lo sviluppatore ha confermato su Postman il cambio password, la revoca del vecchio token, l’utilizzo del nuovo token e i casi di errore descritti nella sezione Test. Nel repository è presente anche il nuovo test del caso positivo di aggiornamento password/versione. Durante l’aggiornamento backend l’assistente ha controllato i sorgenti senza eseguire test; le diverse verifiche sono registrate separatamente. Gli ulteriori test del cambio password sono rinviati per scelta dello sviluppatore.

Il 17 ottobre è stata completata la gestione del `409 Conflict` anche quando due registrazioni superano il controllo iniziale dell’email. Lo sviluppatore ha confermato le prove manuali con Postman e PostgreSQL locale: registrazione normale `201`, email già presente `409`, due INSERT concorrenti con esiti `201` e `409` e una sola riga finale. L’assistente ha controllato il codice senza eseguire o aggiungere test automatici per questa modifica.

Il 18 ottobre sono stati completati backend e frontend del flusso minimo di login e registrazione. La registrazione pubblica crea soltanto OPERATORI: il ruolo non fa parte della richiesta ed è assegnato dal server. La pagina `/registrazione` torna alla login con una conferma, senza salvare il token restituito; la login conserva il token soltanto in memoria e apre la landing comune `/home`, protetta da guard. La pagina `/profilo` resta disponibile separatamente. Lo sviluppatore ha confermato il funzionamento del flusso principale e la predisposizione manuale del primo ADMIN con Postman e pgAdmin, seguita da nuova login e ping ADMIN riuscito.

Sono stati inoltre applicati il restyling azzurro e l’icona SVG dell’aereo. Nella precedente attività l’assistente ha completato il build senza avvisi e verificato in una preview locale desktop/mobile impaginazione, messaggi, focus da tastiera, stato disabilitato e movimento ridotto. I 9 test frontend superati appartengono alla precedente esecuzione, distinta da queste verifiche grafiche. Gli ulteriori test automatici restano rinviati per scelta dello sviluppatore; gli scenari specifici non confermati sono indicati separatamente. Questo aggiornamento modifica soltanto la documentazione e non esegue nuovi test.

Sono state successivamente completate la landing comune `/home` e l’uscita dal frontend. L’intestazione mostra nome e ruolo a sinistra, Airport App centrato e Logout a destra; la scheda centrale contiene l’illustrazione SVG dell’aereo in costruzione. Lo sviluppatore ha confermato le prove guidate: logout con backend attivo e risposta `204`, ritorno alla login senza avviso; uscita locale con backend spento e avviso di revoca non confermata; nuova login dopo il riavvio. Il timeout di 10 secondi è configurato nel codice, ma non è stata confermata una prova dedicata di una richiesta lasciata in attesa fino al timeout. Questo aggiornamento documentale non esegue nuovi build o test.

È stata inoltre completata la transizione “sfoglia libro” tra login e registrazione: View Transitions del router Angular e animazioni SCSS globali fanno ruotare le schede con prospettiva e ombreggiatura, in avanti verso la registrazione e nel verso opposto tornando alla login. Lo sviluppatore ne ha confermato il funzionamento manuale. L’assistente ha verificato separatamente la compilazione SCSS dei tre fogli interessati, senza eseguire un nuovo build Angular completo o test automatici per questa modifica. Movimento ridotto e fallback di compatibilità sono presenti nel codice, ma non hanno una prova dedicata registrata per la nuova animazione.

- **FATTO**: implementazione presente nel codice; le verifiche svolte sono indicate esplicitamente nella sezione Test.
- **PARZIALE**: presente con aspetti da completare.
- **NON IMPLEMENTATO**: assente nei sorgenti esaminati.
- **DA VERIFICARE**: comportamento o scenario non ancora confermato dalle prove registrate.

## Obiettivo

Questo documento descrive lo stato attuale del backend per autenticazione, registrazione pubblica OPERATORI, predisposizione riservata del primo ADMIN, login con JWT, logout, cambio password e gestione base dei ruoli `ADMIN` e `OPERATORI`, insieme al frontend di login, registrazione, landing comune con logout e consultazione separata del profilo autenticato.

La priorita attuale e avere un flusso minimo funzionante:

1. registrazione pubblica di un OPERATORE e predisposizione riservata del primo ADMIN;
2. login con email e password;
3. generazione di un access token JWT;
4. uso del token per chiamare API protette;
5. distinzione tra area ADMIN e area OPERATORI;
6. logout con revoca dei token precedenti;
7. cambio della propria password con emissione di un nuovo token.

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
- `backend/src/main/java/it/raffo/backend/auth/ChangePasswordRequest.java`
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
- `backend/src/main/java/it/raffo/backend/common/InvalidPasswordException.java`

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
- `auth_version`: contatore usato per revocare token precedenti al logout, al cambio password o a una modifica riservata del ruolo;
- `created_at`: data di creazione.

## Registration flow

Endpoint:

```http
POST /api/v1/auth/register
```

**FATTO** — Registrazione pubblica consentita soltanto per il ruolo `OPERATORI`, secondo il requisito concordato il 18 ottobre. `RegisterRequest` contiene esclusivamente `firstName`, `lastName`, `email` e `password`. `AuthService.register()` assegna sempre `Role.OPERATORI`: il client non può scegliere il ruolo né creare ADMIN tramite questo endpoint, anche usando Postman.

Il backend restituisce ancora `AuthResponse` con JWT. Il frontend di registrazione non salva quel token e rimanda a `/login?registered=true`; l’utente deve effettuare la login con le credenziali appena scelte. La conferma password è un controllo del form Angular e non viene inviata al backend.

La policy attuale di `RegisterRequest` resta di 8–128 caratteri con `@NotBlank`. Il limite di 72 byte UTF-8 e il minimo di 15 caratteri introdotti per il cambio password non sono stati applicati alla registrazione.

### Passaggi logici

1. Il client invia nome, cognome, email e password; il ruolo è assente dal DTO.
2. `AuthController` riceve la richiesta e valida il DTO `RegisterRequest` con `@Valid`.
3. `AuthService.register()` normalizza l'email:
   - trim degli spazi esterni;
   - conversione in minuscolo.
4. Il service controlla l’email con `existsByEmail()`: se è già presente, lancia `DuplicateResourceException`, gestita con `409 Conflict`.
5. La password viene trasformata in hash tramite `PasswordEncoder`.
6. Viene creata una entity `AppUser` con ruolo `OPERATORI` assegnato dal server, account attivo e `authVersion` iniziale pari a zero.
7. Il repository esegue `saveAndFlush()` dentro un `try/catch`: l’INSERT viene inviato al database prima di proseguire con la generazione del token.
8. Se una registrazione concorrente ha inserito la stessa email, il service riconosce la violazione del relativo vincolo e lancia `DuplicateResourceException` con la causa originale. Gli altri errori di integrità vengono propagati.
9. Se il salvataggio riesce, il backend genera un JWT e prepara la risposta con dati utente essenziali e token. Il metodo è transazionale: la risposta di successo arriva al controller dopo il commit.

### Email duplicate e registrazioni concorrenti — FATTO

Il controllo `existsByEmail()` intercetta un’email già presente, ma non impedisce a due richieste di trovare entrambe l’email libera prima dei rispettivi inserimenti. La protezione finale resta il vincolo univoco nel database.

Nel database locale è stato verificato il vincolo `app_users_email_key`, definito come `UNIQUE (email)` e non differibile (`deferrable = false`). `AuthService` ne conserva il nome nella costante `EMAIL_UNIQUE_CONSTRAINT`.

`saveAndFlush()` permette di far emergere la violazione dentro il `try/catch` del service: il flush invia le modifiche al database, ma non equivale al commit. Il vincolo non differibile viene controllato durante l’inserimento.

Il metodo `isUniqueConstraintViolation()` percorre la catena delle cause di `DataIntegrityViolationException` e riconosce soltanto una `org.hibernate.exception.ConstraintViolationException` che presenti entrambe queste condizioni:

- SQLSTATE `23505`, cioè violazione di unicità;
- nome del vincolo uguale a `app_users_email_key`.

In questo caso il service lancia `DuplicateResourceException("Email already exists", exception)`. Il nuovo costruttore conserva la causa originale; il gestore esistente restituisce `409 Conflict` con body `ApiErrorResponse`, messaggio `Email already exists` e `fieldErrors` vuoto. La causa tecnica non viene inserita nel JSON e il percorso di errore non genera un token.

L’eccezione runtime fa eseguire il rollback della transazione di registrazione. Se il vincolo o il codice SQLSTATE non corrispondono, il service rilancia l’eccezione originale senza attribuire l’errore all’email.

Il nome del vincolo deve corrispondere a quello dello schema: un’eventuale rinomina richiederà il relativo allineamento della costante. La modifica del 17 ottobre relativa ai duplicati non ha cambiato migrazioni o regole di accesso a `/register`; il successivo sviluppo del 18 ottobre ha rimosso il ruolo dal DTO e imposto `OPERATORI` nel service.

### Esempio richiesta

La password seguente è un segnaposto da sostituire localmente, non una credenziale predefinita:

```json
{
  "firstName": "Mario",
  "lastName": "Operatore",
  "email": "operatore@test.it",
  "password": "<PASSWORD_SCELTA_LOCALMENTE>"
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
  "lastName": "Operatore",
  "email": "operatore@test.it",
  "role": "OPERATORI",
  "accessToken": "...",
  "expiresInSeconds": 900
}
```

### Predisposizione riservata del primo ADMIN — FATTO, procedura manuale locale

La procedura seguente è stata confermata dallo sviluppatore per il primo account ADMIN, Raffo. Le credenziali reali restano locali e non sono riportate nel documento.

1. Con Postman, eseguire `POST /api/v1/auth/register` con Authorization `No Auth` e i quattro campi del DTO. Il risultato atteso è `201`, con ruolo `OPERATORI` e password salvata come hash BCrypt.
2. Nel Query Tool di pgAdmin, sul database `airport_app`, promuovere soltanto l’account scelto e incrementare la versione di autenticazione. Sostituire il segnaposto con l’email normalizzata effettiva:

```sql
BEGIN;

UPDATE public.app_users
SET role = 'ADMIN',
    auth_version = auth_version + 1
WHERE email = '<EMAIL_NORMALIZZATA_ADMIN>'
  AND role = 'OPERATORI'
RETURNING id, first_name, email, role, auth_version;
```

3. Verificare che venga restituita esattamente la riga desiderata, poi eseguire separatamente `COMMIT;`. Per annullare prima della conferma, usare `ROLLBACK;` al posto del commit.
4. Eseguire una nuova login con Postman o con l’applicazione. L’incremento di `auth_version` invalida i token precedenti; la nuova login restituisce ruolo `ADMIN` e un nuovo token.
5. Con Postman, usare il token della nuova login come Bearer su `GET /api/v1/admin/ping`: esito `200` confermato dallo sviluppatore.

Questa è una procedura manuale riservata a chi accede al database. Non è stato aggiunto un endpoint pubblico per scegliere `ADMIN`, né uno script automatico di inizializzazione o un pannello utenti. Il ruolo ADMIN non è vincolato a una sola riga dallo schema: la procedura descrive la predisposizione del primo account locale, non una regola automatica di unicità degli amministratori.

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

Dopo login, register o cambio password riuscito, il client riceve `accessToken`. Dopo il cambio password deve usare il nuovo token: quelli emessi con la versione precedente vengono rifiutati alle richieste successive.

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
POST /api/v1/auth/change-password
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
- `GET /api/v1/auth/me`, `POST /api/v1/auth/logout` e `POST /api/v1/auth/change-password` richiedono token valido, account attivo e versione coincidente; il cambio password è accessibile a entrambi i ruoli;
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

### Logout frontend — FATTO per gli scenari registrati

Dalla home il pulsante Logout chiama `AuthService.logout()`: `POST /api/v1/auth/logout` con body nullo e risposta attesa `204 No Content`. Il token resta disponibile quando parte la richiesta, affinché l’interceptor aggiunga il Bearer. Il servizio applica `timeout(10_000)`.

`Home.loggingOut` impedisce invii ripetuti, disabilita i pulsanti Logout e Riprova e mostra “Uscita…”. Il caricamento del profilo non può essere avviato mentre si sta uscendo. La richiesta di logout, limitata dal timeout, viene lasciata terminare anche se la Home viene chiusa.

Al successo la Home cancella la sessione locale e naviga a `/login` con `replaceUrl: true`. In caso di errore, incluso un `401`, o di timeout, cancella comunque la sessione locale e naviga a `/login?reason=logout-unconfirmed`. La login mostra: “Sei uscito dall’applicazione. La revoca delle altre sessioni non è stata confermata.” L’avviso viene rimosso all’inizio di un nuovo tentativo di accesso. L’uscita locale non prova che il server abbia eseguito la revoca.

Sul `401` del logout l’interceptor cancella il token corrente ma lascia la navigazione alla Home, evitando il doppio reindirizzamento con motivo `session-expired`. Per le altre API conserva il comportamento precedente. La Home confronta inoltre il token della richiesta con quello corrente: una risposta tardiva non cancella una nuova sessione con token diverso.

Il comportamento backend resta quello descritto sopra: un logout riuscito revoca tutti i token precedenti dello stesso utente, anche quelli delle altre sessioni. Le prove manuali confermate e gli scenari residui sono registrati nella sezione Test.

## Cambio password — FATTO

Endpoint autenticato, disponibile sia ad ADMIN sia a OPERATORI:

```http
POST /api/v1/auth/change-password
Authorization: Bearer TOKEN_ATTUALE
Content-Type: application/json
```

Esempio di body, con valori dimostrativi:

```json
{
  "currentPassword": "PasswordAttuale!2026",
  "newPassword": "NuovaPassword!2026"
}
```

### Validazione della richiesta

- `currentPassword`: obbligatoria e non composta da soli spazi, massimo 128 caratteri nel DTO; non viene imposto il nuovo minimo per consentire l’uso delle password già esistenti. Il service rifiuta con `401` valori oltre 72 byte UTF-8 prima di chiamare BCrypt.
- `newPassword`: obbligatoria e non composta da soli spazi, lunghezza 15–72 secondo `@Size`, massimo 72 byte UTF-8 verificati nel service e diversa dalla password attuale.
- Le password non vengono normalizzate con `trim`: gli spazi fanno parte del valore. Con caratteri accentati o altri caratteri Unicode, numero di caratteri e numero di byte possono differire.
- Resta in uso `BCryptPasswordEncoder`. La policy della registrazione è distinta e conserva i limiti attuali di 8–128 caratteri.

### Passaggi logici e transazione

1. `AuthController` valida `ChangePasswordRequest` ed estrae UUID utente e `authVersion` dal JWT già autenticato. Il body non contiene un identificativo utente: ciascun utente può cambiare la propria password.
2. `AuthService.changePassword()` apre una transazione indipendente con `TransactionTemplate` e propagazione `REQUIRES_NEW`.
3. Il service rilegge l’utente, verifica account attivo e versione attesa, poi confronta la password attuale con l’hash tramite `PasswordEncoder.matches()`.
4. Dopo la validazione della nuova password, il service ne calcola l’hash BCrypt.
5. `AppUserRepository.changePasswordIfUnchanged()` aggiorna insieme `passwordHash` e `authVersion = authVersion + 1`. La clausola WHERE richiede UUID, account attivo, versione attesa e hash precedente corrispondenti.
6. Una riga aggiornata indica successo; zero righe causano `InvalidCredentialsException`. La condizione evita di sovrascrivere un cambio password o una revoca intervenuti dopo la lettura iniziale. Questo comportamento concorrente è implementato, ma non è coperto dal nuovo test del caso positivo.
7. La query esegue flush e clear del contesto JPA. Il service rilegge l’utente dentro la transazione e restituisce lo stato aggiornato al chiamante.
8. Dopo il completamento del commit, il service genera il JWT usando quello stato e restituisce `AuthResponse`. Se la transazione fallisce, il flusso non raggiunge la generazione del token.

La modifica della password e l’incremento della versione avvengono con un unico UPDATE; non viene chiamato separatamente il metodo di logout. Il converter rifiuta alle richieste successive tutti i token con la versione precedente, anche se emessi da altri login dello stesso utente. Il caso del vecchio token è stato confermato su Postman.

### Risposta ed errori

- Successo: `200 OK`, body `AuthResponse` con dati utente, nuovo `accessToken` ed `expiresInSeconds`; header `Cache-Control: no-store`.
- DTO non valido: `400 Bad Request`, body `ApiErrorResponse` con `fieldErrors` relativi ai campi non validi.
- Nuova password identica a quella attuale oppure oltre 72 byte UTF-8: `InvalidPasswordException`, trasformata in `400` con messaggio `Validation failed` e dettaglio in `fieldErrors.newPassword`.
- Password attuale errata, utente inesistente/inattivo, versione non coincidente o UPDATE con zero righe: `401` con messaggio generico `Invalid credentials`.
- Token assente, non valido o revocato: `401` gestito dalla catena di sicurezza con il messaggio generico del relativo entry point.

Il flusso riguarda il cambio volontario della propria password. Password temporanee, cambio obbligatorio e funzionalità frontend restano sviluppi separati.

## Test manuali con PowerShell

I comandi seguenti restano esempi con risultati attesi. Le prove effettivamente confermate, svolte anche con Postman, sono registrate separatamente nella sezione Test; questo elenco non certifica che ogni comando sia stato eseguito.

### Predisporre ADMIN

`POST /api/v1/auth/register` crea esclusivamente OPERATORI. Per predisporre il primo ADMIN seguire la procedura riservata descritta nella sezione Registration flow: registrazione, UPDATE mirato nel database con incremento di `auth_version`, nuova login e verifica del ping ADMIN.

### Registrare OPERATORI

Sostituire il segnaposto della password con un valore scelto localmente:

```powershell
curl.exe -i -X POST "http://localhost:8080/api/v1/auth/register" -H "Content-Type: application/json" -d '{"firstName":"Luca","lastName":"Operatore","email":"operatore@test.it","password":"<PASSWORD_SCELTA_LOCALMENTE>"}'
```

Gli esempi di login seguenti richiedono credenziali di account realmente predisposti. Email e password dimostrative non sono account o segreti inclusi nel progetto.

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
- **FATTO** — PostgreSQL e Liquibase configurati; migrazioni e repository verificati nei tre test su PostgreSQL 17 temporaneo, con esito positivo confermato dallo sviluppatore. Le prove manuali di registrazione del 17 ottobre sono confermate sul database locale PostgreSQL 18; gli ambienti distribuiti non sono stati esaminati.
- **FATTO** — Migrazione `app_users`, entity `AppUser`, enum `ADMIN`/`OPERATORI` e repository utenti.
- **FATTO** — DTO register/login/me/change-password e risposta autenticazione; la richiesta di registrazione contiene soltanto nome, cognome, email e password, con ruolo `OPERATORI` assegnato dal server.
- **FATTO** — Registrazione pubblica OPERATORI e predisposizione manuale riservata del primo ADMIN con incremento di `auth_version`, nuova login e ping ADMIN; esito confermato dallo sviluppatore il 18 ottobre.
- **FATTO** — Registrazione con normalizzazione email, controllo duplicati e vincolo univoco nel database. Gestione `409` anche per registrazioni concorrenti tramite `saveAndFlush()` e riconoscimento di SQLSTATE `23505`/vincolo `app_users_email_key`; causa originale conservata e altri errori propagati. Prove manuali del 17 ottobre confermate dallo sviluppatore.
- **FATTO** — Hash password con `BCryptPasswordEncoder`.
- **FATTO** — Login con verifica password e account attivo; errore generico per credenziali non valide.
- **FATTO** — Emissione JWT HS256 dopo register/login e cambio password, durata 900 secondi.
- **FATTO** — Resource Server per validazione JWT; controlli espliciti issuer/audience ancora assenti.
- **FATTO** — Controllo account attivo e `authVersion` a ogni richiesta autenticata; authority dal ruolo corrente nel database.
- **FATTO** — `/me`, ping ADMIN/OPERATORI e autorizzazione separata delle due aree.
- **FATTO** — Logout `204` e incremento atomico di `authVersion` tramite UPDATE; test concorrente del repository superato e revoca su `/me` di due token precedenti confermata dallo sviluppatore.
- **FATTO** — Cambio password autenticato con verifica della password attuale, validazione della nuova password, UPDATE condizionale di hash/versione e nuovo JWT dopo il commit; risposta `200` con `Cache-Control: no-store`. Prove Postman confermate dallo sviluppatore; copertura automatica limitata al caso positivo del repository presente nei sorgenti.
- **FATTO** — Errori applicativi per duplicati, credenziali errate, validazione DTO e nuova password non ammessa.
- **FATTO** — Gestori JSON 401/403 collegati a `exceptionHandling` e al Resource Server; 401 con header Bearer. Provati manualmente token assente, malformato e revocato, e accesso ADMIN negato a OPERATORI.
- **FATTO** — Controlli espliciti di subject e tipo/valore di `authVersion`, con rifiuto tramite `BadCredentialsException`.
- **FATTO** — 20 casi del converter superati con JDK 17; tre test di integrazione PostgreSQL con esito positivo confermato dallo sviluppatore. Esecuzioni e limiti distinti nella sezione Test.

- **FATTO** — Frontend Angular con login, registrazione pubblica OPERATORI, token della login solo in memoria, interceptor Bearer e landing comune `/home` protetta; `/profilo` resta una pagina separata. Flusso principale confermato dallo sviluppatore; dopo la registrazione si torna alla login senza salvare il token ricevuto.
- **FATTO** — Home con tema azzurro condiviso, benvenuto e ruolo a sinistra, marchio centrato, Logout a destra e illustrazione centrale dell’aereo in costruzione.
- **FATTO** — Logout frontend con richiesta autenticata, uscita locale anche in caso di errore, messaggio di revoca non confermata e timeout configurato a 10 secondi. Prove con backend attivo e spento confermate dallo sviluppatore; prova dedicata del timeout ancora da verificare.
- **FATTO** — Restyling azzurro di login e registrazione, icona SVG dell’aereo e stili SCSS condivisi. Build senza avvisi e verifiche grafiche desktop/mobile svolti dall’assistente nella precedente attività; i 9 test frontend restano un’esecuzione storica distinta.

- **FATTO** — Animazione “sfoglia libro” tra login e registrazione, con verso opposto al ritorno, durata totale di 650 ms e stile azzurro mantenuto. Funzionamento confermato manualmente dallo sviluppatore; compilazione SCSS verificata dall’assistente. Nessuna nuova dipendenza o modifica alle API.

## Cose da fare a breve

### Backend auth

- **NON IMPLEMENTATO** — Automazione della predisposizione riservata degli ADMIN e pannello web utenti. La procedura manuale locale del primo ADMIN è già completata; `/register` è pubblico e crea soltanto OPERATORI.

- **DA VERIFICARE** — Automatizzare le risposte HTTP 401/403 già provate manualmente; completare la verifica di token scaduti e con firma alterata. I test del converter non attraversano la catena HTTP.
- **DA VERIFICARE** — `Cache-Control: no-store` su login e register: già osservato nelle risposte 401 riportate durante le prove, ma non ancora registrato per tutte le risposte di autenticazione.

### Differenze rispetto all’analisi iniziale

Queste differenze riguardano i requisiti dell’analisi funzionale e tecnica, oltre al flusso minimo già implementato.

- **FATTO** — Liquibase anziché Flyway: continuare con Liquibase e riallineare la documentazione in una revisione successiva.
- **NON IMPLEMENTATO** — RS256 con chiave privata esterna; attualmente firma HS256.
- **NON IMPLEMENTATO** — Claim `aud`, `jti` e controlli espliciti di issuer/audience; il token attuale contiene anche email.
- **PARZIALE** — BCrypt mantenuto per scelta dello sviluppatore anziché migrazione ad Argon2id. Il cambio password richiede almeno 15 caratteri e al massimo 72 byte UTF-8; la registrazione conserva 8–128 caratteri e non ha il controllo esplicito dei byte. L’allineamento della policy di registrazione resta separato dal cambio password completato.
- **NON IMPLEMENTATO** — Password temporanee, scadenza, cambio obbligatorio e blocco delle API business; campi `mustChangePassword`, `temporaryPasswordExpiresAt`, `updatedAt` assenti.
- **NON IMPLEMENTATO** — Limite ai tentativi di login e configurazione CORS dedicata.
- **NON IMPLEMENTATO** — Negazione dei percorsi non dichiarati: attualmente richiedono soltanto autenticazione.
- **DA VERIFICARE** — Configurazione di produzione per HTTPS e segreti esterni; ambiente distribuito non esaminato.

### Backend business

- **NON IMPLEMENTATO** — Modulo `aircraftmodels`, tabella `aircraft_models`, GET/POST `/api/v1/aircraft-models` solo ADMIN.
- **NON IMPLEMENTATO** — Modulo `routes`, tabella `routes`, GET/POST `/api/v1/routes` solo ADMIN.

### Frontend minimo

- **FATTO** — Animazione “sfoglia libro” tra `/login` e `/registrazione`. In `frontend/src/app/app.config.ts`, `withViewTransitions` salta la prima navigazione e distingue `auth-forward` da `auth-backward`, leggendo i percorsi delle rotte: il parametro `registered=true` non impedisce il ritorno animato alla login dopo la registrazione. Le altre coppie di rotte non vengono animate. L’integrazione Angular utilizzata è in developer preview.
- **FATTO** — Il mixin in `frontend/src/app/shared/_auth-page.scss` assegna `view-transition-name: auth-card` a entrambe le schede. Gli stili globali in `frontend/src/styles.scss` animano le rappresentazioni precedente e successiva con prospettiva di 1200 px, rotazione sull’asse verticale, bordo di rotazione sinistro in avanti e destro al ritorno, e leggera ombreggiatura. La scheda uscente ruota per 325 ms, quella entrante per altri 325 ms; viene disattivata la dissolvenza predefinita dello sfondo. Le dimensioni diverse dei due form sono accompagnate dalla transizione del gruppo.
- **FATTO nel codice** — Se è richiesto movimento ridotto, l’animazione viene saltata; gli SCSS includono anche una protezione dedicata. Se manca la View Transitions API, Angular mantiene la normale navigazione; se mancano i tipi di transizione o il selettore CSS richiesto, il callback salta l’effetto. Nessuna dipendenza aggiunta, nessuna modifica ai contratti backend; le prove dedicate di compatibilità e movimento ridotto per questa animazione restano da verificare.

- **FATTO** — Template iniziale sostituito con `router-outlet`; route pubbliche `/login` e `/registrazione`, route protette `/home` e `/profilo`, con reindirizzamento alla login per il percorso vuoto o sconosciuto. Proxy di sviluppo `/api/**` verso `http://localhost:8080` e configurazione di HttpClient con interceptor.
- **FATTO** — Pagina login con form reattivo, validazione email/password, rimozione degli spazi esterni dall’email, stato di caricamento e blocco degli invii ripetuti. Gestione degli errori di credenziali, validazione, rete e servizio; password svuotata dopo il successo e navigazione a `/home`.
- **FATTO** — `AuthService.login()` chiama `POST /api/v1/auth/login` e salva il token nella sessione; `register()` chiama `POST /api/v1/auth/register` senza salvare il token della risposta; `me()` chiama `GET /api/v1/auth/me`; `logout()` chiama `POST /api/v1/auth/logout` con timeout di 10 secondi.
- **FATTO** — Pagina `/registrazione` con nome, cognome, email, password e conferma password. Validazione coerente con il DTO e controllo della coincidenza delle password; normalizzazione degli spazi esterni di nome, cognome ed email, senza alterare le password. La richiesta HTTP esclude conferma password e ruolo.
- **FATTO** — Registrazione con stato di caricamento, blocco degli invii ripetuti e gestione di `400`, `409`, rete e servizio. Dopo il successo le password sono svuotate e si torna a `/login?registered=true`, con messaggio di conferma; collegamenti reciproci “Registrati” e “Accedi”.
- **FATTO** — Grafica condivisa per login e registrazione: sfondo azzurro sfumato, schede bianche, pulsanti blu e icona decorativa dell’aereo in SVG. Gli SCSS dei componenti usano il mixin in `frontend/src/app/shared/_auth-page.scss`; nessuna dipendenza aggiunta. Layout adattabile, focus da tastiera visibile e rispetto della preferenza di movimento ridotto.
- **FATTO** — Token conservato soltanto in memoria tramite `AuthSession`, senza localStorage, sessionStorage o cookie. Il refresh della pagina perde la sessione e la route protetta rimanda al login.
- **FATTO** — Interceptor che aggiunge il Bearer soltanto alle richieste della stessa origine con percorso `/api/v1/`, escludendo login e register. Nessun token aggiunto a domini esterni o percorsi diversi.
- **FATTO** — Landing comune `/home` per entrambi i ruoli, con nome e ruolo ottenuti da `/me`, caricamento ed errore con possibilità di riprovare. Il nome è dinamico; il ruolo viene visualizzato come Admin o Operatore. Intestazione a tre colonne su desktop e a due righe sotto 640 px.
- **FATTO** — Scheda centrale “Lavori in corso” con illustrazione locale in `frontend/public/illustration/airport-work-in-progress.svg`: aereo in costruzione, gru e barriere da cantiere. Il template usa `/illustration/airport-work-in-progress.svg`, con testo alternativo e dimensioni adattabili. Il mixin `canvas` di `frontend/src/app/shared/app-theme.scss` condivide sfondo, colore del testo e caratteri tra home, login e registrazione.
- **FATTO** — Pagina separata `/profilo` con dati reali di nome, email e ruolo ottenuti da `/me`, caricamento, messaggi di errore e pulsante per aggiornare il profilo.
- **FATTO** — Guard delle route `/home` e `/profilo`: accesso consentito in presenza di token, altrimenti reindirizzamento al login. La presenza del token non ne dimostra la validità: la verifica effettiva resta al backend.
- **FATTO** — Un `401` su una richiesta autenticata cancella la sessione corrente; per le API diverse dal logout l’interceptor rimanda alla login con motivo `session-expired`, mentre per il logout la navigazione è gestita dalla Home. Un `403` conserva la sessione. Il `401` tardivo di una richiesta con token precedente non cancella una nuova sessione con token diverso; l’errore viene comunque propagato al chiamante.
- **FATTO** — Pulsante Logout, stato “Uscita…”, uscita locale e ritorno alla login con sostituzione della voce corrente nella cronologia. Se la revoca non è confermata compare l’avviso ambrato nella login; dettagli nella sezione Logout frontend.
- **PARZIALE** — Gestione della scadenza: rilevata alla successiva risposta `401` del backend. Non è implementata una verifica preventiva o un timer che interrompa la sessione mentre l’utente non effettua richieste.
- **PARZIALE** — Ruolo letto e mostrato nella home comune e nella pagina profilo; navigazione distinta a `/admin` o `/operatori` ancora **NON IMPLEMENTATA**.
- **NON IMPLEMENTATO** — Landing ADMIN con menu e segnaposto immagine; benvenuto OPERATORI senza menu amministrativo.
- **NON IMPLEMENTATO** — Cambio password frontend; il relativo endpoint backend è già presente. Il logout frontend è completato.
- **NON IMPLEMENTATO** — Liste e form di modelli di aereo e tratte.

Il flusso implementato è: login -> collegamento “Registrati” -> form di registrazione -> POST register con creazione OPERATORI -> ritorno alla login con conferma -> POST login -> token in memoria -> `/home` -> GET `/me` con Bearer -> visualizzazione di nome, ruolo e lavori in corso -> Logout -> uscita locale e ritorno alla login. Il proxy descritto riguarda lo sviluppo locale; l’instradamento delle API in produzione resta da predisporre.

La registrazione pubblica OPERATORI è un requisito concordato il 18 ottobre e sostituisce la precedente esclusione della registrazione pubblica nell’analisi iniziale. La creazione degli ADMIN resta riservata e non è disponibile nel form. La sicurezza frontend non sostituisce quella backend.

### Test — stato al 18 ottobre 2026

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

#### Test di integrazione del repository su PostgreSQL — stato delle verifiche

`AppUserRepositoryIntegrationTest` usa `@DataJpaTest`, Testcontainers e l’immagine `postgres:17-alpine`. `@ServiceConnection` collega Spring al PostgreSQL temporaneo, separato dal database di sviluppo. Liquibase applica le migrazioni e Hibernate valida lo schema.

| Test | Risultato verificato |
| --- | --- |
| `incrementsAuthVersionForExistingUser` | Versione `5 -> 6`; una riga aggiornata |
| `incrementsAuthVersionWithoutLosingConcurrentUpdates` | Due transazioni aggiornano la stessa riga e restituiscono entrambe `1`; dopo entrambi i commit la versione è `7` |
| `returnsZeroWhenUserDoesNotExist` | Nessuna riga aggiornata e risultato `0` per un UUID senza utente |

È presente anche `changesPasswordAndIncrementsAuthVersionForExistingUser`, aggiunto dallo sviluppatore: salva un utente con versione `5`, esegue `changePasswordIfUnchanged()` con versione e hash corretti e verifica una riga aggiornata, il nuovo hash e versione `6`. Usa hash dimostrativi: non verifica BCrypt, il service o le risposte HTTP. La presenza e le asserzioni sono state controllate nei sorgenti; l’assistente non ne ha eseguito il test e non attesta un nuovo esito combinato dei quattro metodi.

Nel test concorrente `Propagation.NOT_SUPPORTED` disabilita la transazione automatica del metodo. L’utente viene inserito e confermato prima dei lavoratori; due thread aprono transazioni separate con `TransactionTemplate` e si sincronizzano con `CyclicBarrier(2)` prima degli UPDATE. I `Future` vengono attesi prima della lettura finale. Il blocco `finally` termina i lavoratori e rimuove l’utente inserito con commit.

**Esito precedentemente confermato: i tre test del logout sono superati, come riportato dallo sviluppatore durante il training.** L’assistente ha verificato i sorgenti senza rieseguire questi test durante l’aggiornamento del documento. La precedente esecuzione dei 20 casi del converter è distinta: non viene attestata un’unica esecuzione combinata.

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

#### Cambio password: prove Postman confermate — FATTO

Lo sviluppatore ha confermato di aver eseguito con successo, passo per passo, le seguenti prove locali:

| Scenario | Risultato confermato |
| --- | --- |
| Login iniziale e `/me` con token A | `200` |
| Cambio password con token A e password attuale corretta | `200`, nuovo token B e header `Cache-Control: no-store` |
| `/me` con token A dopo il cambio | `401` |
| `/me` con token B | `200` |
| Login con la vecchia password | `401` |
| Login con la nuova password | `200` |
| Cambio password senza autenticazione, con body valido | `401` |
| Token valido, password attuale errata e nuova password valida | `401`, messaggio `Invalid credentials` |
| Token valido, password attuale corretta e nuova password troppo corta | `400`, errore in `fieldErrors.newPassword` |
| Token valido, nuova password uguale a quella attuale | `400`, errore in `fieldErrors.newPassword` |
| `/me` con token B dopo le richieste rifiutate | `200` |

Queste sono verifiche manuali dichiarate dallo sviluppatore, non esecuzioni automatiche dell’assistente. Non attestano prove concorrenti, rollback, casi Unicode oltre 72 byte o una ripetizione completa con entrambi i ruoli.

**Ulteriori test del cambio password rinviati per scelta dello sviluppatore:** la copertura automatica attuale comprende il nuovo caso positivo del repository presente nei sorgenti. I casi di rifiuto della query, la concorrenza del cambio password, la validazione completa, il rollback e i controlli HTTP automatici non sono stati aggiunti o eseguiti in questa revisione.

#### Registrazione: duplicati e concorrenza — FATTO, prove manuali del 17 ottobre 2026

Lo sviluppatore ha confermato l’esito positivo delle seguenti prove con Postman e PostgreSQL locale; l’assistente non le ha eseguite direttamente.

| Scenario | Risultato confermato |
| --- | --- |
| Registrazione con email inizialmente libera | `201 Created`, dati utente e token |
| Ripetizione della richiesta con la stessa email già registrata | `409 Conflict`, messaggio `Email already exists`, nessun token |
| Conteggio dopo le richieste sequenziali | Una sola riga per l’email usata |
| Due richieste con la stessa email inizialmente libera, entrambe arrivate all’INSERT | Due INSERT osservati in attesa del lock |
| Rilascio del lock e completamento delle due registrazioni | Una risposta `201` con token e una `409` con `Email already exists`, senza token |
| Conteggio dopo le richieste concorrenti | Una sola riga per l’email usata |

Per coordinare la prova concorrente è stata mantenuta aperta una transazione SQL con `LOCK TABLE app_users IN SHARE MODE`: le letture potevano proseguire, mentre gli INSERT rimanevano in attesa. Due richieste Postman hanno così superato il controllo iniziale. Gli INSERT sono stati osservati tramite `pg_stat_activity`, quindi il `COMMIT` della transazione di controllo ha rilasciato il blocco e consentito il completamento delle richieste.

La ripetizione sequenziale verifica `existsByEmail()`; la prova con entrambi gli INSERT in attesa verifica invece il percorso di gestione della violazione del vincolo. Il lock è stato usato soltanto per coordinare la prova locale e non fa parte del codice applicativo.

Non sono stati aggiunti o eseguiti nuovi test automatici per la gestione dei duplicati concorrenti. La propagazione degli altri errori di integrità è presente nel codice, ma non è stata verificata con una prova dedicata in questa revisione. I risultati automatici storici del converter e del repository restano quelli registrati nelle rispettive sezioni.

#### Frontend: build e test già eseguiti — FATTO per gli scenari registrati

Lo sviluppatore ha riportato il successo di `npm.cmd run build` il 18 ottobre. Nella successiva attività di restyling l’assistente ha eseguito nuovamente il build finale, con compilazione riuscita, nessun avviso sui limiti degli stili e output in `frontend/dist/frontend`. Questi risultati riguardano il precedente restyling di login e registrazione e non attestano una nuova compilazione della landing o del logout. Il presente aggiornamento documentale non ripete il build.

In una precedente attività autorizzata sui test, distinta dal restyling, l’assistente ha eseguito dalla cartella `frontend`:

```powershell
npm.cmd test -- --no-watch
```

**Esito effettivo già registrato: 2 file di test superati, 9 casi superati, nessun fallimento.** Questa esecuzione precede il presente aggiornamento della documentazione; non è stata ripetuta.

| Gruppo | Casi | Risultato verificato |
| --- | --- | --- |
| Creazione di `App` | 1 | Componente radice creato |
| Bearer su richiesta API protetta | 1 | Header Authorization presente con il token della sessione |
| URL esterno assoluto, URL esterno relativo al protocollo, asset e `/api/v10/` | 4 | Bearer non aggiunto |
| `401` riferito al token corrente | 1 | Sessione cancellata, navigazione al login con motivo e propagazione dell’errore |
| `403` | 1 | Sessione conservata, nessuna navigazione e propagazione dell’errore |
| `401` tardivo con nuova sessione | 1 | Nuovo token conservato, nessuna navigazione e propagazione dell’errore |

Gli 8 casi dell’interceptor usano `HttpTestingController` per simulare le risposte HTTP e uno spy sul Router: non contattano il backend e non verificano il flusso completo nel browser. Il test di `App` verifica soltanto la creazione del componente. L’esecuzione dei 9 casi precede la landing e la nuova gestione del logout: non è stata ripetuta e non costituisce una verifica automatica delle modifiche successive.

**Ulteriori test automatici frontend rinviati per scelta dello sviluppatore:** non vengono aggiunti test di AuthService o guard, né altri casi dell’interceptor. Questa scelta non modifica i risultati dei 9 casi già eseguiti.

#### Frontend: verifiche grafiche locali — FATTO per gli scenari registrati

Durante il restyling l’assistente ha aperto il build di produzione in Chrome headless con un server di anteprima locale, senza contattare il backend reale. Sono state verificate le pagine di login e registrazione a larghezze di 1440, 390 e 320 px: campi contenuti nella finestra, nessuno scorrimento orizzontale e parte iniziale della scheda raggiungibile anche quando la pagina di registrazione richiede scorrimento verticale.

Sono stati inoltre controllati l’icona decorativa, il messaggio di registrazione completata, i cinque errori del form di registrazione vuoto, il focus da tastiera con contorno visibile, lo stile del pulsante disabilitato e l’assenza di transizioni con movimento ridotto. Gli screenshot sono stati ispezionati nella precedente attività; queste prove riguardano la grafica e gli stati dell’interfaccia, non una registrazione HTTP sul backend reale.

#### Login, registrazione e primo ADMIN: conferma dello sviluppatore — FATTO per il flusso principale

Il 18 ottobre lo sviluppatore ha dichiarato funzionanti e completati, per il perimetro attuale, backend e frontend delle pagine di login e registrazione. La conferma riguarda il flusso principale: registrazione pubblica come OPERATORI, ritorno alla login con conferma e successivo accesso all’applicazione con le credenziali create.

Lo sviluppatore ha inoltre confermato la procedura del primo ADMIN, Raffo: registrazione tramite Postman, promozione riservata nel database con incremento di `auth_version`, nuova login con ruolo ADMIN e chiamata a `GET /api/v1/admin/ping` con risposta `200`. L’assistente ha guidato la procedura e controllato il codice; non ha eseguito queste operazioni sul database o le richieste Postman.

#### Landing e logout frontend: prove manuali confermate — FATTO per gli scenari registrati

Lo sviluppatore ha completato la home con illustrazione visibile e ha confermato il funzionamento delle prove guidate del logout. I dati del benvenuto arrivano da `/me`; l’intestazione aggiornata dispone utente a sinistra, Airport App al centro e Logout a destra.

| Scenario | Esito confermato dallo sviluppatore |
| --- | --- |
| Login e apertura della home | Landing `/home` con dati dell’utente e illustrazione |
| Backend Spring Boot fermato dopo la login, frontend lasciato attivo e pagina non ricaricata; pressione di Logout | Uscita locale e ritorno a `/login?reason=logout-unconfirmed`, con avviso di revoca non confermata |
| Riavvio del backend e nuova login | Accesso nuovamente consentito |
| Logout con backend attivo | `POST /api/v1/auth/logout` con stato `204`, ritorno alla login senza avviso ambrato |

L’assistente ha controllato i sorgenti e guidato le prove; non ha eseguito queste richieste al backend né arrestato o avviato il processo dello sviluppatore. La prova con backend spento verifica la gestione dell’errore di connessione e non dimostra l’intervento del timeout: il limite di 10 secondi è presente nel codice, ma non è stata registrata una richiesta mantenuta in attesa per tutta la durata prevista.

#### Animazione login/registrazione — FATTO per gli scenari registrati

Il 18 ottobre lo sviluppatore ha dichiarato funzionante la transizione dopo le prove guidate nel browser. La conferma registra il funzionamento manuale dell’effetto tra login e registrazione nei due versi; non costituisce un test automatico o una verifica su tutti i browser.

L’assistente ha controllato il collegamento tra callback del router, nome condiviso delle schede e selettori globali. Ha inoltre compilato in memoria con Sass `src/styles.scss`, `src/app/login/login.scss` e `src/app/register/register.scss`, dalla cartella `frontend`: tutti e tre i fogli compilano senza errori. Questa verifica riguarda gli SCSS e non equivale a un nuovo `npm.cmd run build`, a test automatici Angular o a una verifica visiva eseguita dall’assistente. Non sono state aggiunte nuove prove automatiche per l’animazione; i risultati storici già riportati restano separati.

#### Frontend: scenari specifici ancora DA VERIFICARE

La conferma del flusso principale non attesta una nuova prova dettagliata di ogni caso di errore o di sessione. Restano senza una conferma manuale specifica nel browser:

- Credenziali errate: messaggio generico e possibilità di riprovare, senza creazione di una nuova sessione.
- Registrazione con email già presente: messaggio relativo al `409`; il percorso backend dei duplicati è già stato verificato nelle prove storiche del 17 ottobre.
- Accesso diretto a `/profilo` senza sessione e refresh della pagina autenticata: ritorno al login.
- Token revocato dal backend: premendo “Aggiorna profilo”, risposta `401`, ritorno al login e messaggio di sessione non valida.
- Richiesta di registrazione manipolata per tentare di scegliere `ADMIN`: impossibilità di ottenere il ruolo amministrativo. La protezione è presente nel codice, ma non è stata registrata una prova dedicata dopo la modifica.

- Logout con richiesta lasciata in attesa fino al timeout di 10 secondi: uscita locale con avviso di revoca non confermata.
- Logout che riceve `401` e risposta tardiva del logout dopo una nuova sessione: coordinamento fra interceptor e Home e protezione del nuovo token, presenti nel codice ma senza una prova manuale dedicata registrata.
- Verifica dedicata della home a 320 px e con nomi molto lunghi; le precedenti verifiche grafiche dell’assistente riguardavano login e registrazione.
- Animazione login/registrazione: prove dedicate con movimento ridotto e browser privi delle API o dei selettori richiesti; i percorsi di fallback sono presenti nel codice. Il ritorno dopo una registrazione riuscita con `registered=true` è previsto dalla selezione delle rotte, ma non è stato confermato separatamente per la nuova animazione.

Gli ulteriori test automatici restano rinviati per scelta dello sviluppatore; questi scenari sono distinti dal completamento concordato del flusso minimo.

#### Scenari backend ancora DA VERIFICARE

- Test HTTP automatici di 401/403, content type, body JSON e header Bearer.
- Login con email inesistente o account disattivato e ulteriori casi di password errata: `401` generico, nessun token. Il rifiuto della vecchia password dopo il cambio è già confermato nella tabella Postman.
- Token ADMIN su ping OPERATORI: `403`, ancora da confermare. Il ping ADMIN `200` è stato confermato nella procedura del primo ADMIN; non dedurre il caso OPERATORI dai test unitari dei ruoli.
- Token con firma alterata o scaduto: `401`; issuer/audience errati dopo i relativi controlli, ancora da implementare.
- Estensione della revoca dei due token ai ping e a richieste HTTP di logout concorrenti. La revoca su `/me` è confermata manualmente; la concorrenza dell’UPDATE è verificata nel test del repository.
- Disattivazione dell’account dopo emissione token verificata attraverso una richiesta HTTP e database reale.
- Header di cache di login/register e ulteriori test di integrazione dei flussi auth. Il successo dei tre test PostgreSQL del logout è stato confermato in precedenza; il nuovo test positivo del cambio password è presente nei sorgenti. `contextLoads()` e la classe aggiornata nel suo insieme non sono stati eseguiti dall’assistente in questa revisione.

## Ordine di ripresa

1. **FATTO per gli scenari registrati** — Errori JSON 401/403, controlli dei claim e 20 casi del converter; incremento atomico di `authVersion`, tre test PostgreSQL del repository e prova Postman di revoca di due token su `/me`.
2. **FATTO per gli scenari registrati** — Cambio password autenticato, aggiornamento atomico di hash/versione e nuovo JWT dopo il commit; prove Postman confermate e test positivo del repository presente. Ulteriori test rinviati per scelta dello sviluppatore.
3. **FATTO per gli scenari registrati** — `409` per email già presente e per violazione del vincolo causata da registrazioni concorrenti; prove Postman/SQL del 17 ottobre confermate con esiti `201`/`409` e una sola riga finale. Nessun nuovo test automatico aggiunto o eseguito.
4. Prossimi sviluppi backend auth: automazione della predisposizione riservata degli ADMIN, validazione JWT e allineamento della policy di registrazione. La procedura manuale del primo ADMIN è già confermata; le verifiche residue restano documentate senza introdurre nuovi test in questa revisione.
5. **FATTO per gli scenari registrati** — Backend e frontend di login e registrazione pubblica OPERATORI; ritorno alla login dopo la registrazione, token della login in memoria e Bearer alle API. Flusso principale e predisposizione del primo ADMIN confermati dallo sviluppatore. Build, controlli grafici e 9 test frontend del precedente restyling restano risultati storici distinti, non nuove esecuzioni sulla home o sul logout.
6. **FATTO per gli scenari registrati** — Landing comune `/home` protetta, illustrazione dell’aereo in costruzione, intestazione con utente a sinistra, marchio centrato e Logout a destra. Logout frontend con uscita locale e avviso quando la revoca non è confermata; prove con backend attivo e spento confermate. Timeout configurato a 10 secondi, con prova dedicata ancora da verificare. `/profilo` resta disponibile separatamente.
7. **FATTO per gli scenari registrati** — Animazione “sfoglia libro” tra login e registrazione, con verso opposto al ritorno, View Transitions e SCSS globali, durata di 650 ms e nessuna nuova dipendenza. Funzionamento manuale confermato dallo sviluppatore e compilazione SCSS verificata dall’assistente; compatibilità e movimento ridotto presenti nel codice, con prove dedicate ancora da verificare.
8. Prossimi sviluppi frontend: aree distinte ADMIN/OPERATORI con menu amministrativo e cambio password. La scadenza è attualmente rilevata su risposta `401`; la gestione preventiva resta separata. Ulteriori test automatici rinviati per scelta dello sviluppatore.
9. Sviluppare database, backend e frontend di modelli di aereo e tratte.

Il codice applicativo resta scritto dallo sviluppatore junior con guida passo per passo, salvo le eccezioni esplicitamente autorizzate: test e restyling delle pagine di autenticazione. La predisposizione del primo ADMIN è stata eseguita e confermata dallo sviluppatore. Questo aggiornamento riguarda esclusivamente la documentazione: nessuna modifica al codice applicativo o ai test, nessuna esecuzione di test e nessun commit.

## Nota didattica importante

Il flow attuale e volutamente semplice. Serve a capire bene il ciclo:

```text
credenziali -> verifica -> JWT -> Authorization header -> verifica JWT -> lettura utente dal DB -> controllo active/authVersion -> ruolo corrente -> autorizzazione -> controller protetto
```

Quando questo ciclo e chiaro e verificato, aggiungere le funzioni business diventa molto piu semplice, perche ogni nuova API dovra solo dichiarare quale ruolo puo usarla e applicare le proprie regole di dominio.
