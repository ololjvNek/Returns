import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen bg-slate-50">
      <nav class="fixed top-0 z-40 w-full border-b border-slate-200 bg-white">
        <div class="flex items-center justify-between px-6 py-3">
          <div class="flex items-center gap-3">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-700 text-sm font-bold text-white">EZ</div>
            <div>
              <p class="text-sm font-semibold text-slate-900">Ewidencja zwrotów</p>
              <p class="text-xs text-slate-500">Allegro + Apilo</p>
            </div>
          </div>
          <div class="flex items-center gap-4">
            <span class="text-sm text-slate-600">{{ auth.username() }}</span>
            <button class="btn-alt" type="button" (click)="logout()">Wyloguj</button>
          </div>
        </div>
      </nav>
      <aside class="fixed top-[61px] left-0 z-30 h-[calc(100vh-61px)] w-64 border-r border-slate-200 bg-white p-4">
        <ul class="space-y-1">
          @for (item of links; track item.path) {
            <li>
              <a
                [routerLink]="item.path"
                routerLinkActive="bg-blue-50 text-blue-700"
                class="flex rounded-lg px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
              >
                {{ item.label }}
              </a>
            </li>
          }
        </ul>
      </aside>
      <main class="ml-64 pt-[61px]">
        <div class="p-6">
          <router-outlet />
        </div>
      </main>
    </div>
  `,
})
export class ShellComponent {
  readonly auth = inject(AuthService);
  readonly links = [
    { path: '/zwroty', label: 'Zwroty Allegro' },
    { path: '/ewidencja', label: 'Ewidencja' },
    { path: '/ustawienia', label: 'Ustawienia' },
  ];

  logout() {
    this.auth.logout().subscribe();
  }
}
