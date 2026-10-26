# Catalogo paesi estendibile

**Stato:** pianificato — sviluppo futuro.

## Obiettivo e scelte concordate

Rendere i paesi dati del catalogo, così l’aggiunta di una nazione non richiederà modifiche a enum, condizioni HTML o schema.

Partiamo da Italia, Regno Unito, Francia e Paesi Bassi. La gestione avviene tramite file e comando manuale; il percorso resta didattico, con il codice scritto dallo sviluppatore e la guida passo per passo dell’assistente.

La relazione sarà:

**Tratta → Città → Paese**

## Piano di sviluppo

### 1. Introdurre la tabella dei paesi

Creare la migrazione `010-create-countries.yaml`, senza modificare le precedenti:

- Tabella `countries` con `code` come chiave primaria: due lettere maiuscole.
- Campo `name` obbligatorio, massimo 200 caratteri, per il nome visualizzato in italiano.
- Inserimento iniziale di `IT / Italia` e `GB / Regno Unito`, necessari alle città già presenti.
- Foreign key da `destination_cities.country_code` a `countries.code`, con cancellazione `RESTRICT`.
- Rimozione del vincolo che ammette soltanto `IT` e `GB`.

La foreign key garantirà che ogni città faccia riferimento a un paese registrato. [Documentazione PostgreSQL](https://www.postgresql.org/docs/current/ddl-constraints.html).

### 2. Aggiornare il modello Java

Nel modulo `destinations` introdurre l’entità `Country`. `DestinationCity` avrà un’associazione obbligatoria al paese, usando l’attuale colonna `country_code`.

Rimuovere `RouteCountry` e sostituirne gli utilizzi con codici stringa o associazioni, secondo la responsabilità:

- Le entità usano l’associazione a `Country`.
- DTO e dati dell’importatore usano il codice stringa.
- Nome e codice della destinazione continuano a provenire dal catalogo.
- Il tipo resta nazionale per `IT`, internazionale per gli altri paesi, dato che l’origine è Napoli.

Aggiornare ordinamenti JPQL e caricamenti delle associazioni per includere il paese nelle risposte. Le regole su Napoli, Milano e Roma restano basate sugli ID.

### 3. Rendere configurabile l’importatore

Aggiungere `backend/geodata/geonames/countries.json`: un elenco di oggetti con `code` e `name`, inizialmente per `IT`, `GB`, `FR` e `NL`.

L’importatore:

- Legge una configurazione non vuota e rifiuta codici duplicati, codici diversi da due lettere maiuscole e nomi vuoti o troppo lunghi.
- Ricava i percorsi degli archivi dal codice del paese.
- Calcola dinamicamente l’elenco dei file richiesti dal manifesto SHA-256, eliminando l’elenco fisso IT/GB.
- Continua a controllare che il paese di ogni località corrisponda all’archivio letto.
- Mantiene categorie geografiche, alias italiani/inglesi e criteri di scelta del nome già adottati.

GeoNames distribuisce gli archivi delle località per codice paese, quindi possiamo mantenere il formato attuale delle cartelle. [Formato GeoNames](https://download.geonames.org/export/dump/readme.txt).

### 4. Salvare paesi, città e alias insieme

Estendere la transazione dell’importatore:

1. Inserire o aggiornare i paesi definiti nel file.
2. Inserire o aggiornare le città.
3. Sincronizzare gli alias delle città importate.

Tutti i file vengono controllati e preparati prima delle scritture. Un errore di salvataggio annulla l’intera transazione.

Gli ID delle città e i collegamenti delle tratte esistenti vengono conservati. Togliere un paese dalla configurazione non elimina i dati precedentemente importati.

### 5. Aggiornare API e frontend

Le richieste POST e PUT rimangono:

`destinationCityId` e `returnEnabled`.

Estendere invece le risposte:

| Risposta | Modifica |
| --- | --- |
| Città | `countryCode` stringa e nuovo `countryName` |
| Tratta | `destinationCountryCode` stringa e nuovo `destinationCountryName` |

Nel frontend eliminare l’unione fissa `'IT' | 'GB'` e mostrare i nomi ricevuti dal backend nei risultati, nella selezione e nella tabella.

Aggiornare anche la preselezione durante la modifica. Il paese continuerà a essere determinato dalla città scelta, senza un selettore separato.

### 6. Importare Francia e Paesi Bassi

Dopo l’allineamento di database, backend e frontend:

- Preparare gli archivi delle località e degli alias per `FR` e `NL`.
- Aggiornare `SHA256SUMS` con le impronte dei file effettivamente utilizzati.
- Eseguire il comando manuale mantenendo l’attuale argomento: percorso della cartella `geonames`.

Il normale avvio dell’applicazione continuerà a non scaricare o importare dati.

### 7. Aggiornare la documentazione

Allineare `Aircraft_Routes.md` e il README del catalogo: configurazione dei paesi, nuovi campi delle risposte, procedura di importazione ed evidenze disponibili.

## Criteri di completamento

Senza aggiungere o eseguire test, come richiesto, i criteri funzionali sono:

- Parigi/Paris e Amsterdam sono ricercabili nel catalogo importato, rispettivamente con Francia e Paesi Bassi.
- Le tratte esistenti conservano ID e destinazioni.
- Il paese di una tratta continua a essere ricavato esclusivamente dalla città.
- Aggiungere successivamente un paese richiede configurazione, archivi, checksum e importazione, senza ulteriori modifiche applicative o strutturali.

I controlli di compilazione restano separati dalle evidenze di funzionamento reale.
