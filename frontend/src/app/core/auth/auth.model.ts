export type UserRole = 'ADMIN' | 'OPERATORI';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserProfile {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  role: UserRole;
}

export interface AuthResponse extends UserProfile {
  accessToken: string;
  expiresInSeconds: number;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fieldErrors: Record<string, string>;
}
