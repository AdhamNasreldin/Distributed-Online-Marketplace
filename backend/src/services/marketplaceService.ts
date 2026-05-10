import bcrypt from "bcryptjs";
import { env } from "../config/env.js";
import { type DbClient, pool, withTransaction } from "../db/pool.js";
import { mapProduct, mapPurchase, mapStoredProduct, mapStoredUser, mapTransaction, mapUser, toNumber } from "../db/mappers.js";
import { shardForKey, shardSchemas } from "../db/shards.js";
import { AppError } from "../middleware/errors.js";
import type {
  AuthChallenge,
  CsvImportRow,
  InventoryItem,
  Listing,
  MarketplaceSnapshot,
  Product,
  ProductStatus,
  Purchase,
  PurchaseChallenge,
  ReportSummary,
  ShardSchema,
  StoredProduct,
  StoredUser,
  Transaction,
  User
} from "../types/market.js";
import { makeId } from "../utils/ids.js";

type CreateProductInput = Omit<Product, "id" | "ownerId" | "listedAt" | "soldCount">;

function publicUser(user: StoredUser): User {
  const { passwordHash: _passwordHash, schema: _schema, ...safeUser } = user;
  return safeUser;
}

function expiresInMinutes(minutes: number) {
  return new Date(Date.now() + minutes * 60 * 1000).toISOString();
}

async function verifyPassword(passwordHash: string, password: string) {
  if (passwordHash.startsWith("plain:")) {
    return passwordHash.slice("plain:".length) === password;
  }

  return bcrypt.compare(password, passwordHash);
}

async function findUserByEmail(email: string, client: DbClient = pool) {
  for (const schema of shardSchemas) {
    const result = await client.query(`select * from ${schema}.users where lower(email) = lower($1) limit 1`, [email]);
    if (result.rows[0]) return mapStoredUser(result.rows[0], schema);
  }

  return null;
}

async function findUserById(userId: string, client: DbClient = pool) {
  for (const schema of shardSchemas) {
    const result = await client.query(`select * from ${schema}.users where id = $1 limit 1`, [userId]);
    if (result.rows[0]) return mapStoredUser(result.rows[0], schema);
  }

  return null;
}

async function lockUserById(userId: string, client: DbClient) {
  const existing = await findUserById(userId, client);
  if (!existing) throw new AppError(404, "User was not found.");

  const result = await client.query(`select * from ${existing.schema}.users where id = $1 for update`, [userId]);
  return mapStoredUser(result.rows[0], existing.schema);
}

async function allUsers(client: DbClient = pool) {
  const rows: StoredUser[] = [];

  for (const schema of shardSchemas) {
    const result = await client.query(`select * from ${schema}.users`);
    rows.push(...result.rows.map((row) => mapStoredUser(row, schema)));
  }

  return rows;
}

async function findProductById(productId: string, client: DbClient = pool) {
  for (const schema of shardSchemas) {
    const result = await client.query(`select * from ${schema}.products where id = $1 limit 1`, [productId]);
    if (result.rows[0]) return mapStoredProduct(result.rows[0], schema);
  }

  return null;
}

async function lockProductById(productId: string, client: DbClient) {
  const existing = await findProductById(productId, client);
  if (!existing) throw new AppError(404, "Product was not found.");

  const result = await client.query(`select * from ${existing.schema}.products where id = $1 for update`, [productId]);
  return mapStoredProduct(result.rows[0], existing.schema);
}

async function allProducts(client: DbClient = pool) {
  const rows: StoredProduct[] = [];

  for (const schema of shardSchemas) {
    const result = await client.query(`select * from ${schema}.products order by listed_at desc`);
    rows.push(...result.rows.map((row) => mapStoredProduct(row, schema)));
  }

  return rows.sort((a, b) => Date.parse(b.listedAt) - Date.parse(a.listedAt));
}

function makeListing(product: Product, sellers: Map<string, StoredUser>): Listing {
  const seller = sellers.get(product.ownerId);

  return {
    ...product,
    sellerName: seller?.fullName ?? "Unknown Seller",
    sellerEmail: seller?.email ?? "unknown@marketplace.local"
  };
}

async function activeListings(client: DbClient = pool) {
  const [products, users] = await Promise.all([allProducts(client), allUsers(client)]);
  const sellers = new Map(users.map((user) => [user.id, user]));

  return products
    .filter((product) => product.status === "listed" && product.quantity > 0)
    .map((product) => makeListing(product, sellers));
}

async function userProducts(userId: string, client: DbClient = pool) {
  const products = await allProducts(client);
  return products.filter((product) => product.ownerId === userId);
}

