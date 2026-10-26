# Airport App — Modelli di aereo e tratte

**Aggiornamento:** 26 ottobre 2026.

**Fonte:** [Airport_App_Analisi_Funzionale_Tecnica.md](FROM_CUSTOMER_Airport_App_Analisi_Funzionale_Tecnica.md), versione 0.4 del 18 ottobre 2026. Riferimenti principali: sezioni 1–6, 7, 9–12 e 14.

Questo documento raccoglie il lavoro relativo a modelli di aereo, tratte e relative schermate amministrative. Distingue le richieste del cliente dalle proposte tecniche e dalle decisioni ancora aperte. Registra inoltre le estensioni richieste dall’utente durante lo sviluppo: modifica, attivazione/disattivazione e cancellazione definitiva delle tratte, catalogo geografico e paese ricavato dalla città selezionata.

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
| Tratte backend | CRUD e stato; destinazione risolta per ID e paese derivato dal catalogo | Implementazione letta nei sorgenti; prove precedenti al catalogo conservate come evidenza storica |
| Schema tratte e catalogo | Migrazioni 002–009 presenti nel master; associazione obbligatoria alla città | Applicazione delle migrazioni 007–009 non attestata da un output di esecuzione acquisito |
| Catalogo geografico | Città e alias IT/GB; importatore manuale con SHA-256 | Output condiviso dall’utente: 90414 città elaborate e 96894 alias scritti |
| Tratte frontend | CRUD, ricerca paginata, selezione della città e paese derivato | Sorgenti aggiornati; nessuna nuova evidenza di build o del flusso completo dopo il passaggio al catalogo |
| Modelli di aereo | NON IMPLEMENTATO: modulo, migrazione, API e pagina assenti | DA ESEGUIRE dopo lo sviluppo |

Le prove browser del 23–24 ottobre riguardano la versione precedente al catalogo geografico. Non certificano il nuovo contratto basato su `destinationCityId`. Le evidenze disponibili sono distinte nella sezione 7.

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

## 5. Tratte con catalogo geografico — IMPLEMENTATO NEI SORGENTI

### Regole funzionali

La destinazione è una città selezionata dal catalogo locale GeoNames. Il backend riceve il suo ID e ricava nome e paese dal catalogo.

| Dato | Regola |
| --- | --- |
| Origine | Napoli, assegnata dal backend e non modificabile |
| Destinazione | ID obbligatorio, positivo e appartenente al catalogo |
| Paese | Derivato dalla città selezionata; attualmente `IT` oppure `GB` |
| Ritorno | Scelta esplicita; obbligatorio per gli ID di Milano e Roma |
| Tipo | `NAZIONALE` per IT, `INTERNAZIONALE` per GB |
| Stato iniziale | Attiva |

Le regole identificano le città tramite ID:

| Città | ID GeoNames | Regola |
| --- | --- | --- |
| Napoli | 3172394 | Destinazione vietata |
| Milano | 3173435 | Ritorno obbligatorio |
| Roma | 3169070 | Ritorno obbligatorio |

Le città omonime con ID diversi sono destinazioni distinte. L’unicità comprende origine e ID della destinazione, anche per le tratte disattivate.

Un record rappresenta una rotta commerciale. Il ritorno abilita la direzione inversa senza creare un secondo record. Date, orari, aeromobili e aeroporti specifici restano fuori perimetro.

### Dati iniziali

Le tratte iniziali vanno inserite dal gestionale, non costituiscono un limite massimo di record e non richiedono una preconfigurazione automatica.

| Origine | Destinazione | ID GeoNames | Paese | Tipo derivato | Ritorno |
| --- | --- | --- | --- | --- | --- |
| Napoli | Milano | 3173435 | IT | NAZIONALE | Sì, richiesto |
| Napoli | Roma | 3169070 | IT | NAZIONALE | Sì, richiesto |
| Napoli | Londra | 2643743 | GB | INTERNAZIONALE | Scelta esplicita; requisito del cliente ancora da confermare |

Il perimetro geografico comprende il Regno Unito; l’ampliamento ad altri paesi resta da definire.

### Interfaccia amministrativa

