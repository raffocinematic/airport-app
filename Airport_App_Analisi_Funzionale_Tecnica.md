# Airport App — Incarico del cliente e analisi iniziale

**Versione:** 0.4 — 18 ottobre 2026

**Stato:** perimetro aggiornato — login, registrazione pubblica OPERATORI, landing page ADMIN, modelli di aereo e tratte

**Destinatario:** sviluppatore Java junior

**Stato implementativo:** login, registrazione, landing comune `/home` e logout frontend sono completati per gli scenari registrati e confermati dallo sviluppatore. Dopo la login si apre `/home`; `/profilo` resta disponibile separatamente. Il logout è stato provato con backend attivo e spento; il timeout di 10 secondi è configurato ma non è stato verificato con una richiesta lasciata in attesa fino alla scadenza. Le aree distinte per ruolo, il menu amministrativo e il cambio password frontend restano sviluppi successivi. Dettagli e verifiche sono registrati in [Backend_Auth_JWT_Flow.md](LoginRegisterAuth_JWT_Flow.md).

## 1. Il tuo cliente

> Sono il responsabile di un piccolo aeroporto privato a Napoli. Voglio affidarti la realizzazione di un gestionale web interno per organizzare i nostri collegamenti e il lavoro del personale.
>
> Al momento colleghiamo Napoli con Milano, Roma e Londra. I collegamenti nazionali prevedono andata e ritorno. Voglio iniziare con un'applicazione semplice, comprensibile e sicura, che possa crescere senza dover essere riscritta.
>
> La mia prima priorità è sapere chi entra nel sistema e quali operazioni può eseguire. Gli ADMIN gestiscono l'intera applicazione; gli OPERATORI svolgono soltanto le attività operative autorizzate.
>
> Dopo il login come ADMIN voglio una landing page con un menù a sinistra e, al centro, un’immagine del nostro aeroporto che ti forniremo in seguito. Dal menù, soltanto gli ADMIN devono poter accedere a due funzioni: aggiungere un modello di aereo e aggiungere una tratta.
>
> Per ogni modello voglio indicare se è piccolo o medio, quanti passeggeri può trasportare, il tipo di carburante e se richiede uno oppure due piloti. Per ora realizziamo soltanto questo; le altre funzionalità arriveranno più avanti.

L'aeroporto e la sua organizzazione sono uno scenario simulato. Questa prima analisi non descrive procedure aeronautiche reali né un sistema di controllo del traffico aereo.

## 2. Requisiti confermati e ipotesi

### Richieste del cliente

- Applicazione per un piccolo aeroporto privato con sede a Napoli.
- Tre collegamenti iniziali: Napoli–Milano, Napoli–Roma e Napoli–Londra.
- Andata e ritorno sui due collegamenti nazionali.
- Login semplice ma sicuro con JWT; passaggio a Keycloak in una fase successiva.
- Due ruoli: `ADMIN` e `OPERATORI`, mantenendo questi nomi anche nel codice.
- ADMIN abilitati a tutte le funzioni previste; OPERATORI esclusi dalle funzioni amministrative di questa fase.
- Landing page ADMIN con menù laterale sinistro e immagine centrale dell’aeroporto, fornita successivamente dal cliente.
- Inserimento dei modelli di aereo con categoria, capacità passeggeri personalizzata, carburante e numero di piloti.
- Inserimento delle tratte riservato agli ADMIN.

### Decisione concordata il 18 ottobre 2026

È prevista la registrazione pubblica esclusivamente come `OPERATORI`, senza scelta del ruolo. Il primo ADMIN viene predisposto attraverso una procedura riservata. Dopo la registrazione l’utente torna alla login con una conferma e accede usando le credenziali scelte; il token restituito dalla registrazione non viene salvato dal frontend. Questa decisione sostituisce la precedente ipotesi di assenza di registrazione pubblica.

### Proposte da validare durante lo sviluppo

| Tema | Ipotesi di lavoro |
| --- | --- |
| Londra | Anche il collegamento internazionale prevede andata e ritorno. |
| Aeroporti di destinazione | Per ora identifichiamo le città; gli aeroporti specifici di Milano, Roma e Londra sono da scegliere. |
| Personale | Gli OPERATORI possono autenticarsi, ma per ora vedono soltanto una pagina di benvenuto senza funzioni amministrative. I loro permessi operativi saranno definiti in seguito. |
| Account | Decisione confermata il 18 ottobre: registrazione pubblica solo OPERATORI; predisposizione degli ADMIN riservata. Il pannello di gestione utenti è rinviato. |
| Vendita futura | Vendita allo sportello effettuata dal personale; nessun portale passeggeri nella prima versione. |
| Orari e frequenza | Non sono ancora definiti: tre rotte non significa tre voli al giorno. |
| Modelli e flotta | Si registrano modelli di aereo; i singoli aeromobili con matricola e la loro assegnazione ai voli sono rinviati. |
| Carburante | Campo testuale obbligatorio per ora; un catalogo di valori consentiti sarà concordato in seguito. |

