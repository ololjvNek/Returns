export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleDateString('pl-PL');
}

export function statusLabel(status: string | null): string {
  const map: Record<string, string> = {
    CREATED: 'Zgłoszony',
    DISPATCHED: 'Nadany',
    IN_TRANSIT: 'W transporcie',
    DELIVERED: 'Dostarczony',
    FINISHED: 'Zakończony',
    REJECTED: 'Odrzucony',
    COMMISSION_REFUND_CLAIMED: 'Wniosek o prowizję',
    COMMISSION_REFUNDED: 'Prowizja zwrócona',
    WAREHOUSE_DELIVERED: 'W magazynie Allegro',
    WAREHOUSE_VERIFICATION: 'Weryfikacja magazynu',
  };
  return status ? (map[status] ?? status) : '—';
}

export function localStateLabel(state: string): string {
  switch (state) {
    case 'NEW':
      return 'Nowy';
    case 'RECOGNIZED':
      return 'Uznany';
    case 'IGNORED':
      return 'Ignorowany';
    default:
      return state;
  }
}

export function money(value: number | null | undefined, currency = 'PLN'): string {
  if (value === null || value === undefined) {
    return '—';
  }
  return new Intl.NumberFormat('pl-PL', { style: 'currency', currency }).format(value);
}

const MONTHS_PL = [
  'STYCZEŃ',
  'LUTY',
  'MARZEC',
  'KWIECIEŃ',
  'MAJ',
  'CZERWIEC',
  'LIPIEC',
  'SIERPIEŃ',
  'WRZESIEŃ',
  'PAŹDZIERNIK',
  'LISTOPAD',
  'GRUDZIEŃ',
];

/** Miesiąc ewidencji liczony wyłącznie z daty zwrotu (YYYY-MM-DD). */
export function ledgerMonthFromReturnDate(returnDate: string | null | undefined): {
  period: string;
  title: string;
} {
  if (!returnDate || returnDate.length < 7) {
    return { period: '', title: '' };
  }
  const period = returnDate.slice(0, 7);
  const month = Number(period.slice(5, 7));
  const year = period.slice(0, 4);
  const name = MONTHS_PL[month - 1] ?? '';
  return { period, title: name ? `${name} ${year}` : '' };
}