- Ricerca per inizio del nome o di un alias, con testo da 2 a 100 caratteri.
- Attesa di 300 ms prima della richiesta e annullamento della ricerca precedente.
- Risultati paginati, fino a 20 città per pagina.
- Visualizzazione di nome, paese e informazioni amministrative disponibili.
- Selezione esplicita della città: il solo testo digitato non basta.
- Cambio del testo con cancellazione immediata della precedente selezione.
- Paese mostrato come informazione della città selezionata.
- Modifica precompilata usando ID e dati già ricevuti nella lista.

Il form usa validazioni Angular; il backend applica autonomamente le validazioni e le regole di business.

### Gestione delle tratte

- Lista paginata con tratte attive e disattivate e contesto amministrativo.
- Creazione e modifica di destinazione e ritorno.
- La modifica conserva ID, origine, data di creazione e stato; è consentita anche su tratte disattivate.
- Disattivazione e riattivazione senza eliminare la riga; ripetere lo stesso stato è ammesso.
- L’unicità comprende le tratte disattivate: per riutilizzarle occorre riattivarle.
- Eliminazione definitiva dopo conferma del browser; annullare non invia DELETE.
- Annullamento del modulo senza scritture e con reset dei dati.
- Conservazione dei valori in caso di errore di salvataggio.
- Blocco dei controlli durante il salvataggio e delle azioni sulle righe durante un form aperto.
- Errori del server riconosciuti per `destinationCityId` e `returnEnabled`, ripuliti alla modifica dei valori e al reset.

Dopo la creazione si carica la prima pagina delle tratte. Dopo modifica, cambio stato ed eliminazione si ricarica quella corrente. Se una cancellazione rende inesistente la pagina, viene recuperata l’ultima disponibile. L’elenco vuoto nasconde la paginazione.

Una GET fallita dopo una scrittura riuscita conserva il messaggio di successo. Le scritture non vengono ripetute automaticamente; il comando Riprova dell’elenco ripete soltanto GET.

### Modello dati

L’entità `Route` conserva:

| Campo | Significato |
| --- | --- |
| `id` | UUID della tratta |
| `originCity` | Origine fissa Napoli |
| `destination` | Associazione obbligatoria a DestinationCity |
| `returnEnabled` | Disponibilità del ritorno |
| `active` | Stato della tratta, inizialmente true |
| `createdAt` | Istante di creazione |

Nome, paese e tipo della destinazione sono ricavati dalla città collegata.

La tabella `routes` usa `destination_city_id` come foreign key. Le vecchie colonne `destination_city`, `normalized_destination_city` e `destination_country_code` vengono eliminate dalla migrazione 009. I campi descrittivi restano disponibili nei DTO di risposta.

Vincoli principali definiti nelle migrazioni:

- Foreign key verso `destination_cities` con cancellazione RESTRICT.
- `destination_city_id NOT NULL`.
- `uk_routes_origin_destination_city` su `origin_city` e `destination_city_id`.
- Origine fissa Napoli e divieto dell’ID di Napoli come destinazione.
- Ritorno obbligatorio per gli ID di Milano e Roma.

### Migrazioni

| Numero | Scopo |
| --- | --- |
| 002 | Creazione iniziale delle tratte |
| 003 | Aggiunta dello stato active |
| 004 | Catalogo delle città |
| 005 | Alias delle città |
| 006 | Collegamento inizialmente nullable tra tratta e città |
| 007 | Associazione delle tratte esistenti agli ID approvati |
| 008 | Destinazione obbligatoria e vincoli basati sugli ID |
| 009 | Rimozione delle vecchie colonne geografiche delle tratte |

Le migrazioni sono incluse nel master in questo ordine. La loro presenza nei sorgenti non attesta l’applicazione al database locale.

Per le tratte esistenti, la 007 usa le associazioni approvate: Milano 3173435, Roma 3169070, Londra 2643743, Palermo 2523920 e Torino 3165524. La migrazione interrompe l’operazione in presenza di dati che non possono essere riconciliati secondo queste associazioni.

### Catalogo geografico

Il catalogo locale comprende città e alias per Italia e Regno Unito. Gli ID delle città coincidono con gli ID GeoNames.

L’importazione è un comando manuale con controllo SHA-256 e scrittura transazionale. Il normale avvio del backend non scarica né importa i dati.

Procedura, fonte, licenza e risultato dell’importazione sono documentati nel [README del catalogo](backend/geodata/geonames/README.md).

## 6. Backend, API e integrazione frontend

