import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';
import { ensureTestBed } from '../../testing/test-init';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    ensureTestBed();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), NotificationService]
    });
    service = TestBed.inject(NotificationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should fetch notifications', () => {
    const mockPage = { content: [], totalElements: 0 };
    service.getNotifications(0, 20).subscribe(result => {
      expect(result).toEqual(mockPage);
    });

    const req = httpTesting.expectOne(r => r.url.includes('/notifications'));
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('should mark a notification as read', () => {
    const mockNotification = { id: 1, type: 'LEAVE', title: 'Test', message: 'Test', actionUrl: null, read: true, createdAt: '', readAt: '' };
    service.markAsRead(1).subscribe(result => {
      expect(result).toEqual(mockNotification);
    });

    const req = httpTesting.expectOne(r => r.url.includes('/notifications/1/read'));
    expect(req.request.method).toBe('PUT');
    req.flush(mockNotification);
  });

  it('should mark all notifications as read', () => {
    service.markAllAsRead().subscribe();

    const req = httpTesting.expectOne(r => r.url.includes('/notifications/read-all'));
    expect(req.request.method).toBe('PUT');
    req.flush(null);
  });
});
