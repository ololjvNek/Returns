import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { UserDto } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly username = signal<string | null>(null);

  ensureCsrf() {
    return this.http.get('/api/auth/csrf');
  }

  ensureSession() {
    return this.http.get<UserDto>('/api/auth/me').pipe(
      tap((user) => this.username.set(user.username)),
      map(() => true),
      catchError(() => {
        this.username.set(null);
        return of(false);
      }),
    );
  }

  login(username: string, password: string) {
    return this.ensureCsrf().pipe(
      switchMap(() => this.http.post<UserDto>('/api/auth/login', { username, password })),
      tap((user) => this.username.set(user.username)),
    );
  }

  logout() {
    return this.http.post('/api/auth/logout', {}).pipe(
      tap(() => {
        this.username.set(null);
        void this.router.navigateByUrl('/logowanie');
      }),
    );
  }
}
