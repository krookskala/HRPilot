import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { map } from "rxjs";
import { AuthService } from "../services/auth.service";

export function roleGuard(...allowedRoles: string[]): CanActivateFn {
    return () => {
        const authService = inject(AuthService);
        const router = inject(Router);

        return authService.ensureCurrentUser().pipe(
            map(user => {
                if (user && allowedRoles.includes(user.role)) {
                    return true;
                }

                router.navigate(['/dashboard']);
                return false;
            })
        );
    };
}
