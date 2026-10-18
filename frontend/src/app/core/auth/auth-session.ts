import { computed, Injectable, signal } from '@angular/core';
import type { AuthResponse } from './auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthSession {
  private readonly accessTokenState = signal<string | null>(null);

  readonly isAuthenticated = computed(
    () => this.accessTokenState() !== null
  );

  setSession(response: AuthResponse): void {
    this.accessTokenState.set(response.accessToken);
  }

  getAccessToken(): string | null {
    return this.accessTokenState();
  }

  clear(): void {
    this.accessTokenState.set(null);
  }
}
