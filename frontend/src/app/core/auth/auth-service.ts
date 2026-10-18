import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import type { Observable } from 'rxjs';

import { AuthSession } from './auth-session';
import type {
  AuthResponse,
  LoginRequest,
  UserProfile
} from './auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authSession = inject(AuthSession);

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      '/api/v1/auth/login',
      request
    ).pipe(
      tap(response => this.authSession.setSession(response))
    );
  }

  me(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/api/v1/auth/me');
  }
}
