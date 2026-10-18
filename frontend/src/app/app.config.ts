import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withViewTransitions } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/auth/auth-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withViewTransitions({
        skipInitialTransition: true,

        onViewTransitionCreated: ({ transition, from, to }) => {
          const fromPath = from.firstChild?.routeConfig?.path;
          const toPath = to.firstChild?.routeConfig?.path;

          const isForward =
            fromPath === 'login' && toPath === 'registrazione';

          const isBackward =
            fromPath === 'registrazione' && toPath === 'login';

          // Le altre navigazioni non devono avere animazioni.
          if (!isForward && !isBackward) {
            transition.skipTransition();
            return;
          }

          // Rispetta la preferenza di accessibilità dell'utente.
          const prefersReducedMotion = window.matchMedia(
            '(prefers-reduced-motion: reduce)'
          ).matches;

          const supportsTransitionTypes =
            'types' in transition &&
            CSS.supports(
              'selector(:active-view-transition-type(auth-forward))'
            );

          if (prefersReducedMotion || !supportsTransitionTypes) {
            transition.skipTransition();
            return;
          }

          // Il CSS userà questo nome per scegliere il verso.
          transition.types.add(
            isForward ? 'auth-forward' : 'auth-backward'
          );
        }
      })
    ),
    provideHttpClient(
      withInterceptors([authInterceptor])
    )
  ]
};
