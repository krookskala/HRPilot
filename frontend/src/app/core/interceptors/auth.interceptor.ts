import { HttpInterceptorFn } from "@angular/common/http";
import { HttpRequest } from "@angular/common/http";
import { HttpHandlerFn } from "@angular/common/http";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const token = localStorage.getItem('token');
    if (token) {
        const cloned = req.clone({
            setHeaders: { Authorization: `Bearer ${token}` }
        });
        return next(cloned);
    }
    return next(req);
};