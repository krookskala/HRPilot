import { Routes } from '@angular/router';
import { Login } from './features/auth/login';
import { Layout } from './layout/layout';
import { Dashboard } from './features/dashboard/dashboard';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { EmployeeList } from './features/employees/employee-list';
import { EmployeeDetailPage } from './features/employees/employee-detail';
import { DepartmentList } from './features/departments/department-list';
import { LeaveList } from './features/leaves/leave-list';
import { PayrollList } from './features/payrolls/payroll-list';
import { AcceptInvite } from './features/auth/accept-invite';
import { RequestPasswordReset } from './features/auth/request-password-reset';
import { ResetPassword } from './features/auth/reset-password';
import { UserList } from './features/users/user-list';
import { MyProfile } from './features/profile/my-profile';
import { NotificationCenter } from './features/notifications/notification-center';

export const routes: Routes = [
    { path: 'login', component: Login },
    { path: 'accept-invite/:token', component: AcceptInvite },
    { path: 'forgot-password', component: RequestPasswordReset },
    { path: 'reset-password/:token', component: ResetPassword },
    {
        path: '',
        component: Layout,
        canActivate: [authGuard],
        children: [
            { path: '', redirectTo: 'dashboard', pathMatch: 'full'},
            { path: 'dashboard', component: Dashboard },
            { path: 'profile', component: MyProfile },
            { path: 'notifications', component: NotificationCenter },
            { path: 'employees', component: EmployeeList},
            { path: 'employees/:id', component: EmployeeDetailPage },
            { path: 'departments', component: DepartmentList},
            { path: 'leaves', component: LeaveList},
            { path: 'payrolls', component: PayrollList },
            { path: 'users', component: UserList, canActivate: [roleGuard('ADMIN')]}
        ]
    },
    { path: '**', redirectTo: '' }
];
