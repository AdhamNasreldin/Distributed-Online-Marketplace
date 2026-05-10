New Era Marketplace Backend

Stack:
- Node.js + Express + TypeScript
- PostgreSQL
- Logical distributed database model using schemas:
  - core
  - shard_0
  - shard_1

Environment:
1. Copy .env.example to .env
2. Update DATABASE_URL if your PostgreSQL username/password/database are different.

Example .env:
PORT=4000
DATABASE_URL=postgres://postgres:postgres@localhost:5432/new_era_marketplace
FRONTEND_ORIGIN=http://localhost:5173
DEMO_2FA_CODE=246810

Database setup:
1. Create the database in PostgreSQL:
   createdb new_era_marketplace
2. Run schema:
   npm.cmd run db:schema
3. Run seed data:
   npm.cmd run db:seed
Note: the npm database scripts use Node and the pg package, so psql does not need to be installed.

Run backend:
1. Install dependencies:
   npm.cmd install --cache .npm-cache
   If Windows blocks a package script, use:
   npm.cmd install --cache .npm-cache --ignore-scripts
2. Start development server:
   npm.cmd run dev
3. Check health endpoint:
   http://localhost:4000/health

Connect frontend:
1. In ../frontend, create .env:
   VITE_API_BASE_URL=http://localhost:4000
2. Restart the frontend dev server.

Demo accounts:
- youssef@newera.local / demo1234
- nour@newera.local / demo1234
- partner@store.local / demo1234

Demo 2FA code:
- 246810

Important endpoints:
- POST /auth/login
- POST /auth/register
- POST /auth/verify-2fa
- GET /users/:userId/snapshot
- GET /products/search?userId=&query=&category=
- POST /products
- PATCH /products/:productId
- DELETE /products/:productId
- POST /wallet/deposit
- POST /orders/begin-purchase
- POST /orders/confirm-purchase
- POST /products/import-csv
- GET /reports/transactions?userId=
