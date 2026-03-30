import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { map } from "rxjs";
import { AuthService } from "../services/auth.service";

export const authGuard: CanActivateFn = () => {
    const router = inject(Router);
    const authService = inject(AuthService);

    return authService.ensureCurrentUser().pipe(
        map(user => {
            if (user) {
                return true;
            }

            router.navigate(['/login']);
            return false;
        })
    );
}
