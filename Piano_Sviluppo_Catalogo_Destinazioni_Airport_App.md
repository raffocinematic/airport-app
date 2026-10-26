# Airport App — Catalogo destinazioni GeoNames e integrazione tratte

**Tipo documento:** analisi funzionale, analisi tecnica e piano di sviluppo incrementale  
**Progetto:** Airport App  
**Stack:** Java / Spring Boot, Angular / TypeScript, PostgreSQL, Liquibase  
**Stato:** pianificato — nessuna modifica applicata al codice o al database  
**Perimetro geografico:** Italia (`IT`) e Regno Unito (`GB`)  
**Strategia di lavoro:** piccoli incrementi verificabili; il primo intervento riguarda **il modello dati e la prima migrazione Liquibase**.

> **Nota importante:** i nomi di classi, cartelle, endpoint e colonne qui proposti sono *convenzioni da verificare sul repository reale*. Non modificare changelog Liquibase già eseguiti. Il documento descrive cosa sviluppare; gli esiti dei test non sono ancora disponibili.

---

## 1. Obiettivo e motivazione

Sostituire l'inserimento libero della città e del paese di destinazione con un **catalogo locale di località abitate identificabili in maniera univoca** tramite l'ID GeoNames.

Il salvataggio di una tratta dovrà dipendere dall'**identità canonica** della destinazione (`destinationCityId`) e non da stringhe digitate dall'utente. Per esempio, **Londra** e **London** devono essere ricondotte alla stessa località e non generare tratte distinte.

### Risultati attesi

- Catalogo popolato **solo** con località abitate in `IT` e `GB`, senza soglia minima di abitanti.
- Nomi italiani preferiti per la visualizzazione quando disponibili; altrimenti nome della fonte GeoNames.
- Ricerca ADMIN paginata e selezione esplicita di una destinazione in Angular.
- `routes.destination_city_id` come riferimento al catalogo; successiva eliminazione delle vecchie colonne di destinazione ridondanti.
- Unicità della tratta per **origine + ID di destinazione**, anche per tratte disattivate.
- Migrazione dei dati esistenti verificabile e senza correzioni arbitrarie.
- Nessuna dipendenza da servizi geografici esterni durante ricerca, POST e PUT ordinari: si usa **solo PostgreSQL**.

### Fuori perimetro

- Paesi diversi da `IT`/`GB`.
- Geocoding online durante il normale utilizzo.
- Calcolo della distanza o dei voli in base alle coordinate.
- Cancellazione/fusione automatica delle tratte preesistenti.
- Introduzione di una soglia sulla popolazione.
- Refactoring non necessario di autenticazione, aeromobili o altre sezioni.

---

## 2. Fonte geografica: GeoNames

### 2.1 File da utilizzare

| File ufficiale | Utilizzo |
|---|---|
| `../../Desktop/IT.zip` | Record geografici dell'Italia. |
| `../../Desktop/GB.zip` | Record geografici del Regno Unito. **Il codice è `GB`, non `UK`.** |
| `alternateNamesV2.zip` | Alias e nomi in lingua italiana; filtrare i record ai GeoNames ID inclusi nel catalogo. |
| `admin1CodesASCII.txt` | Nomi delle suddivisioni amministrative di primo livello. |
| `admin2Codes.txt` | Nomi delle suddivisioni amministrative di secondo livello. |
| `readme.txt` | Specifiche dei tracciati di importazione. |

