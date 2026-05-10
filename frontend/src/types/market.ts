export type ProductStatus = "listed" | "sold" | "draft";

export type TransactionType = "deposit" | "purchase" | "sale" | "refund";

export type TransactionStatus = "completed" | "pending" | "failed";

export interface User {
  id: string;
  fullName: string;
  email: string;
  balance: number;
  createdAt: string;
  twoFactorEnabled: boolean;
}

export interface Product {
  id: string;
  ownerId: string;
  name: string;
  brand: string;
  category: string;
  description: string;
  price: number;
  quantity: number;
  condition: "New" | "Like New" | "Used";
  status: ProductStatus;
  listedAt: string;
  soldCount: number;
  color: string;
}

export interface Listing extends Product {
  sellerName: string;
  sellerEmail: string;
}

export interface InventoryItem {
  productId: string;
  productName: string;
  brand: string;
  quantity: number;
  reserved: number;
  sold: number;
  status: ProductStatus;
  updatedAt: string;
}

export interface Purchase {
  id: string;
  productId: string;
  productName: string;
  buyerId: string;
  sellerId: string;
  amount: number;
  purchasedAt: string;
  status: TransactionStatus;
}

export interface Transaction {
  id: string;
  type: TransactionType;
  amount: number;
  fromUserId?: string;
  toUserId?: string;
  productId?: string;
  description: string;
  createdAt: string;
  status: TransactionStatus;
}

export interface ReportSummary {
  totalRevenue: number;
  totalTransactions: number;
  activeListings: number;
  soldItems: number;
  lowStockItems: number;
  topCategories: Array<{ category: string; count: number; revenue: number }>;
  recentTransactions: Transaction[];
}

export interface CsvImportRow {
  name: string;
  brand: string;
  price: number;
  quantity: number;
  category: string;
  description: string;
  valid: boolean;
  errors: string[];
}

export interface Credentials {
  email: string;
  password: string;
}

export interface RegisterPayload extends Credentials {
  fullName: string;
}

export interface AuthChallenge {
  challengeId: string;
  user: User;
  message: string;
}

export interface PurchaseChallenge {
  challengeId: string;
  product: Listing;
  amount: number;
  message: string;
}

export interface MarketplaceSnapshot {
  listings: Listing[];
  inventory: InventoryItem[];
  transactions: Transaction[];
  purchases: Purchase[];
  report: ReportSummary;
  currentUser: User;
}
