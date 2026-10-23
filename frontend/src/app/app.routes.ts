import { Routes } from '@angular/router';

import { Login } from './login/login';
import { Profile } from './profile/profile';
import { authGuard } from './core/auth/auth-guard';
import { Register } from './register/register';
import { Home } from './home/home';
import { SessionLayout } from './layout/session-layout';

import { AdminHome } from './admin/home/admin-home';
import { AdminRoutes } from './admin/routes/admin-routes';
import { adminGuard } from './core/auth/admin-guard';

export const routes: Routes = [
  {
    path: 'login',
    component: Login
  },
  {
    path: 'registrazione',
    component: Register
  },
  {
    path: 'home',
    component: SessionLayout,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        component: Home
      }
    ]
  },
  {
    path: 'admin',
    component: SessionLayout,
    canActivate: [authGuard],
    canActivateChild: [adminGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: AdminHome
      },
      {
        path: 'routes',
        component: AdminRoutes
      }
    ]
  },
  {
    path: 'profilo',
    component: Profile,
    canActivate: [authGuard]
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
