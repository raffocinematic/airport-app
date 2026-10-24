# Airport App — Modelli di aereo e tratte

**Aggiornamento:** 24 ottobre 2026.

**Fonte:** [Airport_App_Analisi_Funzionale_Tecnica.md](FROM_CUSTOMER_Airport_App_Analisi_Funzionale_Tecnica.md), versione 0.4 del 18 ottobre 2026. Riferimenti principali: sezioni 1–6, 7, 9–12 e 14.

Questo documento raccoglie il lavoro relativo a modelli di aereo, tratte e relative schermate amministrative. Distingue le richieste del cliente dalle proposte tecniche e dalle decisioni ancora aperte. Registra inoltre l’estensione richiesta dall’utente durante lo sviluppo: modifica, attivazione/disattivazione e cancellazione definitiva delle tratte.

## 1. Stato attuale e legenda

Lo stato deriva dai sorgenti e dalle prove registrate nella sessione. Implementazione, compilazione, verifica frontend con API simulate e prova reale sono evidenze distinte.

- **IMPLEMENTATO** — Funzionalità presente nei sorgenti.
- **VERIFICATO DIRETTAMENTE** — Prova eseguita dall’assistente, con perimetro esplicito.
- **VERIFICATO DALL’UTENTE** — Esito positivo riferito dall’utente; comando o risposta non necessariamente acquisiti dall’assistente.
- **DA VERIFICARE / DA ESEGUIRE** — Scenario non eseguito o privo di evidenza sufficiente.
- **NON IMPLEMENTATO / PARZIALE / DA CONFERMARE** — Funzionalità assente, incompleta o decisione aperta.

| Area | Implementazione | Verifica |
| --- | --- | --- |
| Auth, profilo e logout | Presenti; riferimento in [LoginRegisterAuth_JWT_Flow.md](LoginRegisterAuth_JWT_Flow.md) | Non estendere gli esiti auth a scenari non provati |
| Landing e navigazione | ADMIN su `/admin`, OPERATORI su `/home`, menu destro e guard | Riscontri visivi; navigazione ADMIN verificata in Chromium con auth simulata |
| Tratte backend | GET, POST, PUT, PATCH stato e DELETE; regole ADMIN, validazioni e transazioni | Prove Postman/SQL e primo test di integrazione confermati dall’utente; concorrenza da verificare |
| Schema tratte | Migrazioni `002-create-routes.yaml` e `003-add-route-active.yaml` nel master | Colonna `active` visibile nello screenshot pgAdmin; verifica dei cambiamenti riferita dall’utente |
| Tratte frontend | Lista, creazione, modifica, stato, cancellazione con conferma, paginazione ed errori | Build e 10 controlli Chromium con API simulate superati il 24 ottobre; test Angular del 23 ottobre conservati come evidenza storica |
| Modelli di aereo | NON IMPLEMENTATO: modulo, migrazione, API e pagina assenti | DA ESEGUIRE dopo lo sviluppo |

Le prove odierne nel browser non hanno chiamato il backend reale né modificato PostgreSQL. Le conferme dell’utente sono riportate separatamente nella sezione 7.

## 2. Richiesta del cliente e perimetro

Il gestionale riguarda un piccolo aeroporto privato a Napoli. La richiesta originale prevede che gli ADMIN possano aggiungere e consultare modelli di aereo e tratte attraverso un menu a sinistra. Gli OPERATORI non possono usare queste funzioni. Durante lo sviluppo l'utente ha scelto il menu a destra e il riuso dell'aereo esistente: queste decisioni sono registrate nella sezione 3, senza modificare il documento cliente.

L’analisi originale descrive due anagrafiche indipendenti con inserimento e lista. Il perimetro tratte è stato esteso su richiesta esplicita dell’utente a modifica, attivazione/disattivazione e cancellazione definitiva, ora implementate. I modelli di aereo restano da sviluppare.

| Termine | Significato nel progetto | Esempio |
| --- | --- | --- |
| Modello di aereo | Configurazione riutilizzabile con categoria, capacità, carburante e piloti richiesti | Un modello con 12 posti passeggeri |
| Aeromobile | Singolo mezzo fisico, eventualmente collegato a un modello; rinviato | Aereo identificato da una matricola |
| Rotta o tratta commerciale | Collegamento tra Napoli e una città, indipendente da data e orario | Napoli–Milano |
| Direzione | Verso di percorrenza di una rotta | Napoli → Milano oppure Milano → Napoli |
| Volo | Partenza concreta con data, orario e direzione; rinviato | Napoli → Milano in un giorno e a un’ora specifici |

Le tre rotte iniziali sono Napoli–Milano, Napoli–Roma e Napoli–Londra. I collegamenti nazionali prevedono andata e ritorno. Tre rotte non significano tre voli al giorno. Solo assumendo il ritorno anche da Londra si avrebbero sei direzioni; questa scelta resta da confermare.