### Architettura

Controller, service, repository e DTO hanno responsabilità separate. Le entità JPA non vengono esposte direttamente nelle risposte HTTP.

Liquibase gestisce lo schema; Hibernate usa `ddl-auto=validate`. Flyway compare nella proposta tecnica iniziale, ma non è lo strumento adottato.

Le operazioni di scrittura sono transazionali. Modifica, cambio stato ed eliminazione acquisiscono la tratta tramite `findByIdForUpdate` con lock pessimista.

Il controllo preliminare dei duplicati usa l’ID della destinazione. In modifica esclude la tratta corrente. Il vincolo SQL garantisce l’unicità anche tra richieste concorrenti; la violazione del vincolo nominato viene tradotta in HTTP 409. L’esecuzione degli scenari concorrenti non è attestata.

La lista usa un EntityGraph per caricare anche la destinazione. I DTO vengono costruiti all’interno delle transazioni del service, con `open-in-view=false`.

`AircraftModel` e `Route` restano indipendenti: nessuna relazione o tabella di associazione in questa fase.

### API

| Metodo | Percorso | Accesso | Risultato positivo |
| --- | --- | --- | --- |
| GET | `/api/v1/destination-cities` | ADMIN | Ricerca paginata, 200 |
| GET | `/api/v1/routes` | ADMIN | Lista paginata, 200 |
| POST | `/api/v1/routes` | ADMIN | Creazione, 201 |
| PUT | `/api/v1/routes/{id}` | ADMIN | Modifica, 200 |
| PATCH | `/api/v1/routes/{id}/status` | ADMIN | Cambio stato, 200 |
| DELETE | `/api/v1/routes/{id}` | ADMIN | Eliminazione, 204 senza body |

GET e POST `/api/v1/aircraft-models` restano NON IMPLEMENTATE.

### Richieste di creazione e modifica

POST e PUT ricevono:

```json
{
  "destinationCityId": 3173435,
  "returnEnabled": true
}
```

`destinationCityId` deve essere positivo ed esistere nel catalogo. `returnEnabled` è obbligatorio; false è un valore valido quando non contrasta con le regole di Milano e Roma.

Nome e paese non sono campi del contratto di scrittura. Il backend li ricava dalla città identificata dall’ID.

PATCH riceve soltanto `active`, obbligatorio:

```json
{
  "active": false
}
```

### Risposte

`RouteResponse` contiene:

- `id` e `originCity`.
- `destinationCityId`, `destinationCity` e `destinationCountryCode`.
- `destinationAdmin1Name` e `destinationAdmin2Name`, eventualmente null.
- `routeType`, `returnEnabled`, `active` e `createdAt`.

POST, PUT e PATCH restituiscono RouteResponse. GET delle tratte restituisce `RoutePageResponse`, con `content`, `page`, `size`, `totalElements` e `totalPages`.

La ricerca delle città restituisce `DestinationCityPageResponse` con gli stessi campi di paginazione. Ogni città contiene `id`, `name`, `countryCode`, `admin1Name` e `admin2Name`.

La modifica non richiede un GET dedicato della tratta: i dati necessari sono già presenti nella lista.

### Ricerca e paginazione

| API | Parametri e limiti |
| --- | --- |
| Tratte | page da 0, default 0; size default 10, minimo 1, massimo 50 |
| Città | q normalizzata da 2 a 100 caratteri; page da 0, default 0; size default 20, minimo 1, massimo 20 |

Le tratte sono ordinate per `createdAt DESC, id DESC`. Le città sono ordinate per nome, paese e ID. Il frontend mostra i numeri di pagina partendo da 1.

La ricerca usa gli alias normalizzati e confronta l’inizio del nome. La normalizzazione elimina i segni diacritici, uniforma gli spazi e converte in minuscolo. I caratteri speciali LIKE vengono trattati come testo. I parametri fuori limite producono HTTP 400.

### Sicurezza ed errori

SecurityConfig riserva ad ADMIN tutte le API elencate. I guard Angular proteggono la navigazione amministrativa. L’interceptor aggiunge il Bearer alle richieste sullo stesso origin. La configurazione CORS include PUT, PATCH e DELETE.

`ApiErrorResponse` contiene `timestamp`, `status`, `error`, `message` e `fieldErrors`.

