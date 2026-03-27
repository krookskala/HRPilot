import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { EmployeeService } from "../../core/services/employee.service";
import { Employee } from "../../shared/models/employee.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { EmployeeDialog } from "./employee-dialog";

@Component({
    selector: 'app-employee-list',
    standalone: true,
    imports: [MatTableModule, MatButtonModule, MatDialogModule],
    templateUrl: './employee-list.html',
    styleUrl: './employee-list.scss'
})

export class EmployeeList implements OnInit {
    private employeeService = inject(EmployeeService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);

    employees: Employee[] = [];
    displayedColumns = ['id', 'firstName', 'lastName', 'position', 'salary', 'actions'];

    ngOnInit(): void {
        this.loadEmployees();
    }

    loadEmployees() {
        this.employeeService.getAll().subscribe({
            next: (data) => { 
                this.employees = data;
                this.cdr.detectChanges();
            }
        }); 
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