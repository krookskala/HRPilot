import { APP_INITIALIZER, ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { firstValueFrom } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { provideTranslateHttpLoader, TRANSLATE_HTTP_LOADER_CONFIG } from '@ngx-translate/http-loader';
import { TranslateService } from '@ngx-translate/core';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { AuthService } from './core/services/auth.service';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    importProvidersFrom(
      TranslateModule.forRoot()
    ),
    provideTranslateHttpLoader({ prefix: './i18n/', suffix: '.json' }),
    {
      provide: APP_INITIALIZER,
      useFactory: (authService: AuthService, translate: TranslateService) => () => {
        translate.setDefaultLang('en');
        const user = authService.getCurrentUserSnapshot();
        const lang = user?.preferredLang || localStorage.getItem('lang') || 'en';
        translate.use(lang);
        return firstValueFrom(authService.ensureCurrentUser());
      },
      deps: [AuthService, TranslateService],
      multi: true
    }
  ]
};