## 3. Permessi, landing e navigazione — PARZIALE

### Accessi richiesti

| Funzione | ADMIN | OPERATORI |
| --- | --- | --- |
| Landing e menu amministrativi | Sì | No |
| Inserimento e consultazione dei modelli | Sì | No |
| Consultazione, creazione, modifica, cambio stato ed eliminazione delle tratte | Sì | No |
| Pagina di benvenuto OPERATORI | Non necessaria | Sì |

I permessi devono essere applicati dal backend, anche quando l’utente chiama direttamente le API. Guard Angular e visibilità del menu rendono coerente l’interfaccia, ma non sostituiscono l’autorizzazione sul server. Anche l’ADMIN deve rispettare tutte le validazioni.

### Landing ADMIN prevista dall'analisi

- Dopo la login, l’ADMIN deve raggiungere `/admin`, secondo il flusso per ruolo previsto dall’analisi.
- Menu persistente a sinistra con `SessionLayout`, `Modelli di aereo` e `Tratte`; evidenziare la voce selezionata.
- Area centrale con l’immagine dell’aeroporto, da fornire successivamente dal cliente.
- In attesa dell’asset, mostrare un riquadro neutro con “Immagine dell’aeroporto in arrivo”. Non generare un’immagine sostitutiva.
- La futura immagine deve mantenere le proporzioni e avere un testo alternativo. La sostituzione avviene tramite asset dell’applicazione; non serve un pannello di caricamento immagini.
- Selezionando Modelli di aereo o Tratte, visualizzare lista e form nell’area centrale mantenendo disponibile il menu. `SessionLayout` riporta all’immagine.
- Nell’area utente mostrare nome, ruolo e comando di uscita, riutilizzando i dati del profilo autenticato e il logout esistente.
- Rendere utilizzabile il layout su schermi piccoli; l’analisi consente un menu richiudibile.

### Stato implementato e scelte concordate

- La login indirizza ADMIN a `/admin` e OPERATORI a `/home`.
- `SessionLayout` mantiene intestazione, nome, ruolo, logout e area contenuti con `router-outlet`. Il menu è visibile soltanto agli ADMIN.
- Su scelta esplicita dell'utente, il menu è **a destra**, con `SessionLayout` e `Tratte`; la voce attiva è evidenziata. Sotto 800 px il menu passa sopra il contenuto; sotto 640 px l'intestazione usa due righe.
- Su scelta esplicita dell'utente, `AdminHome` riutilizza `/illustration/airport-work-in-progress.svg`, con testo alternativo e dimensioni responsive, al posto del precedente segnaposto. L'immagine dell'aeroporto fornita dal cliente resta futura; non è stato generato un nuovo asset.
- La pagina Tratte viene visualizzata nell'area contenuti mantenendo il menu. La voce Modelli di aereo è assente.
- `authGuard` protegge la sessione; `adminGuard` è applicato tramite `canActivateChild` ai figli di `/admin`. Il backend dichiara separatamente l'accesso ADMIN alle API tratte. Le risposte `401` senza token e `403` con ruolo OPERATORI per PUT/PATCH/DELETE sono state confermate dall’utente; token scaduti/revocati e navigazione reale per ruolo restano da verificare.

### Schermate: stato attuale

| Percorso Angular | Contenuto | Stato |
| --- | --- | --- |
| `/admin` | Landing ADMIN con aereo esistente e menu | IMPLEMENTATO |
| `/admin/routes` | Lista paginata, form creazione/modifica, stato e cancellazione con conferma | IMPLEMENTATO |
| `/home` | Pagina con aereo in costruzione e avviso di lavori; destinazione dopo login OPERATORI | IMPLEMENTATO; richiede autenticazione, non è una pagina esclusiva OPERATORI |
| `/admin/aircraft-models` | Lista e form Aggiungi modello | NON IMPLEMENTATO |
| `/operatori` | Percorso dedicato previsto dall'analisi | NON IMPLEMENTATO |

Il contenitore di sessione è già riutilizzato da home e area ADMIN. La landing statica non richiede un endpoint dedicato. Il redirect dopo login distingue già i ruoli, ma il percorso `/operatori` e l'asset dell'aeroporto del cliente non sono completati.

## 4. Modelli di aereo — NON IMPLEMENTATO

### Campi e regole funzionali

Dalla voce `Modelli di aereo`, l’ADMIN seleziona `Aggiungi modello`.

| Campo | Obbligatorio | Regola dell’analisi |
| --- | --- | --- |
| Nome del modello | Sì | 1–100 caratteri dopo la rimozione degli spazi esterni; nome proposto dall’analisi per identificare il modello |
| Categoria | Sì | Soltanto `PICCOLO` o `MEDIO` |
| Capacità passeggeri | Sì | Numero intero strettamente positivo, scelto dall’ADMIN |
| Tipo di carburante | Sì | Testo non vuoto, massimo 50 caratteri; catalogo dei valori da concordare |
| Piloti richiesti | Sì | Numero intero, esclusivamente `1` oppure `2` |