Queste ipotesi permettono di iniziare, ma non devono essere confuse con richieste già espresse dal cliente.

## 3. Glossario: rotta e volo non sono la stessa cosa

| Termine | Significato | Esempio |
| --- | --- | --- |
| Rotta commerciale | Collegamento tra Napoli e una città, indipendente dalla data | Napoli–Milano |
| Direzione | Verso di percorrenza della rotta | Napoli → Milano oppure Milano → Napoli |
| Modello di aereo | Configurazione riutilizzabile con categoria, capacità, carburante e piloti richiesti | Modello dimostrativo da 12 passeggeri |
| Aeromobile | Singolo mezzo fisico, eventualmente riconducibile a un modello; fuori perimetro | Aereo identificato da una matricola |
| Volo | Una partenza concreta, con data, orario, direzione e capacità | Napoli → Milano il 10 ottobre alle 09:00 |
| Utente | Membro del personale con credenziali di accesso | Un addetto allo sportello |
| Passeggero | Persona che viaggia; non è necessariamente un utente del sistema | Intestatario di un biglietto |

Nel modello iniziale avremo **tre rotte commerciali**. Assumendo il ritorno anche da Londra, queste consentono sei direzioni e un numero variabile di voli programmati.

| Rotta | Tipo | Direzioni iniziali |
| --- | --- | --- |
| Napoli–Milano | Nazionale | Napoli → Milano; Milano → Napoli |
| Napoli–Roma | Nazionale | Napoli → Roma; Roma → Napoli |
| Napoli–Londra | Internazionale | Napoli → Londra; Londra → Napoli, come ipotesi |

## 4. Perimetro della fase attuale

La nuova richiesta sostituisce la precedente pianificazione del primo rilascio. Sono inclusi soltanto:

1. Registrazione pubblica OPERATORI, predisposizione riservata del primo ADMIN, login JWT, logout e distinzione tra `ADMIN` e `OPERATORI`, con le protezioni di autenticazione già previste.
2. Landing page ADMIN con menù a sinistra e immagine centrale dell’aeroporto.
3. Inserimento e lista minima di consultazione dei modelli di aereo, riservati agli ADMIN.
4. Inserimento e lista minima di consultazione delle tratte, riservati agli ADMIN.

Le liste servono a verificare gli inserimenti. Modifica, eliminazione, disattivazione e filtri avanzati delle anagrafiche non fanno parte della richiesta attuale.

Sono rinviati: pannello di gestione utenti, dashboard statistiche, programmazione e gestione voli, flotta fisica, associazioni modello–tratta, vendita biglietti, passeggeri, check-in, imbarco, pagamenti, gestione equipaggi, schermata del registro attività e Keycloak. Non creare ora relative schermate, API o tabelle di dominio. Restano ammessi i log tecnici necessari alla sicurezza.

## 5. Matrice dei permessi

| Funzione | ADMIN | OPERATORI |
| --- | --- | --- |
| Login, logout e cambio della propria password | Sì | Sì |
| Consultare il proprio stato di autenticazione | Sì | Sì |
| Accedere alla landing page amministrativa | Sì | No |
| Vedere il menù amministrativo | Sì | No |
| Aggiungere e consultare modelli di aereo | Sì | No |
| Aggiungere e consultare tratte | Sì | No |
| Accedere alla pagina di benvenuto OPERATORI | Non necessaria | Sì |

Gli OPERATORI non possono accedere a queste funzioni nemmeno digitando direttamente l’URL o chiamando le API. Il backend applica i permessi; guard e visibilità del menù Angular rendono coerente l’interfaccia.

Anche gli ADMIN devono rispettare le validazioni: non possono, ad esempio, registrare un modello con zero passeggeri o tre piloti.

## 6. Analisi funzionale del primo rilascio

### RF-01 — Login

