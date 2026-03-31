import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { CurrentUser, Role } from '../../shared/models/user.model';
import { ensureTestBed } from '../../testing/test-init';

const mockUser: CurrentUser = {
  id: 1, email: 'a@b.com', role: Role.ADMIN, isActive: true,
  preferredLang: 'en', employeeId: 1, firstName: 'A', lastName: 'B',
  departmentId: null, departmentName: null, unreadNotifications: 0,
  activatedAt: null, lastLoginAt: null
};

describe('authGuard', () => {
  let authService: { ensureCurrentUser: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    ensureTestBed();
    authService = { ensureCurrentUser: vi.fn() };
    router = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    });
  });

  it('should allow access when user is authenticated', () => {
    authService.ensureCurrentUser.mockReturnValue(of(mockUser));

    TestBed.runInInjectionContext(() => {
      const result$ = authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot);
      (result$ as any).subscribe((val: boolean) => {
        expect(val).toBe(true);
        expect(router.navigate).not.toHaveBeenCalled();
      });
    });
  });

  it('should redirect to /login when user is not authenticated', () => {
    authService.ensureCurrentUser.mockReturnValue(of(null));

    TestBed.runInInjectionContext(() => {
      const result$ = authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot);
      (result$ as any).subscribe((val: boolean) => {
        expect(val).toBe(false);
        expect(router.navigate).toHaveBeenCalledWith(['/login']);
      });
    });
  });
});
