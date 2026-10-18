import { inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import type { HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthSession } from './auth-session';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authSession = inject(AuthSession);

  const router = inject(Router);

  const appOrigin = window.location.origin;
  const requestUrl = new URL(request.url, appOrigin);

  const isAirportApi =
    requestUrl.origin === appOrigin
    && requestUrl.pathname.startsWith('/api/v1/');

  const isPublicAuthEndpoint =
    requestUrl.pathname === '/api/v1/auth/login'
    || requestUrl.pathname === '/api/v1/auth/register';

  const token = authSession.getAccessToken();

  if (!isAirportApi || isPublicAuthEndpoint || token === null) {
    return next(request);
  }

  const authenticatedRequest = request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse
        && error.status === 401
        && authSession.getAccessToken() === token
      ) {
        authSession.clear();

        void router.navigate(['/login'], {
          queryParams: {
            reason: 'session-expired'
          },
          replaceUrl: true
        });
      }

      return throwError(() => error);
    })
  );
};
