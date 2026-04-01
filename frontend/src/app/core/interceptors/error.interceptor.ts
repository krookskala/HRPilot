import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    const snackBar = inject(MatSnackBar);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            let errorMessage = 'An unexpected error occurred';

            if (error.error && error.error.message) {
                errorMessage = error.error.message;
            } else if (error.status === 400) {
                errorMessage = 'Invalid request. Please check your input.';
            } else if (error.status === 401) {
                errorMessage = 'Your session has expired or you are unauthorized. Please login again.';
            } else if (error.status === 403) {
                errorMessage = 'You do not have permission to perform this action.';
            } else if (error.status === 409) {
                errorMessage = 'This resource already exists or conflicts with another.';
            } else if (error.status === 422) {
                errorMessage = 'The request could not be processed.';
            } else if (error.status === 429) {
                errorMessage = 'Too many requests. Please wait a moment and try again.';
            } else if (error.status >= 500) {
                errorMessage = 'A server error occurred. Please try again later.';
            }

            snackBar.open(errorMessage, 'Close', {
                duration: 5000,
                panelClass: ['error-snackbar'],
                horizontalPosition: 'right',
                verticalPosition: 'top',
            });

            return throwError(() => error);
        })
    );
};