Scaricare i file dalla [directory ufficiale dei dump GeoNames](https://download.geonames.org/export/dump/), anziché utilizzare `cities500.zip`, `cities1000.zip` e simili: i file `citiesN` applicherebbero una selezione legata alla popolazione, incompatibile con i requisiti.

I dump sono **testo UTF-8 delimitato da tabulazioni**. Nel record geografico servono principalmente:

- `geonameid`, `name`, `asciiname`, `alternatenames`;
- `feature class`, `feature code`, `country code`;
- `admin1 code`, `admin2 code` (se presenti);
- `modification date` (per tracciabilità).

Il file `alternateNamesV2` espone anche `isolanguage`, `isPreferredName`, `isColloquial`, `isHistoric` e intervalli temporali, utili per decidere quali nomi siano affidabili per display e ricerca. Non interpretare codici `iata`, `icao`, `post`, identificativi esterni o URL come normali alias di città.

### 2.2 Politica dei filtri

1. Accettare esclusivamente `country_code IN ('IT', 'GB')`.
2. Accettare esclusivamente `feature_class = 'P'` e una **allowlist versionata** di codici di località abitate.
3. **Prima allowlist proposta:** `PPL`, `PPLA`, `PPLA2`, `PPLA3`, `PPLA4`, `PPLA5`, `PPLC`, `PPLCD`, `PPLF`, `PPLG`, `PPLL`, `PPLR`. Validare gli effetti sui file reali prima di confermarla. Si possono introdurre altri codici solo dopo analisi documentata.
4. Escludere sempre `PPLX` (sezione/quartiere), `PPLH` (luogo storico non più esistente), `PPLQ` (abbandonato), `PPLW` (distrutto), `PPLCH` (capitale storica). Escludere inoltre feature di classe diversa da `P` (ad es. `ADM*`, edifici, attrazioni).
5. Valutare con attenzione record collettivi quali `PPLS`, per evitare una voce che rappresenti *più località* anziché una destinazione selezionabile: **esclusi nella prima allowlist**.
6. Nessun controllo `population > N`: anche una località di popolazione zero/non censita può essere valida se il suo **tipo** è ammesso.
7. Non fondere due record GeoNames differenti soltanto perché nome e paese coincidono; l'omonimia è un caso da gestire mediante identificativo e suddivisione amministrativa.

> **Decisione da validare sul dataset:** le classificazioni GeoNames sono una fonte esterna e possono contenere casi limite. L'allowlist è deliberatamente configurabile, versionata e accompagnata da statistiche di importazione.

### 2.3 Normalizzazione di nomi e alias

- Conservare il nome originale GeoNames (`source_name`).
- Preferire un **nome italiano** di `alternateNamesV2` (`isolanguage = 'it'`) se presente, privilegiando quello marcato `isPreferredName`; in caso di più candidati, applicare una regola deterministica e registrare eventuali conflitti.
- In assenza di nome italiano utilizzare `source_name` come `display_name`.
- Salvare gli alias usabili nella ricerca, compresi nome originale, forma ASCII e nomi alternativi appropriati. Escludere URL, codici non geografici e, per prudenza nella corrispondenza automatica, alias storici; i nomi colloquiali possono essere esclusi dalla **riconciliazione automatica**, anche se resi ricercabili dopo valutazione.
- Preparare una forma normalizzata per ricerca e matching (trim, case folding, gestione accenti e spazi), preservando sempre la grafia originale da mostrare.
- Non considerare l'uguaglianza del testo come unicità **globale**: due località diverse possono condividere un alias. La selezione finale deve sempre restituire l'ID corretto.

Esempio concettuale:

| Input di ricerca | Località selezionata | Identità salvata |
|---|---|---|
| `Londra` | Londra, Regno Unito | GeoNames ID della città di London |
| `London` | Londra, Regno Unito | **Lo stesso** GeoNames ID |
| `Firenze` | Firenze, Italia | GeoNames ID della città di Firenze |

### 2.4 Licenza, provenienza e versione

I dati GeoNames sono pubblicati con licenza **Creative Commons Attribution 4.0 (CC BY 4.0)**. In documentazione e in un punto visibile della funzionalità che usa questi dati riportare, ad esempio:

> Dati geografici: © GeoNames — https://www.geonames.org — licenza CC BY 4.0 (https://creativecommons.org/licenses/by/4.0/). I dati possono essere stati filtrati e riorganizzati per Airport App.

Per ogni esecuzione registrare almeno data/versione dello snapshot, nomi e hash SHA-256 dei file, versione dell'importer, allowlist/esclusioni, conteggi IT/GB, alias importati, eventuali scarti e stato finale.

---

## 3. Modello dati del catalogo — **primo incremento**

### 3.1 Tabella `destination_cities`

Proposta iniziale (compatibile con PostgreSQL; lunghezze da verificare rispetto ai file):

| Campo | Tipo | Vincolo / uso |
|---|---|---|
| `geonames_id` | `BIGINT` | **PK**: ID stabile della fonte, non ID autogenerato. |
| `source_name` | `VARCHAR(200)` | `NOT NULL`: nome GeoNames. |
| `display_name` | `VARCHAR(400)` | `NOT NULL`: nome italiano se disponibile, altrimenti `source_name`. |
| `country_code` | `CHAR(2)` | `NOT NULL`, `CHECK IN ('IT','GB')`. |
| `admin1_code` | `VARCHAR(20)` | Codice amministrativo del primo livello, nullable. |
| `admin1_name` | `VARCHAR(200)` | Nome relativo al codice, nullable. |
| `admin2_code` | `VARCHAR(80)` | Secondo livello, nullable. |
| `admin2_name` | `VARCHAR(200)` | Nome secondo livello, nullable. |
| `feature_code` | `VARCHAR(10)` | `NOT NULL`, codice GeoNames ammesso dall'importer. |
| `source_modified_at` | `DATE` | Data modifica GeoNames, nullable. |

Non usare `display_name` come chiave naturale e non imporre `UNIQUE(name, country_code)`: **gli omonimi sono legittimi**.

### 3.2 Tabella `destination_city_aliases`

| Campo | Tipo | Vincolo / uso |
|---|---|---|
| `city_geonames_id` | `BIGINT` | FK verso `destination_cities.geonames_id`, `NOT NULL`. |
| `alias_name` | `VARCHAR(400)` | Forma testuale da mostrare/ricercare. |
| `normalized_alias` | `VARCHAR(400)` | Forma normalizzata, `NOT NULL`. |
| `searchable` | `BOOLEAN` | `NOT NULL`, default `TRUE`. |

**PK proposta:** (`city_geonames_id`, `normalized_alias`). Un alias può appartenere a più città (nessun `UNIQUE(normalized_alias)` globale). Il programma di importazione deve risolvere gli alias ripetuti per **la stessa città** in modo deterministico, senza perdere il collegamento all'identità GeoNames. L'alias `display_name` e il nome sorgente entrano nella ricerca anche quando non provengono dal file degli alternativi.

Se in futuro serviranno tutte le varianti linguistiche o la provenienza di ogni singolo nome, si potrà estendere il modello con una tabella di dettaglio degli `alternateNameId`.

### 3.3 Tabella `destination_catalog_imports`

| Campo | Tipo | Descrizione |
|---|---|---|
| `id` | `BIGINT GENERATED BY DEFAULT AS IDENTITY` | PK tecnica dell'importazione. |
| `dataset_version` | `VARCHAR(100)` | Etichetta snapshot, per es. data del dump. |
| `importer_version` | `VARCHAR(50)` | Versione del codice importatore. |
| `sources_manifest` | `TEXT` | Manifest JSON: file, SHA-256, paesi, filtri. |
| `status` | `VARCHAR(20)` | `STARTED`, `COMPLETED`, `FAILED`. |
| `started_at` | `TIMESTAMPTZ` | Inizio esecuzione. |
| `finished_at` | `TIMESTAMPTZ` | Fine esecuzione, nullable. |
| `statistics` | `TEXT` | Manifest JSON con conteggi e anomalie, nullable. |

Per PostgreSQL è possibile preferire `JSONB` a `TEXT` per i due manifest, se il progetto già usa questo tipo e il team lo approva.

### 3.4 Collegamento a `routes`

Aggiungere inizialmente la colonna **nullable**:

```sql
ALTER TABLE routes
    ADD COLUMN destination_city_id BIGINT NULL;
```

Nella **prima migrazione non** eliminare le colonne legacy di città/paese e non rendere la nuova colonna `NOT NULL`. Come richiesto, introdurre la FK `routes → destination_cities` solo nell'ultimo incremento, dopo la riconciliazione completa. La FK tra alias e città può invece essere creata subito perché riguarda solo nuove tabelle.

### 3.5 Relazioni logiche

```text
destination_cities
  PK geonames_id
      │
      ├──< destination_city_aliases
      │       PK (city_geonames_id, normalized_alias)
      │       FK city_geonames_id
      │
      └──< routes
              destination_city_id (NULL nella fase iniziale)
              FK e NOT NULL soltanto alla fine della migrazione

destination_catalog_imports
  registra versioni dei dati, filtri ed esiti di importazione
```

### 3.6 Indici

- La PK di `destination_cities` copre le ricerche puntuali per GeoNames ID.
- Indice su `destination_city_aliases(normalized_alias)` per matching esatto/ricerca iniziale.
- Indice su `destination_cities(country_code, display_name)` per ordinamento e filtri.
- Per ricerche testuali più flessibili, valutare `pg_trgm` e relativo indice GIN in un changeSet separato; misurare le prestazioni prima di introdurlo.
- L'eventuale indice su `routes(destination_city_id)` può essere anticipato, ma il vincolo unico composito si applica solo alla fine.

---

## 4. Piano Liquibase, senza riscrivere le migrazioni esistenti

### Migrazione A — struttura iniziale (PRIMO TASK)

**ChangeSet proposto:** `add-destination-catalog-and-nullable-route-link`.

Operazioni:

1. Creare `destination_cities`.
2. Creare `destination_city_aliases`, inclusa la FK interna e gli indici iniziali.
3. Creare `destination_catalog_imports`.
4. Aggiungere `routes.destination_city_id BIGINT NULL`.
5. **Non** introdurre ancora `NOT NULL`, FK su `routes`, nuovi `UNIQUE` o eliminazioni di colonne legacy.

**Criteri di accettazione:** un DB vuoto e uno già popolato eseguono la migrazione; le tratte precedenti restano presenti, con stessi ID/stato/date; le vecchie operazioni continuano a funzionare finché il contratto non cambia.

**Esempio didattico di intestazione changelog (non sostituisce i file del repository):**

```yaml
databaseChangeLog:
  - changeSet:
      id: 2026-01-create-destination-catalog
      author: airport-app
      changes:
        # createTable: destination_cities
        # createTable: destination_city_aliases
        # addForeignKeyConstraint: aliases -> destination_cities
        # createTable: destination_catalog_imports
        # addColumn: routes.destination_city_id (nullable)
```

I commenti vanno convertiti in istruzioni Liquibase reali nel formato effettivamente usato dal progetto (YAML, XML o SQL). Registrare un rollback per oggetti *nuovi* quando sicuro; non proporre un rollback distruttivo se le tabelle contengono già dati importati o collegamenti effettivi.

### Migrazione B — popolamento e riconciliazione

L'importer GeoNames si avvia **esplicitamente**, dopo che lo schema è disponibile. Successivamente si esegue un processo di riconciliazione separato, preferibilmente con report e modalità `dry-run`.

1. Importare località, nomi italiani, alias, suddivisioni e metadata dello snapshot.
2. Eseguire una scansione di **tutte** le tratte, incluse le disattivate.
3. Produrre suggerimenti di match usando nome/alias normalizzato **e paese legacy**, senza correggere automaticamente il paese.
4. Scrivere `destination_city_id` **solo** in presenza di esattamente un candidato affidabile.
5. Generare un report persistente delle anomalie e far correggere esplicitamente i casi da un operatore/sviluppatore autorizzato.
6. Rieseguire la riconciliazione fino a `0` irrisolti, `0` incompatibilità e `0` duplicati non risolti.

Questa fase può richiedere script applicativi e report, non necessariamente una sola modifica strutturale Liquibase. Rendere ogni passaggio ripetibile e verificabile; **non usare una migrazione Liquibase per scaricare dinamicamente dati esterni all'avvio**.

### Migrazione C — vincoli definitivi (solo dopo esito positivo)

**Precondizioni obbligatorie:** backup verificato; scritture locali sulle tratte sospese; report puliti; nuova release backend/frontend coordinata.

1. Verificare `COUNT(*) WHERE destination_city_id IS NULL = 0`.
2. Verificare che ogni destinazione punti a una città esistente `IT`/`GB`.
3. Verificare assenza di duplicati per `(origine, destination_city_id)` comprese le disattivate.
4. Introdurre FK `routes.destination_city_id → destination_cities.geonames_id`.
5. Introdurre `NOT NULL` su `routes.destination_city_id`.
6. Introdurre `UNIQUE (origine, destination_city_id)` in base al **nome reale della colonna origine**.
7. Aggiornare i vincoli PostgreSQL per Napoli/Milano/Roma **usando gli ID GeoNames verificati**.
8. Eliminare le vecchie colonne `destination_name`/`destination_country` **solo dopo** aver sostituito tutti i riferimenti nel codice e verificato i dati. I nomi citati sono indicativi: ispezionare le colonne reali.
9. Riabilitare le scritture soltanto dopo controlli di integrità e smoke test con la nuova versione.

**Nota PostgreSQL:** un semplice `CHECK` non può leggere il `country_code` della tabella collegata. Il database garantisce l'identità tramite FK e vincoli su ID canonici; la derivazione del paese avviene tramite `JOIN`, non fidandosi del payload client.

---

## 5. Importazione esplicita e ripetibile

### Comportamento dell'importer

L'importazione **non** deve essere inserita in `CommandLineRunner`/`ApplicationRunner` dell'avvio ordinario del backend. Predisporre un comando applicativo distinto (CLI Java/Spring Batch o modulo dedicato) che lavori sul DB configurato e termini con un codice di uscita osservabile.

Flusso consigliato:

1. Ricevere il percorso dei file già scaricati e uno `dataset_version` esplicito.
2. Verificare presenza dei file, encoding, integrità e manifest SHA-256.
3. Registrare `STARTED` in `destination_catalog_imports`.
4. Leggere gli archivi per paese in streaming; filtrare per paese, classe e allowlist; raccogliere ID ammessi.
5. Risolvere suddivisioni con `admin1CodesASCII`/`admin2Codes` usando **chiavi composite comprensive del paese**, non il solo codice.
6. Leggere `alternateNamesV2`, selezionare gli alias relativi agli ID importati, decidere `display_name` italiano in modo deterministico.
7. Effettuare upsert in batch sulle città e sugli alias. Una riesecuzione dello **stesso snapshot** non deve creare doppioni né cambiare arbitrariamente gli ID.
8. Eseguire validazioni: paesi ammessi, record senza nome, alias vuoti, riferimenti orfani, conteggi, collisioni di nomi.
9. Scrivere statistiche ed esito `COMPLETED`; in caso di errore registrare `FAILED` e impedire che un catalogo parziale venga considerato pronto.

**Politica di aggiornamento:** non cancellare automaticamente città assenti in un dump successivo se sono referenziate da tratte. Segnalare prima la divergenza e richiedere una strategia esplicita (mantenimento, sostituzione verificata o gestione di un record dismesso). Valutare staging e pubblicazione atomica del catalogo per evitare letture incoerenti durante reimport massivi.

**Esempio d'interfaccia CLI da implementare, non comando già disponibile:**

```bash
java -jar airport-geonames-importer.jar \
  --dataset-version=2026-10-25 \
  --input-dir=./data/geonames \
  --mode=import
```

Non eseguire download e import GeoNames come side effect di ogni avvio Spring Boot.

---

## 6. Riconciliazione delle tratte esistenti

### Algoritmo conservativo

Per ogni riga `routes` (anche `active = false`, o equivalente nello schema reale):

1. Leggere origine, città legacy, paese legacy, ID tratta, stato e timestamp.
2. Tradurre il formato del paese legacy in `IT`/`GB` **solo mediante mappatura esplicita approvata**; valori non previsti sono anomalie, non correzioni automatiche.
3. Calcolare il nome normalizzato e cercare candidati nel catalogo che condividano **nome principale o alias e paese**.
4. Se i candidati distinti sono **esattamente uno**, proporre il suo GeoNames ID.
5. Se non ce ne sono, classificare come **SCONOSCIUTA**. Se sono più di uno, **OMONIMO_AMBIGUO**.
6. Segnalare **PAESE_INCOMPATIBILE** qualora il match desiderato richieda di cambiare il paese precedente; non applicarlo implicitamente.
7. Calcolare le chiavi `(origine, destination_city_id)` su tutte le righe candidate e rilevare i **DUPLICATI_POST_CANONICALIZZAZIONE** (ad es. tratte a Londra e London già separate).
8. Applicare gli update solo dopo la fase `dry-run` e i controlli; conservare esattamente **ID tratta, data di creazione e stato**.

Anche i match automatici non devono aggirare le regole di dominio; eventuali violazioni legacy devono essere elencate e risolte consapevolmente.

### Report da produrre

Formato consigliato CSV/JSON, per ogni tratta:

`routeId, origin, legacyDestination, legacyCountry, proposedGeonamesId, candidateCount, issueType, resolution, reviewer`

Il report deve contenere totali e dettaglio per categoria, oltre alla versione del catalogo utilizzato. Non cancellare, non fondere e non modificare automaticamente il paese di alcuna tratta. Le anomalie aperte **bloccano** la migrazione finale.

---

## 7. Contratto API backend

### 7.1 POST e PUT delle tratte

Le richieste di creazione e modifica ricevono come proprietà rilevanti:

```json
{
  "origin": "NAPOLI",
  "destinationCityId": 123456,
  "returnEnabled": true
}
```

`123456` è **un ID segnaposto**, non l'identificativo reale di una città. Il campo `origin` è illustrativo: **mantenere la rappresentazione effettiva già usata dall'API** e gli altri eventuali campi previsti dal contratto corrente.

- `destinationCityId`: obbligatorio; non nullo, deve corrispondere a una località del catalogo.
- `returnEnabled`: booleano richiesto, validato sia in POST sia in PUT.
- `countryCode` e nome città passati dal client **non sono fonti autorevoli**. Preferire l'eliminazione dal DTO di scrittura, con comportamento esplicito per vecchi payload (rifiuto dei campi non previsti oppure ignorati, ma mai persistiti).
- Il servizio legge la destinazione in PostgreSQL e usa la relativa identità/paese per ogni regola applicativa.

### 7.2 Risposta delle tratte

```json
{
  "id": 42,
  "origin": "NAPOLI",
  "destinationCityId": 123456,
  "destinationName": "Firenze",
  "destinationCountry": "IT",
  "returnEnabled": true,
  "active": true
}
```

Anche qui gli ID e i campi non geografici sono **illustrativi**: si preservano le proprietà già necessarie al frontend, aggiungendo `destinationCityId`. Nome e paese sono **derivati dal JOIN con `destination_cities`**.

### 7.3 Errori HTTP

| Scenario | HTTP | Regola |
|---|---:|---|
| `destinationCityId` mancante o nullo | `400` | Errore associato al campo. |
| `destinationCityId` inesistente | `400` | Errore associato al campo; non `404` della tratta. |
| Testo digitato senza selezione | `400` | Il frontend impedisce la richiesta; se arriva senza ID, il backend la rifiuta. |
| Tratta indicata nel PUT inesistente | `404` | Invariato. |
| Origine + città canonica già esistenti | `409` | Vale anche con alias diversi e tratte disattivate. |
| Destinazione Napoli | `400` | Violazione regola di dominio; errore sulla destinazione. |
| Milano/Roma senza ritorno | `400` | Violazione di `returnEnabled`. |

Esempio di risposta di validazione, da adattare al formato `ProblemDetail`/error DTO già adottato:

```json
{
  "status": 400,
  "fieldErrors": [
    { "field": "destinationCityId", "message": "Destinazione non valida" }
  ]
}
```

### 7.4 Endpoint ricerca ADMIN

**Proposta:** `GET /api/admin/destinations?q=lon&page=0&size=20`

- Accesso esclusivo ADMIN mediante la **nuova regola** di `SecurityConfig` per questo endpoint; mantenere invariati gli altri permessi esistenti.
- Ricerca sui nomi principali, `display_name` e alias; paginazione lato DB.
- `size <= 20`, con massimo imposto dal server indipendentemente dal client.
- `page` zero-based e ordinamento stabile (es. corrispondenza esatta prima, poi `display_name`, paese, admin1/admin2, GeoNames ID).
- Stabilire una policy per `q` vuota o troppo corta: proposta iniziale `400` se meno di 2 caratteri dopo trim, per evitare scansioni involontarie; da concordare prima dell'implementazione.
- Deduplicare i **risultati per GeoNames ID**, non per nome testuale.

Esempio risposta paginata (schematica):

```json
{
  "content": [
    {
      "id": 123456,
      "name": "Londra",
      "region": "England",
      "admin2": "Greater London",
      "countryCode": "GB"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

---

## 8. Regole di business basate su identità canonica

| Regola | Implementazione backend | Vincolo DB finale |
|---|---|---|
| Napoli non è una destinazione | Confrontare il GeoNames ID selezionato con quello **verificato** di Napoli. | `CHECK (destination_city_id <> <ID_NAPOLI>)`. |
| Milano richiede ritorno | Se l'ID è quello **verificato** di Milano, `returnEnabled = true`. | Vincolo su ID Milano e `return_enabled`. |
| Roma richiede ritorno | Se l'ID è quello **verificato** di Roma, `returnEnabled = true`. | Vincolo su ID Roma e `return_enabled`. |
| Unicità tratta | Controllo applicativo con errore `409`. | `UNIQUE (origine, destination_city_id)` contro race condition. |

I tre ID **non sono inseriti qui deliberatamente**: andranno identificati e approvati sul catalogo importato, verificando paese e località corretti, prima di scrivere i vincoli SQL. Le regole non devono dipendere dalle stringhe `Napoli`, `Naples`, `Roma`, `Rome`, `Milano` o `Milan`.

Implementare la stessa validazione in una logica riusabile richiamata da **POST e PUT**. L'applicazione deve gestire anche le violazioni native del DB trasformando i duplicati in `409`; per altre violazioni usare la mappatura errori condivisa già esistente.

---

## 9. Modifiche Angular

### Ricerca e selezione

1. Sostituire l'input libero di destinazione con un campo di ricerca e un elenco selezionabile.
2. Eseguire richieste paginabili all'endpoint ADMIN (preferibilmente con debounce e annullamento delle ricerche superate).
3. Mostrare per ogni risultato **località, regione/suddivisione e paese** per distinguere gli omonimi.
4. Alla selezione memorizzare `destinationCityId`, `display_name` e paese derivato dal risultato.
5. Il paese è visualizzato **read-only**, mai editabile manualmente.
6. A ogni modifica successiva del testo di ricerca **azzerare** ID e selezione precedenti; non lasciare una città nascosta associata a testo differente.
7. Disabilitare il salvataggio fino a selezione valida e `returnEnabled` valorizzato.
8. In modifica, preselezionare la destinazione usando `destinationCityId` della tratta caricata, non cercandola per nome.
9. Aggiornare gli DTO/servizi TypeScript e i messaggi d'errore `400`, `404`, `409`.

### Comportamenti UX da verificare

- Ricerca `London` e `Londra` → stessa città selezionabile.
- Due omonimi → voci visivamente distinte grazie alla regione/paese.
- Modifica testo dopo selezione → ID azzerato e Save disabilitato.
- Cambio destinazione sul PUT → applica regole business esattamente come il POST.
- Risultati numerosi → paginazione e limite 20 imposto dal server.

---

## 10. Piano di test e criteri di accettazione

### 10.1 Backend e database

| ID | Verifica | Atteso | Stato |
|---|---|---|---|
| T01 | Selezionare Firenze via ID | `destinationCountry = IT`. | Da eseguire |
| T02 | Tentare di indicare un altro paese nel payload | Il payload non può alterare il paese derivato. | Da eseguire |
| T03 | `destinationCityId` assente/null | `400` con errore sul campo. | Da eseguire |
| T04 | `destinationCityId` sconosciuto | `400` con errore sul campo. | Da eseguire |
| T05 | `Londra` e `London` | Ricondotti allo stesso GeoNames ID. | Da eseguire |
| T06 | Due località omonime | Due risultati distinti per ID e suddivisione. | Da eseguire |
| T07 | POST/PUT con Napoli | Entrambi rifiutati anche tramite alias. | Da eseguire |
| T08 | POST/PUT con Milano/Roma e `returnEnabled=false` | Entrambi rifiutati. | Da eseguire |
| T09 | Origine + stesso ID esistente, anche disattivato | `409`; il DB impedisce il doppione. | Da eseguire |
| T10 | PUT con ID tratta inesistente | `404`. | Da eseguire |
| T11 | Ricerca ADMIN oltre 20 richiesti | Massimo 20 risultati per pagina. | Da eseguire |
| T12 | Ricerca come non ADMIN | Accesso negato secondo SecurityConfig. | Da eseguire |
| T13 | Import dello stesso snapshot ripetuto | Nessuna duplicazione; metadata coerenti. | Da eseguire |
| T14 | Import IT/GB con filtro | Nessuna classe/quartiere/luogo abbandonato non ammesso; nessuna soglia abitanti. | Da eseguire |
| T15 | Migrazione di tratte attive e inattive | ID, date, stato e paese mantenuti; ambiguità segnalate. | Da eseguire |
| T16 | Riconciliazione con alias che unificano tratte | Duplicato intercettato e migrazione bloccata. | Da eseguire |
| T17 | Violazione vincolo DB in concorrenza | Un solo inserimento valido; altro `409`. | Da eseguire |
| T18 | Ricerca/POST/PUT senza rete GeoNames | Il normale utilizzo opera solo su PostgreSQL. | Da eseguire |

### 10.2 Frontend

| ID | Verifica | Atteso | Stato |
|---|---|---|---|
| F01 | Testo immesso senza clic su un risultato | Impossibile salvare. | Da eseguire |
| F02 | Modifica ricerca dopo selezione | ID e paese selezionati vengono azzerati. | Da eseguire |
| F03 | Selezione località | Regione e paese visibili; paese readonly. | Da eseguire |
| F04 | Modifica tratta preesistente | Preselezione per ID canonico. | Da eseguire |
| F05 | Gestione errori e paginazione | Messaggi corretti e max 20. | Da eseguire |

### 10.3 Cosa riportare in `Aircraft_Routes.md`

Aggiornare **solo a lavoro effettivamente svolto**, inserendo:

- decisioni su schema e GeoNames ID canonici verificati;
- elenco changeSet Liquibase e ordine di esecuzione;
- comandi effettivi di importazione, versione dataset, hash e licenza;
- processo e risultati del report di riconciliazione;
- contratti finali di POST/PUT/GET ricerca con payload reali;
- modifica dei permessi ADMIN;
- esiti **reali** dei test (PASS/FAIL/BLOCKED, data, ambiente, breve evidenza), senza dichiarare test non eseguiti.

---

## 11. Roadmap didattica a incrementi

### Step 1 — Modello del catalogo e prima migrazione Liquibase (INIZIARE QUI)

**Attività**

- [ ] Ispezionare l'entità `Route`, schema `routes`, DTO e changelog Liquibase esistenti.
- [ ] Verificare formato dei changelog già adottati e convenzioni dei nomi.
- [ ] Definire le tre nuove tabelle e i relativi indici/vincoli iniziali.
- [ ] Aggiungere `routes.destination_city_id` nullable.
- [ ] Applicare la migration su un DB locale vuoto e su un DB con tratte esistenti.
- [ ] Verificare che i dati legacy non siano alterati.
- [ ] Commit dedicato e breve nota tecnica sullo schema.

**Definition of Done Step 1:** la struttura esiste, Liquibase parte senza errori, nessuna tratta viene modificata e l'applicazione ordinaria continua a funzionare. **Niente import e niente modifiche UI in questo step.**

### Step 2 — Importer GeoNames

- [ ] Preparare input files IT/GB, nomenclature amministrative e alternates V2.
- [ ] Implementare parsing robusto, filtri, nomi italiani e normalizzazione.
- [ ] Implementare upsert e audit per esecuzione esplicita.
- [ ] Validare ripetibilità, casi reali e statistiche dello snapshot.

**DoD:** catalogo IT/GB coerente, alias consultabili, import ripetibile e documentato.

### Step 3 — Riconciliazione legacy

- [ ] Dry-run di tutte le tratte attive e disattivate.
- [ ] Report di sconosciuti, omonimi, paesi incompatibili e duplicati.
- [ ] Correzioni manuali autorizzate dove necessarie.
- [ ] Backfill dei `destination_city_id` e riconteggio delle anomalie.

**DoD:** tutte le tratte risolte univocamente, nessuna perdita di ID/date/stato, zero problemi aperti.

### Step 4 — Backend/API e regole canoniche

- [ ] Repository e service del catalogo.
- [ ] DTO POST/PUT con `destinationCityId` e `returnEnabled`.
- [ ] Error handling 400/404/409 e regole Napoli/Milano/Roma per ID.
- [ ] Endpoint ADMIN ricerca max 20, paginazione e test.

**DoD:** API e test di integrazione pronti per il nuovo contratto.

### Step 5 — Angular

- [ ] Form di ricerca/selezione con alias e omonimi.
- [ ] Paese read-only, reset selezione al cambio testo.
- [ ] DTO Angular, modifica tratte ed errori aggiornati.
- [ ] Test unitari/E2E del nuovo form.

**DoD:** frontend e backend parlano lo stesso contratto, nessun testo libero crea destinazioni.

### Step 6 — Migrazione finale e rilascio coordinato

- [ ] Backup, controlli preflight e sospensione scritture locali.
- [ ] Applicazione FK + `NOT NULL` + `UNIQUE` + CHECK per le regole.
- [ ] Rimozione dei campi legacy dopo controllo delle dipendenze.
- [ ] Deploy coordinato FE/BE, smoke test e riapertura scritture.
- [ ] Aggiornamento `Aircraft_Routes.md` con risultati effettivi.

**DoD:** integrità referenziale garantita; dati vecchi conservati; regole non aggirabili dagli alias; documentazione e test tracciati.

---

## 12. Rischi e contromisure

| Rischio | Contromisura |
|---|---|
| GeoNames contiene omonimi / classificazioni imprecise | ID stabile, admin1/admin2, allowlist verificata e selezione esplicita. |
| Gli alias fanno emergere tratte duplicate | Report prima del vincolo; nessuna fusione silenziosa. |
| Un aggiornamento GeoNames rimuove una località usata | Vietate cancellazioni automatiche delle città referenziate. |
| L'importer viene lanciato con dataset diversi | Versioni, hash, lock operativo e audit. |
| DB con dati preesistenti non passa i nuovi vincoli | Tre fasi distinte; NULL temporaneo; preflight bloccante. |
| FE e BE cambiano il contratto in momenti diversi | Release coordinata e finestra con scritture sospese. |
| Un `CHECK` basato sul nome è aggirabile da alias | Vincoli su GeoNames ID verificati. |
| Query sugli alias molto grandi risultano lente | Paginazione nel DB, indici e test di performance. |
| Il matching ignora il paese legacy o le tratte inattive | Matching con paese obbligatorio; scansione completa. |

---

## 13. Prossima sessione di sviluppo: ordine operativo

1. Aprire l'ultima migrazione Liquibase esistente e il file master changelog.
2. Controllare la struttura concreta di `routes` e il nome della colonna origine.
3. Definire le entity JPA `DestinationCity` e `DestinationCityAlias` **oppure** iniziare direttamente dallo schema DB (in base alle convenzioni presenti).
4. Creare il nuovo changeSet incrementale per le tre tabelle e `destination_city_id NULL`.
5. Avviare Spring Boot sul PostgreSQL locale e verificare la tabella `databasechangelog`.
6. Controllare con query SQL che le nuove strutture ci siano e che il numero delle tratte esistenti non sia cambiato.
7. Eseguire il commit del solo Step 1.

**Condizione di stop:** non passare allo Step 2 finché lo Step 1 non è stato verificato su dati esistenti e documentato.

---

## 14. Riferimenti ufficiali

- [GeoNames — Download dump per paese](https://download.geonames.org/export/dump/)
- [GeoNames — Readme e formato dei tracciati](https://download.geonames.org/export/dump/readme.txt)
- [GeoNames — Elenco e significato dei feature code](https://www.geonames.org/export/codes.html)
- [GeoNames — Condizioni di utilizzo](https://www.geonames.org/export/)
- [Licenza Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/)

**Ultimo aggiornamento del piano:** 25 ottobre 2026.  
**Stato delle verifiche:** tutte da eseguire: questo file è un piano di sviluppo, **non** un rapporto di esecuzione.