- Il nome è univoco ignorando maiuscole/minuscole e spazi esterni. La normalizzazione è calcolata dal backend.
- La categoria non impone una capacità fissa o una soglia automatica. Due modelli della stessa categoria possono avere capacità differenti.
- I piloti non sono conteggiati nella capacità passeggeri. Gli assistenti di bordo non rientrano nella fase attuale.
- Non occorre associare il modello a una tratta.
- Applicare le validazioni anche sul server e i vincoli pertinenti nel database; il form non è sufficiente.

Esempio didattico riportato nell’analisi: “Modello Demo 12”, categoria `PICCOLO`, 12 passeggeri, “Carburante demo”, 2 piloti. Non rappresenta specifiche tecniche di un aereo reale.

### Inserimento e lista

- `Salva` registra il modello, aggiorna la lista e mostra una conferma di successo.
- `Annulla` non salva e non crea record.
- In caso di errore, conservare i dati inseriti e mostrare il problema.
- Durante la richiesta disabilitare temporaneamente `Salva` per evitare invii ripetuti; indicare i campi obbligatori.
- La lista mostra nome, categoria, passeggeri, carburante e piloti richiesti.

### Modello dati proposto

Modulo backend `aircraftmodels`, entità `AircraftModel`, tabella `aircraft_models`.

| Campo dell’entità | Significato e vincolo |
| --- | --- |
| `id` | Identificatore UUID |
| `name` | Nome del modello, secondo i limiti del form |
| `normalizedName` | Valore normalizzato calcolato dal server, univoco anche nel database; non accettato dal client |
| `category` | Valori ammessi `PICCOLO`, `MEDIO` |
| `passengerCapacity` | Intero > 0 |
| `fuelType` | Testo obbligatorio, massimo 50 caratteri |
| `requiredPilots` | Intero 1 oppure 2 |
| `createdAt` | Istante di creazione UTC |

L’unicità deve essere garantita anche se due inserimenti concorrenti superano il controllo preliminare: nessun duplicato persistito e risposta `409` per il conflitto.

## 5. Tratte — IMPLEMENTATO, VERIFICHE PARZIALI

### Campi e regole funzionali

Dalla voce `Tratte`, l’ADMIN seleziona `Aggiungi tratta`.

**Stato:** backend e frontend presenti. Il form usa validazioni reattive, rimozione degli spazi esterni, scelte iniziali vuote per paese e ritorno e controlli sulle regole di Napoli, Milano e Roma. Sono ammessi attualmente soltanto `IT` e `GB`; il catalogo europeo più ampio resta da definire.

| Campo | Obbligatorio | Regola dell’analisi |
| --- | --- | --- |
| Città di origine | Sì | Napoli, precompilata e non modificabile in questa fase; vincolo applicato anche sul server |
| Città di destinazione | Sì | Testo non vuoto, massimo 100 caratteri, diverso da Napoli |
| Paese di destinazione | Sì | Codice selezionato da un elenco coerente con le destinazioni europee; `IT` per Italia, `GB` per Regno Unito |
| Collegamento di ritorno | Sì | Scelta esplicita Sì/No; deve essere Sì per Milano e Roma |

- Il tipo della tratta è derivato: `NAZIONALE` per `IT`, `INTERNAZIONALE` per gli altri paesi. Non deve essere un secondo valore liberamente modificabile.
- Un record rappresenta una rotta commerciale con origine Napoli. Il flag di ritorno abilita la direzione inversa senza creare una seconda rotta.
- Bloccare duplicati sulla combinazione origine, destinazione normalizzata e paese, anche nel database e con richieste concorrenti.
- La destinazione normalizzata è calcolata dal backend; il client non decide i valori usati per l’unicità.
- Non richiedere modello di aereo, aeromobile, date, orari, prezzi o numero di voli giornalieri.

### Dati iniziali

Le tratte iniziali vanno inserite dal gestionale, non costituiscono un limite massimo di record e non richiedono una preconfigurazione automatica.

| Origine | Destinazione | Paese | Tipo derivato | Ritorno |
| --- | --- | --- | --- | --- |
| Napoli | Milano | `IT` | `NAZIONALE` | Sì, richiesto |
| Napoli | Roma | `IT` | `NAZIONALE` | Sì, richiesto |
| Napoli | Londra | `GB` | `INTERNAZIONALE` | Scelta esplicita; requisito del cliente ancora da confermare |

Le destinazioni europee comprendono il Regno Unito: non confondere il perimetro geografico europeo con l’appartenenza all’Unione europea. Gli aeroporti specifici nelle città non sono ancora stati scelti.

