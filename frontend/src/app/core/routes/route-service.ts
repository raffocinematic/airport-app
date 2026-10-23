import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import type { Observable } from 'rxjs';

import type {
  AirportRoute,
  CreateRouteRequest,
  RoutePageResponse
} from './route.model';

// Injectable rende il servizio disponibile tramite il sistema di dependency injection di Angular
@Injectable({
  providedIn: 'root'
})
export class RouteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/routes';

  create(request: CreateRouteRequest): Observable<AirportRoute> {
    return this.http.post<AirportRoute>(
      this.apiUrl,
      request
    );
  }

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
