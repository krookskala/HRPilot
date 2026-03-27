import { Component, inject } from "@angular/core";
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from "@angular/material/dialog";
import { FormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatButtonModule } from "@angular/material/button";
import { CreateDepartmentRequest } from "../../shared/models/department.model";
import { MatInputModule } from "@angular/material/input";

@Component({
    selector: 'app-department-dialog',
    standalone: true,
    imports: [MatDialogModule, FormsModule, MatFormFieldModule,
    MatInputModule, MatButtonModule],
    templateUrl: './department-dialog.html',
    styleUrl: './department-dialog.scss'
})

export class DepartmentDialog {
    private dialogRef = inject(MatDialogRef<DepartmentDialog>);
    request: CreateDepartmentRequest = {
        name: '',
        managerId: null,
        parentDepartmentId: null
    };

    save() {
        this.dialogRef.close(this.request);
    }

    cancel() {
        this.dialogRef.close();
    }
}
