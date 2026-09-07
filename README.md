# Ewidencja zwrotów (Allegro + Apilo)

Aplikacja do ewidencji zwrotów klienckich z Allegro. Po kliknięciu **Uznaj zwrot** dociąga numer zamówienia z Apilo i kwotę zwrotu z Allegro, a następnie zapisuje wiersz w miesięcznej ewidencji (układ arkusza ze zdjęcia).

## Wymagania

- Java 21
- Docker (PostgreSQL 17 i Redis 7)
- Node.js 22+

PostgreSQL jest mapowany na port **5436**, Redis na **6381**, backend na **8081**, a frontend na **4201** (żeby nie kolidować z innymi projektami na tej maszynie).

## Uruchomienie

```bash
docker compose up -d
cd backend
./mvnw spring-boot:run
```

W drugim terminalu:

```bash
cd frontend
npm start
```

Aplikacja: [http://localhost:4201](http://localhost:4201)  
Backend: [http://localhost:8081](http://localhost:8081)  
Domyślne logowanie: `admin` / `admin` (zmień w Ustawieniach).

## Integracja Allegro (tylko zwroty)

1. Wejdź na [apps.developer.allegro.pl](https://apps.developer.allegro.pl/new) i zarejestruj aplikację typu **Device**.
2. Zakresy (scope) wyłącznie:
   - `allegro:api:orders:read`
   - `allegro:api:payments:read`
3. W aplikacji: **Ustawienia → Konta Allegro** wpisz Client ID i Client Secret, potem **Autoryzuj** (Device Flow).
4. Każde konto Allegro ma własny klucz — nie udostępniaj go osobom trzecim (regulamin REST API, kara 50 000 zł).

Zgodność z regulaminem:

- OAuth Device Flow, bez haseł Allegro w aplikacji
- nagłówek `User-Agent: Returns-Ewidencja/1.0.0`
- limit `GET /order/customer-returns` = 1 req/s na konto
- nie zapisujemy danych kont bankowych kupujących

## Integracja Apilo (numer zamówienia)

1. W panelu Apilo utwórz aplikację API i pobierz Client ID, Client Secret oraz jednorazowy kod autoryzacyjny.
2. **Ustawienia → Apilo**: adres API, dane klienta, wymiana kodu na token, test połączenia.
3. Limit: 200 req/min (edytowalny) z marginesem bezpieczeństwa 80%. Odpowiedzi zamówień są cache’owane w Redis (7 dni).

## Ewidencja

Kolumny: Lp., Nr Apilo, Data sprzedaży, Dane kupującego, Nazwa towaru/kod, Data zwrotu, Wartość brutto, Podatek VAT, Uwagi.

- Wartość brutto pochodzi z `GET /payments/refunds`. Gdy zwrotu płatności jeszcze nie ma, wpisywana jest suma pozycji (oznaczenie „szac.”).
- VAT zostaje pusty do przycisku **Wypełnij VAT** na widoku miesiąca (domyślnie 23%).
- Eksport XLSX odwzorowuje układ arkusza.

## Bezpieczeństwo

Ustaw `APP_ENCRYPTION_KEY` (długi losowy ciąg) przed produkcją. Sekrety i tokeny są szyfrowane AES-GCM w PostgreSQL.
