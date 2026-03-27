import { Component, inject } from "@angular/core";
import { MatDialogRef, MatDialogModule } from "@angular/material/dialog";
import { FormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatButtonModule } from "@angular/material/button";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { CreateLeaveRequest, LeaveType } from "../../shared/models/leave.model";

@Component({
    selector: 'app-leave-dialog',
    standalone: true,
    imports: [MatDialogModule, FormsModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatSelectModule],
    templateUrl: './leave-dialog.html',
    styleUrl: './leave-dialog.scss'
})

export class LeaveDialog {
    private dialogRef = inject(MatDialogRef<LeaveDialog>);

    leaveTypes = Object.values(LeaveType);

    request: CreateLeaveRequest = {
        employeeId: 0,
        type: LeaveType.ANNUAL,
        startDate: '',
        endDate: '',
        reason: ''
    };

    save() {
        this.dialogRef.close(this.request);
    }

    cancel() {
        this.dialogRef.close();
    }
}