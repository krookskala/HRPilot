import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { LeaveService } from "../../core/services/leave.service";
import { LeaveRequest } from "../../shared/models/leave.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { LeaveDialog } from "./leave-dialog";

@Component({
    selector: 'app-leave-list',
    standalone: true,
    imports: [MatTableModule, MatButtonModule, MatDialogModule],
    templateUrl: './leave-list.html',
    styleUrl: './leave-list.scss'
})

export class LeaveList implements OnInit {
    private leaveService = inject(LeaveService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);

    leaves: LeaveRequest[] = [];
    displayedColumns = ['id', 'employeeFullName', 'type', 'startDate', 
'endDate', 'status', 'actions'];
    
    ngOnInit(): void {
        this.loadLeaves();
    }

    loadLeaves() {
        this.leaveService.getAll().subscribe({
            next: (data) => {
                this.leaves = data;
                this.cdr.detectChanges();
            }
        });
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