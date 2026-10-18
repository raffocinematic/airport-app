import { Routes } from '@angular/router';

import { Login } from './login/login';
import { Profile } from './profile/profile';
import { authGuard } from './core/auth/auth-guard';
import { Register } from './register/register';
import { Home } from './home/home';

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
    component: Home,
    canActivate: [authGuard]
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