| Stato | Significato |
| --- | --- |
| 400 | Richiesta non valida, destinazione assente/inesistente, regola business violata o parametri di ricerca non validi |
| 401 | Autenticazione assente o non valida |
| 403 | Ruolo insufficiente |
| 404 | UUID valido di una tratta inesistente |
| 409 | Tratta duplicata |

Un `destinationCityId` inesistente produce 400. Un ID di tratta inesistente produce 404 per le operazioni sulla tratta, anche ripetendo DELETE. UUID non convertibile e body assente o non leggibile producono 400. Un oggetto JSON `{}` sulla PATCH non soddisfa l’obbligatorietà di `active`.

Il frontend distingue gli errori applicabili alle diverse operazioni. Il 429 resta relativo al login.

### Sessione e dipendenze auth

**IMPLEMENTATO:** `RouteService` usa HTTP sullo stesso origin; l’interceptor aggiunge il Bearer in memoria e gestisce i `401` della sessione corrente con ritorno alla login. Non viene ripetuta automaticamente alcuna scrittura dopo la login. **VERIFICATO DALL’UTENTE:** `401` senza token per PUT/PATCH/DELETE. **DA VERIFICARE:** token non valido, scaduto o revocato sui flussi reali delle nuove API.

L’analisi prevede che un account con cambio password obbligatorio non possa usare le API business, anche se ADMIN, e completi il cambio prima della landing. **Dipendenza auth non ancora implementata:** password temporanee, scadenza e flag di cambio obbligatorio sono descritti come lavoro futuro in [LoginRegisterAuth_JWT_Flow.md](LoginRegisterAuth_JWT_Flow.md). Non considerarli già disponibili né implementarli implicitamente con queste anagrafiche. Anche i controlli espliciti issuer/audience restano un’attività auth separata.

## 7. Criteri di accettazione e verifiche

### Aggiornamento geografico del 26 ottobre 2026

- **IMPLEMENTATO NEI SORGENTI:** catalogo, importatore, API di ricerca, destinazione delle tratte per ID, migrazioni e selezione nel frontend.
- **EVIDENZA DI ESECUZIONE:** output dell’importazione condiviso dall’utente, con 90414 città elaborate, 96894 alias scritti ed exit code 0.
- Il risultato dell’importazione non certifica l’esecuzione delle migrazioni successive o il flusso completo Angular → backend → PostgreSQL.
- Le verifiche del 23–24 ottobre riportate sotto sono evidenze storiche della versione precedente al catalogo.
- Per la nuova versione non è stato acquisito un esito di build o di esecuzione del flusso completo.

### Criteri ed evidenze pregresse

Gli ID AC e i risultati attesi mantengono il riferimento all'analisi originale. Le colonne finali separano presenza del codice ed evidenza di esecuzione; le variazioni concordate del layout sono descritte nella sezione 3. Un esito con HTTP simulato non certifica backend, autorizzazioni o persistenza.

