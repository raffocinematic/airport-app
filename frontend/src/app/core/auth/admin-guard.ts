import { inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import type { CanActivateChildFn } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AuthService } from './auth-service';
import { AuthSession } from './auth-session';

export const adminGuard: CanActivateChildFn = () => {
  const authService = inject(AuthService);
  const authSession = inject(AuthSession);
  const router = inject(Router);

  if (!authSession.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  return authService.me().pipe(
    map(profile => {
      if (profile.role === 'ADMIN') {
        return true;
      }

      return router.createUrlTree(['/home']);
    }),
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        return of(false);
      }

      return of(router.createUrlTree(['/home']));
    })
  );
};
