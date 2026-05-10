# Backend Integration Notes

The frontend works now with local mock data. To connect it to the real backend, set an API base URL and make the backend match these routes, or edit `src/api/marketplaceApi.ts` to match the backend team's route names.

## Setup

Create a `.env` file in this folder:

```env
VITE_API_BASE_URL=http://localhost:YOUR_BACKEND_PORT
```

Then restart the frontend dev server.

## Expected Endpoints

All request/response bodies are JSON.

| Feature | Method | Path |
| --- | --- | --- |
| Login | `POST` | `/auth/login` |
| Register | `POST` | `/auth/register` |
| Verify 2FA | `POST` | `/auth/verify-2fa` |
| Account snapshot | `GET` | `/users/:userId/snapshot` |
| Search listings | `GET` | `/products/search?userId=&query=&category=` |
| Create product | `POST` | `/products` |
| Edit product | `PATCH` | `/products/:productId` |
| Remove product | `DELETE` | `/products/:productId` |
| Deposit wallet cash | `POST` | `/wallet/deposit` |
| Start purchase | `POST` | `/orders/begin-purchase` |
| Confirm purchase with 2FA | `POST` | `/orders/confirm-purchase` |
| Import CSV products | `POST` | `/products/import-csv` |
| Reports | `GET` | `/reports/transactions?userId=` |

## Important

- The backend does not have to use the exact same route names. If it already has different routes, update `src/api/marketplaceApi.ts`.
- Keep the UI untouched during integration. Only the API adapter should change.
- The mock mode remains useful for the demo if the backend is unavailable.
- Demo 2FA code used by the mock frontend is `246810`; the real backend can replace this with email-based 2FA.