### Inserimento, modifica, stato ed eliminazione

- La lista mostra origine, destinazione, paese, tipo derivato, ritorno, stato e azioni. Include sia tratte attive sia disattivate, senza filtri in questa versione.
- `Aggiungi tratta` apre il form vuoto; `Modifica` lo compila usando i dati della riga. L’ID selezionato distingue POST e PUT.
- La modifica riguarda destinazione, paese e ritorno. Conserva ID, origine, data di creazione e stato; è consentita anche su tratte disattivate.
- `Annulla` chiude il form e azzera la selezione senza inviare una scrittura. Gli errori di salvataggio conservano i valori.
- Le nuove tratte sono attive. `Disattiva` invia `{ "active": false }`; `Attiva` invia `{ "active": true }`. Ripetere lo stesso stato è ammesso dal backend.
- L’unicità comprende anche le tratte disattivate: per riutilizzarle occorre riattivarle, non creare duplicati.
- `Elimina` mostra una conferma del browser con origine, destinazione e paese. Annullare non invia DELETE; confermare elimina fisicamente il record, attivo o disattivato.
- I tre comandi sono raggruppati con spaziatura e disposizione flessibile; Elimina è rosso e mantiene focus visibile e comportamento da tastiera.

**Richieste e messaggi:** durante una scrittura vengono bloccate altre azioni e la paginazione; durante un form aperto sono bloccate le azioni sulle righe. Il form viene disabilitato durante POST/PUT. Al successo si chiude e si azzera; dopo la creazione si carica la prima pagina, dopo modifica/stato/eliminazione si ricarica quella corrente. Gli errori delle azioni sono visibili anche fuori dal form. I dettagli `fieldErrors` sono mostrati sui tre campi noti e ripuliti alla modifica dei valori, apertura/reset e nuovo invio.

**Paginazione:** il frontend richiede 10 elementi. Se la pagina non esiste più dopo una cancellazione, `loadValidRoutePage()` calcola `max(0, totalPages - 1)` e carica l’ultima disponibile, ripetendo il controllo se necessario. Il caricamento rimane attivo fino alla risposta finale. Se non rimangono tratte, carica la pagina 0 vuota, mostra il messaggio dedicato e nasconde la paginazione.

Un errore della GET successiva non cancella la conferma della scrittura riuscita. `Riprova` ripete soltanto il caricamento della lista. Le scritture non vengono ritentate automaticamente in caso di esito incerto.

### Modello dati implementato

Modulo backend `routes`, entità `Route`, tabella `routes`. [002-create-routes.yaml](backend/src/main/resources/db/changelog/002-create-routes.yaml) crea la tabella; [003-add-route-active.yaml](backend/src/main/resources/db/changelog/003-add-route-active.yaml) aggiunge `active` booleano NOT NULL con default `true`, anche per le righe esistenti. Entrambe sono incluse nel master. Lo screenshot pgAdmin del 24 ottobre mostra la colonna e i dati; l’assistente non ha rieseguito migrazioni sul database dell’utente.

| Campo dell’entità | Significato e vincolo |
| --- | --- |
| `id` | Identificatore UUID |
| `originCity` | Napoli |
| `destinationCity` | Destinazione obbligatoria, massimo 100 caratteri, diversa da Napoli |
| `normalizedDestinationCity` | Destinazione normalizzata calcolata dal backend |
| `destinationCountryCode` | Codice del paese ammesso |
| `returnEnabled` | Scelta esplicita del ritorno; vincoli nazionali descritti sopra |
| `active` | Stato booleano obbligatorio; default `true` |
| `createdAt` | Istante di creazione UTC |

Vincolo univoco composto: origine + destinazione normalizzata + paese. Il tipo nazionale/internazionale si calcola dal paese senza introdurre un campo modificabile ridondante.

## 6. Backend, API e integrazione frontend — TRATTE IMPLEMENTATE

### Architettura e persistenza

I punti seguenti sono requisiti comuni: implementati per le tratte, ancora da sviluppare per i modelli di aereo. La configurazione attuale usa `spring.jpa.hibernate.ddl-auto=validate`.

- Mantenere Java/Spring Boot, Spring Data JPA/Hibernate, PostgreSQL e Angular/TypeScript già adottati.
- Organizzare ciascuna funzionalità con controller, service, repository e DTO dedicati; non esporre direttamente le entità JPA.
- Usare transazioni per le scritture e migrazioni versionate per lo schema. Modifica, cambio stato e cancellazione acquisiscono la riga con lock pessimista tramite `findByIdForUpdate`; l’unicità rimane garantita anche dal database. Il controllo dei duplicati in modifica esclude l’ID corrente.
- **Allineamento al progetto attuale:** usare Liquibase, già configurato. Flyway compare nella proposta tecnica iniziale, ma non è lo strumento effettivamente adottato.
- Negli ambienti persistenti Hibernate valida lo schema senza modificarlo automaticamente.
- Applicare limiti testuali, valori ammessi, obbligatorietà e vincoli numerici nelle API e nello schema PostgreSQL, oltre all’unicità.
- `AircraftModel` e `Route` restano indipendenti: nessuna relazione o tabella di associazione in questa fase.

