import { Component, DestroyRef, inject, signal } from '@angular/core';
import type { OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  catchError,
  finalize,
  of,
  Subject,
  switchMap,
  timer
} from 'rxjs';
import type { Observable } from 'rxjs';

import { RouteService } from '../../core/routes/route-service';

import {
  DestinationCityService
} from '../../core/destinations/destination-city-service';

import type {
  AirportRoute,
  CreateRouteRequest,
  RoutePageResponse
} from '../../core/routes/route.model';

import type {
  DestinationCity,
  DestinationCityPageResponse
} from '../../core/destinations/destination-city.model';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import type { ValidatorFn } from '@angular/forms';

type RouteFieldErrors = Partial<Record<keyof CreateRouteRequest, string>>;

const ORIGIN_CITY_ID = 3172394;

const RETURN_REQUIRED_CITY_IDS = new Set([
  3173435, // Milano
  3169070  // Roma
]);

// Anticipa nel modulo le regole applicate dal backend.
const routeBusinessValidator: ValidatorFn = form => {
  const destinationCityId = form.get('destinationCityId')?.value;
  const returnEnabled = form.get('returnEnabled')?.value;

  if (destinationCityId === ORIGIN_CITY_ID) {
    return { destinationIsOrigin: true };
  }

  if (
    RETURN_REQUIRED_CITY_IDS.has(destinationCityId)
    && returnEnabled === false
  ) {
    return { returnRequired: true };
  }

  return null;
};

type DestinationSearchRequest = {
  query: string;
  page: number;
};

@Component({
  selector: 'app-admin-routes',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-routes.html',
  styleUrl: './admin-routes.scss'
})
export class AdminRoutes implements OnInit {

  // dipendenza che ottiene il servizio tramite dependency injection di Angular, concetto simile a Spring
  private readonly routeService = inject(RouteService);

  private readonly destinationCityService = inject(DestinationCityService);

  // ci permette di legare la sottoscrizione alla durata del componente, quando abbandoni la pagina la richiesta
  // ancora in corsi viene annullata
  private readonly destroyRef = inject(DestroyRef);

  private requestedPage = 0;

  // Una richiesta avvia la ricerca; null annulla quella precedente.
  private readonly destinationSearchRequests =
    new Subject<DestinationSearchRequest | null>();

  private readonly formBuilder = inject(FormBuilder);

  readonly routePage = signal<RoutePageResponse | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal('');

  // Testo digitato per cercare: non viene inviato al backend.
  readonly destinationSearch =
    this.formBuilder.nonNullable.control('');

// Dati effettivamente necessari per creare o modificare la tratta.
  readonly form = this.formBuilder.group({
    destinationCityId: this.formBuilder.control<number | null>(
      null,
      [
        Validators.required,
        Validators.min(1)
      ]
    ),
    returnEnabled: this.formBuilder.control<boolean | null>(
      null,
      Validators.required
    )
  }, {
    validators: routeBusinessValidator
  });

  // Città scelta dall'utente, con i dati necessari alla visualizzazione.
  readonly selectedDestination = signal<DestinationCity | null>(null);

// Pagina restituita dall'ultima ricerca.
  readonly destinationPage =
    signal<DestinationCityPageResponse | null>(null);

  readonly searchingDestinations = signal(false);
  readonly destinationSearchErrorMessage = signal('');

  readonly formVisible = signal(false);

  // null indica la creazione; un ID indica la modifica.
  readonly editingRouteId = signal<string | null>(null);

  readonly saving = signal(false);
  readonly saveErrorMessage = signal('');
  readonly serverFieldErrors = signal<RouteFieldErrors>({});
  readonly successMessage = signal('');
  readonly actionErrorMessage = signal('');


