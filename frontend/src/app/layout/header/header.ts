import { Component, inject } from "@angular/core";
import { Router } from "@angular/router";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";

@Component({
    selector: 'app-header',
    standalone: true,
    imports: [MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule],
    templateUrl: './header.html',
    styleUrl: './header.scss'
})

export class Header {
    private router = inject(Router);
    logout() {
        localStorage.removeItem('token');
        this.router.navigate(['/login']);
    }
}