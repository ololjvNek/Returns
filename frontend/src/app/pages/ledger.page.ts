import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { formatDate, ledgerMonthFromReturnDate, money } from '../core/labels';
import { LedgerPageDto, LedgerRowDto, RecognizeRequest } from '../core/models';

@Component({
  selector: 'app-ledger-page',
  imports: [FormsModule],
  template: `
    <div class="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">Ewidencja</h1>
        <p class="text-sm text-slate-500">Miesiąc wynika z kolumny Data zwrotu.</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <input class="input w-44" type="month" [(ngModel)]="period" (ngModelChange)="changePeriod($event)" />
        <button class="btn-alt" type="button" (click)="fillVat()">Wypełnij VAT ({{ page()?.vatRatePercent ?? 23 }}%)</button>
        <button class="btn-alt" type="button" (click)="exportXlsx()">Eksport XLSX</button>
        <button class="btn-primary" type="button" (click)="startManual()">Dodaj ręcznie</button>
      </div>
    </div>

    @if (notice()) {
      <p class="mb-4 rounded-lg bg-blue-50 p-3 text-sm text-blue-800">{{ notice() }}</p>
    }
    @if (error()) {
      <p class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{{ error() }}</p>
    }

    <div class="mb-3 rounded-lg bg-blue-700 px-4 py-3 text-center text-xl font-bold tracking-wide text-white">
      {{ page()?.monthTitle || '—' }}
    </div>

    <div class="table-wrap">
      <table class="w-full text-left text-sm text-slate-700">
        <thead class="bg-slate-100 text-xs uppercase text-slate-600">
          <tr>
            <th class="px-3 py-3" rowspan="2">Lp.</th>
            <th class="px-3 py-3" rowspan="2">Nr Apilo</th>
            <th class="px-3 py-3" rowspan="2">Data sprzedaży</th>
            <th class="px-3 py-3" rowspan="2">Dane kupującego</th>
            <th class="px-3 py-3" rowspan="2">Nazwa towaru / kod</th>
            <th class="px-3 py-3" rowspan="2">Data zwrotu</th>
            <th class="px-3 py-3 text-center" colspan="2">Zwrot należności za towar/usługę</th>
            <th class="px-3 py-3" rowspan="2">Uwagi</th>
            <th class="px-3 py-3" rowspan="2"></th>
          </tr>
          <tr>
            <th class="px-3 py-3">Wartość brutto</th>
            <th class="px-3 py-3">Podatek VAT należny</th>
          </tr>
        </thead>
        <tbody>
          @for (row of page()?.rows ?? []; track row.id) {
            <tr class="border-t border-slate-100 bg-white">
              <td class="px-3 py-2">{{ row.lp }}</td>
              <td class="px-3 py-2">
                <input class="input" [(ngModel)]="row.apiloOrderNumber" (change)="save(row)" />
              </td>
              <td class="px-3 py-2">
                <input class="input" type="date" [(ngModel)]="row.saleDate" (change)="save(row)" />
              </td>
              <td class="px-3 py-2">
                <input class="input" [(ngModel)]="row.buyerName" (change)="save(row)" />
              </td>
              <td class="px-3 py-2">
                <input class="input" [(ngModel)]="row.productCodes" (change)="save(row)" />
              </td>
              <td class="px-3 py-2">
                <input class="input" type="date" [(ngModel)]="row.returnDate" (change)="save(row)" />
              </td>
              <td class="px-3 py-2">
                <input class="input" type="number" step="0.01" [(ngModel)]="row.grossAmount" (change)="save(row)" />
                @if (row.amountEstimated) {
                  <span class="mt-1 badge bg-amber-100 text-amber-800">szac.</span>
                }
              </td>
              <td class="px-3 py-2">
                <input class="input" type="number" step="0.01" [(ngModel)]="row.vatAmount" (change)="save(row)" />
              </td>
              <td class="px-3 py-2">
                <input class="input" [(ngModel)]="row.notes" (change)="save(row)" />
              </td>
              <td class="px-3 py-2">
                <button class="btn-danger py-2" type="button" (click)="remove(row)">Usuń</button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td class="px-4 py-8 text-center text-slate-500" colspan="10">Brak wpisów w tym miesiącu.</td>
            </tr>
          }
        </tbody>
        <tfoot class="bg-slate-50 font-semibold">
          <tr>
            <td class="px-3 py-3 text-right" colspan="6">Razem</td>
            <td class="px-3 py-3">{{ money(page()?.totalGross ?? 0) }}</td>
            <td class="px-3 py-3">{{ money(page()?.totalVat ?? 0) }}</td>
            <td colspan="2"></td>
          </tr>
        </tfoot>
      </table>
    </div>

    @if (manualOpen()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
        <div class="w-full max-w-2xl rounded-xl bg-white p-6">
          <h2 class="mb-4 text-xl font-bold">Dodaj wiersz ręcznie</h2>
          <div class="grid gap-4 md:grid-cols-2">
            <label class="text-sm font-medium">Nr Apilo<input class="input mt-1" [(ngModel)]="manual.apiloOrderNumber" /></label>
            <label class="text-sm font-medium">Data sprzedaży<input class="input mt-1" type="date" [(ngModel)]="manual.saleDate" /></label>
            <label class="text-sm font-medium">Kupujący<input class="input mt-1" [(ngModel)]="manual.buyerName" /></label>
            <label class="text-sm font-medium">Data zwrotu
              <input class="input mt-1" type="date" [(ngModel)]="manual.returnDate" />
              @if (ledgerMonthFromReturnDate(manual.returnDate).title) {
                <span class="mt-1 block text-xs font-normal text-blue-700">
                  Trafi do: {{ ledgerMonthFromReturnDate(manual.returnDate).title }}
                </span>
              }
            </label>
            <label class="text-sm font-medium md:col-span-2">Kod produktu<input class="input mt-1" [(ngModel)]="manual.productCodes" /></label>
            <label class="text-sm font-medium">Wartość brutto<input class="input mt-1" type="number" step="0.01" [(ngModel)]="manual.grossAmount" /></label>
            <label class="text-sm font-medium">Uwagi<input class="input mt-1" [(ngModel)]="manual.notes" /></label>
          </div>
          <div class="mt-6 flex justify-end gap-3">
            <button class="btn-alt" type="button" (click)="manualOpen.set(false)">Anuluj</button>
            <button class="btn-primary" type="button" (click)="saveManual()">Zapisz</button>
          </div>
        </div>
      </div>
    }
  `,
})
export class LedgerPage {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly page = signal<LedgerPageDto | null>(null);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly manualOpen = signal(false);
  readonly money = money;
  readonly formatDate = formatDate;
  readonly ledgerMonthFromReturnDate = ledgerMonthFromReturnDate;
  period = currentPeriod();
  manual: RecognizeRequest = emptyManual(this.period);

