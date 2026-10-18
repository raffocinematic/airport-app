import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';

import { authInterceptor } from './auth-interceptor';
import { AuthSession } from './auth-session';
import type { AuthResponse } from './auth.model';

describe('authInterceptor', () => {
  const response: AuthResponse = {
    userId: '00000000-0000-4000-8000-000000000001',
    firstName: 'Mario',
    lastName: 'Test',
    email: 'mario@airport.test',
    role: 'OPERATORI',
    accessToken: 'token-di-test',
    expiresInSeconds: 900
  };

  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let authSession: AuthSession;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(
          withInterceptors([authInterceptor])
        ),
        provideHttpClientTesting()
      ]
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    authSession = TestBed.inject(AuthSession);
    router = TestBed.inject(Router);

    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.restoreAllMocks();
  });

  it('adds the bearer token to a protected API request', () => {
    authSession.setSession(response);

    http.get('/api/v1/auth/me').subscribe();

    const request = httpTesting.expectOne({
      method: 'GET',
      url: '/api/v1/auth/me'
    });

    expect(
      request.request.headers.get('Authorization')
    ).toBe('Bearer token-di-test');

    request.flush({});
  });

  it.each([
    'https://external.example/api/v1/auth/me',
    '//external.example/api/v1/auth/me',
    '/assets/config.json',
    '/api/v10/auth/me'
  ])('does not add the bearer token to %s', url => {
    authSession.setSession(response);

    http.get(url).subscribe();

    const request = httpTesting.expectOne(url);

    expect(
      request.request.headers.has('Authorization')
    ).toBe(false);

    request.flush({});
  });

  it('clears the current session and redirects on 401', () => {
    authSession.setSession(response);

    const onError = vi.fn();

    http.get('/api/v1/auth/me').subscribe({
      error: onError
    });

    const request = httpTesting.expectOne('/api/v1/auth/me');

    request.flush({}, {
      status: 401,
      statusText: 'Unauthorized'
    });

    expect(authSession.getAccessToken()).toBeNull();

    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: {
        reason: 'session-expired'
      },
      replaceUrl: true
    });

    expect(onError).toHaveBeenCalledWith(
      expect.objectContaining({ status: 401 })
    );
  });

  it('preserves the session on 403', () => {
    authSession.setSession(response);

    const onError = vi.fn();

    http.get('/api/v1/admin/ping').subscribe({
      error: onError
    });

    const request = httpTesting.expectOne('/api/v1/admin/ping');

    request.flush({}, {
      status: 403,
      statusText: 'Forbidden'
    });

    expect(authSession.getAccessToken()).toBe(response.accessToken);
    expect(router.navigate).not.toHaveBeenCalled();

    expect(onError).toHaveBeenCalledWith(
      expect.objectContaining({ status: 403 })
    );
  });

  it('preserves a newer session when an old request returns 401', () => {
    authSession.setSession(response);

    const onError = vi.fn();

    http.get('/api/v1/auth/me').subscribe({
      error: onError
    });

    const request = httpTesting.expectOne('/api/v1/auth/me');

    authSession.setSession({
      ...response,
      accessToken: 'nuovo-token-di-test'
    });

    request.flush({}, {
      status: 401,
      statusText: 'Unauthorized'
    });

    expect(authSession.getAccessToken()).toBe('nuovo-token-di-test');
    expect(router.navigate).not.toHaveBeenCalled();

    expect(onError).toHaveBeenCalledWith(
      expect.objectContaining({ status: 401 })
    );
  });
});
