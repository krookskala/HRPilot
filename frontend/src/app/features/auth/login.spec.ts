import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { Login } from './login';
import { AuthService } from '../../core/services/auth.service';
import { CurrentUser, Role } from '../../shared/models/user.model';
import { ensureTestBed } from '../../testing/test-init';

const mockUser: CurrentUser = {
  id: 1, email: 'a@b.com', role: Role.ADMIN, isActive: true,
  preferredLang: 'en', employeeId: 1, firstName: 'A', lastName: 'B',
  departmentId: null, departmentName: null, unreadNotifications: 0,
  activatedAt: null, lastLoginAt: null
};

describe('Login Component', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let authService: { login: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    ensureTestBed();
    authService = { login: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Login, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'dashboard', component: Login }]),
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have an invalid form when empty', () => {
    expect(component.form.valid).toBe(false);
  });

  it('should require email', () => {
    component.form.patchValue({ email: '', password: 'password123' });
    expect(component.form.get('email')!.hasError('required')).toBe(true);
  });

  it('should validate email format', () => {
    component.form.patchValue({ email: 'not-an-email', password: 'password123' });
    expect(component.form.get('email')!.hasError('email')).toBe(true);
  });

  it('should require password minimum 8 characters', () => {
    component.form.patchValue({ email: 'a@b.com', password: '1234567' });
    expect(component.form.get('password')!.hasError('minlength')).toBe(true);
  });

  it('should have a valid form with correct values', () => {
    component.form.patchValue({ email: 'a@b.com', password: 'password123' });
    expect(component.form.valid).toBe(true);
  });

  it('should not call authService.login when form is invalid', () => {
    component.login();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('should call authService.login with form values when valid', () => {
    authService.login.mockReturnValue(of(mockUser));
    component.form.patchValue({ email: 'a@b.com', password: 'password123' });

    component.login();

    expect(authService.login).toHaveBeenCalledWith({ email: 'a@b.com', password: 'password123' });
  });

  it('should set loading=true while logging in', () => {
    authService.login.mockReturnValue(of(mockUser));
    component.form.patchValue({ email: 'a@b.com', password: 'password123' });

    component.login();

    expect(component.loading).toBe(true);
  });

  it('should set error message on login failure', () => {
    authService.login.mockReturnValue(throwError(() => ({ error: { message: 'Bad credentials' } })));
    component.form.patchValue({ email: 'a@b.com', password: 'wrongpass1' });

    component.login();

    expect(component.error).toBe('Bad credentials');
    expect(component.loading).toBe(false);
  });

  it('should use default error message when server provides none', () => {
    authService.login.mockReturnValue(throwError(() => ({ error: {} })));
    component.form.patchValue({ email: 'a@b.com', password: 'wrongpass1' });

    component.login();

    expect(component.error).toBe('Email or Password Wrong');
  });
});
