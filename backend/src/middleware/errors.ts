import type { ErrorRequestHandler, NextFunction, Request, Response } from "express";

export class AppError extends Error {
  constructor(
    public status: number,
    message: string
  ) {
    super(message);
  }
}

export function asyncHandler<TRequest extends Request = Request>(
  handler: (request: TRequest, response: Response, next: NextFunction) => Promise<unknown>
) {
  return (request: TRequest, response: Response, next: NextFunction) => {
    void handler(request, response, next).catch(next);
  };
}

export function notFoundHandler(request: Request, _response: Response, next: NextFunction) {
  next(new AppError(404, `Route not found: ${request.method} ${request.path}`));
}

export const errorHandler: ErrorRequestHandler = (error, _request, response, _next) => {
  const status = error instanceof AppError ? error.status : 500;
  const message = error instanceof Error ? error.message : "Unexpected server error";

  if (status >= 500) {
    console.error(error);
  }

  response.status(status).json({ error: message });
};
