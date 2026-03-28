import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { catchError, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);
    const token = localStorage.getItem('token');

    if (token) {
        req = req.clone({
            setHeaders: { Authorization: `Bearer ${token}` }
        });
    }

    return next(req).pipe(
        catchError((error) => {
            if (error.status === 401) {
                localStorage.removeItem('token');
                router.navigate(['/login']);
            }
            if (error.status === 403) {
                alert('You do not have permission to perform this action');
            }
            return throwError(() => error);
        })
    );
};