  constructor() {
    this.route.queryParamMap.subscribe((params) => {
      const requested = params.get('period');
      if (requested && /^\d{4}-\d{2}$/.test(requested)) {
        this.period = requested;
      }
      this.load();
    });
  }

  changePeriod(value: string) {
    this.period = value;
    void this.router.navigate([], { queryParams: { period: value }, queryParamsHandling: 'merge' });
  }

  load() {
    this.api.ledger(this.period).subscribe({
      next: (page) => this.page.set(page),
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się pobrać ewidencji'),
    });
  }

  save(row: LedgerRowDto) {
    this.api.updateLedger(row.id, toRequest(row)).subscribe({
      next: (saved) => this.followReturnDate(saved.ledgerPeriod || row.returnDate, 'Wiersz przeniesiony do ewidencji'),
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się zapisać wiersza'),
    });
  }

  remove(row: LedgerRowDto) {
    this.api.deleteLedger(row.id).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się usunąć wiersza'),
    });
  }

  fillVat() {
    this.api.fillVat(this.period).subscribe({
      next: (page) => this.page.set(page),
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się wypełnić VAT'),
    });
  }

  exportXlsx() {
    this.api.exportXlsx(this.period).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `ewidencja-zwrotow-${this.period}.xlsx`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się pobrać XLSX'),
    });
  }

  startManual() {
    this.manual = emptyManual(this.period);
    this.manualOpen.set(true);
  }

  saveManual() {
    this.api.createLedger(this.manual).subscribe({
      next: (saved) => {
        this.manualOpen.set(false);
        this.followReturnDate(saved.ledgerPeriod || this.manual.returnDate, 'Wpis dodany do ewidencji');
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Nie udało się dodać wiersza'),
    });
  }

  private followReturnDate(returnDateOrPeriod: string | null, prefix: string) {
    const month = ledgerMonthFromReturnDate(
      returnDateOrPeriod && returnDateOrPeriod.length === 7 ? `${returnDateOrPeriod}-01` : returnDateOrPeriod,
    );
    if (month.period && month.period !== this.period) {
      this.notice.set(`${prefix} ${month.title} (według daty zwrotu).`);
      this.changePeriod(month.period);
      return;
    }
    this.load();
  }
}

function currentPeriod(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

function localIsoDate(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

function emptyManual(period: string): RecognizeRequest {
  const today = localIsoDate();
  return {
    apiloOrderNumber: '',
    saleDate: today,
    buyerName: '',
    productCodes: '',
    returnDate: today.startsWith(period) ? today : `${period}-01`,
    grossAmount: 0,
    currency: 'PLN',
    amountEstimated: false,
    vatAmount: null,
    notes: '',
  };
}

function toRequest(row: LedgerRowDto): RecognizeRequest {
  return {
    apiloOrderNumber: row.apiloOrderNumber,
    saleDate: row.saleDate,
    buyerName: row.buyerName,
    productCodes: row.productCodes,
    returnDate: row.returnDate,
    grossAmount: Number(row.grossAmount),
    currency: row.currency || 'PLN',
    amountEstimated: row.amountEstimated,
    vatAmount: row.vatAmount === null || row.vatAmount === undefined || (row.vatAmount as unknown) === ''
      ? null
      : Number(row.vatAmount),
    notes: row.notes,
  };
}
