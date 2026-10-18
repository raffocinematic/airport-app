import { Component, DestroyRef, inject, signal } from '@angular/core';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import type { ValidatorFn } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { AuthService } from '../core/auth/auth-service';
import type { RegisterRequest } from '../core/auth/auth.model';

const passwordsMatchValidator: ValidatorFn = form => {
  const password = form.get('password')?.value;
  const confirmPassword = form.get('confirmPassword')?.value;

  return password === confirmPassword
    ? null
    : { passwordMismatch: true };
};

@Component({
  selector: 'app-register',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})

export class Register {

  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(false);
  readonly errorMessage = signal('');

  readonly form = this.formBuilder.group({
    firstName: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(100)
      ]
    ],
    lastName: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(100)
      ]
    ],
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
        Validators.pattern(/\S/),
        Validators.minLength(8),
        Validators.maxLength(128)
      ]
    ],
    confirmPassword: [
      '',
      [
        Validators.required,
        Validators.maxLength(128)
      ]
    ]
  }, {
    validators: passwordsMatchValidator
  });

  normalizeDetails(): void {
    const controls = this.form.controls;

    this.form.patchValue({
      firstName: controls.firstName.value.trim(),
      lastName: controls.lastName.value.trim(),
      email: controls.email.value.trim()
    });
  }

  submit(): void {
    if (this.loading()) {
      return;
    }

    this.errorMessage.set('');
    this.normalizeDetails();
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    const values = this.form.getRawValue();

    const request: RegisterRequest = {
      firstName: values.firstName,
      lastName: values.lastName,
      email: values.email,
      password: values.password
    };

    this.loading.set(true);
    this.form.disable({ emitEvent: false });

    this.authService.register(request).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.loading.set(false);
        this.form.enable({ emitEvent: false });
      })
    ).subscribe({
      next: () => {
        this.form.controls.password.reset('');
        this.form.controls.confirmPassword.reset('');

        void this.router.navigate(['/login'], {
          queryParams: { registered: true },
          replaceUrl: true
        });
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse) {
          if (error.status === 409) {
            this.errorMessage.set(
              'Esiste già un account con questa email.'
            );
            return;
          }

          if (error.status === 400) {
            this.errorMessage.set(
              'Controlla i dati inseriti e riprova.'
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
          'Servizio momentaneamente non disponibile. Riprova.'
        );
      }
    });
  }

}