L'utente inserisce email e password. Se le credenziali sono valide e l'account è attivo, accede alla landing comune `/home` nel flusso minimo attuale; la navigazione alle landing distinte per ruolo resta lo sviluppo successivo descritto in RF-04. Il sistema restituisce un messaggio generico in caso di errore, senza distinguere tra email inesistente, account disattivato e password errata.

Il sistema limita i tentativi ripetuti, considerando sia l'account sia l'origine della richiesta. Soglie iniziali proposte: cinque errori per account in quindici minuti e un limite separato per IP, configurabile e da verificare considerando la rete condivisa dell'aeroporto.

### RF-02 — Sessione e logout

Durata proposta dell'access token: **15 minuti**. Nella prima versione non esiste refresh token: alla scadenza l'utente deve autenticarsi nuovamente. Angular conserva il token soltanto in memoria; ricaricare la pagina richiede quindi un nuovo login. È un compromesso esplicito di semplicità, da rivalutare per l'uso continuativo allo sportello.

Il pulsante Logout della home invia `POST /api/v1/auth/logout` con il Bearer corrente. Durante la richiesta mostra “Uscita…” e impedisce invii ripetuti. La risposta `204` conferma la revoca lato server dei token precedenti dello stesso utente, anche nelle altre sessioni; il frontend cancella il token locale e torna alla login con sostituzione della voce corrente nella cronologia.

Se la richiesta fallisce o supera il timeout configurato di 10 secondi, il frontend completa comunque l’uscita locale e torna a `/login?reason=logout-unconfirmed`, mostrando: “Sei uscito dall’applicazione. La revoca delle altre sessioni non è stata confermata.” Il messaggio viene rimosso all’inizio di un nuovo tentativo di login. In questo caso la revoca sul server non è confermata. Il `401` della richiesta di logout lascia alla Home la gestione della navigazione; gli altri `401` continuano a essere gestiti dall’interceptor come sessione scaduta o non valida.

Lo sviluppatore ha confermato le prove con backend attivo e spento e la nuova login dopo il riavvio. La prova dedicata del timeout resta da verificare; i dettagli sono distinti nel documento del flusso JWT.

### RF-03 — Registrazione OPERATORI e predisposizione degli ADMIN

Dalla login, il collegamento “Non hai un account? Registrati” apre `/registrazione`. Il form richiede nome, cognome, email, password e conferma password. La conferma deve coincidere e resta un controllo frontend: `POST /api/v1/auth/register` riceve soltanto nome, cognome, email e password e assegna sempre `OPERATORI` sul server. L’utente non può selezionare `ADMIN` né ottenerlo modificando la richiesta HTTP.

La registrazione riuscita restituisce `201` e il frontend torna alla login con un messaggio di conferma, senza salvare il token ricevuto. L’email duplicata produce `409`; gli errori del form e della chiamata sono mostrati all’utente e gli invii ripetuti sono bloccati durante la richiesta. Login e registrazione condividono una grafica azzurra adattabile agli schermi piccoli, con icona decorativa dell’aereo.

Il primo ADMIN è predisposto con una procedura tecnica riservata, senza password predefinite nel repository. La procedura manuale locale già confermata consiste nella registrazione dell’account come OPERATORI, nella promozione mirata tramite pgAdmin con incremento di `auth_version` e in una nuova login con verifica dell’accesso ADMIN. Non esistono un endpoint pubblico per creare ADMIN, uno script automatico di inizializzazione o un pannello di gestione utenti in questa fase. La procedura è descritta in [Backend_Auth_JWT_Flow.md](LoginRegisterAuth_JWT_Flow.md).

Email univoca, normalizzata eliminando gli spazi esterni e convertendola in minuscolo. Se si consegna una password temporanea, questa deve scadere e imporre il cambio: fino ad allora l’utente può soltanto consultare il proprio stato, cambiare password e uscire. Il completamento di questo passaggio precede l’accesso alla landing page.

### RF-04 — Landing comune e successive aree per ruolo

#### Landing comune implementata

Dopo la login, ADMIN e OPERATORI arrivano attualmente a `/home`, protetta dal guard di autenticazione. Nome e ruolo sono caricati tramite `/api/v1/auth/me`, con stato di caricamento ed errore con possibilità di riprovare.

L’intestazione mostra il benvenuto con nome dinamico e ruolo a sinistra, Airport App centrato rispetto all’intera barra e il pulsante Logout a destra. Sotto 640 px il marchio occupa la prima riga, mentre benvenuto e Logout occupano la seconda. Lo sfondo azzurro e i caratteri sono condivisi con login e registrazione.