function buildInventory(products: Product[]): InventoryItem[] {
  return products.map((product) => ({
    productId: product.id,
    productName: product.name,
    brand: product.brand,
    quantity: product.quantity,
    reserved: product.status === "listed" ? Math.min(1, product.quantity) : 0,
    sold: product.soldCount,
    status: product.status,
    updatedAt: product.listedAt
  }));
}

async function userTransactions(userId: string, client: DbClient = pool) {
  const result = await client.query(
    `select * from core.transactions
     where from_user_id = $1 or to_user_id = $1
     order by created_at desc`,
    [userId]
  );
  return result.rows.map(mapTransaction);
}

async function userPurchases(userId: string, client: DbClient = pool) {
  const result = await client.query(
    `select * from core.purchases
     where buyer_id = $1 or seller_id = $1
     order by purchased_at desc`,
    [userId]
  );
  return result.rows.map(mapPurchase);
}

async function buildReport(userId: string, client: DbClient = pool): Promise<ReportSummary> {
  const [products, transactions] = await Promise.all([userProducts(userId, client), userTransactions(userId, client)]);
  const revenue = transactions
    .filter((transaction) => transaction.type === "sale" && transaction.toUserId === userId)
    .reduce((total, transaction) => total + transaction.amount, 0);

  const categories = products.reduce<Record<string, { category: string; count: number; revenue: number }>>((acc, product) => {
    const current = acc[product.category] ?? { category: product.category, count: 0, revenue: 0 };
    current.count += product.soldCount;
    current.revenue += product.soldCount * product.price;
    acc[product.category] = current;
    return acc;
  }, {});

  return {
    totalRevenue: revenue,
    totalTransactions: transactions.length,
    activeListings: products.filter((product) => product.status === "listed").length,
    soldItems: products.reduce((total, product) => total + product.soldCount, 0),
    lowStockItems: products.filter((product) => product.quantity <= 1).length,
    topCategories: Object.values(categories).sort((a, b) => b.revenue - a.revenue),
    recentTransactions: transactions.slice(0, 6)
  };
}

function validateCsvRow(row: CsvImportRow) {
  const errors: string[] = [];
  if (!row.name?.trim()) errors.push("Name is required");
  if (!row.brand?.trim()) errors.push("Brand is required");
  if (!row.category?.trim()) errors.push("Category is required");
  if (!Number.isFinite(Number(row.price)) || Number(row.price) <= 0) errors.push("Price must be positive");
  if (!Number.isInteger(Number(row.quantity)) || Number(row.quantity) < 0) errors.push("Quantity must be a whole number");
  return errors;
}

