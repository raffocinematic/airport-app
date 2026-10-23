# Airport App — Modelli di aereo e tratte

**Aggiornamento:** 23 ottobre 2026.

**Fonte:** [Airport_App_Analisi_Funzionale_Tecnica.md](FROM_CUSTOMER_Airport_App_Analisi_Funzionale_Tecnica.md), versione 0.4 del 18 ottobre 2026. Riferimenti principali: sezioni 1–6, 7, 9–12 e 14.

Questo documento raccoglie il lavoro relativo a modelli di aereo, tratte e relative schermate amministrative. Distingue le richieste del cliente dalle proposte tecniche dell’analisi e dalle decisioni ancora aperte. Non introduce funzionalità ulteriori.

## 1. Stato attuale e legenda

Lo stato seguente deriva dai sorgenti presenti e dalle verifiche registrate nella sessione di sviluppo. La presenza di codice o di una migrazione non dimostra che il comportamento sia stato provato su un backend e un database reali.

- **IMPLEMENTATO (FATTO nel codice)** — Funzionalità presente nei sorgenti; non equivale a una verifica completa.
- **VERIFICATO** — Prova eseguita con esito registrato e perimetro esplicito.
- **DA VERIFICARE / DA ESEGUIRE** — Prova ancora da completare o priva di un esito documentato.
- **NON IMPLEMENTATO** — Funzionalità assente dai sorgenti.
- **PARZIALE** — Solo parte del requisito o delle relative verifiche è completata.
- **DA CONFERMARE** — Proposta ancora aperta con il cliente.

| Area | Implementazione | Verifica |
| --- | --- | --- |
| Auth, profilo e logout | Presenti; riferimenti in [LoginRegisterAuth_JWT_Flow.md](LoginRegisterAuth_JWT_Flow.md) | Restano validi soltanto gli scenari registrati nel documento auth |
| Landing e navigazione | ADMIN su `/admin`, OPERATORI su `/home`; menu ADMIN e guard presenti | Riscontro visivo della pagina Tratte e del menu nello screenshot della sessione; flussi completi per ruolo da verificare |
| Tratte backend | Modulo `routes`, DTO, service, repository, GET/POST e regole ADMIN presenti | Integrazione reale, permessi API e concorrenza da verificare |
| Schema tratte | Migrazione `002-create-routes.yaml` inclusa nel changelog master | Applicazione e vincoli su PostgreSQL da verificare |
| Tratte frontend | Lista, paginazione, inserimento, annullamento ed errori presenti | Build e 10 test Angular con HTTP simulato superati; dettagli nella sezione 7 |
| Modelli di aereo | NON IMPLEMENTATO: modulo, migrazione, API e pagina assenti | DA ESEGUIRE dopo lo sviluppo |
| Modifica e cancellazione tratte | Rinviate, fuori dal perimetro concordato di lista e inserimento | DA ESEGUIRE nella relativa fase futura |

L'aggiornamento documentale registra il codice e le prove già esistenti; non applica migrazioni né certifica prove manuali non riportate.

## 2. Richiesta del cliente e perimetro

Il gestionale riguarda un piccolo aeroporto privato a Napoli. La richiesta originale prevede che gli ADMIN possano aggiungere e consultare modelli di aereo e tratte attraverso un menu a sinistra. Gli OPERATORI non possono usare queste funzioni. Durante lo sviluppo l'utente ha scelto il menu a destra e il riuso dell'aereo esistente: queste decisioni sono registrate nella sezione 3, senza modificare il documento cliente.

Il documento descrive due anagrafiche indipendenti, ciascuna con inserimento e lista minima di consultazione. Il percorso di sviluppo corrente riguarda lista e inserimento delle tratte; i modelli di aereo restano da implementare. Le liste permettono di consultare i dati salvati.

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
| Inserimento e consultazione delle tratte | Sì | No |
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
- `authGuard` protegge la sessione; `adminGuard` è applicato tramite `canActivateChild` ai figli di `/admin`. Il backend dichiara separatamente l'accesso ADMIN alle API tratte. Le prove complete di autorizzazione restano da eseguire.

### Schermate: stato attuale

| Percorso Angular | Contenuto | Stato |
| --- | --- | --- |
| `/admin` | Landing ADMIN con aereo esistente e menu | IMPLEMENTATO |
| `/admin/routes` | Lista paginata e form Aggiungi tratta | IMPLEMENTATO |
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

### Inserimento e lista

