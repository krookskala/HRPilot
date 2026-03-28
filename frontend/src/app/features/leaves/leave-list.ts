import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { LeaveService } from "../../core/services/leave.service";
import { AuthService } from "../../core/services/auth.service";
import { LeaveRequest } from "../../shared/models/leave.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { NgIf } from "@angular/common";
import { LeaveDialog } from "./leave-dialog";

@Component({
    selector: 'app-leave-list',
    standalone: true,
    imports: [MatTableModule, MatButtonModule, MatDialogModule, MatPaginatorModule, MatProgressSpinnerModule, NgIf],
    templateUrl: './leave-list.html',
    styleUrl: './leave-list.scss'
})

export class LeaveList implements OnInit {
    private leaveService = inject(LeaveService);
    private authService = inject(AuthService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);

    canApprove = this.authService.hasRole('ADMIN', 'HR_MANAGER', 'DEPARTMENT_MANAGER');
    leaves: LeaveRequest[] = [];
    displayedColumns = ['id', 'employeeFullName', 'type', 'startDate',
'endDate', 'status', 'actions'];
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;
    loading = false;
    error = '';

    ngOnInit(): void {
        this.loadLeaves();
    }

    loadLeaves() {
        this.loading = true;
        this.error = '';
        this.leaveService.getAll(this.pageIndex, this.pageSize).subscribe({
            next: (page) => {
                this.leaves = page.content;
                this.totalElements = page.totalElements;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.error = 'Failed to load leave requests';
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    onPageChange(event: PageEvent) {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadLeaves();
    }

    approve(id: number) {
        this.leaveService.approve(id).subscribe({
            next: () => { this.loadLeaves(); }
        });
    }

    reject(id: number) {
        this.leaveService.reject(id).subscribe({
            next: () => { this.loadLeaves(); }
        });
    }

    openDialog() {
        const ref = this.dialog.open(LeaveDialog, { width: '400px' });
        ref.afterClosed().subscribe(result => {
            if (result) {
                this.leaveService.create(result).subscribe({
                    next: () => { this.loadLeaves(); }
                });
            }
        });
    }
}
