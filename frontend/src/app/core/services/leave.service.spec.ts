import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { LeaveService } from './leave.service';
import { environment } from '../../../environments/environment';
import { ensureTestBed } from '../../testing/test-init';

describe('LeaveService', () => {
  let service: LeaveService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl;

  beforeEach(() => {
    ensureTestBed();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(LeaveService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all leave requests with default pagination', () => {
    service.getAll().subscribe();

    const req = httpMock.expectOne(r => r.url === `${apiUrl}/leave-requests` && r.params.get('page') === '0');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('should fetch leave requests with status filter', () => {
    service.getAll(0, 20, { status: 'PENDING' as any }).subscribe();

    const req = httpMock.expectOne(r => r.url === `${apiUrl}/leave-requests` && r.params.get('status') === 'PENDING');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('should get leave requests by employee', () => {
    service.getByEmployee(5).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/leave-requests/employee/5`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get my leave requests', () => {
    service.getMine().subscribe();

    const req = httpMock.expectOne(`${apiUrl}/me/leave-requests`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should create a leave request', () => {
    const request = { type: 'ANNUAL', startDate: '2026-04-01', endDate: '2026-04-05', reason: 'Vacation' };

    service.create(request as any).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/leave-requests`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 1, ...request });
  });

  it('should approve a leave request', () => {
    service.approve(10).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/leave-requests/10/approve`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 10, status: 'APPROVED' });
  });

  it('should reject a leave request with reason', () => {
    service.reject(10, 'Too many requests').subscribe();

    const req = httpMock.expectOne(`${apiUrl}/leave-requests/10/reject`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ reason: 'Too many requests' });
    req.flush({ id: 10, status: 'REJECTED' });
  });

  it('should cancel a leave request with reason', () => {
    service.cancel(10, 'Changed plans').subscribe();

    const req = httpMock.expectOne(`${apiUrl}/leave-requests/10/cancel`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ reason: 'Changed plans' });
    req.flush({ id: 10, status: 'CANCELLED' });
  });

  it('should get leave request history', () => {
    service.getHistory(7).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/leave-requests/7/history`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get leave balances for an employee', () => {
    service.getBalances(3, 2026).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/leave-requests/balances/3?year=2026`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should get my leave balances', () => {
    service.getMyBalances().subscribe();

    const req = httpMock.expectOne(`${apiUrl}/me/leave-balances`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
