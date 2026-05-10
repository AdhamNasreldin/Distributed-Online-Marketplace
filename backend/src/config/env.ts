import "dotenv/config";

export const env = {
  port: Number(process.env.PORT ?? 4000),
  databaseUrl: process.env.DATABASE_URL ?? "postgres://postgres:postgres@localhost:5432/new_era_marketplace",
  frontendOrigin: process.env.FRONTEND_ORIGIN ?? "http://localhost:5173",
  demoTwoFactorCode: process.env.DEMO_2FA_CODE ?? "246810"
};
