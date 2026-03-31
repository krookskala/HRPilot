import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';
import { CurrentUser, Role } from '../../shared/models/user.model';
import { ensureTestBed } from '../../testing/test-init';

const mockUser: CurrentUser = {
  id: 1,
  email: 'admin@test.com',
  role: Role.ADMIN,
  isActive: true,
  preferredLang: 'en',
  employeeId: 1,
  firstName: 'Admin',
  lastName: 'User',
  departmentId: 1,
  departmentName: 'IT',
  unreadNotifications: 0,
  activatedAt: '2026-01-01T00:00:00',
  lastLoginAt: '2026-03-30T00:00:00'
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl;

  beforeEach(() => {
    ensureTestBed();
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should store tokens and set current user on successful login', () => {
      const authResponse = {
        accessToken: 'access-123',
        refreshToken: 'refresh-456',
        user: mockUser
      };

      service.login({ email: 'admin@test.com', password: 'password' }).subscribe(user => {
        expect(user).toEqual(mockUser);
      });

      const req = httpMock.expectOne(`${apiUrl}/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'admin@test.com', password: 'password' });
      req.flush(authResponse);

      expect(localStorage.getItem('token')).toBe('access-123');
      expect(localStorage.getItem('refreshToken')).toBe('refresh-456');
      expect(service.getCurrentUserSnapshot()).toEqual(mockUser);
    });
  });

  describe('logout', () => {
    it('should clear session and call logout endpoint', () => {
      localStorage.setItem('token', 'access-123');
      localStorage.setItem('refreshToken', 'refresh-456');

      service.logout().subscribe();

      const req = httpMock.expectOne(`${apiUrl}/auth/logout`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'refresh-456' });
      req.flush(null);

      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('refreshToken')).toBeNull();
      expect(service.getCurrentUserSnapshot()).toBeNull();
    });

    it('should clear session even without refresh token', () => {
      service.logout().subscribe();

      expect(localStorage.getItem('token')).toBeNull();
      expect(service.getCurrentUserSnapshot()).toBeNull();
    });
  });

  describe('ensureCurrentUser', () => {
    it('should return null when no token exists', () => {
      service.ensureCurrentUser().subscribe(user => {
        expect(user).toBeNull();
      });
    });

    it('should fetch user from /me when token exists', () => {
      localStorage.setItem('token', 'some-token');

      service.ensureCurrentUser().subscribe(user => {
        expect(user).toEqual(mockUser);
      });

      const req = httpMock.expectOne(`${apiUrl}/me`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });

    it('should return cached user when not forced', () => {
      localStorage.setItem('token', 'some-token');

      // First call fetches
      service.ensureCurrentUser().subscribe();
      httpMock.expectOne(`${apiUrl}/me`).flush(mockUser);

      // Second call uses cache
      service.ensureCurrentUser().subscribe(user => {
        expect(user).toEqual(mockUser);
      });

      httpMock.expectNone(`${apiUrl}/me`);
    });

    it('should refetch when forced', () => {
      localStorage.setItem('token', 'some-token');

      service.ensureCurrentUser().subscribe();
      httpMock.expectOne(`${apiUrl}/me`).flush(mockUser);

      service.ensureCurrentUser(true).subscribe();
      httpMock.expectOne(`${apiUrl}/me`).flush(mockUser);
    });

    it('should clear session on /me failure', () => {
      localStorage.setItem('token', 'bad-token');

      service.ensureCurrentUser().subscribe(user => {
        expect(user).toBeNull();
      });

      httpMock.expectOne(`${apiUrl}/me`).error(new ProgressEvent('error'), { status: 401 });

      expect(localStorage.getItem('token')).toBeNull();
    });
  });

  describe('hasRole', () => {
    it('should return false when no user is logged in', () => {
      expect(service.hasRole('ADMIN')).toBe(false);
    });

    it('should return true when user has the role', () => {
      localStorage.setItem('token', 'token');
      service.ensureCurrentUser().subscribe();
      httpMock.expectOne(`${apiUrl}/me`).flush(mockUser);

      expect(service.hasRole('ADMIN')).toBe(true);
    });

    it('should return false when user does not have the role', () => {
      localStorage.setItem('token', 'token');
      service.ensureCurrentUser().subscribe();
      httpMock.expectOne(`${apiUrl}/me`).flush(mockUser);

      expect(service.hasRole('EMPLOYEE')).toBe(false);
    });
  });

  describe('refreshToken', () => {
    it('should update stored tokens', () => {
      localStorage.setItem('refreshToken', 'old-refresh');

      service.refreshToken().subscribe();

      const req = httpMock.expectOne(`${apiUrl}/auth/refresh`);
      expect(req.request.body).toEqual({ refreshToken: 'old-refresh' });
      req.flush({ accessToken: 'new-access', refreshToken: 'new-refresh' });

      expect(localStorage.getItem('token')).toBe('new-access');
      expect(localStorage.getItem('refreshToken')).toBe('new-refresh');
    });
  });
});