| Riferimento | Scenario | Risultato atteso | Implementazione | Verifica |
| --- | --- | --- | --- | --- |
| AC-01 | Login ADMIN nel flusso finale per ruolo | Apertura della landing ADMIN; eventuale cambio password obbligatorio subordinato allo sviluppo auth dedicato | IMPLEMENTATO; dipendenza cambio obbligatorio separata | PARZIALE: accesso ADMIN e redirect verificati in Chromium con auth simulata; login reale per ruolo non certificata da questa prova |
| AC-02 | Landing prima della consegna dell’immagine | Menu sinistro e segnaposto centrale leggibile | IMPLEMENTATO con scelte concordate: menu destro e aereo | PARZIALE: layout di sessione della pagina Tratte controllato a 1440 e 390 px con API simulate; landing completa da verificare |
| AC-03 | Consegna dell’immagine dell’aeroporto | Asset sostituisce il segnaposto mantenendo navigazione, permessi, proporzioni e testo alternativo | NON IMPLEMENTATO: asset cliente non disponibile | DA ESEGUIRE dopo consegna asset |
| AC-04 | Selezione delle voci Modelli di aereo e Tratte | Lista e form nell’area centrale, menu sempre disponibile | PARZIALE: Tratte presente, Modelli assente | PARZIALE: navigazione ADMIN → Tratte verificata in Chromium con auth/API simulate; Modelli dipende dallo sviluppo |
| AC-05 | Login OPERATORI nel flusso per ruolo | Benvenuto senza menu amministrativo | PARZIALE: OPERATORI su /home; /operatori assente | Login API OPERATORI utilizzata nelle prove confermate dall’utente; navigazione frontend reale OPERATORI da verificare |
| AC-06 | OPERATORI apre URL ADMIN o chiama GET/POST di entrambe le anagrafiche | Navigazione negata; API `403`, nessun dato amministrativo restituito o modificato | PARZIALE: controlli tratte presenti, modelli assenti | URL ADMIN e GET/POST del criterio originale da verificare; `403` su PUT/PATCH/DELETE confermati dall’utente nell’estensione tratte |
| AC-07 | ADMIN salva un modello valido | `201`, record persistito e lista con nome, categoria, passeggeri, carburante e piloti | NON IMPLEMENTATO | DA ESEGUIRE |
| AC-08 | Stessa categoria con capacità differenti | Entrambi i modelli ammessi | NON IMPLEMENTATO | DA ESEGUIRE |
| AC-09 | Capacità zero, negativa o decimale; categoria non ammessa; piloti 0, 3 o decimali; carburante vuoto | `400`, nessun salvataggio | NON IMPLEMENTATO | DA ESEGUIRE |
| AC-10 | Nome modello duplicato, anche con maiuscole o spazi esterni diversi e richieste concorrenti | `409`, nessun duplicato nel database | NON IMPLEMENTATO | DA ESEGUIRE |
| AC-11 | Inserimento Milano e Roma con ritorno e Londra con scelta esplicita | Tre rotte commerciali, tipo derivato corretto; possibile aggiungerne altre | IMPLEMENTATO per le tratte | PARZIALE: righe Milano, Roma e Londra visibili nello screenshot pgAdmin; creazione di una tratta di prova confermata dall’utente. Inserimento iniziale completo tramite frontend non attestato |
| AC-12 | Tratta duplicata oppure Napoli come destinazione | Duplicato `409`; destinazione non valida `400`; nessun salvataggio | IMPLEMENTATO per le tratte | PARZIALE: gestione frontend del `409` simulato verificata il 23 ottobre; rifiuto reale di duplicati e destinazione Napoli da verificare |
| AC-13 | Annullamento di uno dei form | Nessun nuovo record | PARZIALE: form tratte presente, form modelli assente | PARZIALE: reset e riapertura tratte verificati con HTTP simulato il 23 ottobre; persistenza e form modelli non verificati |
| Integrazione API | destinationCityId assente, non positivo o inesistente; ritorno assente; query fuori limite | 400, nessun salvataggio | IMPLEMENTATO nei sorgenti | Nessuna nuova evidenza di esecuzione registrata per il contratto geografico |
| Integrazione tratte | Ritorno non indicato o disabilitato per Milano/Roma; creazioni concorrenti della stessa tratta | Rifiuto dei dati non validi; duplicato `409` e una sola rotta persistita | IMPLEMENTATO per le tratte | DA VERIFICARE: casi business e concorrenza PostgreSQL; le prove CRUD positive non certificano questi rifiuti |
| Integrazione liste | Consultazione dopo salvataggio e richieste di paginazione | Record visibile e dimensione massima rispettata dal server | IMPLEMENTATO per le tratte | PARZIALE: lista aggiornata dopo PUT/PATCH/DELETE, più pagine e recupero dell’ultima pagina verificati in Chromium con API simulate; limite server e flusso browser/backend/DB reali da verificare |
| Integrazione frontend | Errore durante il salvataggio o richiesta in corso | Dati conservati, errore mostrato e invii ripetuti impediti | IMPLEMENTATO per le tratte | PARZIALE: 10 test Angular storici del 23 ottobre; flussi positivi PUT/PATCH/DELETE, blocco azioni e GET fallita dopo DELETE verificati in Chromium il 24 ottobre. Doppio clic e ulteriori errori con backend reale da verificare |
| Integrazione sessione | Token assente, non valido o revocato sulle nuove API | `401`; nessuna scrittura ritentata automaticamente dopo login | IMPLEMENTATO tramite gestione auth esistente | PARZIALE: assenza token su PUT/PATCH/DELETE confermata dall’utente; token non valido, scaduto o revocato e ripresa della sessione da verificare |
| Estensione: modifica | ADMIN modifica una tratta esistente | PUT `200`, dati aggiornati, ID/origine/data/stato conservati | IMPLEMENTATO | Prova Postman/SQL e test di integrazione confermati dall’utente; form precompilato, body PUT e aggiornamento lista verificati in Chromium con API simulate |
| Estensione: stato | ADMIN disattiva e riattiva una tratta | PATCH `200`, riga conservata, stato aggiornato | IMPLEMENTATO | PATCH, ripetizione dello stesso stato e riscontro DB confermati dall’utente; pulsanti, messaggi e body `false`/`true` verificati in Chromium con API simulate |
| Estensione: eliminazione | ADMIN annulla o conferma l’eliminazione | Annullamento senza DELETE; conferma con `204`, riga rimossa e pagina valida | IMPLEMENTATO | DELETE/SQL/seconda DELETE `404` confermati dall’utente; conferma e annullamento, lista vuota, recupero pagina e GET fallita successiva verificati in Chromium con API simulate |

