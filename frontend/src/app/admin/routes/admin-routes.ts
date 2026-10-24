import { Component, DestroyRef, inject, signal } from '@angular/core';
import type { OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, of, switchMap } from 'rxjs';
import type { Observable } from 'rxjs';

import { RouteService } from '../../core/routes/route-service';

import type {
  AirportRoute,
  CreateRouteRequest,
  RouteCountry,
  RoutePageResponse
} from '../../core/routes/route.model';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import type { ValidatorFn } from '@angular/forms';

type RouteFieldErrors = Partial<Record<keyof CreateRouteRequest, string>>;

/**
 * Validator che riprende le regole già applicate dal BE
 * @param form
 */
const routeBusinessValidator: ValidatorFn = form => {
  const destinationCity = form.get('destinationCity')?.value ?? '';
  const returnEnabled = form.get('returnEnabled')?.value;

  const normalizedDestination = destinationCity.trim().toLowerCase();

  if (normalizedDestination === 'napoli') {
    return { destinationIsOrigin: true };
  }

  const returnRequired =
    normalizedDestination === 'milano'
    || normalizedDestination === 'roma';

  if (returnRequired && returnEnabled === false) {
    return { returnRequired: true };
  }

  return null;
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

  // ci permette di legare la sottoscrizione alla durata del componente, quando abbandoni la pagina la richiesta
  // ancora in corsi viene annullata
  private readonly destroyRef = inject(DestroyRef);

  private requestedPage = 0;
  private readonly formBuilder = inject(FormBuilder);

  readonly routePage = signal<RoutePageResponse | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal('');

  readonly form = this.formBuilder.group({
    destinationCity: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.pattern(/\S/),
      Validators.maxLength(100)
    ]),
    destinationCountryCode: this.formBuilder.control<RouteCountry | null>(
      null,
      Validators.required
    ),
    returnEnabled: this.formBuilder.control<boolean | null>(
      null,
      Validators.required
    )
  }, {
    validators: routeBusinessValidator
  });

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

    this.loadRoutes();
  }


  normalizeDestination(): void {
    const control = this.form.controls.destinationCity;
    control.setValue(control.value.trim());
  }

  openForm(): void {
    if (this.saving() || this.loading()) {
      return;
    }

    this.editingRouteId.set(null);
    this.form.reset();
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

    this.editingRouteId.set(route.id);

    this.form.reset({
      destinationCity: route.destinationCity,
      destinationCountryCode: route.destinationCountryCode,
      returnEnabled: route.returnEnabled
    });

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

    this.normalizeDestination();
    this.form.markAllAsTouched();

    const values = this.form.getRawValue();

    if (
      this.form.invalid
      || values.destinationCountryCode === null
      || values.returnEnabled === null
    ) {
      return;
    }

    const request: CreateRouteRequest = {
      destinationCity: values.destinationCity,
      destinationCountryCode: values.destinationCountryCode,
      returnEnabled: values.returnEnabled
    };

    // Memorizziamo la modalità e la pagina per questa operazione.
    const editingId = this.editingRouteId();
    const pageToReload = editingId === null
      ? 0
      : this.requestedPage;

    const request$ = editingId === null
      ? this.routeService.create(request)
      : this.routeService.update(editingId, request);

    this.saving.set(true);
    this.form.disable({ emitEvent: false });

      request$.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.saving.set(false);
        this.form.enable({ emitEvent: false });
      })
    ).subscribe({
        next: () => {
          this.editingRouteId.set(null);
          this.form.reset();
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
                'Esiste già una tratta con questa destinazione e paese.'
              );
              return;

            case 0:
              this.saveErrorMessage.set(
                'Impossibile confermare il salvataggio. '
                + 'Verifica l’elenco prima di riprovare.'
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
          field === 'destinationCity'
          || field === 'destinationCountryCode'
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
