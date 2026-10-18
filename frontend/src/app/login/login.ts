import { Component, DestroyRef, inject, signal } from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../core/auth/auth-service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  private readonly formBuilder = inject(NonNullableFormBuilder);

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(false);
  readonly errorMessage = signal(
    this.route.snapshot.queryParamMap.get('reason') === 'session-expired'
      ? 'La sessione è scaduta o non è più valida. Accedi nuovamente.'
      : ''
  );
  readonly successMessage = signal(
    this.route.snapshot.queryParamMap.get('registered') === 'true'
      ? 'Registrazione completata. Ora puoi accedere.'
      : ''
  );
  readonly warningMessage = signal(
    this.route.snapshot.queryParamMap.get('reason') === 'logout-unconfirmed'
      ? 'Sei uscito dall’applicazione. '
      + 'La revoca delle altre sessioni non è stata confermata.'
      : ''
  );

  readonly form = this.formBuilder.group({
    email: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.maxLength(255)
      ]
    ],
    password: [
      '',
      [
        Validators.required,
        Validators.maxLength(255)
      ]
    ]
  });

  normalizeEmail(): void {
    const emailControl = this.form.controls.email;
    emailControl.setValue(emailControl.value.trim());
  }

  submit(): void {
    if (this.loading()) {
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');
    this.warningMessage.set('');
    this.normalizeEmail();
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    const request = this.form.getRawValue();

    this.loading.set(true);
    this.form.disable({ emitEvent: false });

    this.authService.login(request).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.loading.set(false);
        this.form.enable({ emitEvent: false });
      })
    ).subscribe({
      next: () => {
        this.form.controls.password.reset('');
        void this.router.navigateByUrl('/home');
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse) {
          if (error.status === 401) {
            this.errorMessage.set('Email o password non valide.');
            return;
          }

          if (error.status === 400) {
            this.errorMessage.set('Controlla email e password inserite.');
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
          'Servizio momentaneamente non disponibile. Riprova.'
        );
      }
    });
  }
}