### Verifiche eseguite il 23 ottobre 2026

| Comando eseguito dalla cartella `frontend` | Esito | Perimetro |
| --- | --- | --- |
| `npm.cmd run build` | VERIFICATO: build riuscita | Compilazione frontend; non prova integrazione o persistenza |
| `npm.cmd test -- --watch=false --include=src/app/admin/routes/admin-routes.spec.ts` | VERIFICATO: 1 file, 10 test superati | Componente Angular con richieste intercettate da `HttpTestingController`; nessun backend o database reale |

Nel precedente contratto testuale, i test di [admin-routes.spec.ts](frontend/src/app/admin/routes/admin-routes.spec.ts) verificano:

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

Nell’esecuzione storica con il precedente contratto testuale, il test Java crea Torino, modifica in Londra e verifica normalizzazione, paese, ritorno, stato, ID e data invariati, rileggendo PostgreSQL dopo `EntityManager.clear()`. Usa Testcontainers e Liquibase; l’assistente ha letto il sorgente ma non ha rieseguito il comando né acquisito il report Maven.

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
| Paesi europei | Catalogo attuale limitato a IT e GB; il paese è derivato dalla città. L’ampliamento ad altri paesi resta da definire |
| Posizione del menu | Scelta concordata con l'utente durante lo sviluppo: destra su desktop, sopra il contenuto su schermi piccoli; diversa dal menu sinistro dell'analisi originale |
| Immagine della landing ADMIN | Scelta concordata con l'utente: riuso dell'aereo SVG esistente; immagine dell'aeroporto del cliente ancora futura |
| Permessi operativi | OPERATORI senza funzioni amministrative; future attività operative da definire |
| Orari e frequenza | Non definiti; non dedurre un numero di voli giornalieri dalle rotte iniziali |

Le regole tecniche dei campi provengono da RF-05/RF-06 e dal modello dati dell’analisi, con l’estensione geografica richiesta dall’utente: selezione per ID e paese derivato dalla città. Le ipotesi ancora aperte nella fonte restano distinte dalle decisioni concordate.

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

1. **IMPLEMENTATO NEI SORGENTI** — Gestione tratte con creazione, modifica, stato, eliminazione, paginazione e selezione della destinazione dal catalogo geografico.
2. **IMPLEMENTATO NEI SORGENTI** — Schema con foreign key, vincoli basati sugli ID e rimozione dei campi geografici duplicati.
3. **ESECUZIONE DOCUMENTATA** — Importazione del catalogo: 90414 città elaborate e 96894 alias scritti.
4. **EVIDENZA STORICA** — Risultati del 23–24 ottobre relativi alla versione precedente al catalogo, descritti nella sezione 7.
5. **NON ATTESTATO** — Esecuzione delle migrazioni 007–009, build aggiornata e flusso completo della nuova versione attraverso browser, backend e database.
6. **SVILUPPI FUTURI** — Modelli di aereo e funzionalità rinviate elencate nella sezione 9.

Il percorso resta didattico: lo sviluppatore scrive il codice con guida passo per passo, salvo eccezioni autorizzate espressamente. Per questo aggiornamento documentale l’utente ha autorizzato la modifica diretta di Aircraft_Routes.md.

La documentazione distingue le funzionalità presenti nei sorgenti dalle evidenze di esecuzione effettivamente disponibili.
