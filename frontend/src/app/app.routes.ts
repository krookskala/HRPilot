import { Routes } from '@angular/router';
import { Login } from './features/auth/login';
import { Layout } from './layout/layout';
import { Dashboard } from './features/dashboard/dashboard';
import { authGuard } from './core/guards/auth.guard';
import { EmployeeList } from './features/employees/employee-list';
import { DepartmentList } from './features/departments/department-list';
import { LeaveList } from './features/leaves/leave-list';

export const routes: Routes = [
    { path: 'login', component: Login },
    { path: '', redirectTo: 'login', pathMatch: 'full'},
    {
        path: '',
        component: Layout,
        canActivate: [authGuard],
        children: [
            { path: 'dashboard', component: Dashboard },
            { path: 'employees', component: EmployeeList},
            { path: 'departments', component: DepartmentList},
            { path: 'leaves', component: LeaveList}
        ]
    }
];
