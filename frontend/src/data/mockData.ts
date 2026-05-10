import type { Product, Purchase, Transaction, User } from "../types/market";

export const demoVerificationCode = "246810";

export const initialUsers: User[] = [
  {
    id: "u-100",
    fullName: "Youssef Adel",
    email: "youssef@newera.local",
    balance: 18650,
    createdAt: "2026-04-10T09:00:00.000Z",
    twoFactorEnabled: true
  },
  {
    id: "u-200",
    fullName: "Nour Hassan",
    email: "nour@newera.local",
    balance: 4300,
    createdAt: "2026-04-12T13:30:00.000Z",
    twoFactorEnabled: true
  },
  {
    id: "u-300",
    fullName: "Store Partner Cairo",
    email: "partner@store.local",
    balance: 9000,
    createdAt: "2026-04-18T17:15:00.000Z",
    twoFactorEnabled: false
  }
];

export const initialProducts: Product[] = [
  {
    id: "p-1001",
    ownerId: "u-200",
    name: "Galaxy Tab S9",
    brand: "Samsung",
    category: "Tablets",
    description: "AMOLED Android tablet with keyboard cover, S Pen, and original charger.",
    price: 22500,
    quantity: 2,
    condition: "Like New",
    status: "listed",
    listedAt: "2026-05-01T10:20:00.000Z",
    soldCount: 1,
    color: "#0f766e"
  },
  {
    id: "p-1002",
    ownerId: "u-300",
    name: "ThinkPad X1 Carbon",
    brand: "Lenovo",
    category: "Laptops",
    description: "Business ultrabook, i7 processor, 16 GB RAM, 512 GB SSD, excellent battery.",
    price: 41000,
    quantity: 1,
    condition: "Used",
    status: "listed",
    listedAt: "2026-05-02T15:12:00.000Z",
    soldCount: 3,
    color: "#334155"
  },
  {
    id: "p-1003",
    ownerId: "u-200",
    name: "WH-1000XM5 Headphones",
    brand: "Sony",
    category: "Audio",
    description: "Noise cancelling headphones with carrying case and Type-C cable.",
    price: 14200,
    quantity: 3,
    condition: "Like New",
    status: "listed",
    listedAt: "2026-05-03T11:48:00.000Z",
    soldCount: 2,
    color: "#7c3aed"
  },
  {
    id: "p-1004",
    ownerId: "u-100",
    name: "Magic Keyboard",
    brand: "Apple",
    category: "Accessories",
    description: "Arabic-English layout keyboard for iPad Pro 11 inch.",
    price: 9800,
    quantity: 4,
    condition: "New",
    status: "listed",
    listedAt: "2026-04-29T08:10:00.000Z",
    soldCount: 4,
    color: "#b45309"
  },
  {
    id: "p-1005",
    ownerId: "u-300",
    name: "EOS M50 Mark II",
    brand: "Canon",
    category: "Cameras",
    description: "Mirrorless camera with kit lens, two batteries, and travel bag.",
    price: 27200,
    quantity: 1,
    condition: "Used",
    status: "listed",
    listedAt: "2026-05-04T19:05:00.000Z",
    soldCount: 0,
    color: "#be123c"
  },
  {
    id: "p-1006",
    ownerId: "u-100",
    name: "Ergo MX Mouse",
    brand: "Logitech",
    category: "Accessories",
    description: "Wireless ergonomic mouse with USB receiver and Bluetooth pairing.",
    price: 3600,
    quantity: 2,
    condition: "New",
    status: "listed",
    listedAt: "2026-05-05T12:26:00.000Z",
    soldCount: 1,
    color: "#2563eb"
  }
];

export const initialPurchases: Purchase[] = [
  {
    id: "ord-501",
    productId: "p-1004",
    productName: "Magic Keyboard",
    buyerId: "u-200",
    sellerId: "u-100",
    amount: 9800,
    purchasedAt: "2026-05-06T14:40:00.000Z",
    status: "completed"
  },
  {
    id: "ord-502",
    productId: "p-1006",
    productName: "Ergo MX Mouse",
    buyerId: "u-300",
    sellerId: "u-100",
    amount: 3600,
    purchasedAt: "2026-05-07T09:05:00.000Z",
    status: "completed"
  }
];

export const initialTransactions: Transaction[] = [
  {
    id: "tx-9001",
    type: "deposit",
    amount: 8000,
    toUserId: "u-100",
    description: "Manual verified wallet deposit",
    createdAt: "2026-05-03T09:00:00.000Z",
    status: "completed"
  },
  {
    id: "tx-9002",
    type: "sale",
    amount: 9800,
    fromUserId: "u-200",
    toUserId: "u-100",
    productId: "p-1004",
    description: "Sold Magic Keyboard to Nour Hassan",
    createdAt: "2026-05-06T14:40:00.000Z",
    status: "completed"
  },
  {
    id: "tx-9003",
    type: "sale",
    amount: 3600,
    fromUserId: "u-300",
    toUserId: "u-100",
    productId: "p-1006",
    description: "Sold Ergo MX Mouse to Store Partner Cairo",
    createdAt: "2026-05-07T09:05:00.000Z",
    status: "completed"
  }
];
