import { Router } from "express";
import { asyncHandler } from "../middleware/errors.js";
import { marketplaceService } from "../services/marketplaceService.js";

export const reportsRouter = Router();

reportsRouter.get(
  "/transactions",
  asyncHandler(async (request, response) => {
    const userId = String(request.query.userId ?? "");
    const report = await marketplaceService.getReport(userId);
    response.json(report);
  })
);
