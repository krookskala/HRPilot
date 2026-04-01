import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { Dashboard } from './dashboard';
import { DashboardData, DashboardService } from '../../core/services/dashboard.service';
import { ensureTestBed } from '../../testing/test-init';

const mockData: DashboardData = {
  role: 'ADMIN',
  headline: 'Company Command Center',
  subheadline: 'Monitor everything',
  keyMetrics: [
    { label: 'Employees', value: '42', icon: 'people', accent: 'cyan' },
    { label: 'Departments', value: '5', icon: 'apartment', accent: 'orange' },
    { label: 'Pending Leave', value: '3', icon: 'event_available', accent: 'green' },
    { label: 'Audit Events', value: '100', icon: 'shield', accent: 'slate' }
  ],
  recentActivities: [
    { type: 'EMPLOYEE', description: 'New employee joined', timestamp: '2026-03-30T10:00:00' }
  ],
  leaveOverview: { pending: 3, approved: 10, rejected: 2, cancelled: 1 },
  payrollOverview: { draft: 5, published: 3, paid: 20, totalNetSalary: 150000 },
  notificationOverview: { unreadNotifications: 2 },
  teamOverview: null,
  personalOverview: null,
  auditOverview: { totalEvents: 100, recentEvents: 5 },
  departmentDistribution: [
    { department: 'IT', count: 15 },
    { department: 'HR', count: 8 },
    { department: 'Sales', count: 19 }
  ]
};

describe('Dashboard Component', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let dashboardService: { getDashboardData: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    ensureTestBed();
    dashboardService = { getDashboardData: vi.fn().mockReturnValue(of(mockData)) };

    await TestBed.configureTestingModule({
      imports: [Dashboard, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: DashboardService, useValue: dashboardService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load dashboard data on init', () => {
    fixture.detectChanges();

    expect(component.data).toEqual(mockData);
    expect(component.loading).toBe(false);
    expect(component.error).toBe('');
  });

  it('should set error on data load failure', () => {
    dashboardService.getDashboardData.mockReturnValue(throwError(() => new Error('fail')));

    fixture.detectChanges();

    expect(component.data).toBeNull();
    expect(component.error).toBe('Failed to load dashboard data');
    expect(component.loading).toBe(false);
  });

  it('should build leave chart config from data', () => {
    fixture.detectChanges();

    expect(component.leaveChartConfig).not.toBeNull();
    expect(component.leaveChartConfig!.type).toBe('pie');
    expect(component.leaveChartConfig!.data.datasets[0].data).toEqual([3, 10, 2, 1]);
  });

  it('should build payroll chart config from data', () => {
    fixture.detectChanges();

    expect(component.payrollChartConfig).not.toBeNull();
    expect(component.payrollChartConfig!.type).toBe('bar');
    expect(component.payrollChartConfig!.data.datasets[0].data).toEqual([5, 3, 20]);
  });

  it('should build department chart config from data', () => {
    fixture.detectChanges();

    expect(component.departmentChartConfig).not.toBeNull();
    expect(component.departmentChartConfig!.type).toBe('doughnut');
    expect(component.departmentChartConfig!.data.labels).toEqual(['IT', 'HR', 'Sales']);
  });

  it('should not build leave chart when all values are zero', () => {
    const zeroData = { ...mockData, leaveOverview: { pending: 0, approved: 0, rejected: 0, cancelled: 0 } };
    dashboardService.getDashboardData.mockReturnValue(of(zeroData));

    fixture.detectChanges();

    expect(component.leaveChartConfig).toBeNull();
  });

  it('should not build department chart when distribution is null', () => {
    const noDepData = { ...mockData, departmentDistribution: null };
    dashboardService.getDashboardData.mockReturnValue(of(noDepData));

    fixture.detectChanges();

    expect(component.departmentChartConfig).toBeNull();
  });

  it('should return ADMIN quick actions when role is ADMIN', () => {
    fixture.detectChanges();

    const actions = component.quickActions;
    expect(actions.length).toBe(4);
    expect(actions[0].label).toBe('Manage Employees');
  });

  it('should return EMPLOYEE quick actions when role is EMPLOYEE', () => {
    const empData = { ...mockData, role: 'EMPLOYEE' as const };
    dashboardService.getDashboardData.mockReturnValue(of(empData));
    fixture.detectChanges();

    const actions = component.quickActions;
    expect(actions[0].label).toBe('My Profile');
  });

  it('should map activity types to correct icons', () => {
    expect(component.activityIcon('EMPLOYEE')).toBe('person_add');
    expect(component.activityIcon('LEAVE')).toBe('event_available');
    expect(component.activityIcon('PAYROLL')).toBe('payments');
    expect(component.activityIcon('AUDIT')).toBe('shield');
    expect(component.activityIcon('UNKNOWN')).toBe('info');
  });
});
