export type RouteCountry = 'IT' | 'GB';

export type RouteType = 'NAZIONALE' | 'INTERNAZIONALE';

export interface CreateRouteRequest {
  destinationCity: string;
  destinationCountryCode: RouteCountry;
  returnEnabled: boolean;
}

export interface AirportRoute {
  id: string;
  originCity: string;
  destinationCity: string;
  destinationCountryCode: RouteCountry;
  routeType: RouteType;
  returnEnabled: boolean;
  createdAt: string;
}

export interface RoutePageResponse {
  content: AirportRoute[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