- `Salva` registra la tratta e aggiorna la lista con conferma; `Annulla` non salva.
- In caso di errore conservare i valori e mostrare il problema; disabilitare `Salva` durante la richiesta e indicare i campi obbligatori.
- La lista mostra origine, destinazione, paese, tipo derivato e disponibilità del ritorno.
- Consentire in seguito altri inserimenti oltre alle tre tratte iniziali, rispettando il perimetro geografico.

**Comportamento implementato:** lista a 10 elementi per pagina, stati di caricamento/vuoto/errore e comando Riprova; pulsanti Precedente/Successiva con controllo dei limiti. Durante il POST i campi, Salva, Annulla e la paginazione sono bloccati; il metodo di invio impedisce richieste ripetute. Al successo il form viene resettato e chiuso, compare la conferma e viene richiesta la prima pagina. Un errore della GET successiva rimane distinto dalla conferma del POST. Gli errori mantengono i dati; i dettagli `fieldErrors` sono mostrati accanto ai tre campi e ripuliti quando i valori cambiano, al reset e al nuovo invio. Le prove con HTTP simulato non dimostrano la persistenza reale.

### Modello dati implementato

Modulo backend `routes`, entità `Route`, tabella `routes`. La creazione dello schema è definita in [002-create-routes.yaml](backend/src/main/resources/db/changelog/002-create-routes.yaml), inclusa nel changelog master; la sua applicazione al database non è stata verificata in questa sessione.

| Campo dell’entità | Significato e vincolo |
| --- | --- |
| `id` | Identificatore UUID |
| `originCity` | Napoli |
| `destinationCity` | Destinazione obbligatoria, massimo 100 caratteri, diversa da Napoli |
| `normalizedDestinationCity` | Destinazione normalizzata calcolata dal backend |
| `destinationCountryCode` | Codice del paese ammesso |
| `returnEnabled` | Scelta esplicita del ritorno; vincoli nazionali descritti sopra |
| `createdAt` | Istante di creazione UTC |

Vincolo univoco composto: origine + destinazione normalizzata + paese. Il tipo nazionale/internazionale si calcola dal paese senza introdurre un campo modificabile ridondante.

## 6. Backend, API e integrazione frontend — TRATTE IMPLEMENTATE

### Architettura e persistenza

I punti seguenti sono requisiti comuni: implementati per le tratte, ancora da sviluppare per i modelli di aereo. La configurazione attuale usa `spring.jpa.hibernate.ddl-auto=validate`.

- Mantenere Java/Spring Boot, Spring Data JPA/Hibernate, PostgreSQL e Angular/TypeScript già adottati.
- Organizzare ciascuna funzionalità con controller, service, repository e DTO dedicati; non esporre direttamente le entità JPA.
- Usare transazioni per gli inserimenti e migrazioni versionate per lo schema.
- **Allineamento al progetto attuale:** usare Liquibase, già configurato. Flyway compare nella proposta tecnica iniziale, ma non è lo strumento effettivamente adottato.
- Negli ambienti persistenti Hibernate valida lo schema senza modificarlo automaticamente.
- Applicare limiti testuali, valori ammessi, obbligatorietà e vincoli numerici nelle API e nello schema PostgreSQL, oltre all’unicità.
- `AircraftModel` e `Route` restano indipendenti: nessuna relazione o tabella di associazione in questa fase.

### API e stato

