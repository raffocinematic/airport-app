import {
  Component,
  DestroyRef,
  inject,
  signal,
  viewChild
} from '@angular/core';
import type { ElementRef, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { AuthService } from '../core/auth/auth-service';
import type { UserProfile } from '../core/auth/auth.model';

import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { AuthSession } from '../core/auth/auth-session';

@Component({
  selector: 'app-session-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './session-layout.html',
  styleUrl: './session-layout.scss'
})

export class SessionLayout implements OnInit {

  // Concetto simile alla iniezione dei servizi di Spring, le altre dipendenze servono per rilevare la distruzione
  // del componente, navigare e gestire la sessione

  // Ottiene dal sistema di dependency injection il servizio di autenticazione
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  // profilo non ancora disponibile
  readonly profile = signal<UserProfile | null>(null);
  // Caricamento del profilo in corso
  readonly loading = signal(false);
  // Errore mostrato sulla pagina
  readonly errorMessage = signal('');
  // Logout in corso
  readonly loggingOut = signal(false);

  // Stato del menù utilizzato dal template.
  readonly menuOpen = signal(false);

// Riferimento al dialog identificato da #adminMenu nel template.
  private readonly menuDialog =
    viewChild<ElementRef<HTMLDialogElement>>('adminMenu');

// Conserva gli stili originali per ripristinare lo scorrimento.
  private pageScroll: {
    root: HTMLElement;
    overflow: string;
    scrollbarGutter: string;
  } | null = null;

  private readonly router = inject(Router);
  private readonly authSession = inject(AuthSession);

  ngOnInit(): void {
    // Ripristina lo scorrimento anche se si abbandona il layout
    // mentre il menù è aperto.
    this.destroyRef.onDestroy(() => this.restorePageScroll());

    this.loadProfile();
  }

  openMenu(): void {
    const dialog = this.menuDialog()?.nativeElement;

    // Il pannello è disponibile soltanto per l'amministratore.
    if (
      !dialog ||
      dialog.open ||
      this.loggingOut() ||
      this.profile()?.role !== 'ADMIN'
    ) {
      return;
    }

    dialog.showModal();

    const root = dialog.ownerDocument.documentElement;
    const viewportWidth =
      dialog.ownerDocument.defaultView?.innerWidth ?? root.clientWidth;

    this.pageScroll = {
      root,
      overflow: root.style.overflow,
      scrollbarGutter: root.style.scrollbarGutter
    };

    // Mantiene lo spazio della barra di scorrimento, se già presente.
    if (viewportWidth > root.clientWidth) {
      root.style.scrollbarGutter = 'stable';
    }

    root.style.overflow = 'hidden';
    this.menuOpen.set(true);
  }

  closeMenu(): void {
    this.menuDialog()?.nativeElement.close();
    this.onMenuClosed();
  }

  onMenuClosed(): void {
    // Evita che un vecchio evento di chiusura interferisca
    // con un pannello già riaperto.
    if (this.menuDialog()?.nativeElement.open) {
      return;
    }

    this.menuOpen.set(false);
    this.restorePageScroll();
  }

  onMenuBackdropClick(event: MouseEvent): void {
    // Chiude soltanto quando il clic arriva sullo sfondo,
    // non quando proviene dagli elementi interni del pannello.
    if (event.target === event.currentTarget) {
      this.closeMenu();
    }
  }

  private restorePageScroll(): void {
    const previous = this.pageScroll;

    if (!previous) {
      return;
    }

    previous.root.style.overflow = previous.overflow;
    previous.root.style.scrollbarGutter = previous.scrollbarGutter;
    this.pageScroll = null;
  }

  // Evita caricamenti sovrapposti e nuove richieste mentre l’utente sta uscendo.
  loadProfile(): void {

    if (this.loading() || this.loggingOut()) {
      return;
    }

    // Prepara il nuovo caricamento eliminando errori e dati precedenti.
    this.loading.set(true);
    this.errorMessage.set('');
    this.profile.set(null);

    // Richiede il profilo; subscribe avvia la richiesta HTTP.
    this.authService.me().pipe(
      // Chiude la sottoscrizione quando Angular distrugge il componente.
      takeUntilDestroyed(this.destroyRef),
      // Ripristina lo stato anche in caso di errore o annullamento.
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: response => {
        // Aggiorna lo stato usato dal template per mostrare nome e ruolo.
        this.profile.set(response);
      },
    // Distingue l’accesso negato dagli altri errori di caricamento.
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 403) {
          this.errorMessage.set('Accesso non consentito.');
          return;
        }

        this.errorMessage.set(
          'Impossibile caricare i dati dell’utente. Riprova.'
        );
      }
    });
  }

  // Richiede la revoca sul server e completa comunque l’uscita locale.
  logout(): void {
    if (this.loggingOut()) {
      return;
    }

    // Conserva il token della richiesta per riconoscere eventuali cambi di sessione.
    const logoutToken = this.authSession.getAccessToken();

    if (logoutToken === null) {
      void this.router.navigate(['/login'], {
        replaceUrl: true
      });
      return;
    }

    this.loggingOut.set(true);

    // Lascia terminare il logout anche se cambia pagina; il servizio limita l’attesa.
    this.authService.logout().pipe(
      finalize(() => this.loggingOut.set(false))
    ).subscribe({
      next: () => {
        // Completa l’uscita locale e decide se mostrare l’avviso sulla revoca.
        this.finishLogout(logoutToken, true);
      },
      error: () => {
        this.finishLogout(logoutToken, false);
      }
    });
  }

  private finishLogout(
    logoutToken: string,
    revocationConfirmed: boolean
  ): void {
    const currentToken = this.authSession.getAccessToken();

    // Una risposta del vecchio logout non deve cancellare una nuova sessione.
    if (currentToken !== null && currentToken !== logoutToken) {
      return;
    }

    // Elimina il token dalla memoria del frontend.
    this.authSession.clear();

    // Torna alla login sostituendo la voce corrente nella cronologia.
    // Se manca la conferma del server, passa il motivo da mostrare all’utente.
    void this.router.navigate(['/login'], {
      replaceUrl: true,
      queryParams: revocationConfirmed
        ? {}
        : { reason: 'logout-unconfirmed' }
    });
  }
}
