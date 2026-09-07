import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  AllegroAccountDto,
  ApiloDto,
  DeviceStatusDto,
  LedgerPageDto,
  LedgerRowDto,
  PrefillResponse,
  RecognizeRequest,
  ReturnListItem,
  SettingDto,
  UserDto,
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  settings() {
    return this.http.get<SettingDto[]>('/api/settings');
  }

  updateSetting(key: string, value: string) {
    return this.http.put<SettingDto>(`/api/settings/${key}`, { value });
  }

  updateProfile(username: string, currentPassword: string, newPassword: string | null) {
    return this.http.put<UserDto>('/api/auth/profile', { username, currentPassword, newPassword });
  }

  allegroAccounts() {
    return this.http.get<AllegroAccountDto[]>('/api/allegro-accounts');
  }

  createAllegroAccount(body: Partial<AllegroAccountDto> & { clientSecret?: string }) {
    return this.http.post<AllegroAccountDto>('/api/allegro-accounts', body);
  }

  updateAllegroAccount(id: number, body: Partial<AllegroAccountDto> & { clientSecret?: string }) {
    return this.http.put<AllegroAccountDto>(`/api/allegro-accounts/${id}`, body);
  }

  deleteAllegroAccount(id: number) {
    return this.http.delete(`/api/allegro-accounts/${id}`);
  }

  startDeviceFlow(id: number) {
    return this.http.post<DeviceStatusDto>(`/api/allegro-accounts/${id}/device-flow/start`, {});
  }

  deviceStatus(id: number) {
    return this.http.get<DeviceStatusDto>(`/api/allegro-accounts/${id}/device-flow/status`);
  }

  syncAllegro(id?: number) {
    return id
      ? this.http.post(`/api/allegro-accounts/${id}/sync`, {})
      : this.http.post('/api/allegro-accounts/sync', {});
  }

  apilo() {
    return this.http.get<ApiloDto>('/api/apilo');
  }

  updateApilo(body: { baseUrl: string; clientId: string; clientSecret?: string }) {
    return this.http.put<ApiloDto>('/api/apilo', body);
  }

  authorizeApilo(authorizationCode: string) {
    return this.http.post<ApiloDto>('/api/apilo/authorize', { authorizationCode });
  }

  testApilo() {
    return this.http.post<{ ok: boolean; message: string }>('/api/apilo/test', {});
  }

  returns(filters: { accountId?: number; status?: string; localState?: string }) {
    let params = new HttpParams();
    if (filters.accountId) {
      params = params.set('accountId', filters.accountId);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.localState) {
      params = params.set('localState', filters.localState);
    }
    return this.http.get<ReturnListItem[]>('/api/returns', { params });
  }

  prefill(id: string) {
    return this.http.post<PrefillResponse>(`/api/returns/${id}/prefill`, {});
  }

  recognize(id: string, body: RecognizeRequest) {
    return this.http.post<LedgerRowDto>(`/api/returns/${id}/recognize`, body);
  }

  ignore(id: string) {
    return this.http.post(`/api/returns/${id}/ignore`, {});
  }

  ledger(period: string) {
    return this.http.get<LedgerPageDto>('/api/ledger', { params: { period } });
  }

  createLedger(body: RecognizeRequest) {
    return this.http.post<LedgerRowDto>('/api/ledger', body);
  }

  updateLedger(id: number, body: RecognizeRequest) {
    return this.http.put<LedgerRowDto>(`/api/ledger/${id}`, body);
  }

  deleteLedger(id: number) {
    return this.http.delete(`/api/ledger/${id}`);
  }

  fillVat(period: string) {
    return this.http.post<LedgerPageDto>('/api/ledger/fill-vat', {}, { params: { period } });
  }

  exportXlsx(period: string) {
    return this.http.get(`/api/ledger/export.xlsx`, {
      params: { period },
      responseType: 'blob',
    });
  }
}
