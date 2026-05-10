import { Router } from "express";
import { AppError, asyncHandler } from "../middleware/errors.js";
import { marketplaceService } from "../services/marketplaceService.js";
import type { CsvImportRow, Product } from "../types/market.js";

export const productsRouter = Router();

productsRouter.get(
  "/search",
  asyncHandler(async (request, response) => {
    const userId = String(request.query.userId ?? "");
    const query = String(request.query.query ?? "");
    const category = String(request.query.category ?? "All");
    const listings = await marketplaceService.searchListings(userId, query, category);
    response.json(listings);
  })
);

productsRouter.post(
  "/",
  asyncHandler(async (request, response) => {
    const { userId, product } = request.body as {
      userId?: string;
      product?: Omit<Product, "id" | "ownerId" | "listedAt" | "soldCount">;
    };
    if (!product) throw new AppError(400, "Product payload is required.");
    const created = await marketplaceService.createProduct(userId ?? "", product);
    response.status(201).json(created);
  })
);

productsRouter.patch(
  "/:productId",
  asyncHandler(async (request, response) => {
    const { userId, updates } = request.body as { userId?: string; updates?: Partial<Product> };
    const updated = await marketplaceService.updateProduct(userId ?? "", String(request.params.productId), updates ?? {});
    response.json(updated);
  })
);

productsRouter.delete(
  "/:productId",
  asyncHandler(async (request, response) => {
    const { userId } = request.body as { userId?: string };
    await marketplaceService.removeProduct(userId ?? "", String(request.params.productId));
    response.status(204).send();
  })
);

productsRouter.post(
  "/import-csv",
  asyncHandler(async (request, response) => {
    const { userId, rows } = request.body as { userId?: string; rows?: CsvImportRow[] };
    const created = await marketplaceService.importProducts(userId ?? "", rows ?? []);
    response.status(201).json(created);
  })
);