### API e stato

| Metodo | Percorso | Accesso | Risultato positivo | Implementazione |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/aircraft-models` | ADMIN | Lista, `200` | NON IMPLEMENTATO |
| POST | `/api/v1/aircraft-models` | ADMIN | Creazione, `201` | NON IMPLEMENTATO |
| GET | `/api/v1/routes` | ADMIN | Lista paginata, `200` | IMPLEMENTATO |
| POST | `/api/v1/routes` | ADMIN | Creazione, `201` e `RouteResponse` | IMPLEMENTATO |
| PUT | `/api/v1/routes/{id}` | ADMIN | Modifica, `200` e `RouteResponse` | IMPLEMENTATO |
| PATCH | `/api/v1/routes/{id}/status` | ADMIN | Cambio stato, `200` e `RouteResponse` | IMPLEMENTATO |
| DELETE | `/api/v1/routes/{id}` | ADMIN | Eliminazione, `204` senza body | IMPLEMENTATO |

POST e PUT ricevono `destinationCity`, `destinationCountryCode` e `returnEnabled`. PATCH riceve soltanto `active`, obbligatorio (`false` è valido). `RouteResponse` contiene `id`, `originCity`, `destinationCity`, `destinationCountryCode`, `routeType`, `returnEnabled`, `active`, `createdAt`. Non serve un GET dedicato per ID per compilare il form: i dati sono già nella lista.

**Paginazione:** `page` parte da 0 e ha default 0; `size` ha default 10 e ammette valori da 1 a 50. Parametri fuori limite producono `400`. Ordinamento `createdAt DESC, id DESC`. `RoutePageResponse` contiene `content`, `page`, `size`, `totalElements`, `totalPages`. Il frontend visualizza il numero pagina partendo da 1.

**Sicurezza:** `SecurityConfig` autorizza esplicitamente ADMIN per tutti e cinque i metodi prima di `.anyRequest().denyAll()`. La configurazione CORS include PUT/PATCH/DELETE. I figli di `/admin` restano protetti dal guard frontend. I modelli di aereo richiederanno regole proprie quando saranno implementati.

**Errori:** `ApiErrorResponse` espone `timestamp`, `status`, `error`, `message`, `fieldErrors`. DTO e regole business producono `400`; UUID non convertibile e body assente/non leggibile sono gestiti esplicitamente. ID valido ma inesistente produce `404`, anche ripetendo DELETE. Autenticazione assente/non valida produce `401`; ruolo insufficiente `403`; duplicati `409`. Un corpo `{}` sulla PATCH è JSON leggibile ma fallisce `@NotNull` prima della ricerca della tratta.

Il servizio controlla l’unicità prima della scrittura e traduce la violazione del vincolo nominato durante `saveAndFlush`/`flush`. Le collisioni concorrenti e l’effettiva serializzazione dei lock rimangono da verificare. Il frontend distingue gli errori applicabili a ogni operazione e non ripete automaticamente le scritture. Il `429` resta relativo al login.

### Sessione e dipendenze auth

**IMPLEMENTATO:** `RouteService` usa HTTP sullo stesso origin; l’interceptor aggiunge il Bearer in memoria e gestisce i `401` della sessione corrente con ritorno alla login. Non viene ripetuta automaticamente alcuna scrittura dopo la login. **VERIFICATO DALL’UTENTE:** `401` senza token per PUT/PATCH/DELETE. **DA VERIFICARE:** token non valido, scaduto o revocato sui flussi reali delle nuove API.

L’analisi prevede che un account con cambio password obbligatorio non possa usare le API business, anche se ADMIN, e completi il cambio prima della landing. **Dipendenza auth non ancora implementata:** password temporanee, scadenza e flag di cambio obbligatorio sono descritti come lavoro futuro in [LoginRegisterAuth_JWT_Flow.md](LoginRegisterAuth_JWT_Flow.md). Non considerarli già disponibili né implementarli implicitamente con queste anagrafiche. Anche i controlli espliciti issuer/audience restano un’attività auth separata.

## 7. Criteri di accettazione e verifiche

Gli ID AC e i risultati attesi mantengono il riferimento all'analisi originale. Le colonne finali separano presenza del codice ed evidenza di esecuzione; le variazioni concordate del layout sono descritte nella sezione 3. Un esito con HTTP simulato non certifica backend, autorizzazioni o persistenza.

| Riferimento | Scenario | Risultato atteso | Implementazione | Verifica |
| --- | --- | --- | --- | --- |
| AC-01 | Login ADMIN nel flusso finale per ruolo | Apertura della landing ADMIN; eventuale cambio password obbligatorio subordinato allo sviluppo auth dedicato | IMPLEMENTATO; dipendenza cambio obbligatorio separata | DA VERIFICARE: login reale per ruolo |
| AC-02 | Landing prima della consegna dell’immagine | Menu sinistro e segnaposto centrale leggibile | IMPLEMENTATO con scelte concordate: menu destro e aereo | DA VERIFICARE: landing completa e schermi piccoli |
| AC-03 | Consegna dell’immagine dell’aeroporto | Asset sostituisce il segnaposto mantenendo navigazione, permessi, proporzioni e testo alternativo | NON IMPLEMENTATO: asset cliente non disponibile | DA ESEGUIRE dopo consegna asset |
| AC-04 | Selezione delle voci Modelli di aereo e Tratte | Lista e form nell’area centrale, menu sempre disponibile | PARZIALE: Tratte presente, Modelli assente | DA VERIFICARE: navigazione completa |
| AC-05 | Login OPERATORI nel flusso per ruolo | Benvenuto senza menu amministrativo | PARZIALE: OPERATORI su /home; /operatori assente | DA VERIFICARE: login OPERATORI |
| AC-06 | OPERATORI apre URL ADMIN o chiama GET/POST di entrambe le anagrafiche | Navigazione negata; API `403`, nessun dato amministrativo restituito o modificato | PARZIALE: controlli tratte presenti, modelli assenti | DA VERIFICARE: URL ADMIN e chiamate API |
| AC-07 | ADMIN salva un modello valido | `201`, record persistito e lista con nome, categoria, passeggeri, carburante e piloti | NON IMPLEMENTATO | DA ESEGUIRE |
| AC-08 | Stessa categoria con capacità differenti | Entrambi i modelli ammessi | NON IMPLEMENTATO | DA ESEGUIRE |
| AC-09 | Capacità zero, negativa o decimale; categoria non ammessa; piloti 0, 3 o decimali; carburante vuoto | `400`, nessun salvataggio | NON IMPLEMENTATO | DA ESEGUIRE |
| AC-10 | Nome modello duplicato, anche con maiuscole o spazi esterni diversi e richieste concorrenti | `409`, nessun duplicato nel database | NON IMPLEMENTATO | DA ESEGUIRE |
| AC-11 | Inserimento Milano e Roma con ritorno e Londra con scelta esplicita | Tre rotte commerciali, tipo derivato corretto; possibile aggiungerne altre | IMPLEMENTATO per le tratte; inserimento iniziale non attestato | DA VERIFICARE: POST e dati reali |
| AC-12 | Tratta duplicata oppure Napoli come destinazione | Duplicato `409`; destinazione non valida `400`; nessun salvataggio | IMPLEMENTATO per le tratte | PARZIALE: gestione frontend del 409 simulato verificata; backend e DB da verificare |
| AC-13 | Annullamento di uno dei form | Nessun nuovo record | PARZIALE: form tratte presente, form modelli assente | PARZIALE: reset e riapertura tratte verificati con HTTP simulato; DB non verificato |
| Integrazione API | Input obbligatori assenti, testi oltre i limiti o paese non ammesso | `400`, nessun salvataggio | IMPLEMENTATO per le tratte | DA VERIFICARE: validazione effettiva delle API; sola gestione frontend del 400 verificata |
| Integrazione tratte | Ritorno non indicato o disabilitato per Milano/Roma; creazioni concorrenti della stessa tratta | Rifiuto dei dati non validi; duplicato `409` e una sola rotta persistita | IMPLEMENTATO per le tratte | DA VERIFICARE: vincoli e concorrenza su PostgreSQL |
| Integrazione liste | Consultazione dopo salvataggio e richieste di paginazione | Record visibile e dimensione massima rispettata dal server | IMPLEMENTATO per le tratte | DA VERIFICARE: salvataggio riuscito, pagine multiple e limite server |
| Integrazione frontend | Errore durante il salvataggio o richiesta in corso | Dati conservati, errore mostrato e invii ripetuti impediti | IMPLEMENTATO per le tratte | PARZIALE: 10 test Angular superati; doppio clic e flusso di successo da verificare |
| Integrazione sessione | Token assente, non valido o revocato sulle nuove API | `401`; nessuna scrittura ritentata automaticamente dopo login | IMPLEMENTATO tramite gestione auth esistente | PARZIALE: assenza token su PUT/PATCH/DELETE confermata dall’utente; altri casi da verificare |

### Verifiche eseguite il 23 ottobre 2026

| Comando eseguito dalla cartella `frontend` | Esito | Perimetro |
| --- | --- | --- |
| `npm.cmd run build` | VERIFICATO: build riuscita | Compilazione frontend; non prova integrazione o persistenza |
| `npm.cmd test -- --watch=false --include=src/app/admin/routes/admin-routes.spec.ts` | VERIFICATO: 1 file, 10 test superati | Componente Angular con richieste intercettate da `HttpTestingController`; nessun backend o database reale |

I test di [admin-routes.spec.ts](frontend/src/app/admin/routes/admin-routes.spec.ts) verificano:

- Messaggi del server sotto i campi, valori conservati, form ancora aperto e controlli riabilitati dopo un `400`.
- Pulizia degli errori dopo modifica dei dati, annullamento/riapertura e nuovo invio.
- Gestione del `409` simulato e conservazione dei valori.
- Fallback per cinque corpi inattesi: corpo nullo, testo, mappa assente, mappa nulla e array al posto della mappa.
- Esclusione di campi sconosciuti e messaggi non testuali o vuoti.

Le prove sono state eseguite durante lo sviluppo precedente a questo aggiornamento documentale. Non sono prove del vincolo univoco reale, delle regole business del backend o del salvataggio riuscito.

### Verifiche del 24 ottobre 2026 eseguite direttamente

- `npm.cmd run build` nella cartella `frontend`: **SUPERATO**, dopo il completamento dell’interfaccia. Compila TypeScript, template e stili; non prova la persistenza.
- Verifica ad hoc con Playwright/Chromium e tutte le API intercettate: **10 controlli SUPERATI**, senza aggiungere una suite permanente. Credenziali e token utilizzati sono dati fittizi della simulazione; nessuna richiesta al backend reale.

| Controllo Chromium | Esito osservato con API simulate |
| --- | --- |
| Modifica | Form precompilato, altre azioni bloccate, PUT con ID e body corretti, messaggio e lista aggiornata |
| Disattivazione e riattivazione | PATCH con `false`/`true`, messaggi e pulsanti coerenti |
| Annullamento eliminazione | Conferma rifiutata, nessuna DELETE inviata |
| Conferma eliminazione | Una DELETE, messaggio positivo e riga rimossa |
| Tastiera e stile | Ordine Modifica/Stato/Elimina, focus visibile, conferma aperta con Invio; pulsante Elimina rosso |
| Richiesta in corso | Altre azioni disabilitate e poi riabilitate |
| Ultima pagina rimossa | Con 11 record, eliminazione dell’unico record della pagina 1 e caricamento automatico della pagina 0 |
| Ultima tratta eliminata | Messaggio elenco vuoto, conferma conservata, paginazione nascosta |
| GET fallita dopo DELETE riuscita | Conferma della cancellazione conservata; Riprova esegue soltanto GET |
| Schermo piccolo | A 390 px pagina senza overflow orizzontale, tabella scorrevole, pulsanti raggiungibili |

Acquisiti e controllati screenshot desktop (1440 px) e mobile (390 px). Artefatti locali temporanei: `%TEMP%/airport-routes-review-20261024` (`desktop.png`, `mobile.png`, `results.json`). Questi controlli non certificano autenticazione reale, vincoli PostgreSQL o integrazione completa Angular → backend → database.

### Verifiche del 24 ottobre confermate dall’utente

| Prova reale o comando | Esito riferito / evidenza disponibile |
| --- | --- |
| Postman e pgAdmin: creazione e modifica, ripetizione della PUT | Funzionanti secondo conferma dell’utente; screenshot SQL con Bologna, normalizzazione `bologna`, paese IT e stato attivo |
| PATCH di disattivazione, ripetizione e riattivazione | Funzionanti, con controllo dello stato nel database riferito dall’utente |
| DELETE, query SQL e seconda DELETE | Prima risposta `204`, riga assente e seconda risposta `404`, confermati dall’utente |
| Input non valido | `400` per UUID non valido, PATCH `{}` e JSON malformato, confermati dall’utente |
| Sicurezza delle scritture | PUT/PATCH/DELETE senza token: `401`; con token OPERATORI: `403`, confermati dall’utente |
| Primo test di integrazione tratte | Esecuzione positiva riferita dall’utente; sorgente [RoueServiceIntegrationTest.java](backend/src/test/java/it/raffo/backend/routes/RoueServiceIntegrationTest.java), classe `RouteServiceIntegrationTest` |

Il test Java crea Torino, modifica in Londra e verifica normalizzazione, paese, ritorno, stato, ID e data invariati, rileggendo PostgreSQL dopo `EntityManager.clear()`. Usa Testcontainers e Liquibase; l’assistente ha letto il sorgente ma non ha rieseguito il comando né acquisito il report Maven.

### Verifiche residue e limiti

- Flusso completo del frontend con backend e PostgreSQL reali: non eseguito direttamente dall’assistente.
- Duplicati reali in modifica, riuso delle tratte disattivate, casi business non provati e concorrenza: restano da verificare.
- Lock concorrenti, tutti i limiti di paginazione API, token scaduti/revocati e comportamenti in caso di modifiche simultanee: restano da verificare.
- Gli altri test automatici backend/frontend delle nuove operazioni sono stati rinviati su richiesta dell’utente. I 10 test Angular del 23 ottobre sono evidenza storica e non sono stati rieseguiti in questo completamento.
- La verifica dei modelli di aereo dipende dal loro sviluppo. Gli scenari auth AC-14/AC-15 restano distinti nel documento JWT.

## 8. Decisioni concordate e questioni aperte

| Tema | Stato e limite della decisione |
| --- | --- |
| Nome del modello | Campo identificativo proposto nell’analisi, con regole di lunghezza e unicità; non compare tra i quattro attributi espressamente elencati dal cliente |
| Carburante | Campo testuale obbligatorio previsto per iniziare; catalogo dei valori consentiti ancora da concordare |
| Ritorno da Londra | Ipotesi di andata e ritorno, non requisito confermato; il form richiede comunque una scelta esplicita |
| Aeroporti specifici | Per ora si identificano le città; gli aeroporti di Milano, Roma e Londra restano da scegliere |
| Paesi europei | Implementazione attuale limitata a `IT` e `GB` in enum, DTO, migrazione e form; eventuale ampliamento del catalogo europeo ancora da definire |
| Posizione del menu | Scelta concordata con l'utente durante lo sviluppo: destra su desktop, sopra il contenuto su schermi piccoli; diversa dal menu sinistro dell'analisi originale |
| Immagine della landing ADMIN | Scelta concordata con l'utente: riuso dell'aereo SVG esistente; immagine dell'aeroporto del cliente ancora futura |
| Permessi operativi | OPERATORI senza funzioni amministrative; future attività operative da definire |
| Orari e frequenza | Non definiti; non dedurre un numero di voli giornalieri dalle rotte iniziali |

Le regole tecniche dettagliate dei campi provengono da RF-05/RF-06 e dal modello dati dell’analisi. Sono la specifica di lavoro proposta, senza trasformare le ipotesi dichiarate nella fonte in nuove conferme del cliente.

## 9. Funzionalità rinviate e fuori perimetro

- Anagrafica modelli di aereo, incluse eventuali successive modifica, eliminazione e disattivazione; filtri avanzati delle anagrafiche. La gestione completa delle tratte descritta sopra rientra invece nel perimetro implementato.
- Flotta fisica, matricole e relativa entità `Aircraft`; assegnazione dei mezzi ai voli.
- Associazioni fra modelli e tratte.
- Programmazione e gestione dei voli, relativa entità `Flight`, date, orari, frequenze e capacità dei singoli voli.
- Vendita biglietti, passeggeri, check-in, imbarco, pagamenti ed entità `Ticket` e `Passenger`.
- Gestione equipaggi e assistenti di bordo.
- Pannello di gestione utenti, dashboard statistiche e schermata del registro attività; restano ammessi i log tecnici necessari alla sicurezza.
- Pannello di caricamento dell’immagine dell’aeroporto.
- Integrazione Keycloak, trattata separatamente nel documento auth.

L’eventuale vendita futura è ipotizzata allo sportello tramite personale, senza portale passeggeri nella prima versione. Non creare adesso schermate, API o tabelle per questi sviluppi rinviati.

## 10. Stato di avanzamento e ordine di ripresa

1. **IMPLEMENTATO** — Gestione tratte completa nel perimetro concordato: lista, creazione, modifica, disattivazione/riattivazione, cancellazione con conferma e recupero della pagina valida.
2. **VERIFICATO DIRETTAMENTE** — Build frontend e 10 controlli Chromium con API simulate del 24 ottobre, oltre alle evidenze storiche del 23 ottobre.
3. **VERIFICATO DALL’UTENTE** — Prove Postman/SQL, autorizzazioni PUT/PATCH/DELETE e primo test di integrazione Java, con i limiti indicati nella sezione 7.
4. **DA VERIFICARE** — Integrazione completa del browser con backend/database reali e scenari residui; non confonderli con la compilazione o la simulazione HTTP.
5. **PARZIALE rispetto all’analisi originale** — Landing ADMIN e navigazione presenti; restano asset cliente, percorso `/operatori` dedicato e anagrafica modelli di aereo.
6. **NON IMPLEMENTATO, fase futura** — Modelli di aereo e gli altri sviluppi elencati nella sezione 9.
7. **DA PREPARARE per la consegna** — Istruzioni di avvio senza segreti e risultati delle verifiche necessarie alla consegna.

Il percorso resta didattico: il codice viene scritto dallo sviluppatore con guida passo per passo, salvo eccezioni esplicitamente autorizzate. Il completamento di HTML/SCSS, gli interventi sul componente e l’aggiornamento documentale sono stati autorizzati espressamente dall’utente.