La scheda centrale mostra “Lavori in corso”, il testo “Stiamo preparando le nuove funzionalità.” e un’illustrazione SVG minimale dell’aereo in costruzione, con gru e barriere. L’asset è `frontend/public/illustration/airport-work-in-progress.svg`, con testo alternativo e proporzioni adattabili. Questa illustrazione descrive lo stato dei lavori del gestionale ed è distinta dalla futura immagine dell’aeroporto prevista per l’area ADMIN.

#### Landing ADMIN prevista

Dopo il login, l’ADMIN arriva a `/admin`. La pagina presenta:

- **A sinistra:** menù persistente con le voci `Home`, `Modelli di aereo` e `Tratte`.
- **Al centro:** immagine dell’aeroporto, che sarà fornita successivamente dal committente.
- **Nell’area utente:** nome, ruolo e comando `Esci`.

In attesa dell’immagine usare un riquadro neutro con la dicitura “Immagine dell’aeroporto in arrivo”. Non generare un’immagine sostitutiva. La futura immagine deve mantenere le proporzioni e avere un testo alternativo. La sostituzione avverrà tramite asset dell’applicazione: non è richiesto un pannello di caricamento immagini.

Selezionando una voce, il contenuto centrale mostra la relativa lista e il form di inserimento; il menù rimane disponibile. `Home` riporta all’immagine. Evidenziare la voce selezionata. Su schermi piccoli il menù può diventare richiudibile.

Gli OPERATORI arrivano a `/operatori`, con un messaggio di benvenuto e l’indicazione che le funzioni operative saranno disponibili in seguito. Non vedono il menù amministrativo.

### RF-05 — Aggiungere un modello di aereo

Dalla voce `Modelli di aereo`, l’ADMIN seleziona `Aggiungi modello` e compila:

| Campo | Obbligatorio | Regola |
| --- | --- | --- |
| Nome del modello | Sì | Da 1 a 100 caratteri dopo la rimozione degli spazi esterni; proposto per identificare il modello |
| Categoria | Sì | Soltanto `PICCOLO` o `MEDIO` |
| Capacità passeggeri | Sì | Numero intero positivo, scelto dall’ADMIN |
| Tipo di carburante | Sì | Testo non vuoto, massimo 50 caratteri; valori da concordare con il cliente |
| Piloti richiesti | Sì | Numero intero: esclusivamente `1` oppure `2` |

La categoria non impone una capacità predefinita: il numero dei passeggeri è personalizzabile. Non introdurre soglie diverse per piccoli e medi senza una nuova richiesta. I piloti non sono conteggiati nella capacità passeggeri. Non gestiamo ancora assistenti di bordo.

Il nome è univoco ignorando maiuscole/minuscole e spazi esterni. Validare questi campi anche sul server. Un esempio puramente didattico è “Modello Demo 12”, categoria `PICCOLO`, 12 passeggeri, carburante “Carburante demo”, 2 piloti: non rappresenta specifiche tecniche di un aereo reale.

`Salva` registra il modello e aggiorna la lista con un messaggio di successo; `Annulla` non salva. In caso di errore conservare i dati inseriti e mostrare il problema. La lista riporta nome, categoria, passeggeri, carburante e piloti. Nessuna associazione obbligatoria a una tratta in questa fase.

### RF-06 — Aggiungere una tratta

Dalla voce `Tratte`, l’ADMIN seleziona `Aggiungi tratta` e compila:

| Campo | Obbligatorio | Regola |
| --- | --- | --- |
| Città di origine | Sì | Napoli, precompilata e non modificabile in questa fase |
| Città di destinazione | Sì | Testo non vuoto, massimo 100 caratteri, diverso da Napoli |
| Paese di destinazione | Sì | Codice paese selezionato da un elenco; `IT` per Italia, `GB` per Regno Unito |
| Collegamento di ritorno | Sì | Scelta esplicita Sì/No; per Milano e Roma deve essere Sì |

Il tipo è derivato dal paese: `NAZIONALE` per `IT`, `INTERNAZIONALE` per gli altri paesi. Perimetro geografico: destinazioni europee; la lista dei paesi selezionabili deve essere configurata coerentemente.

Ogni record rappresenta una rotta commerciale con origine Napoli; il flag di ritorno abilita la direzione inversa senza creare una seconda rotta. Bloccare duplicati per origine, destinazione normalizzata e paese, anche nel database. Le tre tratte iniziali si inseriscono dal gestionale: Milano/Italia, Roma/Italia e Londra/Regno Unito. Non limitare il numero totale a tre: il cliente potrà aggiungerne altre in seguito. Il ritorno da Londra rimane un’ipotesi da confermare.

