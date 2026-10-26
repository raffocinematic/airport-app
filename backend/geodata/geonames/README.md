# Catalogo geografico delle destinazioni

## Scopo

Il catalogo identifica le destinazioni tramite ID GeoNames.
Ogni città contiene il proprio paese: creando o modificando una
tratta, il backend ricava il paese dalla città selezionata.

Paesi attualmente gestiti:

- IT: Italia.
- GB: Regno Unito.

I dati rappresentano località geografiche. Gli aeroporti specifici
non sono identificati in questa fase.

## Fonte e licenza

- Fonte: [GeoNames](https://www.geonames.org/).
- [Dati geografici](https://download.geonames.org/export/dump/).
- [Nomi alternativi](https://download.geonames.org/export/dump/alternatenames/).
- Licenza: [Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/).
- Attribuzione: dati geografici forniti da GeoNames.

Data di rilevazione locale dei file sorgenti: 2026-10-25.

## File necessari

La struttura locale attesa è:

```text
geonames/
├── README.md
├── SHA256SUMS
└── raw/
    ├── places/
    │   ├── IT.zip
    │   └── GB.zip
    ├── aliases/
    │   ├── IT.zip
    │   └── GB.zip
    ├── admin1CodesASCII.txt
    ├── admin2Codes.txt
    └── readme.txt
```

La cartella `raw` è esclusa dal controllo versione.
`SHA256SUMS` registra le impronte dei sette file sorgenti,
usando percorsi relativi alla cartella `geonames`.
L'importatore controlla le impronte prima di scrivere nel database.

## Importazione manuale

Classe da eseguire:
`tools.geonames.GeoNamesImportCommand`
File:
`backend/src/test/java/tools/geonames/GeoNamesImportCommand.java`

È un comando manuale che scrive nel database configurato.

Prerequisiti:

- PostgreSQL disponibile.
- Tabelle `destination_cities` e `destination_city_aliases`
  già create dalle migrazioni Liquibase 004 e 005.
- File sorgenti e `SHA256SUMS` disponibili nella struttura indicata.
- Configurazione datasource in `application.properties`.
- Variabile d'ambiente `AIRPORT_APP_DB_PASSWORD` disponibile
  nella configurazione di esecuzione.

In IntelliJ:

1. Aprire `GeoNamesImportCommand.java`.
2. Creare o aprire la configurazione Application del suo `main`.
3. Impostare come unico Program argument il percorso della
   cartella `geonames`, non quello di `raw`.
4. Avviare il comando.

Esempio di Program arguments:

```text
"C:\Users\raffo\IdeaProjects\airport-app\backend\geodata\geonames"
```

Il comando esegue:

1. Controllo delle impronte SHA-256.
2. Lettura delle località ammesse e degli alias.
3. Preparazione del catalogo con informazioni amministrative.
4. Salvataggio transazionale delle città e degli alias.

Il comando non esegue le migrazioni Liquibase.
Il normale avvio del backend non scarica né importa il catalogo.

## Rapporto con le migrazioni delle tratte

Quando esistono tratte del precedente modello testuale,
il catalogo deve essere disponibile prima dell'esecuzione
della migrazione 007, che collega quelle tratte agli ID approvati.

La migrazione 008 rende obbligatoria la destinazione
e introduce i vincoli basati sugli ID.

La migrazione 009 elimina dalle tratte le vecchie colonne
contenenti nome, nome normalizzato e paese della destinazione.

Le migrazioni già applicate restano nella storia dello schema.

## Ripetizione dell'importazione

Le città vengono inserite o aggiornate mantenendo gli ID GeoNames.

Per ciascuna città importata, gli alias precedenti vengono
sostituiti con quelli preparati dall'importatore.

Le città assenti dai file importati non vengono eliminate.

Tutte le scritture del catalogo appartengono a un'unica
transazione: un errore durante il salvataggio ne provoca il rollback.

## Evidenza disponibile

L'utente ha condiviso l'output di un'importazione riuscita:

```text
Verifica SHA-256 completata.
Catalogo caricato: 90414 città.
Avvio salvataggio nel database...
Importazione completata: 90414 città elaborate, 96894 alias scritti.
Process finished with exit code 0
```

I numeri descrivono quella specifica esecuzione.
Non costituiscono un conteggio aggiornato del database
né una verifica del flusso completo del frontend.