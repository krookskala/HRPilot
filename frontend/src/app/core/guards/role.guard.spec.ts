import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';
import { CurrentUser, Role } from '../../shared/models/user.model';
import { ensureTestBed } from '../../testing/test-init';

const makeUser = (role: Role): CurrentUser => ({
  id: 1, email: 'a@b.com', role, isActive: true,
  preferredLang: 'en', employeeId: 1, firstName: 'A', lastName: 'B',
  departmentId: null, departmentName: null, unreadNotifications: 0,
  activatedAt: null, lastLoginAt: null
});

describe('roleGuard', () => {
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

  it('should allow access when user has an allowed role', () => {
    authService.ensureCurrentUser.mockReturnValue(of(makeUser(Role.ADMIN)));
    const guard = roleGuard('ADMIN', 'HR_MANAGER');

    TestBed.runInInjectionContext(() => {
      const result$ = guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot);
      (result$ as any).subscribe((val: boolean) => {
        expect(val).toBe(true);
        expect(router.navigate).not.toHaveBeenCalled();
      });
    });
  });

  it('should redirect to /dashboard when user does not have the required role', () => {
    authService.ensureCurrentUser.mockReturnValue(of(makeUser(Role.EMPLOYEE)));
    const guard = roleGuard('ADMIN', 'HR_MANAGER');

    TestBed.runInInjectionContext(() => {
      const result$ = guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot);
      (result$ as any).subscribe((val: boolean) => {
        expect(val).toBe(false);
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
      });
    });
  });

  it('should redirect when user is null', () => {
    authService.ensureCurrentUser.mockReturnValue(of(null));
    const guard = roleGuard('ADMIN');

    TestBed.runInInjectionContext(() => {
      const result$ = guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot);
      (result$ as any).subscribe((val: boolean) => {
        expect(val).toBe(false);
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
      });
    });
  });
});