`Salva` registra la tratta e aggiorna la lista; `Annulla` non salva. Mostrare origine, destinazione, paese, tipo e disponibilità del ritorno. Non richiedere aereo, data, orari, prezzi o numero di voli giornalieri: questi dati non appartengono alla sola anagrafica della tratta.

## 7. Prima proposta tecnica

| Area | Scelta proposta |
| --- | --- |
| Backend | Java e Spring Boot |
| API | REST con JSON e prefisso `/api/v1` |
| Autenticazione e autorizzazione | Spring Security, access token JWT e controlli sui ruoli |
| Persistenza | Spring Data JPA / Hibernate e PostgreSQL |
| Frontend | Angular e TypeScript |
| Migrazioni database | Flyway, con script SQL versionati |
| Build | Maven per il backend; strumenti Angular per il frontend |
| Verifica | JUnit, test di integrazione su PostgreSQL e pochi test end-to-end mirati |

Le versioni precise andranno fissate nel repository verificando la compatibilità tra Java, Spring Boot, Node.js e Angular. Non occorre aggiornare un progetto già avviato soltanto per adottare l'ultima versione.

Architettura: un unico backend organizzato per funzionalità (`auth`, `users`, `aircraftmodels`, `routes`), un frontend separato e un database. In ogni funzionalità il controller riceve le richieste, il service applica le regole e il repository accede ai dati. DTO dedicati separano le API dalle entità JPA; le entità utente non devono mai essere serializzate direttamente.

Le operazioni di modifica usano transazioni. Configurazioni e segreti sono esterni al codice. Le migrazioni governano lo schema; negli ambienti persistenti Hibernate valida lo schema senza modificarlo automaticamente.

## 8. Progetto del login JWT

### Flusso

1. Angular invia email e password a `POST /api/v1/auth/login`.
2. Spring Security verifica la password e lo stato dell'account.
3. Il backend firma un access token e lo restituisce con scadenza e dati essenziali dell'utente.
4. Angular lo conserva in memoria e lo invia nell'header `Authorization: Bearer ...` esclusivamente alle API del gestionale.
5. Il backend verifica il token, lo stato corrente dell'account e il permesso richiesto prima di eseguire la funzione.

### Password e firma

Le password sono conservate attraverso un `PasswordEncoder` con hash adattivo, proponendo Argon2id e parametri da misurare sull'ambiente scelto. Mai password in chiaro, cifratura reversibile o semplici hash SHA-256. Spring Security mette a disposizione l'astrazione per questa verifica: [Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).

Policy iniziale proposta: almeno 15 caratteri e massimo 128, supporto alle passphrase, nessun troncamento silenzioso. Una password temporanea deve essere casuale, scadere e imporre il cambio prima di accedere al gestionale.

