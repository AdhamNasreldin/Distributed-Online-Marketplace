import cors from "cors";
import express from "express";
import { env } from "./config/env.js";
import { pool } from "./db/pool.js";
import { errorHandler, notFoundHandler } from "./middleware/errors.js";
import { authRouter } from "./routes/auth.js";
import { ordersRouter } from "./routes/orders.js";
import { productsRouter } from "./routes/products.js";
import { reportsRouter } from "./routes/reports.js";
import { usersRouter } from "./routes/users.js";
import { walletRouter } from "./routes/wallet.js";

const app = express();

app.use(cors({ origin: env.frontendOrigin, credentials: true }));
app.use(express.json({ limit: "2mb" }));

app.get("/health", async (_request, response, next) => {
  try {
    await pool.query("select 1");
    response.json({ status: "ok", database: "connected" });
  } catch (error) {
    next(error);
  }
});

app.use("/auth", authRouter);
app.use("/users", usersRouter);
app.use("/products", productsRouter);
app.use("/wallet", walletRouter);
app.use("/orders", ordersRouter);
app.use("/reports", reportsRouter);

app.use(notFoundHandler);
app.use(errorHandler);

app.listen(env.port, () => {
  console.log(`New Era marketplace backend running on http://localhost:${env.port}`);
});