| Metodo | Percorso | Accesso previsto | Risultato atteso | Implementazione |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/aircraft-models` | ADMIN | Lista modelli, `200` | NON IMPLEMENTATO |
| POST | `/api/v1/aircraft-models` | ADMIN | Inserimento modello valido, `201` | NON IMPLEMENTATO |
| GET | `/api/v1/routes` | ADMIN | Lista tratte, `200` | IMPLEMENTATO; integrazione reale DA VERIFICARE |
| POST | `/api/v1/routes` | ADMIN | Inserimento tratta valida, `201` | IMPLEMENTATO; integrazione reale DA VERIFICARE |

**Paginazione tratte implementata:** `page` parte da `0` e ha valore predefinito `0`; `size` ha valore predefinito `10` ed è ammesso da `1` a `50`. Parametri fuori limite producono `400` con dettagli sui campi. L'ordinamento è `createdAt DESC, id DESC`. `RoutePageResponse` contiene `content`, `page`, `size`, `totalElements`, `totalPages`. Il frontend richiede 10 elementi e visualizza il numero pagina partendo da 1. La paginazione dei modelli resta da sviluppare.

Le API tratte restano a `/api/v1/routes`: `SecurityConfig` contiene già le regole esplicite ADMIN per GET e POST prima di `.anyRequest().denyAll()`. I figli di `/admin` sono protetti dal guard frontend. Per le API dei modelli, ancora assenti, le regole specifiche restano da introdurre con il relativo modulo. Configurazione presente non significa autorizzazioni già provate sulle nuove API.

Convenzioni per le nuove API: `400` per input non valido, `401` per autenticazione assente o token non valido, `403` per ruolo insufficiente, `409` per duplicati. Gestire i conflitti di unicità anche quando emergono al momento del salvataggio concorrente.

**Errori implementati:** `ApiErrorResponse` espone `timestamp`, `status`, `error`, `message`, `fieldErrors`. Le validazioni del DTO e `InvalidRouteException` producono `400`; i duplicati producono `409`. Il service esegue il controllo preliminare e traduce anche la violazione del vincolo univoco nominato durante `saveAndFlush`; la concorrenza resta DA VERIFICARE. Il frontend distingue `400`, `401`, `403`, `409` ed errori di rete/server; per `400` accetta soltanto messaggi testuali dei tre campi noti, usando interpolazione testuale e fallback generale per corpi inattesi. Non esiste un campo aggiuntivo di codice applicativo. Il `429` resta relativo al login.

### Sessione e dipendenze auth

**IMPLEMENTATO:** `RouteService` usa HTTP sullo stesso origin, l'interceptor esistente aggiunge il Bearer in memoria e gestisce i `401` della sessione corrente con ritorno alla login e messaggio di sessione scaduta. Non è previsto un nuovo invio automatico del POST dopo la login. **DA VERIFICARE:** comportamento effettivo sulle nuove API con token assente, non valido, scaduto o revocato.

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
| Integrazione sessione | Token assente, non valido o revocato sulle nuove API | `401`; nessuna creazione automatica ritentata dopo una nuova login | IMPLEMENTATO tramite gestione auth esistente | DA VERIFICARE sulle nuove API |

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

### Verifiche residue

Restano DA VERIFICARE: applicazione della migrazione su PostgreSQL; inserimento e lettura reali, con riscontro SQL; duplicati e vincoli, anche concorrenti; paginazione con più di 10 record e limiti API; annullamento e doppio clic nel browser; POST riuscito seguito da GET fallita; permessi ADMIN/OPERATORI e sessione sulle nuove API; layout e uso da tastiera su schermi piccoli.

Non risultano test dedicati alle tratte in `backend/src/test` né scenari Playwright dedicati in `test-automation/tests`. La dimostrazione completa richiesta dall'analisi comprende anche i modelli di aereo, ancora assenti; per il perimetro corrente occorre dimostrare ADMIN che inserisce una tratta e OPERATORI bloccato anche chiamando direttamente le API.

Gli AC-14 e AC-15 dell'analisi riguardano inoltre token e revoca. Le verifiche auth già registrate restano distinte nel documento JWT e non certificano automaticamente le API tratte. Questo aggiornamento modifica soltanto la documentazione: registra build e test Angular precedenti, senza aggiungere o eseguire nuove prove Java, Angular, Playwright o database.

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

- Modifica, eliminazione e disattivazione di modelli e tratte; filtri avanzati delle anagrafiche.
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

1. **IMPLEMENTATO, PARZIALE rispetto all'analisi** — Landing ADMIN, menu destro, immagine esistente, redirect per ruolo e guard. Restano asset cliente, pagina `/operatori` dedicata e voce Modelli.
2. **IMPLEMENTATO** — Tratte: migrazione, modello dati, repository, service, DTO, API ADMIN, lista paginata e form. Non richiede l'implementazione di modifica o cancellazione per chiudere il perimetro corrente.
3. **VERIFICATO con limiti espliciti** — Build frontend e 10 test Angular sugli errori del form, come registrato nella sezione 7.
4. **DA VERIFICARE — prossimo passo** — Flusso reale form → API → PostgreSQL → lista, dati iniziali, query SQL, duplicati e paginazione.
5. **DA VERIFICARE** — Permessi API e navigazione per ruolo, sessione scaduta/revocata e casi residui della sezione 7; documentare gli esiti.
6. **NON IMPLEMENTATO, fase successiva** — Anagrafica modelli di aereo completa.
7. **DA PREPARARE per la consegna** — Istruzioni di avvio locale senza segreti, migrazioni e risultati delle prove essenziali.

Il percorso resta didattico: il codice viene scritto dallo sviluppatore con guida passo per passo, salvo eccezioni esplicitamente autorizzate. Questo intervento aggiorna soltanto `Aircraft_Routes.md`; conserva i requisiti originali distinguendoli dalle scelte concordate e dalle evidenze di verifica.
