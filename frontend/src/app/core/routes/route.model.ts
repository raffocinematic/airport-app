export type RouteCountry = 'IT' | 'GB';

export type RouteType = 'NAZIONALE' | 'INTERNAZIONALE';

// Il backend ricava nome e paese dalla città identificata dall'ID.
export interface CreateRouteRequest {
  destinationCityId: number;
  returnEnabled: boolean;
}

export interface UpdateRouteRequest {
  destinationCityId: number;
  returnEnabled: boolean;
}

export interface UpdateRouteStatusRequest {
  active: boolean;
}

// Corrisponde al JSON restituito da RouteResponse.
export interface AirportRoute {
  id: string;
  originCity: string;
  destinationCityId: number;
  destinationCity: string;
  destinationCountryCode: RouteCountry;
  destinationAdmin1Name: string | null;
  destinationAdmin2Name: string | null;
  routeType: RouteType;
  returnEnabled: boolean;
  active: boolean;
  createdAt: string;
}

export interface RoutePageResponse {
  content: AirportRoute[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
