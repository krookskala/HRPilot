import { Component } from "@angular/core";
import { RouterLink, RouterLinkActive } from "@angular/router";
import { MatListModule } from "@angular/material/list";

@Component({
    selector: 'app-sidebar',
    standalone: true,
    imports: [RouterLink, RouterLinkActive, MatListModule],
    templateUrl: './sidebar.html',
    styleUrl: './sidebar.scss'
})

export class Sidebar {
    
}