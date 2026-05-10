import { Router } from "express";
import { asyncHandler } from "../middleware/errors.js";
import { marketplaceService } from "../services/marketplaceService.js";

export const walletRouter = Router();

walletRouter.post(
  "/deposit",
  asyncHandler(async (request, response) => {
    const { userId, amount } = request.body as { userId?: string; amount?: number };
    const user = await marketplaceService.deposit(userId ?? "", Number(amount));
    response.json(user);
  })
);
