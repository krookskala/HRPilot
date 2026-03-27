import { Component, inject, OnInit, ChangeDetectorRef } from "@angular/core";
import { DepartmentService } from "../../core/services/department.service";
import { Department } from "../../shared/models/department.model";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { DepartmentDialog } from "./department-dialog";

@Component({
    selector: 'app-department-list',
    standalone: true,
    imports: [MatTableModule, MatButtonModule, MatDialogModule],
    templateUrl: './department-list.html',
    styleUrl: './department-list.scss'
})

export class DepartmentList implements OnInit {
    private departmentService = inject(DepartmentService);
    private cdr = inject(ChangeDetectorRef);
    private dialog = inject(MatDialog);

    departments: Department[] = [];
    displayedColumns = ['id', 'name', 'managerEmail', 'parentDepartmentName', 'actions'];

    ngOnInit(): void {
        this.loadDepartments();
    }

    loadDepartments() {
        this.departmentService.getAll().subscribe({
            next: (data) => { 
                this.departments = data;
                this.cdr.detectChanges();
            }
        });
    }

    delete(id: number) {
        this.departmentService.deleteDepartment(id).subscribe({
            next: () => { this.loadDepartments(); }
        });
    }

    openDialog() {
        const ref = this.dialog.open(DepartmentDialog, { width: '400px' });
        ref.afterClosed().subscribe(result => {
            if (result) {
                this.departmentService.createDepartment(result).subscribe({
                    next: () => { this.loadDepartments(); }
                });
            }
        });
    }
}