  // L'avvio del caricamento
  ngOnInit(): void {
    this.form.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.serverFieldErrors.set({});
      this.saveErrorMessage.set('');
    });

    this.initializeDestinationSearch();
    this.loadRoutes();
  }


  normalizeDestination(): void {
    const control = this.destinationSearch;

    control.setValue(
      control.value.trim(),
      { emitEvent: false }
    );
  }

  private initializeDestinationSearch(): void {
    this.destinationSearchRequests.pipe(
      switchMap(
        (request): Observable<DestinationCityPageResponse | null> => {
          this.destinationPage.set(null);
          this.destinationSearchErrorMessage.set('');
          this.searchingDestinations.set(false);

          if (request === null) {
            return of(null);
          }

          if (request.query.length < 2) {
            return of(null);
          }

          if (request.query.length > 100) {
            this.destinationSearchErrorMessage.set(
              'Inserisci al massimo 100 caratteri per la ricerca.'
            );
            return of(null);
          }

          this.searchingDestinations.set(true);

          // Ogni nuova richiesta annulla anche l'eventuale attesa precedente.
          return timer(300).pipe(
            switchMap(() =>
              this.destinationCityService.search(
                request.query,
                request.page,
                20
              )
            ),
            catchError((error: unknown) => {
              if (
                error instanceof HttpErrorResponse
                && error.status === 401
              ) {
                return of(null);
              }

              this.destinationSearchErrorMessage.set(
                error instanceof HttpErrorResponse
                && error.status === 403
                  ? 'Non hai i permessi per consultare le destinazioni.'
                  : 'Impossibile cercare le destinazioni. Riprova.'
              );

              return of(null);
            }),
            finalize(() => {
              this.searchingDestinations.set(false);
            })
          );
        }
      ),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(page => {
      this.destinationPage.set(page);
    });

    this.destinationSearch.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      // Il testo è cambiato: la precedente città non è più selezionata.
      this.selectedDestination.set(null);
      this.form.controls.destinationCityId.setValue(null);
      this.form.controls.destinationCityId.markAsDirty();

      this.searchDestinations();
    });
  }

  searchDestinations(page = 0): void {
    if (
      this.saving()
      || !this.formVisible()
      || this.selectedDestination() !== null
    ) {
      return;
    }

    this.destinationSearchRequests.next({
      query: this.destinationSearch.value.trim(),
      page
    });
  }

  selectDestination(city: DestinationCity): void {
    if (
      this.saving()
      || this.searchingDestinations()
      || !this.formVisible()
    ) {
      return;
    }

    // Annulla la ricerca e nasconde l'elenco dei risultati.
    this.destinationSearchRequests.next(null);

    this.selectedDestination.set(city);

    // Scrittura programmatica: conserva la selezione appena effettuata.
    this.destinationSearch.setValue(
      city.name,
      { emitEvent: false }
    );

    this.form.controls.destinationCityId.setValue(city.id);
    this.form.controls.destinationCityId.markAsDirty();
    this.form.controls.destinationCityId.markAsTouched();
  }

  changeDestinationPage(page: number): void {
    const currentPage = this.destinationPage();

    if (
      this.saving()
      || this.searchingDestinations()
      || currentPage === null
      || page < 0
      || page >= currentPage.totalPages
      || page === currentPage.page
    ) {
      return;
    }

    this.searchDestinations(page);
  }

  private resetDestinationSearch(): void {
    // Annulla sia una richiesta HTTP sia un'attesa ancora in corso.
    this.destinationSearchRequests.next(null);

    this.destinationSearch.reset('', { emitEvent: false });
    this.selectedDestination.set(null);
    this.destinationPage.set(null);
    this.searchingDestinations.set(false);
    this.destinationSearchErrorMessage.set('');
  }

  openForm(): void {
    if (this.saving() || this.loading()) {
      return;
    }

    this.editingRouteId.set(null);
    this.form.reset();
    this.resetDestinationSearch();
    this.serverFieldErrors.set({});
    this.saveErrorMessage.set('');
    this.actionErrorMessage.set('');
    this.successMessage.set('');
    this.formVisible.set(true);
  }

  openEditForm(route: AirportRoute): void {
    if (this.saving() || this.loading()) {
      return;
    }

    this.resetDestinationSearch();
    this.editingRouteId.set(route.id);

    this.form.reset({
      destinationCityId: route.destinationCityId,
      returnEnabled: route.returnEnabled
    });

    // Ricostruisce la selezione usando i dati già presenti nella risposta.
    this.selectedDestination.set({
      id: route.destinationCityId,
      name: route.destinationCity,
      countryCode: route.destinationCountryCode,
      admin1Name: route.destinationAdmin1Name,
      admin2Name: route.destinationAdmin2Name
    });

    this.destinationSearch.setValue(
      route.destinationCity,
      { emitEvent: false }
    );

    this.serverFieldErrors.set({});
    this.saveErrorMessage.set('');
    this.actionErrorMessage.set('');
    this.successMessage.set('');
    this.formVisible.set(true);
  }

  cancelForm(): void {
    if (this.saving()) {
      return;
    }

    this.editingRouteId.set(null);
    this.form.reset();
    this.resetDestinationSearch();
    this.serverFieldErrors.set({});
    this.saveErrorMessage.set('');
    this.formVisible.set(false);
  }

  // Imposta lo stato opposto e ricarica la pagina corrente dopo la conferma.
  changeRouteStatus(route: AirportRoute): void {
    if (this.saving() || this.loading() || this.formVisible()) {
      return;
    }

    const nextActive = !route.active;
    const pageToReload = this.requestedPage;

    this.actionErrorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);

    this.routeService.changeStatus(
      route.id,
      { active: nextActive }
    ).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.saving.set(false))
    ).subscribe({
      next: updatedRoute => {
        this.successMessage.set(
          updatedRoute.active
            ? 'Tratta attivata correttamente.'
            : 'Tratta disattivata correttamente.'
        );

        this.loadRoutes(pageToReload);
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse) {
          switch (error.status) {
            case 401:
              return;

            case 403:
              this.actionErrorMessage.set(
                'Non hai i permessi per cambiare lo stato delle tratte.'
              );
              return;

            case 404:
              this.actionErrorMessage.set(
                'La tratta non esiste più. L’elenco verrà aggiornato.'
              );
              this.loadRoutes(pageToReload);
              return;
          }
        }

        this.actionErrorMessage.set(
          'Impossibile confermare il cambio di stato. '
          + 'Ricarica la pagina prima di riprovare.'
        );
      }
    });
  }

  deleteRoute(route: AirportRoute): void {
    if (this.saving() || this.loading() || this.formVisible()) {
      return;
    }

    const confirmed = window.confirm(
      'Vuoi eliminare definitivamente la tratta '
      + route.originCity + ' → ' + route.destinationCity
      + ' (' + route.destinationCountryCode + ')?'
    );

    if (!confirmed) {
      return;
    }

    const pageToReload = this.requestedPage;

    this.actionErrorMessage.set('');
    this.successMessage.set('');
    this.saving.set(true);

    this.routeService.delete(route.id).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.saving.set(false))
    ).subscribe({
      next: () => {
        this.successMessage.set('Tratta eliminata correttamente.');
        this.loadRoutes(pageToReload);
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse) {
          switch (error.status) {
            case 401:
              return;

            case 403:
              this.actionErrorMessage.set(
                'Non hai i permessi per eliminare le tratte.'
              );
              return;

            case 404:
              this.actionErrorMessage.set(
                'La tratta non esiste più. L’elenco verrà aggiornato.'
              );
              this.loadRoutes(pageToReload);
              return;
          }
        }

        this.actionErrorMessage.set(
          'Impossibile confermare la cancellazione. '
          + 'Ricarica la pagina prima di riprovare.'
        );
      }
    });
  }


  submit(): void {
    if (this.saving() || this.loading()) {
      return;
    }

    this.saveErrorMessage.set('');
    this.serverFieldErrors.set({});
    this.successMessage.set('');

    this.form.markAllAsTouched();

    const values = this.form.getRawValue();

    if (
      this.form.invalid
      || values.destinationCityId === null
      || values.returnEnabled === null
    ) {
      return;
    }

    // Nome e paese saranno ricavati dal backend attraverso questo ID.
    const request: CreateRouteRequest = {
      destinationCityId: values.destinationCityId,
      returnEnabled: values.returnEnabled
    };

    // Conserva la modalità e la pagina per tutta la richiesta.
    const editingId = this.editingRouteId();
    const pageToReload = editingId === null
      ? 0
      : this.requestedPage;

    const request$ = editingId === null
      ? this.routeService.create(request)
      : this.routeService.update(editingId, request);

    this.saving.set(true);
    this.form.disable({ emitEvent: false });
    this.destinationSearch.disable({ emitEvent: false });

    request$.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.saving.set(false);
        this.form.enable({ emitEvent: false });
        this.destinationSearch.enable({ emitEvent: false });
      })
    ).subscribe({
      next: () => {
        this.editingRouteId.set(null);
        this.form.reset();
        this.resetDestinationSearch();
        this.serverFieldErrors.set({});
        this.formVisible.set(false);

        this.successMessage.set(
          editingId === null
            ? 'Tratta creata correttamente.'
            : 'Tratta modificata correttamente.'
        );

        this.loadRoutes(pageToReload);
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse) {
          switch (error.status) {
            case 401:
              return;

            case 400:
              this.serverFieldErrors.set(
                this.parseFieldErrors(error.error)
              );
              this.saveErrorMessage.set(
                'Controlla i dati inseriti e riprova.'
              );
              return;

            case 403:
              this.saveErrorMessage.set(
                'Non hai i permessi per salvare le tratte.'
              );
              return;

            case 404:
              this.saveErrorMessage.set(
                'La tratta non esiste più. '
                + 'Annulla la modifica e ricarica la pagina.'
              );
              return;

            case 409:
              this.saveErrorMessage.set(
                'Esiste già una tratta con questa destinazione.'
              );
              return;
          }
        }

        this.saveErrorMessage.set(
          'Impossibile confermare il salvataggio. '
          + 'Verifica l’elenco prima di riprovare.'
        );
      }
    });
  }

  // -----------------------------------------------------------------------------------------------------


  private parseFieldErrors(body: unknown): RouteFieldErrors {
    const result: RouteFieldErrors = {};

    if (
      typeof body !== 'object'
      || body === null
      || !('fieldErrors' in body)
    ) {
      return result;
    }

    const errors = body.fieldErrors;

    if (
      typeof errors !== 'object'
      || errors === null
      || Array.isArray(errors)
    ) {
      return result;
    }

    for (const [field, message] of Object.entries(errors)) {
      if (
        (
          field === 'destinationCityId'
          || field === 'returnEnabled'
        )
        && typeof message === 'string'
        && message.trim() !== ''
      ) {
        result[field] = message;
      }
    }

    return result;
  }

  // Se una cancellazione ha rimosso la pagina richiesta, carica l'ultima valida.
  private loadValidRoutePage(
    page: number
  ): Observable<RoutePageResponse> {
    return this.routeService.list(page, 10).pipe(
      switchMap(response => {
        const lastPage = Math.max(0, response.totalPages - 1);

        if (page > lastPage) {
          this.requestedPage = lastPage;

          return this.loadValidRoutePage(lastPage);
        }

        return of(response);
      })
    );
  }

  loadRoutes(page = this.requestedPage): void {
    if (this.loading()) {
      return;
    }

    this.requestedPage = page;

    this.loading.set(true);
    this.errorMessage.set('');

    this.loadValidRoutePage(page).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: response => {
        this.requestedPage = response.page;
        this.routePage.set(response);
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse) {
          if (error.status === 401) {
            return;
          }

          if (error.status === 403) {
            this.errorMessage.set(
              'Non hai i permessi per consultare le tratte.'
            );
            return;
          }

          if (error.status === 0) {
            this.errorMessage.set(
              'Impossibile contattare il server. Riprova.'
            );
            return;
          }
        }

        this.errorMessage.set(
          'Impossibile caricare le tratte. Riprova.'
        );
      }
    });
  }

  changePage(page: number): void {
    const currentPage = this.routePage();

    if (
      this.loading()
      || this.saving()
      || currentPage === null
      || page < 0
      || page >= currentPage.totalPages
    ) {
      return;
    }

    this.loadRoutes(page);
  }

}
