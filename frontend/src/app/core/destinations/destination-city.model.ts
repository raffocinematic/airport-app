import type { RouteCountry } from '../routes/route.model';

/**
 * Questo modello corrisponde ai DTO Java DestinationCityResponse e DestinationCityPageResponse
 */

// Una città selezionabile restituita dal catalogo.
export interface DestinationCity {
  id: number;
  name: string;
  countryCode: RouteCountry;
  admin1Name: string | null;
  admin2Name: string | null;
}

export interface DestinationCityPageResponse {
  content: DestinationCity[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
