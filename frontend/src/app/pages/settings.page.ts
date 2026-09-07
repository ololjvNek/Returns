import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { AllegroAccountDto, ApiloDto, DeviceStatusDto, SettingDto } from '../core/models';

@Component({
  selector: 'app-settings-page',
  imports: [FormsModule],
  template: `
    <h1 class="mb-6 text-2xl font-bold text-slate-900">Ustawienia</h1>
    <div class="mb-6 flex flex-wrap gap-2 border-b border-slate-200 pb-2">
      @for (tab of tabs; track tab.id) {
        <button
          class="rounded-lg px-4 py-2 text-sm font-medium"
          [class.bg-blue-700]="tabId() === tab.id"
          [class.text-white]="tabId() === tab.id"
          [class.text-slate-600]="tabId() !== tab.id"
          type="button"
          (click)="tabId.set(tab.id)"
        >
          {{ tab.label }}
        </button>
      }
    </div>

    @if (message()) {
      <p class="mb-4 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{{ message() }}</p>
    }
    @if (error()) {
      <p class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{{ error() }}</p>
    }

    @if (tabId() === 'allegro') {
      <section class="space-y-4">
        <div class="flex justify-between">
          <h2 class="text-lg font-semibold">Konta Allegro</h2>
          <button class="btn-primary" type="button" (click)="newAccount = emptyAccount(); editing.set(true)">Dodaj konto</button>
        </div>
        <div class="table-wrap">
          <table class="w-full text-left text-sm">
            <thead class="bg-slate-100 text-xs uppercase text-slate-600">
              <tr>
                <th class="px-4 py-3">Nazwa</th>
                <th class="px-4 py-3">Login Allegro</th>
                <th class="px-4 py-3">Sandbox</th>
                <th class="px-4 py-3">Autoryzacja</th>
                <th class="px-4 py-3">Ostatni sync</th>
                <th class="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody>
              @for (account of accounts(); track account.id) {
                <tr class="border-t bg-white">
                  <td class="px-4 py-3 font-medium">{{ account.name }}</td>
                  <td class="px-4 py-3">{{ account.allegroLogin || '—' }}</td>
                  <td class="px-4 py-3">{{ account.sandbox ? 'tak' : 'nie' }}</td>
                  <td class="px-4 py-3">{{ account.authorized ? 'połączone' : 'brak' }}</td>
                  <td class="px-4 py-3">{{ account.lastSyncAt || account.lastError || '—' }}</td>
                  <td class="px-4 py-3">
                    <div class="flex flex-wrap justify-end gap-2">
                      <button class="btn-alt py-2" type="button" (click)="startAuth(account)">Autoryzuj</button>
                      <button class="btn-alt py-2" type="button" (click)="syncOne(account)">Synchronizuj</button>
                      <button class="btn-danger py-2" type="button" (click)="removeAccount(account)">Usuń</button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        @if (editing()) {
          <div class="rounded-xl border border-slate-200 bg-white p-4">
            <h3 class="mb-3 font-semibold">Nowe konto</h3>
            <div class="grid gap-3 md:grid-cols-2">
              <input class="input" placeholder="Nazwa" [(ngModel)]="newAccount.name" />
              <input class="input" placeholder="Client ID" [(ngModel)]="newAccount.clientId" />
              <input class="input" placeholder="Client Secret" [(ngModel)]="newAccount.clientSecret" />
              <label class="flex items-center gap-2 text-sm"><input type="checkbox" [(ngModel)]="newAccount.sandbox" /> Środowisko sandbox</label>
            </div>
            <div class="mt-3 flex gap-2">
              <button class="btn-primary" type="button" (click)="saveAccount()">Zapisz</button>
              <button class="btn-alt" type="button" (click)="editing.set(false)">Anuluj</button>
            </div>
          </div>
        }

        @if (device(); as device) {
          <div class="rounded-xl border border-blue-200 bg-blue-50 p-4 text-sm text-blue-900">
            <p class="font-semibold">Autoryzacja Device Flow</p>
            <p class="mt-2">Kod: <span class="font-mono text-lg">{{ device.userCode }}</span></p>
            @if (device.verificationUriComplete) {
              <a class="mt-2 inline-block text-blue-700 underline" [href]="device.verificationUriComplete" target="_blank" rel="noreferrer">Otwórz stronę Allegro</a>
            }
            <p class="mt-2">Status: {{ device.status }} {{ device.message || '' }}</p>
          </div>
        }
      </section>
    }

    @if (tabId() === 'apilo') {
      <section class="max-w-2xl space-y-4 rounded-xl border border-slate-200 bg-white p-5">
        <label class="block text-sm font-medium">Adres API
          <input class="input mt-1" [(ngModel)]="apilo.baseUrl" />
        </label>
        <label class="block text-sm font-medium">Client ID
          <input class="input mt-1" [(ngModel)]="apilo.clientId" />
        </label>
        <label class="block text-sm font-medium">Client Secret
          <input class="input mt-1" type="password" [(ngModel)]="apiloSecret" placeholder="pozostaw puste, aby nie zmieniać" />
        </label>
        <button class="btn-primary" type="button" (click)="saveApilo()">Zapisz dane Apilo</button>
        <label class="block text-sm font-medium">Kod autoryzacyjny
          <input class="input mt-1" [(ngModel)]="apiloCode" />
        </label>
        <div class="flex gap-2">
          <button class="btn-alt" type="button" (click)="authorizeApilo()">Wymień kod na token</button>
          <button class="btn-alt" type="button" (click)="testApilo()">Testuj połączenie</button>
        </div>
        <p class="text-sm text-slate-500">Status: {{ apiloInfo()?.authorized ? 'połączone' : 'brak autoryzacji' }}</p>
      </section>
    }

    @if (tabId() === 'ogolne') {
      <section class="max-w-2xl space-y-4">
        @for (setting of settings(); track setting.key) {
          <label class="block rounded-xl border border-slate-200 bg-white p-4 text-sm">
            <span class="font-medium">{{ setting.label }}</span>
            <p class="mb-2 text-slate-500">{{ setting.description }}</p>
            @if (setting.type === 'BOOLEAN') {
              <select class="input" [ngModel]="setting.value" (ngModelChange)="saveSetting(setting, $event)">
                <option value="true">Tak</option>
                <option value="false">Nie</option>
              </select>
            } @else {
              <input class="input" [ngModel]="setting.value" (change)="saveSetting(setting, $any($event.target).value)" />
            }
          </label>
        }
      </section>
    }

    @if (tabId() === 'uzytkownik') {
      <section class="max-w-md space-y-4 rounded-xl border border-slate-200 bg-white p-5">
        <label class="block text-sm font-medium">Login
          <input class="input mt-1" [(ngModel)]="profileUsername" />
        </label>
        <label class="block text-sm font-medium">Aktualne hasło
          <input class="input mt-1" type="password" [(ngModel)]="currentPassword" />
        </label>
        <label class="block text-sm font-medium">Nowe hasło
          <input class="input mt-1" type="password" [(ngModel)]="newPassword" />
        </label>
        <button class="btn-primary" type="button" (click)="saveProfile()">Zapisz użytkownika</button>
      </section>
    }
  `,
})
export class SettingsPage {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly tabs = [
    { id: 'allegro', label: 'Konta Allegro' },
    { id: 'apilo', label: 'Apilo' },
    { id: 'ogolne', label: 'Ogólne' },
    { id: 'uzytkownik', label: 'Użytkownik' },
  ];
  readonly tabId = signal('allegro');
  readonly accounts = signal<AllegroAccountDto[]>([]);
  readonly settings = signal<SettingDto[]>([]);
  readonly apiloInfo = signal<ApiloDto | null>(null);
  readonly device = signal<DeviceStatusDto | null>(null);
  readonly editing = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  newAccount: { name: string; clientId: string; clientSecret: string; sandbox: boolean; enabled: boolean } = this.emptyAccount();
  apilo = { baseUrl: 'https://api.apilo.com', clientId: '' };
  apiloSecret = '';
  apiloCode = '';
  profileUsername = this.auth.username() ?? 'admin';
  currentPassword = '';
  newPassword = '';
  private pollHandle: number | null = null;

