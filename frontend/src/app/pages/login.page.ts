import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <div class="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-900 via-blue-900 to-slate-800 px-4">
      <div class="w-full max-w-md rounded-2xl bg-white p-8 shadow-xl">
        <h1 class="mb-1 text-2xl font-bold text-slate-900">Ewidencja zwrotów</h1>
        <p class="mb-6 text-sm text-slate-500">Zaloguj się, aby obsługiwać zwroty Allegro i Apilo.</p>
        <form class="space-y-4" (ngSubmit)="submit()">
          <div>
            <label class="mb-2 block text-sm font-medium">Login</label>
            <input class="input" name="username" [(ngModel)]="username" autocomplete="username" required />
          </div>
          <div>
            <label class="mb-2 block text-sm font-medium">Hasło</label>
            <input class="input" type="password" name="password" [(ngModel)]="password" autocomplete="current-password" required />
          </div>
          @if (error()) {
            <p class="rounded-lg bg-red-50 p-3 text-sm text-red-700">{{ error() }}</p>
          }
          <button class="btn-primary w-full" type="submit" [disabled]="busy()">Zaloguj</button>
        </form>
        <p class="mt-4 text-xs text-slate-400">Domyślnie: admin / admin — zmień hasło w Ustawieniach.</p>
      </div>
    </div>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  username = 'admin';
  password = '';
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  submit() {
    this.busy.set(true);
    this.error.set(null);
    this.auth.login(this.username, this.password).subscribe({
      next: () => void this.router.navigateByUrl('/zwroty'),
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Nie udało się zalogować');
        this.busy.set(false);
      },
    });
  }
}