JWT firmati con RS256, chiave privata esterna al repository e algoritmo accettato fissato sul server. Il payload contiene soltanto `sub` (ID stabile dell'utente), `iss`, `aud`, `iat`, `exp`, `jti`, ruolo e `authVersion`; non contiene password né dati dei passeggeri. La firma non rende segreto il payload.

Usare le componenti JWT di Spring Security per emissione e validazione, evitando un parser o una crittografia scritti a mano. Configurare esplicitamente firma, algoritmo, issuer, audience e scadenza. Il ruolo applicativo richiede un mapping dedicato verso `ROLE_ADMIN` o `ROLE_OPERATORI`: il mapping predefinito degli scope non basta. Riferimento: [Spring Security — JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

### Revoca e cambi dei permessi

Ogni utente ha un contatore `authVersion`. Ogni richiesta autenticata legge dal database account, ruolo e contatore correnti. Un token è valido soltanto se l'account è attivo e la sua versione coincide.

Logout e cambio password incrementano atomicamente il contatore. Anche eventuali reset password, modifiche del ruolo o disattivazioni effettuati dalla procedura tecnica riservata devono incrementarlo; le relative funzioni amministrative web sono rinviate. I token precedenti vengono quindi rifiutati dalle richieste successive. Il cambio password emette un nuovo token solo dopo il completamento della transazione; il token temporaneo non può essere riutilizzato per accedere al gestionale.

Questa scelta introduce volutamente una lettura del database per richiesta: il sistema non è completamente stateless, ma la revoca è comprensibile e adeguata alla scala iniziale. Le operazioni amministrative e di sicurezza richiedono attenzione alle richieste concorrenti.

### Protezioni HTTP e frontend

- HTTPS fuori dall'ambiente locale; password, token e chiavi esclusi dai log.
- Nessun token in URL, `localStorage` o `sessionStorage` nella soluzione scelta.
- Nessuna autenticazione basata su cookie nella prima versione: il bearer token è inviato esplicitamente dal frontend.
- CORS limitato agli origin autorizzati. Valutare la disabilitazione CSRF soltanto per queste API bearer senza credenziali automatiche del browser; rivalutarla se in futuro si introducono cookie.
- Risposte di autenticazione con `Cache-Control: no-store`; controlli di sicurezza non delegati ai route guard Angular.
- Input validati, nessun rendering di HTML non attendibile e nessun dettaglio interno negli errori.

Le scelte su trasporto, validazione e contenuto dei token seguono i principi di [OWASP REST Security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html). Durata e revoca qui descritte sono decisioni specifiche del progetto.

## 9. Modello dati iniziale

| Entità | Campi principali | Vincoli |
| --- | --- | --- |
| `AppUser` | UUID, nome, cognome, email normalizzata, passwordHash, role, active, authVersion, mustChangePassword, temporaryPasswordExpiresAt, createdAt, updatedAt | Email univoca; ruolo `ADMIN` o `OPERATORI` |
| `AircraftModel` | UUID, name, normalizedName, category, passengerCapacity, fuelType, requiredPilots, createdAt | Nome normalizzato univoco; categoria `PICCOLO`/`MEDIO`; capacità intera > 0; piloti interi 1 o 2 |
| `Route` | UUID, originCity, destinationCity, normalizedDestinationCity, destinationCountryCode, returnEnabled, createdAt | Origine Napoli; destinazione diversa; unicità origine + destinazione normalizzata + paese |

Il tipo nazionale/internazionale della tratta è calcolato dal codice paese, evitando due informazioni modificabili che potrebbero contraddirsi. Gli orari di creazione sono istanti UTC.

`AircraftModel` e `Route` sono anagrafiche indipendenti: per ora non esiste una relazione tra loro. Le dimensioni massime dei testi e i vincoli numerici sono applicati nelle API e nello schema PostgreSQL; unicità garantita anche con richieste concorrenti. Il nome normalizzato è calcolato dal backend, non accettato dal client.

Non creare ancora entità `Flight`, `Aircraft`, `Ticket` o `Passenger`. Quando progetteremo la flotta distingueremo i modelli dai singoli mezzi, e definiremo come la capacità del modello si trasferisce ai voli.

## 10. API indicative della fase attuale

| Metodo e percorso | Accesso | Scopo |
| --- | --- | --- |
| `POST /api/v1/auth/register` | Pubblico | Registrazione con ruolo OPERATORI assegnato dal server |
| `POST /api/v1/auth/login` | Pubblico, limitato | Autenticazione |
| `POST /api/v1/auth/logout` | Autenticato | Revoca dei token correnti dell’utente |
| `GET /api/v1/auth/me` | Autenticato | Profilo e obbligo di cambio password |
| `POST /api/v1/auth/change-password` | Autenticato | Cambio con verifica della password corrente |
| `GET /api/v1/aircraft-models` | Solo ADMIN | Lista modelli |
| `POST /api/v1/aircraft-models` | Solo ADMIN | Inserimento modello |
| `GET /api/v1/routes` | Solo ADMIN | Lista tratte |
| `POST /api/v1/routes` | Solo ADMIN | Inserimento tratta |

Non esporre ora API di modifica/cancellazione delle anagrafiche, gestione utenti o gestione voli. Per le liste prevedere paginazione semplice con dimensione massima controllata dal server.

Gli account con cambio password obbligatorio non accedono alle API di business, anche quando hanno ruolo ADMIN. I percorsi non dichiarati sono negati per impostazione predefinita.

Convenzioni: `201` creazione completata, `400` dati non validi, `401` credenziali o token non validi, `403` permesso insufficiente, `409` duplicato, `429` troppi tentativi. Gli errori espongono codice applicativo, messaggio e dettagli sui campi, senza stack trace.

## 11. Schermate e navigazione Angular

| Percorso proposto | Schermata | Accesso |
| --- | --- | --- |
| `/login` | Login e conferma della registrazione | Pubblico |
| `/registrazione` | Registrazione senza scelta del ruolo, ritorno alla login | Pubblico |
| `/home` | Landing comune dopo la login, lavori in corso e Logout | Autenticato |
| `/profilo` | Pagina separata di consultazione del profilo | Autenticato |
| `/change-password` | Cambio password, anche obbligatorio al primo accesso | Autenticato |
| `/admin` | Landing con immagine centrale e menù sinistro | ADMIN |
| `/admin/aircraft-models` | Lista modelli e form “Aggiungi modello” | ADMIN |
| `/admin/routes` | Lista tratte e form “Aggiungi tratta” | ADMIN |
| `/operatori` | Benvenuto senza funzioni amministrative | OPERATORI |

Le route `/login`, `/registrazione`, `/home` e `/profilo` sono implementate; il logout è disponibile dalla home. Le aree distinte con menu amministrativo e la pagina di cambio password frontend rimangono sviluppi successivi. La home comune non sostituisce il requisito finale delle landing ADMIN/OPERATORI.

Proporre un componente layout ADMIN riutilizzabile con menù e area contenuti. Non serve un endpoint per la landing statica o per il suo segnaposto. Nome e ruolo provengono dal profilo autenticato.

La scadenza dell’accesso riporta al login con un messaggio chiaro. Non ritentare automaticamente un inserimento dopo il nuovo accesso: evitare creazioni duplicate. I form disabilitano temporaneamente `Salva` durante la richiesta, indicano i campi obbligatori e mantengono i valori in caso di errore.

## 12. Criteri di accettazione della fase attuale

| ID | Scenario | Risultato atteso |
| --- | --- | --- |
| AC-01 | ADMIN effettua il login | Arriva alla landing, dopo eventuale cambio password obbligatorio |
| AC-02 | Landing ADMIN prima della consegna dell’immagine | Menù a sinistra e segnaposto centrale leggibile |
| AC-03 | Cliente fornisce l’immagine | L’asset sostituisce il segnaposto senza cambiare navigazione o permessi |
| AC-04 | ADMIN seleziona Modelli di aereo o Tratte | Si apre la funzione nell’area centrale; il menù resta disponibile |
| AC-05 | OPERATORI effettua il login | Benvenuto senza menù o funzionalità amministrative |
| AC-06 | OPERATORI apre URL ADMIN o chiama GET/POST delle anagrafiche | Pagina negata; API restituisce `403`, nessun dato amministrativo restituito o modificato |
| AC-07 | ADMIN salva un modello valido | Record persistito e visibile con tutti i cinque campi |
| AC-08 | Modelli della stessa categoria con capacità differenti | Entrambi ammessi: la capacità non è fissata dalla categoria |
| AC-09 | Capacità zero, negativa, decimale; categoria diversa; piloti 0, 3 o decimali; carburante vuoto | `400`, nessun salvataggio |
| AC-10 | Nome modello già esistente con maiuscole/spazi differenti | `409`; nessun duplicato, anche con richieste concorrenti |
| AC-11 | Inserimento Milano e Roma con ritorno e Londra con scelta esplicita del ritorno | Tre rotte commerciali; tipo nazionale/internazionale corretto |
| AC-12 | Tratta duplicata o Napoli come destinazione | Inserimento rifiutato |
| AC-13 | Utente annulla un form | Nessun nuovo record |
| AC-14 | Token assente, alterato, scaduto o con issuer/audience errati | `401` sulle API protette |
| AC-15 | Logout completato e riuso del vecchio token | Token rifiutato |
| AC-16 | Credenziali errate o account disattivato | Messaggio generico; nessun token |
| AC-17 | Registrazione con dati validi | Account OPERATORI creato; ritorno alla login con conferma, senza salvataggio del token della registrazione |
| AC-18 | Tentativo di scegliere ADMIN durante la registrazione pubblica | Nessun account ADMIN creato attraverso `/register` |
| AC-19 | Registrazione con email già presente | `409`, nessun duplicato e messaggio nell’interfaccia |
| AC-20 | Login nel flusso minimo attuale | Apertura di `/home`, benvenuto e ruolo a sinistra, marchio centrato, Logout a destra e illustrazione dei lavori in corso |
| AC-21 | Logout frontend con backend attivo | `204`, cancellazione locale del token e ritorno alla login senza avviso di revoca non confermata |
| AC-22 | Logout con backend non raggiungibile | Uscita locale e ritorno alla login con avviso di revoca non confermata |

Questa tabella definisce i risultati attesi, non certifica l’esecuzione di ogni scenario. Il flusso principale di login e registrazione, la predisposizione manuale del primo ADMIN, la home e le prove guidate di logout con backend attivo e spento sono confermati; i casi specifici e i limiti delle verifiche restano distinti in `LoginRegisterAuth_JWT_Flow.md`.

Verificare autorizzazioni nel backend, vincoli del database, validazioni e due percorsi end-to-end: ADMIN inserisce un modello e una tratta; OPERATORI non può accedere alle stesse funzioni. Non basta verificare che un pulsante sia nascosto.

## 13. Evoluzione verso Keycloak

Keycloak sostituirà la gestione locale delle credenziali e l'emissione dei token. Il frontend utilizzerà un flusso standard OpenID Connect adatto a una SPA, da progettare in quella fase. Il backend continuerà a validare access token e ad applicare i permessi applicativi.

Per preparare la migrazione, separare autenticazione e logica di business e usare ID utente stabili. In futuro associare l'utente locale alla coppia issuer e subject dell'identità esterna: non basarsi soltanto sull'email. Conservare i nomi applicativi `ADMIN` e `OPERATORI` tramite un mapping esplicito.

La migrazione richiederà un piano per credenziali, ruoli, sessioni e revoca: il contatore locale `authVersion` non sarà automaticamente presente nei token di Keycloak. Non assumere che basti cambiare l'URL dell'issuer o copiare gli hash delle password.

## 14. Il compito aggiornato del cliente

> Accedi come ADMIN e mostrami la landing page con il menù a sinistra e lo spazio per l’immagine del nostro aeroporto. Dal menù aggiungi un modello di aereo scegliendo dimensione, numero di passeggeri, carburante e uno o due piloti. Poi aggiungi una tratta. Infine accedi come OPERATORE e dimostrami che non puoi raggiungere queste funzioni, neppure chiamando direttamente le API. Per ora non ti chiedo altro.

Ordine di lavoro:

1. Predisporre Java/Spring Boot, Angular/TypeScript e PostgreSQL.
2. **FATTO per il flusso minimo** — Migrazione utenti, registrazione pubblica OPERATORI, predisposizione manuale del primo ADMIN, login JWT, landing comune `/home` con lavori in corso e logout frontend. `/profilo` resta disponibile separatamente. Completamenti di sicurezza e verifiche residue, inclusa la prova dedicata del timeout, sono indicati in `LoginRegisterAuth_JWT_Flow.md`.
3. Realizzare landing ADMIN, menù sinistro, segnaposto dell’immagine e benvenuto OPERATORI.
4. Implementare modello dati, API e form per i modelli di aereo.
5. Implementare modello dati, API e form per le tratte.
6. Verificare i criteri di accettazione e presentare la dimostrazione.

La consegna comprende codice avviabile, migrazioni, istruzioni locali senza segreti e test dei comportamenti essenziali. Le nuove funzionalità saranno concordate e analizzate in una revisione successiva.

## 15. Registro della revisione

**Versione 0.4 — 18 ottobre 2026:** documentate la landing comune `/home`, l’intestazione con benvenuto a sinistra, marchio centrato e Logout a destra, l’illustrazione dell’aereo in costruzione e il tema condiviso. Allineati flusso di login, sessione, schermate e criteri di accettazione al logout frontend completato e alle prove confermate con backend attivo e spento. Timeout configurato a 10 secondi, senza prova dedicata confermata. Aree distinte per ruolo, menu e cambio password frontend restano sviluppi successivi. Revisione limitata alla documentazione, senza nuovi build, test o commit.

**Versione 0.3 — 18 ottobre 2026:** introdotta la registrazione pubblica esclusivamente OPERATORI, con ruolo assegnato dal backend e ritorno alla login senza salvataggio del token. Documentata la predisposizione manuale riservata del primo ADMIN. Allineati API, schermate, criteri di accettazione e stato implementativo al flusso BE/FE completato; registrati il restyling azzurro e l’icona dell’aereo. Landing per ruolo e funzionalità business restano sviluppi successivi. Le verifiche effettive e quelle residue sono distinte nel documento del flusso JWT.

**Versione 0.2:** introdotti landing ADMIN con menù laterale e immagine differita, anagrafica modelli di aereo e inserimento tratte esclusivamente ADMIN. Aggiornati permessi, modello dati, API, schermate e criteri di accettazione. Rinviate gestione voli, pannello utenti e altre funzioni precedentemente proposte, per rispettare il nuovo perimetro.

