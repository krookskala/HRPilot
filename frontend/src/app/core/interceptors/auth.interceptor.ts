import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from "rxjs";
import { AuthService } from "../services/auth.service";

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);
    const authService = inject(AuthService);
    const token = authService.getToken();

    if (token) {
        req = req.clone({
            setHeaders: { Authorization: `Bearer ${token}` }
        });
    }

    return next(req).pipe(
        catchError((error) => {
            if (error.status === 401 && !req.url.includes('/auth/')) {
                if (!isRefreshing) {
                    isRefreshing = true;
                    refreshTokenSubject.next(null);

                    return authService.refreshToken().pipe(
                        switchMap((response) => {
                            isRefreshing = false;
                            refreshTokenSubject.next(response.accessToken);
                            const clonedReq = req.clone({
                                setHeaders: { Authorization: `Bearer ${response.accessToken}` }
                            });
                            return next(clonedReq);
                        }),
                        catchError((refreshError) => {
                            isRefreshing = false;
                            authService.clearSession();
                            router.navigate(['/login']);
                            return throwError(() => refreshError);
                        })
                    );
                }

                return refreshTokenSubject.pipe(
                    filter(token => token !== null),
                    take(1),
                    switchMap((newToken) => {
                        const clonedReq = req.clone({
                            setHeaders: { Authorization: `Bearer ${newToken}` }
                        });
                        return next(clonedReq);
                    })
                );
            }
            if (error.status === 403) {
                console.warn('Access denied: insufficient permissions');
            }
            return throwError(() => error);
        })
    );
};
