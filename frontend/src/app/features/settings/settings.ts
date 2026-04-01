import { Component, inject } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatRadioModule } from "@angular/material/radio";
import { MatIconModule } from "@angular/material/icon";
import { FormsModule } from "@angular/forms";
import { TranslateModule, TranslateService } from "@ngx-translate/core";

@Component({
    selector: 'app-settings',
    standalone: true,
    imports: [MatCardModule, MatRadioModule, MatIconModule, FormsModule, TranslateModule],
    templateUrl: './settings.html',
    styleUrl: './settings.scss'
})
export class Settings {
    private translate = inject(TranslateService);

    selectedLang = this.translate.currentLang || localStorage.getItem('lang') || 'en';

    languages = [
        { code: 'en', label: 'English' },
        { code: 'de', label: 'Deutsch' },
        { code: 'tr', label: 'Türkçe' }
    ];

    onLangChange(lang: string): void {
        this.selectedLang = lang;
        this.translate.use(lang);
        localStorage.setItem('lang', lang);
    }
}
