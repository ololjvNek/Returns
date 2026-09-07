import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';
import { ShellComponent } from './layout/shell.component';
import { LedgerPage } from './pages/ledger.page';
import { LoginPage } from './pages/login.page';
import { ReturnsPage } from './pages/returns.page';
import { SettingsPage } from './pages/settings.page';

export const routes: Routes = [
  { path: 'logowanie', component: LoginPage, canActivate: [guestGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'zwroty' },
      { path: 'zwroty', component: ReturnsPage },
      { path: 'ewidencja', component: LedgerPage },
      { path: 'ustawienia', component: SettingsPage },
    ],
  },
];
