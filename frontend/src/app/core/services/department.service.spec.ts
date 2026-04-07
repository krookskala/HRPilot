import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DepartmentService } from './department.service';
import { ensureTestBed } from '../../testing/test-init';

describe('DepartmentService', () => {
  let service: DepartmentService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    ensureTestBed();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), DepartmentService]
    });
    service = TestBed.inject(DepartmentService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should fetch all departments', () => {
    const mockPage = { content: [], totalElements: 0 };
    service.getAll(0, 10).subscribe(result => {
      expect(result).toEqual({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 0,
        number: 0,
        first: true,
        last: true
      });
    });

    const req = httpTesting.expectOne(r => r.url.includes('/departments'));
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('should create a department', () => {
    const request = { name: 'IT', managerId: null, parentDepartmentId: null };
    const response = { id: 1, name: 'IT', managerEmail: null, parentDepartmentId: null, parentDepartmentName: null };

    service.createDepartment(request).subscribe(result => {
      expect(result).toEqual(response);
    });

    const req = httpTesting.expectOne(r => r.url.includes('/departments'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);
  });

  it('should update a department', () => {
    const request = { name: 'IT Updated', managerId: 1, parentDepartmentId: null };
    const response = { id: 1, name: 'IT Updated', managerEmail: 'mgr@test.com', parentDepartmentId: null, parentDepartmentName: null };

    service.updateDepartment(1, request).subscribe(result => {
      expect(result).toEqual(response);
    });

    const req = httpTesting.expectOne(r => r.url.includes('/departments/1'));
    expect(req.request.method).toBe('PUT');
    req.flush(response);
  });

  it('should delete a department', () => {
    service.deleteDepartment(1).subscribe();

    const req = httpTesting.expectOne(r => r.url.includes('/departments/1'));
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
