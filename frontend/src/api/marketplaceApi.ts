import {
  demoVerificationCode,
  initialProducts,
  initialPurchases,
  initialTransactions,
  initialUsers
} from "../data/mockData";
import type {
  AuthChallenge,
  Credentials,
  CsvImportRow,
  InventoryItem,
  Listing,
  MarketplaceSnapshot,
  Product,
  Purchase,
  PurchaseChallenge,
  RegisterPayload,
  ReportSummary,
  Transaction,
  User
} from "../types/market";

export interface MarketplaceApi {
  login(credentials: Credentials): Promise<User>;
  register(payload: RegisterPayload): Promise<AuthChallenge>;
  verifyAuthChallenge(challengeId: string, code: string): Promise<User>;
  session(): Promise<User>;
  logout(): Promise<void>;
  getSnapshot(userId: string): Promise<MarketplaceSnapshot>;
  searchListings(userId: string, query: string, category: string): Promise<Listing[]>;
  createProduct(userId: string, product: Omit<Product, "id" | "ownerId" | "listedAt" | "soldCount">): Promise<Product>;
  updateProduct(userId: string, productId: string, updates: Partial<Product>): Promise<Product>;
  removeProduct(userId: string, productId: string): Promise<void>;
  deposit(userId: string, amount: number): Promise<User>;
  beginPurchase(userId: string, productId: string): Promise<PurchaseChallenge>;
  confirmPurchase(userId: string, challengeId: string, code: string): Promise<Purchase>;
  importProducts(userId: string, rows: CsvImportRow[]): Promise<Product[]>;
  getReport(userId: string): Promise<ReportSummary>;
}

const baseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "");

function makeId(prefix: string) {
  if (globalThis.crypto?.randomUUID) {
    return `${prefix}-${globalThis.crypto.randomUUID().slice(0, 8)}`;
  }

  return `${prefix}-${Math.random().toString(16).slice(2, 10)}`;
}

