import { Router } from "express";
import { asyncHandler } from "../middleware/errors.js";
import { marketplaceService } from "../services/marketplaceService.js";

export const ordersRouter = Router();

ordersRouter.post(
  "/begin-purchase",
  asyncHandler(async (request, response) => {
    const { userId, productId } = request.body as { userId?: string; productId?: string };
    const challenge = await marketplaceService.beginPurchase(userId ?? "", productId ?? "");
    response.status(201).json(challenge);
  })
);

ordersRouter.post(
  "/confirm-purchase",
  asyncHandler(async (request, response) => {
    const { userId, challengeId, code } = request.body as { userId?: string; challengeId?: string; code?: string };
    const purchase = await marketplaceService.confirmPurchase(userId ?? "", challengeId ?? "", code ?? "");
    response.status(201).json(purchase);
  })
);
