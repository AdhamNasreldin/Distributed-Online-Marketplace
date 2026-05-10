import { Router } from "express";
import { asyncHandler } from "../middleware/errors.js";
import { marketplaceService } from "../services/marketplaceService.js";

export const authRouter = Router();

authRouter.post(
  "/login",
  asyncHandler(async (request, response) => {
    const { email, password } = request.body as { email?: string; password?: string };
    const user = await marketplaceService.login(email ?? "", password ?? "");
    response.json(user);
  })
);

authRouter.post(
  "/register",
  asyncHandler(async (request, response) => {
    const { fullName, email, password } = request.body as { fullName?: string; email?: string; password?: string };
    const challenge = await marketplaceService.register(fullName ?? "", email ?? "", password ?? "");
    response.status(201).json(challenge);
  })
);

authRouter.post(
  "/verify-2fa",
  asyncHandler(async (request, response) => {
    const { challengeId, code } = request.body as { challengeId?: string; code?: string };
    const user = await marketplaceService.verifyAuthChallenge(challengeId ?? "", code ?? "");
    response.json(user);
  })
);