function delay<T>(value: T, ms = 180) {
  return new Promise<T>((resolve) => {
    window.setTimeout(() => resolve(value), ms);
  });
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function listWithSellers(products: Product[], users: User[]): Listing[] {
  return products
    .filter((product) => product.status === "listed" && product.quantity > 0)
    .map((product) => {
      const seller = users.find((user) => user.id === product.ownerId);

      return {
        ...product,
        sellerName: seller?.fullName ?? "Unknown Seller",
        sellerEmail: seller?.email ?? "unknown@marketplace.local"
      };
    });
}

function buildInventory(products: Product[], userId: string): InventoryItem[] {
  return products
    .filter((product) => product.ownerId === userId)
    .map((product) => ({
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

function buildReport(products: Product[], transactions: Transaction[], userId: string): ReportSummary {
  const userProducts = products.filter((product) => product.ownerId === userId);
  const userProductIds = new Set(userProducts.map((product) => product.id));
  const userTransactions = transactions.filter(
    (transaction) =>
      transaction.fromUserId === userId ||
      transaction.toUserId === userId ||
      (transaction.productId && userProductIds.has(transaction.productId))
  );

  const revenueByCategory = userProducts.reduce<Record<string, { count: number; revenue: number }>>((acc, product) => {
    const bucket = acc[product.category] ?? { count: 0, revenue: 0 };
    bucket.count += product.soldCount;
    bucket.revenue += product.soldCount * product.price;
    acc[product.category] = bucket;
    return acc;
  }, {});

  return {
    totalRevenue: userTransactions
      .filter((transaction) => transaction.type === "sale" && transaction.toUserId === userId)
      .reduce((total, transaction) => total + transaction.amount, 0),
    totalTransactions: userTransactions.length,
    activeListings: userProducts.filter((product) => product.status === "listed").length,
    soldItems: userProducts.reduce((total, product) => total + product.soldCount, 0),
    lowStockItems: userProducts.filter((product) => product.quantity <= 1).length,
    topCategories: Object.entries(revenueByCategory)
      .map(([category, value]) => ({ category, ...value }))
      .sort((a, b) => b.revenue - a.revenue),
    recentTransactions: userTransactions
      .slice()
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
      .slice(0, 6)
  };
}

class MockMarketplaceApi implements MarketplaceApi {
  private users = clone(initialUsers);
  private products = clone(initialProducts);
  private purchases = clone(initialPurchases);
  private transactions = clone(initialTransactions);
  private authChallenges = new Map<string, User>();
  private purchaseChallenges = new Map<string, PurchaseChallenge>();

  async login(credentials: Credentials) {
    const user = this.users.find((item) => item.email.toLowerCase() === credentials.email.toLowerCase());

    if (!user || credentials.password.length < 4) {
      throw new Error("Invalid email or password.");
    }

    localStorage.setItem("mock_session", user.id);
    return delay(clone(user));
  }

  async register(payload: RegisterPayload) {
    if (this.users.some((user) => user.email.toLowerCase() === payload.email.toLowerCase())) {
      throw new Error("Email is already registered.");
    }

    const user: User = {
      id: makeId("u"),
      fullName: payload.fullName,
      email: payload.email,
      balance: 5000,
      createdAt: new Date().toISOString(),
      twoFactorEnabled: true
    };
    const challengeId = makeId("auth");

    this.users.push(user);
    this.authChallenges.set(challengeId, user);

    return delay({
      challengeId,
      user: clone(user),
      message: "A verification code was sent to the account email."
    });
  }

  async verifyAuthChallenge(challengeId: string, code: string) {
    const user = this.authChallenges.get(challengeId);

    if (!user || code !== demoVerificationCode) {
      throw new Error("Verification failed. Use the demo code shown on screen.");
    }

    this.authChallenges.delete(challengeId);
    localStorage.setItem("mock_session", user.id);
    return delay(clone(user));
  }

  async session() {
    const userId = localStorage.getItem("mock_session");
    if (!userId) {
      throw new Error("No active session.");
    }
    const user = this.users.find((u) => u.id === userId);
    if (!user) {
      localStorage.removeItem("mock_session");
      throw new Error("Session user not found.");
    }
    return delay(clone(user));
  }

  async logout() {
    localStorage.removeItem("mock_session");
    return delay(undefined);
  }

  async getSnapshot(userId: string) {
    const currentUser = this.getUser(userId);
    const listings = listWithSellers(this.products, this.users);
    const transactions = this.transactions.filter(
      (transaction) => transaction.fromUserId === userId || transaction.toUserId === userId
    );
    const purchases = this.purchases.filter((purchase) => purchase.buyerId === userId || purchase.sellerId === userId);

    return delay({
      listings: clone(listings),
      inventory: clone(buildInventory(this.products, userId)),
      transactions: clone(transactions),
      purchases: clone(purchases),
      report: clone(buildReport(this.products, this.transactions, userId)),
      currentUser: clone(currentUser)
    });
  }

  async searchListings(userId: string, query: string, category: string) {
    const normalizedQuery = query.trim().toLowerCase();
    const listings = listWithSellers(this.products, this.users).filter((listing) => {
      const matchesCategory = category === "All" || listing.category === category;
      const searchable = `${listing.name} ${listing.brand} ${listing.category}`.toLowerCase();
      const matchesQuery = normalizedQuery.length === 0 || searchable.includes(normalizedQuery);

      return matchesCategory && matchesQuery;
    });

    return delay(clone(listings), 120);
  }

  async createProduct(userId: string, product: Omit<Product, "id" | "ownerId" | "listedAt" | "soldCount">) {
    if (product.name && product.name.trim().length > 200) {
      throw new Error("Product title cannot exceed 200 characters.");
    }
    if (product.description && product.description.trim().length > 20000) {
      throw new Error("Product description cannot exceed 20000 characters.");
    }

    const created: Product = {
      ...product,
      id: makeId("p"),
      ownerId: userId,
      listedAt: new Date().toISOString(),
      soldCount: 0
    };

    this.products.unshift(created);
    return delay(clone(created));
  }

  async updateProduct(userId: string, productId: string, updates: Partial<Product>) {
    if (updates.name && updates.name.trim().length > 200) {
      throw new Error("Product title cannot exceed 200 characters.");
    }
    if (updates.description && updates.description.trim().length > 20000) {
      throw new Error("Product description cannot exceed 20000 characters.");
    }

    const index = this.products.findIndex((product) => product.id === productId && product.ownerId === userId);

    if (index === -1) {
      throw new Error("Product was not found in your inventory.");
    }

    this.products[index] = {
      ...this.products[index],
      ...updates,
      ownerId: userId,
      id: productId
    };

    return delay(clone(this.products[index]));
  }

  async removeProduct(userId: string, productId: string) {
    const before = this.products.length;
    this.products = this.products.filter((product) => !(product.id === productId && product.ownerId === userId));

    if (this.products.length === before) {
      throw new Error("Product was not found in your inventory.");
    }

    return delay(undefined);
  }

  async deposit(userId: string, amount: number) {
    if (!Number.isFinite(amount) || amount <= 0) {
      throw new Error("Deposit amount must be positive.");
    }

    const user = this.getUser(userId);
    if (user.balance + amount > 1000000) {
      const maxAllowed = Math.max(0, 1000000 - user.balance);
      throw new Error(`Wallet balance cannot exceed 1,000,000. Maximum you can deposit is ${maxAllowed}.`);
    }

    user.balance += amount;
    this.transactions.unshift({
      id: makeId("tx"),
      type: "deposit",
      amount,
      toUserId: userId,
      description: "Wallet deposit verified",
      createdAt: new Date().toISOString(),
      status: "completed"
    });

    return delay(clone(user));
  }

  async beginPurchase(userId: string, productId: string) {
    const product = this.products.find((item) => item.id === productId && item.status === "listed" && item.quantity > 0);

    if (!product || product.ownerId === userId) {
      throw new Error("This item is not available for purchase.");
    }

    const buyer = this.getUser(userId);
    if (buyer.balance < product.price) {
      throw new Error("Your wallet balance is not enough for this purchase.");
    }

    const listing = listWithSellers([product], this.users)[0];
    const challenge: PurchaseChallenge = {
      challengeId: makeId("buy"),
      product: clone(listing),
      amount: product.price,
      message: "Confirm this purchase with two-factor verification."
    };

    this.purchaseChallenges.set(challenge.challengeId, challenge);
    return delay(clone(challenge));
  }

  async confirmPurchase(userId: string, challengeId: string, code: string) {
    const challenge = this.purchaseChallenges.get(challengeId);

    if (!challenge || code !== demoVerificationCode) {
      throw new Error("Purchase verification failed. Use the demo code shown on screen.");
    }

    const product = this.products.find((item) => item.id === challenge.product.id);
    if (!product || product.quantity <= 0 || product.status !== "listed") {
      throw new Error("The selected item is no longer available.");
    }

    const buyer = this.getUser(userId);
    const seller = this.getUser(product.ownerId);
    if (buyer.balance < product.price) {
      throw new Error("Your wallet balance is not enough for this purchase.");
    }

    buyer.balance -= product.price;
    seller.balance += product.price;
    product.quantity -= 1;
    product.soldCount += 1;
    product.status = product.quantity === 0 ? "sold" : "listed";

    const purchase: Purchase = {
      id: makeId("ord"),
      productId: product.id,
      productName: product.name,
      buyerId: userId,
      sellerId: seller.id,
      amount: product.price,
      purchasedAt: new Date().toISOString(),
      status: "completed"
    };

    this.purchases.unshift(purchase);
    this.transactions.unshift({
      id: makeId("tx"),
      type: "purchase",
      amount: product.price,
      fromUserId: buyer.id,
      toUserId: seller.id,
      productId: product.id,
      description: `Purchased ${product.name} from ${seller.fullName}`,
      createdAt: purchase.purchasedAt,
      status: "completed"
    });
    this.transactions.unshift({
      id: makeId("tx"),
      type: "sale",
      amount: product.price,
      fromUserId: buyer.id,
      toUserId: seller.id,
      productId: product.id,
      description: `Sold ${product.name} to ${buyer.fullName}`,
      createdAt: purchase.purchasedAt,
      status: "completed"
    });
    this.purchaseChallenges.delete(challengeId);

    return delay(clone(purchase));
  }

  async importProducts(userId: string, rows: CsvImportRow[]) {
    const products = rows
      .filter((row) => row.valid)
      .map<Product>((row, index) => ({
        id: makeId("p"),
        ownerId: userId,
        name: row.name,
        brand: row.brand,
        category: row.category,
        description: row.description || `${row.brand} ${row.name} imported from CSV.`,
        price: row.price,
        quantity: row.quantity,
        condition: "New",
        status: row.quantity > 0 ? "listed" : "draft",
        listedAt: new Date(Date.now() + index).toISOString(),
        soldCount: 0,
        color: ["#0f766e", "#1d4ed8", "#be123c", "#b45309", "#475569"][index % 5]
      }));

    this.products.unshift(...products);
    return delay(clone(products));
  }

  async getReport(userId: string) {
    return delay(clone(buildReport(this.products, this.transactions, userId)));
  }

  private getUser(userId: string) {
    const user = this.users.find((item) => item.id === userId);

    if (!user) {
      throw new Error("User was not found.");
    }

    return user;
  }
}

class RestMarketplaceApi implements MarketplaceApi {
  private fallback = new MockMarketplaceApi();

  async login(credentials: Credentials) {
    return this.request<User>("/auth/login", { method: "POST", body: credentials });
  }

  async register(payload: RegisterPayload) {
    return this.request<AuthChallenge>("/auth/register", { method: "POST", body: payload });
  }

  async verifyAuthChallenge(challengeId: string, code: string) {
    return this.request<User>("/auth/verify-2fa", { method: "POST", body: { challengeId, code } });
  }

  async session() {
    return this.request<User>("/auth/session");
  }

  async logout() {
    return this.request<void>("/auth/logout", { method: "POST" });
  }

  async getSnapshot(userId: string) {
    return this.request<MarketplaceSnapshot>(`/users/${userId}/snapshot`);
  }

  async searchListings(userId: string, query: string, category: string) {
    const params = new URLSearchParams({ userId, query, category });
    return this.request<Listing[]>(`/products/search?${params.toString()}`);
  }

  async createProduct(userId: string, product: Omit<Product, "id" | "ownerId" | "listedAt" | "soldCount">) {
    return this.request<Product>("/products", { method: "POST", body: { userId, product } });
  }

  async updateProduct(userId: string, productId: string, updates: Partial<Product>) {
    return this.request<Product>(`/products/${productId}`, { method: "PATCH", body: { userId, updates } });
  }

  async removeProduct(userId: string, productId: string) {
    return this.request<void>(`/products/${productId}`, { method: "DELETE", body: { userId } });
  }

  async deposit(userId: string, amount: number) {
    return this.request<User>("/wallet/deposit", { method: "POST", body: { userId, amount } });
  }

  async beginPurchase(userId: string, productId: string) {
    return this.request<PurchaseChallenge>("/orders/begin-purchase", { method: "POST", body: { userId, productId } });
  }

  async confirmPurchase(userId: string, challengeId: string, code: string) {
    return this.request<Purchase>("/orders/confirm-purchase", { method: "POST", body: { userId, challengeId, code } });
  }

  async importProducts(userId: string, rows: CsvImportRow[]) {
    return this.request<Product[]>("/products/import-csv", { method: "POST", body: { userId, rows } });
  }

  async getReport(userId: string) {
    return this.request<ReportSummary>(`/reports/transactions?userId=${encodeURIComponent(userId)}`);
  }

  private async request<T>(path: string, options: { method?: string; body?: unknown } = {}): Promise<T> {
    if (!baseUrl) {
      return this.requestFallback<T>(path, options);
    }

    try {
      const response = await fetch(`${baseUrl}${path}`, {
        method: options.method ?? "GET",
        headers: options.body ? { "Content-Type": "application/json" } : undefined,
        body: options.body ? JSON.stringify(options.body) : undefined,
        credentials: "include"
      });

      if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`);
      }

      if (response.status === 204) {
        return undefined as T;
      }

      return (await response.json()) as T;
    } catch (error) {
      console.warn("Backend API unavailable, falling back to local demo data.", error);
      return this.requestFallback<T>(path, options);
    }
  }

  private requestFallback<T>(path: string, options: { method?: string; body?: unknown }) {
    const body = (options.body ?? {}) as Record<string, unknown>;

    if (path === "/auth/login") return this.fallback.login(body as unknown as Credentials) as Promise<T>;
    if (path === "/auth/register") return this.fallback.register(body as unknown as RegisterPayload) as Promise<T>;
    if (path === "/auth/verify-2fa") {
      return this.fallback.verifyAuthChallenge(String(body.challengeId), String(body.code)) as Promise<T>;
    }
    if (path === "/auth/session") return this.fallback.session() as Promise<T>;
    if (path === "/auth/logout" && options.method === "POST") return this.fallback.logout() as Promise<T>;
    if (path.includes("/snapshot")) {
      const userId = path.split("/")[2];
      return this.fallback.getSnapshot(userId) as Promise<T>;
    }
    if (path.startsWith("/products/search")) {
      const params = new URLSearchParams(path.split("?")[1]);
      return this.fallback.searchListings(
        params.get("userId") ?? "",
        params.get("query") ?? "",
        params.get("category") ?? "All"
      ) as Promise<T>;
    }
    if (path === "/products" && options.method === "POST") {
      return this.fallback.createProduct(
        String(body.userId),
        body.product as Omit<Product, "id" | "ownerId" | "listedAt" | "soldCount">
      ) as Promise<T>;
    }
    if (path.startsWith("/products/") && options.method === "PATCH") {
      return this.fallback.updateProduct(String(body.userId), path.split("/")[2], body.updates as Partial<Product>) as Promise<T>;
    }
    if (path.startsWith("/products/") && options.method === "DELETE") {
      return this.fallback.removeProduct(String(body.userId), path.split("/")[2]) as Promise<T>;
    }
    if (path === "/wallet/deposit") return this.fallback.deposit(String(body.userId), Number(body.amount)) as Promise<T>;
    if (path === "/orders/begin-purchase") {
      return this.fallback.beginPurchase(String(body.userId), String(body.productId)) as Promise<T>;
    }
    if (path === "/orders/confirm-purchase") {
      return this.fallback.confirmPurchase(String(body.userId), String(body.challengeId), String(body.code)) as Promise<T>;
    }
    if (path === "/products/import-csv") {
      return this.fallback.importProducts(String(body.userId), body.rows as CsvImportRow[]) as Promise<T>;
    }
    if (path.startsWith("/reports/transactions")) {
      const userId = new URLSearchParams(path.split("?")[1]).get("userId") ?? "";
      return this.fallback.getReport(userId) as Promise<T>;
    }

    throw new Error("No mock fallback exists for this request.");
  }
}

export const marketplaceApi: MarketplaceApi = new RestMarketplaceApi();
