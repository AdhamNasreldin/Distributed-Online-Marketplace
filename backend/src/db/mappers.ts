import type { Product, Purchase, ShardSchema, StoredProduct, StoredUser, Transaction, User } from "../types/market.js";

function toIso(value: string | Date) {
  return new Date(value).toISOString();
}

export function toNumber(value: string | number) {
  return typeof value === "number" ? value : Number(value);
}

export function mapUser(row: Record<string, unknown>, schema?: ShardSchema): User {
  return {
    id: String(row.id),
    fullName: String(row.full_name),
    email: String(row.email),
    balance: toNumber(row.balance as string | number),
    createdAt: toIso(row.created_at as string | Date),
    twoFactorEnabled: Boolean(row.two_factor_enabled)
  };
}

export function mapStoredUser(row: Record<string, unknown>, schema: ShardSchema): StoredUser {
  return {
    ...mapUser(row, schema),
    passwordHash: String(row.password_hash),
    schema
  };
}

export function mapProduct(row: Record<string, unknown>): Product {
  return {
    id: String(row.id),
    ownerId: String(row.owner_id),
    name: String(row.name),
    brand: String(row.brand),
    category: String(row.category),
    description: String(row.description),
    price: toNumber(row.price as string | number),
    quantity: Number(row.quantity),
    condition: row.condition as Product["condition"],
    status: row.status as Product["status"],
    listedAt: toIso(row.listed_at as string | Date),
    soldCount: Number(row.sold_count),
    color: String(row.color)
  };
}

export function mapStoredProduct(row: Record<string, unknown>, schema: ShardSchema): StoredProduct {
  return {
    ...mapProduct(row),
    schema
  };
}

export function mapPurchase(row: Record<string, unknown>): Purchase {
  return {
    id: String(row.id),
    productId: String(row.product_id),
    productName: String(row.product_name),
    buyerId: String(row.buyer_id),
    sellerId: String(row.seller_id),
    amount: toNumber(row.amount as string | number),
    purchasedAt: toIso(row.purchased_at as string | Date),
    status: row.status as Purchase["status"]
  };
}

export function mapTransaction(row: Record<string, unknown>): Transaction {
  return {
    id: String(row.id),
    type: row.type as Transaction["type"],
    amount: toNumber(row.amount as string | number),
    fromUserId: row.from_user_id ? String(row.from_user_id) : undefined,
    toUserId: row.to_user_id ? String(row.to_user_id) : undefined,
    productId: row.product_id ? String(row.product_id) : undefined,
    description: String(row.description),
    createdAt: toIso(row.created_at as string | Date),
    status: row.status as Transaction["status"]
  };
}
