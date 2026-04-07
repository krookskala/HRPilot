import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { DatePipe, DecimalPipe } from "@angular/common";
import { RouterLink } from "@angular/router";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { finalize } from "rxjs";
import { Chart, ArcElement, BarElement, CategoryScale, LinearScale, Tooltip, Legend, PieController, BarController, DoughnutController } from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { ChartConfiguration } from "chart.js";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DashboardData, DashboardMetric, DashboardService } from "../../core/services/dashboard.service";

Chart.register(ArcElement, BarElement, CategoryScale, LinearScale, Tooltip, Legend, PieController, BarController, DoughnutController);

type QuickAction = {
    label: string;
    route: string;
    icon: string;
    accent: 'cyan' | 'orange' | 'green' | 'indigo' | 'slate';
};

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [BaseChartDirective, DatePipe, DecimalPipe, MatCardModule, MatIconModule, MatProgressSpinnerModule, RouterLink, TranslateModule],
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
    private dashboardService = inject(DashboardService);
    private cdr = inject(ChangeDetectorRef);
    private translate = inject(TranslateService);

    data: DashboardData | null = null;
    loading = true;
    error = '';

    leaveChartConfig: ChartConfiguration<'pie'> | null = null;
    payrollChartConfig: ChartConfiguration<'bar'> | null = null;
    departmentChartConfig: ChartConfiguration<'doughnut'> | null = null;

    ngOnInit(): void {
        this.dashboardService.getDashboardData().pipe(
            finalize(() => {
                this.loading = false;
                this.cdr.detectChanges();
            })
        ).subscribe({
            next: data => {
                this.data = data;
                this.buildCharts(data);
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = this.translate.instant('dashboard.failedToLoad');
                this.cdr.detectChanges();
            }
        });
    }

    private buildCharts(data: DashboardData): void {
        this.buildLeaveChart(data);
        this.buildPayrollChart(data);
        this.buildDepartmentChart(data);
    }

    private buildLeaveChart(data: DashboardData): void {
        const leave = data.leaveOverview;
        const values = [leave.pending, leave.approved, leave.rejected, leave.cancelled];
        if (values.every(v => v === 0)) return;

        this.leaveChartConfig = {
            type: 'pie',
            data: {
                labels: [
                    this.translate.instant('dashboard.chartPending'),
                    this.translate.instant('dashboard.chartApproved'),
                    this.translate.instant('dashboard.chartRejected'),
                    this.translate.instant('dashboard.chartCancelled')
                ],
                datasets: [{
                    data: values,
                    backgroundColor: ['#d97706', '#15803d', '#dc2626', '#475569'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom', labels: { padding: 16, usePointStyle: true, pointStyle: 'circle' } }
                }
            }
        };
    }

    private buildPayrollChart(data: DashboardData): void {
        const payroll = data.payrollOverview;
        const values = [payroll.draft, payroll.published, payroll.paid];
        if (values.every(v => v === 0)) return;

        this.payrollChartConfig = {
            type: 'bar',
            data: {
                labels: [
                    this.translate.instant('dashboard.chartDraft'),
                    this.translate.instant('dashboard.chartPublished'),
                    this.translate.instant('dashboard.chartPaid')
                ],
                datasets: [{
                    label: this.translate.instant('dashboard.payrollRecords'),
                    data: values,
                    backgroundColor: ['#4338ca', '#2563eb', '#15803d'],
                    borderRadius: 6,
                    barThickness: 48
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { stepSize: 1 },
                        grid: { color: 'rgba(148,163,184,0.12)' }
                    },
                    x: {
                        grid: { display: false }
                    }
                }
            }
        };
    }

    private buildDepartmentChart(data: DashboardData): void {
        if (!data.departmentDistribution || data.departmentDistribution.length === 0) return;

        const palette = ['#0f766e', '#06b6d4', '#f59e0b', '#ea580c', '#6366f1', '#22c55e', '#dc2626', '#475569'];
        const labels = data.departmentDistribution.map(d => d.department);
        const values = data.departmentDistribution.map(d => d.count);

        this.departmentChartConfig = {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{
                    data: values,
                    backgroundColor: labels.map((_, i) => palette[i % palette.length]),
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '55%',
                plugins: {
                    legend: { position: 'bottom', labels: { padding: 16, usePointStyle: true, pointStyle: 'circle' } }
                }
            }
        };
    }

    get quickActions(): QuickAction[] {
        switch (this.data?.role) {
            case 'ADMIN':
                return [
                    { label: this.translate.instant('actions.manageEmployees'), route: '/employees', icon: 'people', accent: 'cyan' },
                    { label: this.translate.instant('actions.manageDepartments'), route: '/departments', icon: 'apartment', accent: 'orange' },
                    { label: this.translate.instant('actions.reviewLeave'), route: '/leaves', icon: 'event_available', accent: 'green' },
                    { label: this.translate.instant('actions.manageUsers'), route: '/users', icon: 'manage_accounts', accent: 'slate' }
                ];
            case 'HR_MANAGER':
                return [
                    { label: this.translate.instant('actions.reviewLeave'), route: '/leaves', icon: 'pending_actions', accent: 'green' },
                    { label: this.translate.instant('actions.processPayroll'), route: '/payrolls', icon: 'payments', accent: 'indigo' },
                    { label: this.translate.instant('actions.employeeDirectory'), route: '/employees', icon: 'badge', accent: 'cyan' },
                    { label: this.translate.instant('actions.notifications'), route: '/notifications', icon: 'notifications', accent: 'slate' }
                ];
            case 'DEPARTMENT_MANAGER':
                return [
                    { label: this.translate.instant('actions.teamLeaveQueue'), route: '/leaves', icon: 'groups', accent: 'green' },
                    { label: this.translate.instant('actions.teamPayroll'), route: '/payrolls', icon: 'receipt_long', accent: 'indigo' },
                    { label: this.translate.instant('actions.employeeProfiles'), route: '/employees', icon: 'person_search', accent: 'cyan' },
                    { label: this.translate.instant('actions.notifications'), route: '/notifications', icon: 'notifications', accent: 'slate' }
                ];
            default:
                return [
                    { label: this.translate.instant('actions.myProfile'), route: '/profile', icon: 'badge', accent: 'cyan' },
                    { label: this.translate.instant('actions.myLeave'), route: '/leaves', icon: 'beach_access', accent: 'green' },
                    { label: this.translate.instant('actions.myPayroll'), route: '/payrolls', icon: 'payments', accent: 'indigo' },
                    { label: this.translate.instant('actions.notifications'), route: '/notifications', icon: 'notifications', accent: 'slate' }
                ];
        }
    }

    activityIcon(type: string): string {
        switch (type) {
            case 'EMPLOYEE':
                return 'person_add';
            case 'LEAVE':
                return 'event_available';
            case 'PAYROLL':
                return 'payments';
            case 'AUDIT':
                return 'shield';
            case 'NOTIFICATION':
                return 'notifications';
            default:
                return 'info';
        }
    }

    activityAccent(type: string): string {
        switch (type) {
            case 'EMPLOYEE':
                return 'cyan';
            case 'LEAVE':
                return 'green';
            case 'PAYROLL':
                return 'indigo';
            case 'AUDIT':
                return 'slate';
            case 'NOTIFICATION':
                return 'orange';
            default:
                return 'slate';
        }
    }

    trackMetric(_: number, metric: DashboardMetric): string {
        return metric.label;
    }

    trackAction(_: number, action: QuickAction): string {
        return action.label;
    }
}
