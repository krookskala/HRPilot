import { Component, inject, OnInit } from "@angular/core";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { NgIf } from "@angular/common";
import { debounceTime, distinctUntilChanged } from "rxjs";
import { UserService } from "../../core/services/user.service";
import { Role, User } from "../../shared/models/user.model";
import { InviteUserDialog } from "./invite-user-dialog";
import { ConfirmDialog } from "../../shared/components/confirm-dialog/confirm-dialog";

@Component({
    selector: 'app-user-list',
    standalone: true,
    imports: [ReactiveFormsModule, MatTableModule, MatButtonModule, MatDialogModule, MatPaginatorModule, MatProgressSpinnerModule, MatIconModule, MatTooltipModule, MatFormFieldModule, MatInputModule, MatSelectModule, NgIf],
    templateUrl: './user-list.html',
    styleUrl: './user-list.scss'
})
export class UserList implements OnInit {
    private userService = inject(UserService);
    private dialog = inject(MatDialog);

    users: User[] = [];
    roles = Object.values(Role);
    displayedColumns = ['email', 'role', 'status', 'lastLoginAt', 'actions'];
    loading = false;
    error = '';
    inviteLink = '';
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;

    emailControl = new FormControl('');
    roleControl = new FormControl<string | null>(null);
    statusControl = new FormControl<string | null>(null);

    ngOnInit(): void {
        this.loadUsers();

        this.emailControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => {
            this.pageIndex = 0;
            this.loadUsers();
        });
        this.roleControl.valueChanges.subscribe(() => {
            this.pageIndex = 0;
            this.loadUsers();
        });
        this.statusControl.valueChanges.subscribe(() => {
            this.pageIndex = 0;
            this.loadUsers();
        });
    }

    loadUsers(): void {
        this.loading = true;
        this.error = '';

        this.userService.getAll({
            email: this.emailControl.value || undefined,
            role: this.roleControl.value || undefined,
            isActive: this.statusControl.value === null ? null : this.statusControl.value === 'active'
        }, this.pageIndex, this.pageSize).subscribe({
            next: page => {
                this.users = page.content;
                this.totalElements = page.totalElements;
                this.loading = false;
            },
            error: () => {
                this.error = 'Failed to load users';
                this.loading = false;
            }
        });
    }

    onPageChange(event: PageEvent): void {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadUsers();
    }

    openInviteDialog(): void {
        const ref = this.dialog.open(InviteUserDialog, { width: '420px' });
        ref.afterClosed().subscribe(result => {
            if (result) {
                this.userService.inviteUser(result).subscribe({
                    next: response => {
                        this.inviteLink = response.inviteUrl;
                        this.loadUsers();
                    }
                });
            }
        });
    }

    resendInvite(user: User): void {
        this.userService.resendInvite(user.id).subscribe({
            next: response => {
                this.inviteLink = response.inviteUrl;
                this.loadUsers();
            }
        });
    }

    toggleActive(user: User): void {
        this.userService.updateUser(user.id, { isActive: !user.isActive }).subscribe({
            next: () => this.loadUsers()
        });
    }

    delete(user: User): void {
        const ref = this.dialog.open(ConfirmDialog, {
            width: '350px',
            data: { title: 'Delete User', message: `Delete ${user.email}?` }
        });

        ref.afterClosed().subscribe(confirmed => {
            if (confirmed) {
                this.userService.deleteUser(user.id).subscribe({
                    next: () => this.loadUsers()
                });
            }
        });
    }
}
