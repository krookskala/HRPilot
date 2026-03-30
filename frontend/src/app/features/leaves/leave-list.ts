import { Component, inject, OnInit } from "@angular/core";
import { DatePipe, NgFor, NgIf } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatSelectModule } from "@angular/material/select";
import { MatTableModule } from "@angular/material/table";
import { MatTooltipModule } from "@angular/material/tooltip";
import { AuthService } from "../../core/services/auth.service";
import { LeaveService } from "../../core/services/leave.service";
import { LeaveBalance, LeaveRequest, LeaveRequestHistory, LeaveStatus, LeaveType } from "../../shared/models/leave.model";
import { Role } from "../../shared/models/user.model";
import { LeaveActionDialog } from "./leave-action-dialog";
import { LeaveDialog } from "./leave-dialog";

@Component({
    selector: 'app-leave-list',
    standalone: true,
    imports: [
        DatePipe,
        FormsModule,
        MatButtonModule,
        MatCardModule,
        MatDialogModule,
        MatFormFieldModule,
        MatIconModule,
        MatPaginatorModule,
        MatProgressSpinnerModule,
        MatSelectModule,
        MatTableModule,
        MatTooltipModule,
        NgFor,
        NgIf
    ],
    templateUrl: './leave-list.html',
    styleUrl: './leave-list.scss'
})
export class LeaveList implements OnInit {
    private leaveService = inject(LeaveService);
    private authService = inject(AuthService);
    private dialog = inject(MatDialog);

    readonly currentUser = this.authService.getCurrentUserSnapshot();
    readonly leaveTypes = Object.values(LeaveType);
    readonly leaveStatuses = Object.values(LeaveStatus);
    readonly canApprove = this.authService.hasRole(Role.ADMIN, Role.HR_MANAGER, Role.DEPARTMENT_MANAGER);
    readonly isSelfService = this.currentUser?.role === Role.EMPLOYEE;

    leaves: LeaveRequest[] = [];
    balances: LeaveBalance[] = [];
    historyItems: LeaveRequestHistory[] = [];
    selectedLeaveId: number | null = null;
    displayedColumns = this.canApprove
        ? ['id', 'employeeFullName', 'type', 'startDate', 'endDate', 'workingDays', 'status', 'actions']
        : ['type', 'startDate', 'endDate', 'workingDays', 'status', 'actions'];
    statusFilter: LeaveStatus | '' = '';
    typeFilter: LeaveType | '' = '';
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;
    loading = false;
    error = '';

    ngOnInit(): void {
        this.loadLeaves();
        this.loadBalances();
    }

    loadBalances(): void {
        if (!this.currentUser?.employeeId) {
            this.balances = [];
            return;
        }

        this.leaveService.getMyBalances().subscribe({
            next: (balances) => {
                this.balances = balances;
            }
        });
    }

    loadLeaves(): void {
        this.loading = true;
        this.error = '';

        if (this.isSelfService) {
            this.leaveService.getMine().subscribe({
                next: (result: LeaveRequest[]) => {
                    this.leaves = result;
                    this.totalElements = result.length;
                    this.loading = false;
                },
                error: () => {
                    this.error = 'Failed to load leave requests';
                    this.loading = false;
                }
            });
            return;
        }

        this.leaveService.getAll(this.pageIndex, this.pageSize, {
                status: this.statusFilter || null,
                type: this.typeFilter || null
            }).subscribe({
            next: (result: { content: LeaveRequest[]; totalElements: number }) => {
                this.leaves = result.content;
                this.totalElements = result.totalElements;
                this.loading = false;
            },
            error: () => {
                this.error = 'Failed to load leave requests';
                this.loading = false;
            }
        });
    }

    onPageChange(event: PageEvent): void {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadLeaves();
    }

    applyFilters(): void {
        this.pageIndex = 0;
        this.loadLeaves();
    }

    approve(id: number): void {
        this.leaveService.approve(id).subscribe({
            next: () => {
                this.loadLeaves();
                this.loadBalances();
            }
        });
    }

    reject(id: number): void {
        const ref = this.dialog.open(LeaveActionDialog, {
            width: '420px',
            data: {
                title: 'Reject leave request',
                actionLabel: 'Reject',
                reasonLabel: 'Rejection reason'
            }
        });

        ref.afterClosed().subscribe(reason => {
            if (!reason) {
                return;
            }
            this.leaveService.reject(id, reason).subscribe({
                next: () => this.loadLeaves()
            });
        });
    }

    cancel(leave: LeaveRequest): void {
        const ref = this.dialog.open(LeaveActionDialog, {
            width: '420px',
            data: {
                title: 'Cancel leave request',
                actionLabel: 'Cancel request',
                reasonLabel: 'Cancellation reason',
                presetReason: leave.cancellationReason
            }
        });

        ref.afterClosed().subscribe(reason => {
            if (!reason) {
                return;
            }
            this.leaveService.cancel(leave.id, reason).subscribe({
                next: () => {
                    this.loadLeaves();
                    this.loadBalances();
                }
            });
        });
    }

    toggleHistory(leave: LeaveRequest): void {
        if (this.selectedLeaveId === leave.id) {
            this.selectedLeaveId = null;
            this.historyItems = [];
            return;
        }

        this.selectedLeaveId = leave.id;
        this.leaveService.getHistory(leave.id).subscribe({
            next: (history) => {
                this.historyItems = history;
            },
            error: () => {
                this.historyItems = [];
                this.error = 'Failed to load leave history';
            }
        });
    }

    canCancel(leave: LeaveRequest): boolean {
        if (!this.currentUser?.employeeId) {
            return false;
        }

        return this.currentUser.employeeId === leave.employeeId
            && (leave.status === LeaveStatus.PENDING || leave.status === LeaveStatus.APPROVED);
    }

    openDialog(): void {
        const currentUser = this.currentUser;
        if (!currentUser?.employeeId) {
            this.error = 'Current account is not linked to an employee record';
            return;
        }

        const ref = this.dialog.open(LeaveDialog, { width: '400px' });
        ref.afterClosed().subscribe(result => {
            if (!result) {
                return;
            }

            this.leaveService.create({
                ...result,
                employeeId: currentUser.employeeId
            }).subscribe({
                next: () => this.loadLeaves(),
                error: (err) => {
                    this.error = err?.error?.message ?? 'Failed to create leave request';
                }
            });
        });
    }
}
