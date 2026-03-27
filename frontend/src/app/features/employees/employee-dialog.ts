import { Component, inject } from "@angular/core";
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from "@angular/material/dialog";
import { FormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatButtonModule } from "@angular/material/button";
import { CreateEmployeeRequest } from "../../shared/models/employee.model";
import { MatInputModule } from "@angular/material/input";

@Component({
    selector: 'app-employee-dialog',
    standalone: true,
    imports: [MatDialogModule, FormsModule, MatFormFieldModule,
    MatInputModule, MatButtonModule],
    templateUrl: './employee-dialog.html',
    styleUrl: './employee-dialog.scss'
})

export class EmployeeDialog {
    private dialogRef = inject(MatDialogRef<EmployeeDialog>);
    request: CreateEmployeeRequest = {
        userId: 0,
        firstName: '',
        lastName: '',
        position: '',
        salary: 0,
        hireDate: '',
        departmentId: 0,
        photoUrl: ''
    };

    save() {
        this.dialogRef.close(this.request);
    }

    cancel() {
        this.dialogRef.close();
    }
}