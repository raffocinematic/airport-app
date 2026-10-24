import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import type { Observable } from 'rxjs';

import type {
  AirportRoute,
  CreateRouteRequest,
  RoutePageResponse,
  UpdateRouteRequest,
  UpdateRouteStatusRequest
} from './route.model';

// Injectable rende il servizio disponibile tramite il sistema di dependency injection di Angular
@Injectable({
  providedIn: 'root'
})
export class RouteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/routes';

  // Crea una tratta
  create(request: CreateRouteRequest): Observable<AirportRoute> {
    return this.http.post<AirportRoute>(
      this.apiUrl,
      request
    );
  }

  // Modifica i dati di una tratta esistente.
  update(
    id: string,
    request: UpdateRouteRequest
  ): Observable<AirportRoute> {
    return this.http.put<AirportRoute>(
      `${this.apiUrl}/${id}`,
      request
    );
  }

// Disattiva oppure riattiva la tratta.
  changeStatus(
    id: string,
    request: UpdateRouteStatusRequest
  ): Observable<AirportRoute> {
    return this.http.patch<AirportRoute>(
      `${this.apiUrl}/${id}/status`,
      request
    );
  }

// Elimina definitivamente la tratta.
  delete(id: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    );
  }

  // Lista le tratte
  list(page = 0, size = 10): Observable<RoutePageResponse> {
    return this.http.get<RoutePageResponse>(
      this.apiUrl,
      {
        params: {
          page,
          size
        }
      }
    );
  }
}