  constructor() {
    this.reload();
  }

  emptyAccount() {
    return { name: '', clientId: '', clientSecret: '', sandbox: false, enabled: true };
  }

  reload() {
    this.api.allegroAccounts().subscribe((accounts) => this.accounts.set(accounts));
    this.api.settings().subscribe((settings) => this.settings.set(settings));
    this.api.apilo().subscribe((apilo) => {
      this.apiloInfo.set(apilo);
      this.apilo = { baseUrl: apilo.baseUrl, clientId: apilo.clientId ?? '' };
    });
  }

  saveAccount() {
    this.api.createAllegroAccount(this.newAccount).subscribe({
      next: () => {
        this.editing.set(false);
        this.message.set('Konto Allegro dodane.');
        this.reload();
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się zapisać konta'),
    });
  }

  removeAccount(account: AllegroAccountDto) {
    this.api.deleteAllegroAccount(account.id).subscribe({
      next: () => this.reload(),
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się usunąć konta'),
    });
  }

  startAuth(account: AllegroAccountDto) {
    this.api.startDeviceFlow(account.id).subscribe({
      next: (status) => {
        this.device.set(status);
        this.poll(account.id);
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się uruchomić autoryzacji'),
    });
  }

  syncOne(account: AllegroAccountDto) {
    this.api.syncAllegro(account.id).subscribe({
      next: () => this.message.set('Synchronizacja konta uruchomiona.'),
      error: (err) => this.error.set(err?.error?.message ?? 'Synchronizacja nie powiodła się'),
    });
  }

  saveApilo() {
    this.api
      .updateApilo({
        baseUrl: this.apilo.baseUrl,
        clientId: this.apilo.clientId,
        clientSecret: this.apiloSecret || undefined,
      })
      .subscribe({
        next: (dto) => {
          this.apiloInfo.set(dto);
          this.message.set('Zapisano dane Apilo.');
        },
        error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się zapisać Apilo'),
      });
  }

  authorizeApilo() {
    this.api.authorizeApilo(this.apiloCode).subscribe({
      next: (dto) => {
        this.apiloInfo.set(dto);
        this.message.set('Apilo zostało autoryzowane.');
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Autoryzacja Apilo nie powiodła się'),
    });
  }

  testApilo() {
    this.api.testApilo().subscribe({
      next: (result) => (result.ok ? this.message.set(result.message) : this.error.set(result.message)),
      error: (err) => this.error.set(err?.error?.message ?? 'Test Apilo nie powiódł się'),
    });
  }

  saveSetting(setting: SettingDto, value: string) {
    this.api.updateSetting(setting.key, value).subscribe({
      next: () => this.message.set('Ustawienie zapisane.'),
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się zapisać ustawienia'),
    });
  }

  saveProfile() {
    this.api.updateProfile(this.profileUsername, this.currentPassword, this.newPassword || null).subscribe({
      next: (user) => {
        this.auth.username.set(user.username);
        this.message.set('Dane użytkownika zapisane.');
        this.currentPassword = '';
        this.newPassword = '';
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się zapisać użytkownika'),
    });
  }

  private poll(id: number) {
    if (this.pollHandle) {
      clearInterval(this.pollHandle);
    }
    this.pollHandle = window.setInterval(() => {
      this.api.deviceStatus(id).subscribe((status) => {
        this.device.set(status);
        if (status.status === 'AUTHORIZED' || status.status === 'ERROR' || status.status === 'EXPIRED') {
          if (this.pollHandle) {
            clearInterval(this.pollHandle);
          }
          this.reload();
        }
      });
    }, 3000);
  }
}
