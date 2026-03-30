import { Component, inject, OnInit } from "@angular/core";
import { DatePipe, DecimalPipe, NgFor, NgIf } from "@angular/common";
import { RouterLink } from "@angular/router";
import { MatCardModule } from "@angular/material/card";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { finalize } from "rxjs";
import { Chart, ArcElement, BarElement, CategoryScale, LinearScale, Tooltip, Legend, PieController, BarController, DoughnutController } from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { ChartConfiguration } from "chart.js";
import { TranslateModule } from "@ngx-translate/core";
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
    imports: [BaseChartDirective, DatePipe, DecimalPipe, MatCardModule, MatIconModule, MatProgressSpinnerModule, NgFor, NgIf, RouterLink, TranslateModule],
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
    private dashboardService = inject(DashboardService);

    data: DashboardData | null = null;
    loading = true;
    error = '';

    leaveChartConfig: ChartConfiguration<'pie'> | null = null;
    payrollChartConfig: ChartConfiguration<'bar'> | null = null;
    departmentChartConfig: ChartConfiguration<'doughnut'> | null = null;

    ngOnInit(): void {
        this.dashboardService.getDashboardData().pipe(
            finalize(() => this.loading = false)
        ).subscribe({
            next: data => {
                this.data = data;
                this.buildCharts(data);
            },
            error: () => {
                this.error = 'Failed to load dashboard data';
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
                labels: ['Pending', 'Approved', 'Rejected', 'Cancelled'],
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
                labels: ['Draft', 'Published', 'Paid'],
                datasets: [{
                    label: 'Payroll Records',
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
                    { label: 'Manage Employees', route: '/employees', icon: 'people', accent: 'cyan' },
                    { label: 'Manage Departments', route: '/departments', icon: 'apartment', accent: 'orange' },
                    { label: 'Review Leave', route: '/leaves', icon: 'event_available', accent: 'green' },
                    { label: 'Manage Users', route: '/users', icon: 'manage_accounts', accent: 'slate' }
                ];
            case 'HR_MANAGER':
                return [
                    { label: 'Review Leave', route: '/leaves', icon: 'pending_actions', accent: 'green' },
                    { label: 'Process Payroll', route: '/payrolls', icon: 'payments', accent: 'indigo' },
                    { label: 'Employee Directory', route: '/employees', icon: 'badge', accent: 'cyan' },
                    { label: 'Notifications', route: '/notifications', icon: 'notifications', accent: 'slate' }
                ];
            case 'DEPARTMENT_MANAGER':
                return [
                    { label: 'Team Leave Queue', route: '/leaves', icon: 'groups', accent: 'green' },
                    { label: 'Team Payroll', route: '/payrolls', icon: 'receipt_long', accent: 'indigo' },
                    { label: 'Employee Profiles', route: '/employees', icon: 'person_search', accent: 'cyan' },
                    { label: 'Notifications', route: '/notifications', icon: 'notifications', accent: 'slate' }
                ];
            default:
                return [
                    { label: 'My Profile', route: '/profile', icon: 'badge', accent: 'cyan' },
                    { label: 'My Leave', route: '/leaves', icon: 'beach_access', accent: 'green' },
                    { label: 'My Payroll', route: '/payrolls', icon: 'payments', accent: 'indigo' },
                    { label: 'Notifications', route: '/notifications', icon: 'notifications', accent: 'slate' }
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
