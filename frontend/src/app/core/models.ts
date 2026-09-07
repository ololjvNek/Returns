export interface UserDto {
  username: string;
}

export interface SettingDto {
  key: string;
  value: string;
  type: 'STRING' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN';
  label: string;
  description: string | null;
}

export interface AllegroAccountDto {
  id: number;
  name: string;
  clientId: string;
  sandbox: boolean;
  enabled: boolean;
  allegroLogin: string | null;
  lastSyncAt: string | null;
  lastError: string | null;
  authorized: boolean;
  tokenExpiresAt: string | null;
  secretConfigured: boolean;
}

export interface DeviceStatusDto {
  status: 'IDLE' | 'WAITING' | 'AUTHORIZED' | 'ERROR' | 'EXPIRED';
  userCode: string | null;
  verificationUri: string | null;
  verificationUriComplete: string | null;
  message: string | null;
}

export interface ApiloDto {
  baseUrl: string;
  clientId: string | null;
  secretConfigured: boolean;
  authorized: boolean;
  accessExpiresAt: string | null;
  lastError: string | null;
}

export interface ReturnListItem {
  id: string;
  accountId: number;
  accountName: string;
  referenceNumber: string | null;
  orderId: string | null;
  createdAt: string | null;
  status: string | null;
  buyerLogin: string | null;
  itemNames: string[];
  localState: 'NEW' | 'RECOGNIZED' | 'IGNORED';
  apiloOrderNumber: string | null;
}

export interface PrefillResponse {
  returnId: string;
  apiloOrderNumber: string | null;
  saleDate: string | null;
  buyerName: string | null;
  productCodes: string | null;
  returnDate: string;
  grossAmount: number;
  currency: string;
  amountEstimated: boolean;
  notes: string | null;
  warning: string | null;
}

export interface RecognizeRequest {
  apiloOrderNumber: string | null;
  saleDate: string | null;
  buyerName: string | null;
  productCodes: string | null;
  returnDate: string;
  grossAmount: number;
  currency: string;
  amountEstimated: boolean;
  vatAmount: number | null;
  notes: string | null;
}

export interface LedgerRowDto {
  id: number;
  lp: number;
  apiloOrderNumber: string | null;
  saleDate: string | null;
  buyerName: string | null;
  productCodes: string | null;
  returnDate: string;
  grossAmount: number;
  currency: string;
  amountEstimated: boolean;
  vatAmount: number | null;
	notes: string | null;
	allegroReturnId: string | null;
	ledgerPeriod: string;
}

export interface LedgerPageDto {
  period: string;
  monthTitle: string;
  rows: LedgerRowDto[];
  totalGross: number;
  totalVat: number;
  vatRatePercent: number;
}
