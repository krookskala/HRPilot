import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { DepartmentList } from './department-list';
import { DepartmentService } from '../../core/services/department.service';
import { AuthService } from '../../core/services/auth.service';
import { Department } from '../../shared/models/department.model';
import { Page } from '../../shared/models/page.model';
import { ensureTestBed } from '../../testing/test-init';

const mockDepartments: Department[] = [
  { id: 1, name: 'IT', managerEmail: 'mgr@test.com', parentDepartmentId: null, parentDepartmentName: null },
  { id: 2, name: 'HR', managerEmail: null, parentDepartmentId: 1, parentDepartmentName: 'IT' }
];

const mockPage: Page<Department> = {
  content: mockDepartments,
  totalElements: 2,
  totalPages: 1,
  size: 10,
  number: 0,
  first: true,
  last: true
};

describe('DepartmentList Component', () => {
  let component: DepartmentList;
  let fixture: ComponentFixture<DepartmentList>;
  let departmentService: {
    getAll: ReturnType<typeof vi.fn>;
    createDepartment: ReturnType<typeof vi.fn>;
    updateDepartment: ReturnType<typeof vi.fn>;
    deleteDepartment: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    ensureTestBed();
    departmentService = {
      getAll: vi.fn().mockReturnValue(of(mockPage)),
      createDepartment: vi.fn().mockReturnValue(of(mockDepartments[0])),
      updateDepartment: vi.fn().mockReturnValue(of(mockDepartments[0])),
      deleteDepartment: vi.fn().mockReturnValue(of(void 0))
    };

    const authService = { hasRole: vi.fn().mockReturnValue(true) };

    await TestBed.configureTestingModule({
      imports: [DepartmentList, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: DepartmentService, useValue: departmentService },
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('en', { departments: { failedLoad: 'Failed to load departments' } });
    translate.setDefaultLang('en');
    translate.use('en');

    fixture = TestBed.createComponent(DepartmentList);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load departments on init', () => {
    fixture.detectChanges();

    expect(component.departments).toEqual(mockDepartments);
    expect(component.totalElements).toBe(2);
    expect(component.loading).toBe(false);
  });

  it('should set error on load failure', () => {
    departmentService.getAll.mockReturnValue(throwError(() => new Error('fail')));

    fixture.detectChanges();

    expect(component.error).toBe('Failed to load departments');
    expect(component.loading).toBe(false);
  });

  it('should update page on onPageChange', () => {
    fixture.detectChanges();
    component.onPageChange({ pageIndex: 1, pageSize: 20, length: 50 });

    expect(component.pageIndex).toBe(1);
    expect(component.pageSize).toBe(20);
    expect(departmentService.getAll).toHaveBeenCalledWith(1, 20);
  });
});
