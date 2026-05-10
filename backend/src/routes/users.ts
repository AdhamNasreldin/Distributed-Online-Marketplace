import { Router } from "express";
import { asyncHandler } from "../middleware/errors.js";
import { marketplaceService } from "../services/marketplaceService.js";

export const usersRouter = Router();

usersRouter.get(
  "/:userId/snapshot",
  asyncHandler(async (request, response) => {
    const snapshot = await marketplaceService.getSnapshot(String(request.params.userId));
    response.json(snapshot);
  })
);
