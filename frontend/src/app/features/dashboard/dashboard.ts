import { Component, inject, OnInit } from "@angular/core";
import { DashboardService, DashboardData } from "../../core/services/dashboard.service";
import { MatCardModule } from "@angular/material/card";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { NgIf, NgFor, DecimalPipe, DatePipe } from "@angular/common";
import { RouterLink } from "@angular/router";

@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [MatCardModule, MatProgressSpinnerModule, MatIconModule, MatButtonModule, NgIf, NgFor, DecimalPipe, DatePipe, RouterLink],
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
    private dashboardService = inject(DashboardService);

    data: DashboardData | null = null;
    loading = true;
    error = '';

    ngOnInit(): void {
        this.dashboardService.getDashboardData().subscribe({
            next: (data) => {
                this.data = data;
                this.loading = false;
            },
            error: () => {
                this.error = 'Failed to load dashboard data';
                this.loading = false;
            }
        });
    }
}
