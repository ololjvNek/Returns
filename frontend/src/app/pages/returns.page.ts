import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { formatDate, ledgerMonthFromReturnDate, localStateLabel, money, statusLabel } from '../core/labels';
import { AllegroAccountDto, PrefillResponse, RecognizeRequest, ReturnListItem } from '../core/models';

@Component({
  selector: 'app-returns-page',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">Zwroty Allegro</h1>
        <p class="text-sm text-slate-500">Pobierz zwroty i uznaj je do ewidencji. Miesiąc wynika z daty zwrotu.</p>
      </div>
      <button class="btn-primary" type="button" (click)="sync()" [disabled]="busy()">Synchronizuj teraz</button>
    </div>

    <div class="mb-4 grid gap-3 md:grid-cols-3">
      <select class="input" [(ngModel)]="accountId" (ngModelChange)="load()">
        <option [ngValue]="null">Wszystkie konta</option>
        @for (account of accounts(); track account.id) {
          <option [ngValue]="account.id">{{ account.name }}</option>
        }
      </select>
      <select class="input" [(ngModel)]="localState" (ngModelChange)="load()">
        <option value="">Wszystkie stany</option>
        <option value="NEW">Nowe</option>
        <option value="RECOGNIZED">Uznane</option>
        <option value="IGNORED">Ignorowane</option>
      </select>
      <select class="input" [(ngModel)]="status" (ngModelChange)="load()">
        <option value="">Wszystkie statusy Allegro</option>
        <option value="CREATED">Zgłoszony</option>
        <option value="DELIVERED">Dostarczony</option>
        <option value="FINISHED">Zakończony</option>
        <option value="REJECTED">Odrzucony</option>
      </select>
    </div>

    @if (message()) {
      <p class="mb-4 rounded-lg bg-blue-50 p-3 text-sm text-blue-800">
        {{ message() }}
        @if (savedPeriod()) {
          <a class="ml-2 font-medium underline" [routerLink]="'/ewidencja'" [queryParams]="{ period: savedPeriod() }">
            Otwórz ewidencję {{ savedMonthTitle() }}
          </a>
        }
      </p>
    }
    @if (error()) {
      <p class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{{ error() }}</p>
    }

    <div class="table-wrap">
      <table class="w-full text-left text-sm text-slate-700">
        <thead class="bg-slate-100 text-xs uppercase text-slate-600">
          <tr>
            <th class="px-4 py-3">Konto</th>
            <th class="px-4 py-3">Nr referencyjny</th>
            <th class="px-4 py-3">Data zgłoszenia</th>
            <th class="px-4 py-3">Kupujący</th>
            <th class="px-4 py-3">Pozycje</th>
            <th class="px-4 py-3">Status Allegro</th>
            <th class="px-4 py-3">Stan</th>
            <th class="px-4 py-3">Nr Apilo</th>
            <th class="px-4 py-3 text-right">Akcje</th>
          </tr>
        </thead>
        <tbody>
          @for (item of items(); track item.id) {
            <tr class="border-t border-slate-100 bg-white">
              <td class="px-4 py-3">{{ item.accountName }}</td>
              <td class="px-4 py-3 font-medium">{{ item.referenceNumber || '—' }}</td>
              <td class="px-4 py-3">{{ formatDate(item.createdAt) }}</td>
              <td class="px-4 py-3">{{ item.buyerLogin || '—' }}</td>
              <td class="px-4 py-3">{{ item.itemNames.join(', ') || '—' }}</td>
              <td class="px-4 py-3">
                <span class="badge bg-sky-100 text-sky-800">{{ statusLabel(item.status) }}</span>
              </td>
              <td class="px-4 py-3">
                <span class="badge" [class]="stateClass(item.localState)">{{ localStateLabel(item.localState) }}</span>
              </td>
              <td class="px-4 py-3">{{ item.apiloOrderNumber || '—' }}</td>
              <td class="px-4 py-3">
                <div class="flex justify-end gap-2">
                  @if (item.localState === 'NEW') {
                    <button class="btn-success py-2" type="button" (click)="openRecognize(item)">Uznaj zwrot</button>
                    <button class="btn-alt py-2" type="button" (click)="ignore(item)">Ignoruj</button>
                  }
                </div>
              </td>
            </tr>
          } @empty {
            <tr>
              <td class="px-4 py-8 text-center text-slate-500" colspan="9">Brak zwrotów do wyświetlenia.</td>
            </tr>
          }
        </tbody>
      </table>
    </div>

    @if (prefill(); as form) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
        <div class="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-xl bg-white p-6 shadow-xl">
          <h2 class="mb-4 text-xl font-bold">Uznaj zwrot</h2>
          @if (form.warning) {
            <p class="mb-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-800">{{ form.warning }}</p>
          }
          <div class="grid gap-4 md:grid-cols-2">
            <label class="text-sm font-medium">Nr Apilo
              <input class="input mt-1" [(ngModel)]="form.apiloOrderNumber" />
            </label>
            <label class="text-sm font-medium">Data sprzedaży
              <input class="input mt-1" type="date" [(ngModel)]="form.saleDate" />
            </label>
            <label class="text-sm font-medium">Dane kupującego
              <input class="input mt-1" [(ngModel)]="form.buyerName" />
            </label>
            <label class="text-sm font-medium">Data zwrotu
              <input class="input mt-1" type="date" [(ngModel)]="form.returnDate" />
              @if (ledgerMonthFromReturnDate(form.returnDate).title) {
                <span class="mt-1 block text-xs font-normal text-blue-700">
                  Wpis trafi do ewidencji: {{ ledgerMonthFromReturnDate(form.returnDate).title }}
                </span>
              }
            </label>
            <label class="text-sm font-medium md:col-span-2">Nazwa towaru / kod produktu
              <input class="input mt-1" [(ngModel)]="form.productCodes" />
            </label>
            <label class="text-sm font-medium">Wartość brutto
              <input class="input mt-1" type="number" step="0.01" [(ngModel)]="form.grossAmount" />
            </label>
            <label class="text-sm font-medium">Waluta
              <input class="input mt-1" [(ngModel)]="form.currency" />
            </label>
            <label class="text-sm font-medium md:col-span-2">Uwagi
              <input class="input mt-1" [(ngModel)]="form.notes" />
            </label>
          </div>
          <p class="mt-3 text-sm text-slate-500">
            Kwota: {{ money(form.grossAmount, form.currency || 'PLN') }}
            @if (form.amountEstimated) {
              <span class="ml-2 badge bg-amber-100 text-amber-800">kwota szacunkowa</span>
            }
          </p>
          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-alt" type="button" (click)="prefill.set(null)">Anuluj</button>
            <button class="btn-primary" type="button" (click)="saveRecognize()" [disabled]="busy()">Zapisz do ewidencji</button>
          </div>
        </div>
      </div>
    }
  `,
})
export class ReturnsPage {
  private readonly api = inject(ApiService);
  readonly items = signal<ReturnListItem[]>([]);
  readonly accounts = signal<AllegroAccountDto[]>([]);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly savedPeriod = signal<string | null>(null);
  readonly savedMonthTitle = signal<string | null>(null);
  readonly busy = signal(false);
  readonly prefill = signal<PrefillResponse | null>(null);
  readonly formatDate = formatDate;
  readonly statusLabel = statusLabel;
  readonly localStateLabel = localStateLabel;
  readonly money = money;
  readonly ledgerMonthFromReturnDate = ledgerMonthFromReturnDate;

  accountId: number | null = null;
  localState = 'NEW';
  status = '';
  private currentId: string | null = null;

  constructor() {
    this.api.allegroAccounts().subscribe((accounts) => this.accounts.set(accounts));
    this.load();
  }

  load() {
    this.api
      .returns({
        accountId: this.accountId ?? undefined,
        localState: this.localState || undefined,
        status: this.status || undefined,
      })
      .subscribe({
        next: (items) => this.items.set(items),
        error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się pobrać zwrotów'),
      });
  }

  sync() {
    this.busy.set(true);
    this.api.syncAllegro().subscribe({
      next: () => {
        this.message.set('Synchronizacja uruchomiona. Odświeżam listę…');
        this.busy.set(false);
        setTimeout(() => this.load(), 1500);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Synchronizacja nie powiodła się');
        this.busy.set(false);
      },
    });
  }

  openRecognize(item: ReturnListItem) {
    this.currentId = item.id;
    this.busy.set(true);
    this.api.prefill(item.id).subscribe({
      next: (form) => {
        this.prefill.set(form);
        this.busy.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Nie udało się przygotować danych zwrotu');
        this.busy.set(false);
      },
    });
  }

  saveRecognize() {
    const form = this.prefill();
    const id = this.currentId;
    if (!form || !id) {
      return;
    }
    const body: RecognizeRequest = {
      apiloOrderNumber: form.apiloOrderNumber,
      saleDate: form.saleDate,
      buyerName: form.buyerName,
      productCodes: form.productCodes,
      returnDate: form.returnDate,
      grossAmount: Number(form.grossAmount),
      currency: form.currency || 'PLN',
      amountEstimated: form.amountEstimated,
      vatAmount: null,
      notes: form.notes,
    };
    this.busy.set(true);
    this.api.recognize(id, body).subscribe({
      next: (row) => {
        const month = ledgerMonthFromReturnDate(body.returnDate);
        this.prefill.set(null);
        this.busy.set(false);
        this.savedPeriod.set(row.ledgerPeriod || month.period);
        this.savedMonthTitle.set(month.title);
        this.message.set(`Zwrot zapisany w ewidencji ${month.title}.`);
        this.load();
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Nie udało się uznać zwrotu');
        this.busy.set(false);
      },
    });
  }

  ignore(item: ReturnListItem) {
    this.api.ignore(item.id).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się zignorować zwrotu'),
    });
  }

  stateClass(state: string) {
    if (state === 'NEW') {
      return 'bg-emerald-100 text-emerald-800';
    }
    if (state === 'RECOGNIZED') {
      return 'bg-blue-100 text-blue-800';
    }
    return 'bg-slate-200 text-slate-700';
  }
}
