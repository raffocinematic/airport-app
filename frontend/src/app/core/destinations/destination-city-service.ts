import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import type { Observable } from 'rxjs';

import type {
  DestinationCityPageResponse
} from './destination-city.model';

@Injectable({
  providedIn: 'root'
})
export class DestinationCityService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/destination-cities';

  // Cerca nel catalogo usando i parametri previsti dal controller.
  search(
    query: string,
    page = 0,
    size = 20
  ): Observable<DestinationCityPageResponse> {
    return this.http.get<DestinationCityPageResponse>(
      this.apiUrl,
      {
        params: {
          q: query.trim(),
          page,
          size
        }
      }
    );
  }
}
