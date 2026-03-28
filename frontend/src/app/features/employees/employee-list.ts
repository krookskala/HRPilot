import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { EmployeeService } from "../../core/services/employee.service";
import { AuthService } from "../../core/services/auth.service";
import { Employee } from "../../shared/models/employee.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { NgIf } from "@angular/common";
import { EmployeeDialog } from "./employee-dialog";

@Component({
    selector: 'app-employee-list',
    standalone: true,
    imports: [MatTableModule, MatButtonModule, MatDialogModule, MatPaginatorModule, NgIf],
    templateUrl: './employee-list.html',
    styleUrl: './employee-list.scss'
})

export class EmployeeList implements OnInit {
    private employeeService = inject(EmployeeService);
    private authService = inject(AuthService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);

    employees: Employee[] = [];
    displayedColumns = ['id', 'firstName', 'lastName', 'position', 'salary', 'actions'];
    canManage = this.authService.hasRole('ADMIN', 'HR_MANAGER');
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;

    ngOnInit(): void {
        this.loadEmployees();
    }

    loadEmployees() {
        this.employeeService.getAll(this.pageIndex, this.pageSize).subscribe({
            next: (page) => {
                this.employees = page.content;
                this.totalElements = page.totalElements;
                this.cdr.detectChanges();
            }
        });
    }

    onPageChange(event: PageEvent) {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadEmployees();
    }

    delete(id: number) {
        this.employeeService.deleteEmployee(id).subscribe({
            next: () => { this.loadEmployees(); }
        });
    }

    openDialog() {
        const ref = this.dialog.open(EmployeeDialog, { width: '400px' });
        ref.afterClosed().subscribe(result => {
            if (result) {
                this.employeeService.createEmployee(result).subscribe({
                    next: () => { this.loadEmployees(); }
                });
            }
        });
    }
}