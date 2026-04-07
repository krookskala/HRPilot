import { Component, Input, OnChanges, inject } from "@angular/core";
import { BaseChartDirective } from "ng2-charts";
import { Chart, ChartConfiguration, ChartData, BarElement, BarController, LineElement, LineController, PointElement, CategoryScale, LinearScale, Tooltip, Legend, Filler } from "chart.js";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { PayrollRecord } from "../../shared/models/payroll.model";

Chart.register(BarElement, BarController, LineElement, LineController, PointElement, CategoryScale, LinearScale, Tooltip, Legend, Filler);

@Component({
    selector: 'app-payroll-chart',
    standalone: true,
    imports: [BaseChartDirective, TranslateModule],
    templateUrl: './payroll-chart.html',
    styleUrl: './payroll-chart.scss'
})
export class PayrollChart implements OnChanges {
    private translate = inject(TranslateService);

    @Input() payrolls: PayrollRecord[] = [];

    lineChartData: ChartData<'line'> = { labels: [], datasets: [] };
    barChartData: ChartData<'bar'> = { labels: [], datasets: [] };

    lineChartOptions: ChartConfiguration<'line'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { display: true, position: 'top' }
        },
        scales: {
            y: { beginAtZero: false, ticks: { callback: (v) => '€' + v } }
        }
    };

    barChartOptions: ChartConfiguration<'bar'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { display: true, position: 'top' }
        },
        scales: {
            y: { beginAtZero: true, ticks: { callback: (v) => '€' + v } }
        }
    };

    ngOnChanges(): void {
        this.buildCharts();
    }

    private buildCharts(): void {
        if (!this.payrolls.length) return;

        const sorted = [...this.payrolls]
            .filter(p => p.status !== 'DRAFT')
            .sort((a, b) => a.year !== b.year ? a.year - b.year : a.month - b.month);

        const labels = sorted.map(p => `${p.month}/${p.year}`);
        const netData = sorted.map(p => p.netSalary);
        const grossData = sorted.map(p => p.grossSalary);
        const deductionsData = sorted.map(p => p.deductions + p.incomeTax + p.employeeSocialContributions);

        const netLabel = this.translate.instant('payrolls.net');
        const grossLabel = this.translate.instant('payrolls.gross');
        const deductionsLabel = this.translate.instant('payrolls.deductionsLabel');

        this.lineChartData = {
            labels,
            datasets: [
                {
                    data: netData,
                    label: netLabel,
                    borderColor: '#22d3ee',
                    backgroundColor: 'rgba(34, 211, 238, 0.1)',
                    fill: true,
                    tension: 0.3
                }
            ]
        };

        this.barChartData = {
            labels,
            datasets: [
                {
                    data: grossData,
                    label: grossLabel,
                    backgroundColor: 'rgba(34, 211, 238, 0.7)'
                },
                {
                    data: netData,
                    label: netLabel,
                    backgroundColor: 'rgba(34, 197, 94, 0.7)'
                },
                {
                    data: deductionsData,
                    label: deductionsLabel,
                    backgroundColor: 'rgba(251, 146, 60, 0.7)'
                }
            ]
        };
    }
}
