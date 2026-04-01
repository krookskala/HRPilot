import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { NotificationCenter } from './notification-center';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationItem } from '../../shared/models/notification.model';
import { Page } from '../../shared/models/page.model';
import { ensureTestBed } from '../../testing/test-init';

const mockNotifications: NotificationItem[] = [
  { id: 1, type: 'LEAVE', title: 'Leave Approved', message: 'Your leave was approved', actionUrl: null, read: false, createdAt: '2026-03-30T10:00:00', readAt: null },
  { id: 2, type: 'PAYROLL', title: 'Payslip Ready', message: 'March payslip is ready', actionUrl: null, read: true, createdAt: '2026-03-29T10:00:00', readAt: '2026-03-29T12:00:00' }
];

const mockPage: Page<NotificationItem> = {
  content: mockNotifications,
  totalElements: 2,
  totalPages: 1,
  size: 50,
  number: 0,
  first: true,
  last: true
};

describe('NotificationCenter Component', () => {
  let component: NotificationCenter;
  let fixture: ComponentFixture<NotificationCenter>;
  let notificationService: {
    getNotifications: ReturnType<typeof vi.fn>;
    markAsRead: ReturnType<typeof vi.fn>;
    markAllAsRead: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    ensureTestBed();
    notificationService = {
      getNotifications: vi.fn().mockReturnValue(of(mockPage)),
      markAsRead: vi.fn().mockReturnValue(of(mockNotifications[0])),
      markAllAsRead: vi.fn().mockReturnValue(of(void 0))
    };

    await TestBed.configureTestingModule({
      imports: [NotificationCenter, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: NotificationService, useValue: notificationService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationCenter);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load notifications on init', () => {
    fixture.detectChanges();

    expect(component.notifications).toEqual(mockNotifications);
    expect(component.loading).toBe(false);
    expect(component.error).toBe('');
  });

  it('should set error on load failure', () => {
    notificationService.getNotifications.mockReturnValue(throwError(() => new Error('fail')));

    fixture.detectChanges();

    expect(component.error).toBe('Failed to load notifications');
    expect(component.loading).toBe(false);
  });

  it('should call markAsRead and reload', () => {
    fixture.detectChanges();
    component.markAsRead(1);

    expect(notificationService.markAsRead).toHaveBeenCalledWith(1);
  });

  it('should call markAllAsRead and reload', () => {
    fixture.detectChanges();
    component.markAllAsRead();

    expect(notificationService.markAllAsRead).toHaveBeenCalled();
  });

  it('should return true for hasUnread when unread notifications exist', () => {
    fixture.detectChanges();

    expect(component.hasUnread).toBe(true);
  });

  it('should return false for hasUnread when all are read', () => {
    const allRead = mockNotifications.map(n => ({ ...n, read: true }));
    notificationService.getNotifications.mockReturnValue(of({ ...mockPage, content: allRead }));

    fixture.detectChanges();

    expect(component.hasUnread).toBe(false);
  });
});
