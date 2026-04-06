import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { EmployeeList } from './employee-list';
import { EmployeeService } from '../../core/services/employee.service';
import { DepartmentService } from '../../core/services/department.service';
import { AuthService } from '../../core/services/auth.service';
import { ensureTestBed } from '../../testing/test-init';

const mockEmployees = [
  { id: 1, firstName: 'John', lastName: 'Doe', email: 'john@test.com', position: 'Developer', departmentName: 'IT' },
  { id: 2, firstName: 'Jane', lastName: 'Smith', email: 'jane@test.com', position: 'Designer', departmentName: 'HR' }
];

const mockPage = {
  content: mockEmployees,
  totalElements: 2,
  totalPages: 1,
  size: 10,
  number: 0,
  first: true,
  last: true
};

const mockDeptPage = {
  content: [{ id: 1, name: 'IT', managerEmail: null, parentDepartmentId: null, parentDepartmentName: null }],
  totalElements: 1,
  totalPages: 1,
  size: 100,
  number: 0,
  first: true,
  last: true
};

describe('EmployeeList Component', () => {
  let component: EmployeeList;
  let fixture: ComponentFixture<EmployeeList>;
  let employeeService: any;

  beforeEach(async () => {
    ensureTestBed();
    employeeService = {
      search: vi.fn().mockReturnValue(of(mockPage)),
      exportCsv: vi.fn().mockReturnValue(of(new Blob(['csv']))),
      downloadPhoto: vi.fn().mockReturnValue(throwError(() => new Error('no photo')))
    } as any;
    const departmentService = { getAll: vi.fn().mockReturnValue(of(mockDeptPage)) };
    const authService = { hasRole: vi.fn().mockReturnValue(true) };

    await TestBed.configureTestingModule({
      imports: [EmployeeList, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: EmployeeService, useValue: employeeService },
        { provide: DepartmentService, useValue: departmentService },
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeList);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load employees on init', () => {
    fixture.detectChanges();

    expect(component.employees).toEqual(mockEmployees);
    expect(component.totalElements).toBe(2);
    expect(component.loading).toBe(false);
  });

  it('should set error on load failure', () => {
    employeeService.search.mockReturnValue(throwError(() => new Error('fail')));

    fixture.detectChanges();

    expect(component.error).toBeTruthy();
    expect(component.loading).toBe(false);
  });
});
