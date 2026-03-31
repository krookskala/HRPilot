import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { EmployeeService } from './employee.service';
import { environment } from '../../../environments/environment';
import { ensureTestBed } from '../../testing/test-init';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl;

  beforeEach(() => {
    ensureTestBed();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(EmployeeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all employees with default pagination', () => {
    const mockPage = { content: [], totalElements: 0, totalPages: 0 };

    service.getAll().subscribe(data => {
      expect(data).toEqual(mockPage);
    });

    const req = httpMock.expectOne(`${apiUrl}/employees?page=0&size=20`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('should fetch employees with custom pagination', () => {
    service.getAll(2, 10).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/employees?page=2&size=10`);
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('should search employees with filters', () => {
    service.search({ search: 'John', departmentId: 5 }).subscribe();

    const req = httpMock.expectOne(r => r.url === `${apiUrl}/employees` && r.params.get('search') === 'John');
    expect(req.request.params.get('departmentId')).toBe('5');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('should create an employee', () => {
    const request = { firstName: 'John', lastName: 'Doe', email: 'john@test.com', position: 'Dev', salary: 50000, hireDate: '2026-01-01' };

    service.createEmployee(request as any).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/employees`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 1, ...request });
  });

  it('should delete an employee', () => {
    service.deleteEmployee(5).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/employees/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should get employee detail', () => {
    service.getEmployeeDetail(3).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/employees/3/detail`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 3 });
  });

  it('should export CSV', () => {
    service.exportCsv().subscribe();

    const req = httpMock.expectOne(`${apiUrl}/employees/export/csv`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });

  it('should upload a photo', () => {
    const file = new File([''], 'photo.jpg');

    service.uploadPhoto(1, file).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/employees/1/photo`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush({ id: 1 });
  });
});
