import { Component, DestroyRef, inject, signal } from '@angular/core';
import type { OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { AuthService } from '../core/auth/auth-service';
import type { UserProfile } from '../core/auth/auth.model';

import { Router } from '@angular/router';
import { AuthSession } from '../core/auth/auth-session';

@Component({
  selector: 'app-home',
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit {

  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly profile = signal<UserProfile | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly loggingOut = signal(false);

  private readonly router = inject(Router);
  private readonly authSession = inject(AuthSession);

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {

    if (this.loading() || this.loggingOut()) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.profile.set(null);

    this.authService.me().pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: response => {
        this.profile.set(response);
      },
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

  logout(): void {
    if (this.loggingOut()) {
      return;
    }

    const logoutToken = this.authSession.getAccessToken();

    if (logoutToken === null) {
      void this.router.navigate(['/login'], {
        replaceUrl: true
      });
      return;
    }

    this.loggingOut.set(true);

    this.authService.logout().pipe(
      finalize(() => this.loggingOut.set(false))
    ).subscribe({
      next: () => {
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

    if (currentToken !== null && currentToken !== logoutToken) {
      return;
    }

    this.authSession.clear();

    void this.router.navigate(['/login'], {
      replaceUrl: true,
      queryParams: revocationConfirmed
        ? {}
        : { reason: 'logout-unconfirmed' }
    });
  }
}