export const marketplaceService = {
  async login(email: string, password: string) {
    const user = await findUserByEmail(email);

    if (!user || !(await verifyPassword(user.passwordHash, password))) {
      throw new AppError(401, "Invalid email or password.");
    }

    return publicUser(user);
  },

  async register(fullName: string, email: string, password: string): Promise<AuthChallenge> {
    if (!fullName?.trim() || !email?.trim() || password.length < 4) {
      throw new AppError(400, "Full name, valid email, and 4+ character password are required.");
    }

    if (await findUserByEmail(email)) {
      throw new AppError(409, "Email is already registered.");
    }

    const userId = makeId("u");
    const schema = shardForKey(userId);
    const passwordHash = await bcrypt.hash(password, 10);
    const result = await pool.query(
      `insert into ${schema}.users (id, full_name, email, password_hash, balance, two_factor_enabled)
       values ($1, $2, $3, $4, $5, true)
       returning *`,
      [userId, fullName.trim(), email.trim().toLowerCase(), passwordHash, 5000]
    );
    const user = mapStoredUser(result.rows[0], schema);
    const challengeId = makeId("auth");

    await pool.query(
      `insert into core.auth_challenges (id, user_id, code, purpose, expires_at)
       values ($1, $2, $3, 'register', $4)`,
      [challengeId, user.id, env.demoTwoFactorCode, expiresInMinutes(10)]
    );

    return {
      challengeId,
      user: publicUser(user),
      message: "A verification code was sent to the account email."
    };
  },

  async verifyAuthChallenge(challengeId: string, code: string) {
    const result = await pool.query(
      `update core.auth_challenges
       set consumed_at = now()
       where id = $1 and code = $2 and consumed_at is null and expires_at > now()
       returning user_id`,
      [challengeId, code]
    );

    if (!result.rows[0]) {
      throw new AppError(400, "Verification failed or expired.");
    }

    const user = await findUserById(String(result.rows[0].user_id));
    if (!user) throw new AppError(404, "Verified user was not found.");

    return publicUser(user);
  },

  async getSnapshot(userId: string): Promise<MarketplaceSnapshot> {
    const user = await findUserById(userId);
    if (!user) throw new AppError(404, "User was not found.");

    const [listings, products, transactions, purchases, report] = await Promise.all([
      activeListings(),
      userProducts(userId),
      userTransactions(userId),
      userPurchases(userId),
      buildReport(userId)
    ]);

    return {
      listings,
      inventory: buildInventory(products),
      transactions,
      purchases,
      report,
      currentUser: publicUser(user)
    };
  },

  async searchListings(userId: string, query: string, category: string) {
    const normalizedQuery = query.trim().toLowerCase();
    const listings = await activeListings();

    return listings.filter((listing) => {
      const matchesOwner = listing.ownerId !== userId;
      const matchesCategory = category === "All" || !category || listing.category === category;
      const searchable = `${listing.name} ${listing.brand} ${listing.category}`.toLowerCase();
      return matchesOwner && matchesCategory && (normalizedQuery.length === 0 || searchable.includes(normalizedQuery));
    });
  },

  async createProduct(userId: string, product: CreateProductInput) {
    const user = await findUserById(userId);
    if (!user) throw new AppError(404, "User was not found.");

    const price = Number(product.price);
    const quantity = Number(product.quantity);
    if (!product.name?.trim() || !product.brand?.trim() || !product.category?.trim() || price <= 0 || quantity < 0) {
      throw new AppError(400, "Name, brand, category, positive price, and valid quantity are required.");
    }

    const schema = shardForKey(userId);
    const result = await pool.query(
      `insert into ${schema}.products
       (id, owner_id, name, brand, category, description, price, quantity, condition, status, color)
       values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
       returning *`,
      [
        makeId("p"),
        userId,
        product.name.trim(),
        product.brand.trim(),
        product.category.trim(),
        product.description?.trim() || `${product.brand} ${product.name}`,
        price,
        quantity,
        product.condition ?? "New",
        product.status ?? (quantity > 0 ? "listed" : "draft"),
        product.color ?? "#0f766e"
      ]
    );

    return mapProduct(result.rows[0]);
  },

  async updateProduct(userId: string, productId: string, updates: Partial<Product>) {
    const product = await findProductById(productId);
    if (!product || product.ownerId !== userId) {
      throw new AppError(404, "Product was not found in your inventory.");
    }

    const allowed: Record<string, string> = {
      name: "name",
      brand: "brand",
      category: "category",
      description: "description",
      price: "price",
      quantity: "quantity",
      condition: "condition",
      status: "status",
      color: "color"
    };
    const entries = Object.entries(updates).filter(([key, value]) => key in allowed && value !== undefined);

    if (entries.length === 0) {
      return product;
    }

    const setSql = entries.map(([key], index) => `${allowed[key]} = $${index + 1}`).join(", ");
    const values = entries.map(([_key, value]) => value);
    values.push(productId, userId);

    const result = await pool.query(
      `update ${product.schema}.products
       set ${setSql}
       where id = $${values.length - 1} and owner_id = $${values.length}
       returning *`,
      values
    );

    return mapProduct(result.rows[0]);
  },

  async removeProduct(userId: string, productId: string) {
    const product = await findProductById(productId);
    if (!product || product.ownerId !== userId) {
      throw new AppError(404, "Product was not found in your inventory.");
    }

    await pool.query(`delete from ${product.schema}.products where id = $1 and owner_id = $2`, [productId, userId]);
  },

  async deposit(userId: string, amount: number) {
    if (!Number.isFinite(amount) || amount <= 0) {
      throw new AppError(400, "Deposit amount must be positive.");
    }

    return withTransaction(async (client) => {
      const user = await lockUserById(userId, client);
      const updated = await client.query(
        `update ${user.schema}.users set balance = balance + $1 where id = $2 returning *`,
        [amount, userId]
      );

      await client.query(
        `insert into core.transactions (id, type, amount, to_user_id, description, status)
         values ($1, 'deposit', $2, $3, 'Wallet deposit verified', 'completed')`,
        [makeId("tx"), amount, userId]
      );

      return mapUser(updated.rows[0]);
    });
  },

  async beginPurchase(userId: string, productId: string): Promise<PurchaseChallenge> {
    const [buyer, product] = await Promise.all([findUserById(userId), findProductById(productId)]);
    if (!buyer) throw new AppError(404, "Buyer was not found.");
    if (!product || product.status !== "listed" || product.quantity <= 0 || product.ownerId === userId) {
      throw new AppError(400, "This item is not available for purchase.");
    }
    if (buyer.balance < product.price) {
      throw new AppError(400, "Your wallet balance is not enough for this purchase.");
    }

    const seller = await findUserById(product.ownerId);
    if (!seller) throw new AppError(404, "Seller was not found.");

    const challengeId = makeId("buy");
    await pool.query(
      `insert into core.purchase_challenges
       (id, buyer_id, seller_id, product_id, amount, code, expires_at)
       values ($1, $2, $3, $4, $5, $6, $7)`,
      [challengeId, userId, seller.id, product.id, product.price, env.demoTwoFactorCode, expiresInMinutes(10)]
    );

    return {
      challengeId,
      product: makeListing(product, new Map([[seller.id, seller]])),
      amount: product.price,
      message: "Confirm this purchase with two-factor verification."
    };
  },

  async confirmPurchase(userId: string, challengeId: string, code: string): Promise<Purchase> {
    return withTransaction(async (client) => {
      const challenge = await client.query(
        `select * from core.purchase_challenges
         where id = $1 and buyer_id = $2 and code = $3 and consumed_at is null and expires_at > now()
         for update`,
        [challengeId, userId, code]
      );

      if (!challenge.rows[0]) {
        throw new AppError(400, "Purchase verification failed or expired.");
      }

      const product = await lockProductById(String(challenge.rows[0].product_id), client);
      const buyer = await lockUserById(userId, client);
      const seller = await lockUserById(product.ownerId, client);

      if (product.status !== "listed" || product.quantity <= 0) {
        throw new AppError(400, "The selected item is no longer available.");
      }
      if (buyer.balance < product.price) {
        throw new AppError(400, "Your wallet balance is not enough for this purchase.");
      }

      await client.query(`update ${buyer.schema}.users set balance = balance - $1 where id = $2`, [product.price, buyer.id]);
      await client.query(`update ${seller.schema}.users set balance = balance + $1 where id = $2`, [product.price, seller.id]);
      await client.query(
        `update ${product.schema}.products
         set quantity = quantity - 1,
             sold_count = sold_count + 1,
             status = case when quantity - 1 <= 0 then 'sold' else status end
         where id = $1`,
        [product.id]
      );
      await client.query(`update core.purchase_challenges set consumed_at = now() where id = $1`, [challengeId]);

      const purchaseResult = await client.query(
        `insert into core.purchases
         (id, product_id, product_name, buyer_id, seller_id, amount, status)
         values ($1, $2, $3, $4, $5, $6, 'completed')
         returning *`,
        [makeId("ord"), product.id, product.name, buyer.id, seller.id, product.price]
      );
      const purchasedAt = purchaseResult.rows[0].purchased_at;

      await client.query(
        `insert into core.transactions
         (id, type, amount, from_user_id, to_user_id, product_id, description, created_at, status)
         values
         ($1, 'purchase', $3, $4, $5, $6, $7, $8, 'completed'),
         ($2, 'sale', $3, $4, $5, $6, $9, $8, 'completed')`,
        [
          makeId("tx"),
          makeId("tx"),
          product.price,
          buyer.id,
          seller.id,
          product.id,
          `Purchased ${product.name} from ${seller.fullName}`,
          purchasedAt,
          `Sold ${product.name} to ${buyer.fullName}`
        ]
      );

      return mapPurchase(purchaseResult.rows[0]);
    });
  },

  async importProducts(userId: string, rows: CsvImportRow[]) {
    const user = await findUserById(userId);
    if (!user) throw new AppError(404, "User was not found.");

    const validRows = rows.filter((row) => row.valid !== false && validateCsvRow(row).length === 0);
    if (validRows.length === 0) return [];

    return withTransaction(async (client) => {
      const schema = shardForKey(userId);
      const created: Product[] = [];

      for (const row of validRows) {
        const result = await client.query(
          `insert into ${schema}.products
           (id, owner_id, name, brand, category, description, price, quantity, condition, status, color)
           values ($1, $2, $3, $4, $5, $6, $7, $8, 'New', $9, $10)
           returning *`,
          [
            makeId("p"),
            userId,
            row.name.trim(),
            row.brand.trim(),
            row.category.trim(),
            row.description?.trim() || `${row.brand} ${row.name} imported from CSV.`,
            Number(row.price),
            Number(row.quantity),
            Number(row.quantity) > 0 ? "listed" : "draft",
            "#0f766e"
          ]
        );
        created.push(mapProduct(result.rows[0]));
      }

      return created;
    });
  },

  async getReport(userId: string) {
    if (!(await findUserById(userId))) throw new AppError(404, "User was not found.");
    return buildReport(userId);
  },

  async transactionRowsForDebug(userId: string): Promise<Transaction[]> {
    return userTransactions(userId);
  },

  async productById(productId: string) {
    return findProductById(productId);
  },

  numeric(value: string | number) {
    return toNumber(value);
  }
};
