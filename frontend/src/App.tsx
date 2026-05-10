import {
  AlertCircle,
  BadgeCheck,
  BarChart3,
  Camera,
  CheckCircle2,
  ChevronRight,
  CircleDollarSign,
  ClipboardList,
  CreditCard,
  Database,
  FileSpreadsheet,
  Headphones,
  KeyRound,
  Laptop,
  LayoutDashboard,
  LockKeyhole,
  LogOut,
  Package,
  PackageCheck,
  Plus,
  Search,
  ShieldCheck,
  ShoppingBag,
  Store,
  Trash2,
  Upload,
  UserRound,
  Wallet,
  X
} from "lucide-react";
import type { CSSProperties, FormEvent, ReactElement, ReactNode } from "react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { marketplaceApi } from "./api/marketplaceApi";
import { demoVerificationCode } from "./data/mockData";
import type {
  AuthChallenge,
  CsvImportRow,
  InventoryItem,
  Listing,
  MarketplaceSnapshot,
  Product,
  ProductStatus,
  PurchaseChallenge,
  ReportSummary,
  Transaction,
  User
} from "./types/market";
import { parseProductCsv } from "./utils/csv";

type ViewId = "market" | "sell" | "inventory" | "wallet" | "reports" | "csv" | "store";
type Toast = { type: "success" | "error"; message: string };

const categories = ["All", "Laptops", "Tablets", "Audio", "Cameras", "Accessories"];
const palette = ["#0f766e", "#1d4ed8", "#be123c", "#b45309", "#475569", "#7c3aed"];

function formatCurrency(amount: number) {
  return `EGP ${new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(amount)}`;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }).format(
    new Date(value)
  );
}

function makeProductColor(indexSeed: string) {
  const seed = indexSeed.split("").reduce((total, letter) => total + letter.charCodeAt(0), 0);
  return palette[seed % palette.length];
}

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [snapshot, setSnapshot] = useState<MarketplaceSnapshot | null>(null);
  const [activeView, setActiveView] = useState<ViewId>("market");
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<Toast | null>(null);

  const notify = useCallback((type: Toast["type"], message: string) => {
    setToast({ type, message });
    window.setTimeout(() => setToast(null), 3200);
  }, []);

  const refresh = useCallback(
    async (userId = user?.id) => {
      if (!userId) return;

      setLoading(true);
      try {
        const nextSnapshot = await marketplaceApi.getSnapshot(userId);
        setSnapshot(nextSnapshot);
        setUser(nextSnapshot.currentUser);
      } catch (error) {
        notify("error", error instanceof Error ? error.message : "Could not refresh marketplace data.");
      } finally {
        setLoading(false);
      }
    },
    [notify, user?.id]
  );

  const handleAuthenticated = (nextUser: User) => {
    setUser(nextUser);
    void refresh(nextUser.id);
  };

  if (!user) {
    return <AuthPage notify={notify} onAuthenticated={handleAuthenticated} />;
  }

  const pageTitle = {
    market: "Marketplace",
    sell: "Sell Item",
    inventory: "Inventory",
    wallet: "Wallet",
    reports: "Reports",
    csv: "CSV Import",
    store: "Store Portal"
  }[activeView];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-mark">NE</div>
          <div>
            <strong>New Era</strong>
            <span>Distributed Market</span>
          </div>
        </div>

        <nav className="nav-list" aria-label="Main navigation">
          <NavButton icon={<Search />} label="Marketplace" active={activeView === "market"} onClick={() => setActiveView("market")} />
          <NavButton icon={<Plus />} label="Sell Item" active={activeView === "sell"} onClick={() => setActiveView("sell")} />
          <NavButton icon={<ClipboardList />} label="Inventory" active={activeView === "inventory"} onClick={() => setActiveView("inventory")} />
          <NavButton icon={<Wallet />} label="Wallet" active={activeView === "wallet"} onClick={() => setActiveView("wallet")} />
          <NavButton icon={<BarChart3 />} label="Reports" active={activeView === "reports"} onClick={() => setActiveView("reports")} />
          <NavButton icon={<FileSpreadsheet />} label="CSV Import" active={activeView === "csv"} onClick={() => setActiveView("csv")} />
          <NavButton icon={<Store />} label="Store Portal" active={activeView === "store"} onClick={() => setActiveView("store")} />
        </nav>

        <button className="ghost-button logout-button" onClick={() => setUser(null)}>
          <LogOut size={18} />
          Log out
        </button>
      </aside>

      <main className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">CSE352s project frontend</p>
            <h1>{pageTitle}</h1>
          </div>
          <div className="user-pill">
            <div className="avatar">
              <UserRound size={18} />
            </div>
            <div>
              <strong>{user.fullName}</strong>
              <span>{formatCurrency(user.balance)}</span>
            </div>
          </div>
        </header>

        {snapshot ? (
          <>
            <OverviewStrip snapshot={snapshot} />
            {activeView === "market" && <MarketplaceScreen user={user} notify={notify} refresh={refresh} />}
            {activeView === "sell" && <SellItemScreen user={user} notify={notify} refresh={refresh} />}
            {activeView === "inventory" && <InventoryScreen inventory={snapshot.inventory} notify={notify} refresh={refresh} user={user} />}
            {activeView === "wallet" && <WalletScreen snapshot={snapshot} notify={notify} refresh={refresh} user={user} />}
            {activeView === "reports" && <ReportsScreen report={snapshot.report} />}
            {activeView === "csv" && <CsvImportScreen user={user} notify={notify} refresh={refresh} />}
            {activeView === "store" && <StorePortalScreen listings={snapshot.listings} notify={notify} />}
          </>
        ) : (
          <div className="loading-panel">
            <Database className="spin-slow" size={26} />
            Loading marketplace data
          </div>
        )}

        {loading && <div className="loading-ribbon">Syncing latest data...</div>}
      </main>

      {toast && (
        <div className={`toast ${toast.type}`}>
          {toast.type === "success" ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
          {toast.message}
        </div>
      )}
    </div>
  );
}

function NavButton({ icon, label, active, onClick }: { icon: ReactElement; label: string; active: boolean; onClick: () => void }) {
  return (
    <button className={`nav-button ${active ? "active" : ""}`} onClick={onClick}>
      {icon}
      <span>{label}</span>
      <ChevronRight size={16} />
    </button>
  );
}

function OverviewStrip({ snapshot }: { snapshot: MarketplaceSnapshot }) {
  const cards = [
    { label: "Balance", value: formatCurrency(snapshot.currentUser.balance), icon: <Wallet /> },
    { label: "Active listings", value: snapshot.report.activeListings.toString(), icon: <ShoppingBag /> },
    { label: "Sold items", value: snapshot.report.soldItems.toString(), icon: <PackageCheck /> },
    { label: "Transactions", value: snapshot.report.totalTransactions.toString(), icon: <Database /> }
  ];

  return (
    <section className="overview-grid" aria-label="Account overview">
      {cards.map((card) => (
        <div className="metric-card" key={card.label}>
          <div className="metric-icon">{card.icon}</div>
          <span>{card.label}</span>
          <strong>{card.value}</strong>
        </div>
      ))}
    </section>
  );
}

function AuthPage({ onAuthenticated, notify }: { onAuthenticated: (user: User) => void; notify: (type: Toast["type"], message: string) => void }) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [fullName, setFullName] = useState("Demo Student");
  const [email, setEmail] = useState("youssef@newera.local");
  const [password, setPassword] = useState("demo1234");
  const [challenge, setChallenge] = useState<AuthChallenge | null>(null);
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      if (mode === "login") {
        const loggedInUser = await marketplaceApi.login({ email, password });
        onAuthenticated(loggedInUser);
      } else {
        const nextChallenge = await marketplaceApi.register({ fullName, email, password });
        setChallenge(nextChallenge);
        notify("success", "Account created. Complete email verification to continue.");
      }
    } catch (error) {
      notify("error", error instanceof Error ? error.message : "Authentication failed.");
    } finally {
      setLoading(false);
    }
  };

  const verify = async (event: FormEvent) => {
    event.preventDefault();
    if (!challenge) return;

    setLoading(true);
    try {
      const verifiedUser = await marketplaceApi.verifyAuthChallenge(challenge.challengeId, code);
      onAuthenticated(verifiedUser);
    } catch (error) {
      notify("error", error instanceof Error ? error.message : "Verification failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout">
      <section className="auth-panel">
        <div className="brand-block large">
          <div className="brand-mark">NE</div>
          <div>
            <strong>New Era Marketplace</strong>
            <span>Distributed online selling system</span>
          </div>
        </div>

        {!challenge ? (
          <form className="auth-form" onSubmit={submit}>
            <div className="segmented">
              <button type="button" className={mode === "login" ? "selected" : ""} onClick={() => setMode("login")}>
                Login
              </button>
              <button type="button" className={mode === "register" ? "selected" : ""} onClick={() => setMode("register")}>
                Register
              </button>
            </div>

            {mode === "register" && (
              <label>
                Full name
                <input value={fullName} onChange={(event) => setFullName(event.target.value)} required />
              </label>
            )}

            <label>
              Email
              <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
            </label>
            <label>
              Password
              <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={4} />
            </label>

            <button className="primary-button" disabled={loading}>
              <LockKeyhole size={18} />
              {mode === "login" ? "Enter marketplace" : "Create account"}
            </button>

            <div className="demo-note">
              <BadgeCheck size={18} />
              Demo login is prefilled. Any password with 4+ characters works for seeded users.
            </div>
          </form>
        ) : (
          <form className="auth-form" onSubmit={verify}>
            <div className="verification-block">
              <ShieldCheck size={30} />
              <h2>Email verification</h2>
              <p>{challenge.message}</p>
            </div>
            <label>
              Verification code
              <input value={code} onChange={(event) => setCode(event.target.value)} inputMode="numeric" placeholder={demoVerificationCode} required />
            </label>
            <div className="demo-code">Demo code: {demoVerificationCode}</div>
            <button className="primary-button" disabled={loading}>
              <KeyRound size={18} />
              Verify account
            </button>
          </form>
        )}
      </section>

      <section className="auth-aside">
        <div className="flow-node active">
          <UserRound />
          Account creation and login
        </div>
        <div className="flow-node">
          <ShoppingBag />
          Search, sell, and purchase items
        </div>
        <div className="flow-node">
          <CreditCard />
          Wallet deposits and money transfer
        </div>
        <div className="flow-node">
          <BarChart3 />
          Inventory and transaction reports
        </div>
      </section>
    </main>
  );
}

function MarketplaceScreen({ user, notify, refresh }: ScreenProps) {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("All");
  const [listings, setListings] = useState<Listing[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Listing | null>(null);
  const [challenge, setChallenge] = useState<PurchaseChallenge | null>(null);
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    marketplaceApi
      .searchListings(user.id, query, category)
      .then((result) => {
        if (mounted) setListings(result);
      })
      .catch((error) => notify("error", error instanceof Error ? error.message : "Search failed."))
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, [category, notify, query, user.id]);

  const startPurchase = async (listing: Listing) => {
    setBusy(true);
    try {
      const nextChallenge = await marketplaceApi.beginPurchase(user.id, listing.id);
      setChallenge(nextChallenge);
    } catch (error) {
      notify("error", error instanceof Error ? error.message : "Purchase could not start.");
    } finally {
      setBusy(false);
    }
  };

  const confirmPurchase = async (event: FormEvent) => {
    event.preventDefault();
    if (!challenge) return;

    setBusy(true);
    try {
      await marketplaceApi.confirmPurchase(user.id, challenge.challengeId, code);
      notify("success", "Purchase completed. Money and ownership were transferred.");
      setChallenge(null);
      setSelected(null);
      setCode("");
      await refresh();
    } catch (error) {
      notify("error", error instanceof Error ? error.message : "Purchase failed.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="screen-stack">
      <div className="toolbar">
        <label className="search-field">
          <Search size={18} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by name or brand" />
        </label>
        <div className="category-tabs">
          {categories.map((item) => (
            <button key={item} className={category === item ? "active" : ""} onClick={() => setCategory(item)}>
              {item}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <InlineState icon={<Database />} label="Searching distributed product partitions..." />
      ) : listings.length === 0 ? (
        <InlineState icon={<Search />} label="No matching products from other sellers." />
      ) : (
        <div className="product-grid">
          {listings.map((listing) => (
            <ProductCard key={listing.id} product={listing} onView={() => setSelected(listing)} onBuy={() => startPurchase(listing)} busy={busy} />
          ))}
        </div>
      )}

      {selected && (
        <Modal title={selected.name} onClose={() => setSelected(null)}>
          <div className="detail-layout">
            <ProductVisual product={selected} large />
            <div className="detail-copy">
              <p className="eyebrow">{selected.brand} / {selected.category}</p>
              <h2>{formatCurrency(selected.price)}</h2>
              <p>{selected.description}</p>
              <div className="meta-grid">
                <span>Seller<strong>{selected.sellerName}</strong></span>
                <span>Condition<strong>{selected.condition}</strong></span>
                <span>Available<strong>{selected.quantity}</strong></span>
                <span>Listed<strong>{formatDate(selected.listedAt)}</strong></span>
              </div>
              <button className="primary-button" onClick={() => startPurchase(selected)} disabled={busy}>
                <ShieldCheck size={18} />
                Purchase with 2FA
              </button>
            </div>
          </div>
        </Modal>
      )}

      {challenge && (
        <Modal title="Confirm purchase" onClose={() => setChallenge(null)}>
          <form className="modal-form" onSubmit={confirmPurchase}>
            <div className="purchase-summary">
              <ProductVisual product={challenge.product} />
              <div>
                <strong>{challenge.product.name}</strong>
                <span>{formatCurrency(challenge.amount)}</span>
              </div>
            </div>
            <label>
              2FA code
              <input value={code} onChange={(event) => setCode(event.target.value)} inputMode="numeric" placeholder={demoVerificationCode} required />
            </label>
            <div className="demo-code">Demo code: {demoVerificationCode}</div>
            <button className="primary-button" disabled={busy}>
              <ShieldCheck size={18} />
              Confirm transfer
            </button>
          </form>
        </Modal>
      )}
    </section>
  );
}

function ProductCard({ product, onView, onBuy, busy }: { product: Listing; onView: () => void; onBuy: () => void; busy?: boolean }) {
  return (
    <article className="product-card">
      <ProductVisual product={product} />
      <div className="product-body">
        <div>
          <p className="eyebrow">{product.brand}</p>
          <h3>{product.name}</h3>
          <p>{product.description}</p>
        </div>
        <div className="product-footer">
          <span>{formatCurrency(product.price)}</span>
          <div className="button-row">
            <button className="ghost-button" onClick={onView}>Details</button>
            <button className="icon-button filled" onClick={onBuy} disabled={busy} title="Buy now" aria-label={`Buy ${product.name}`}>
              <ShoppingBag size={18} />
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}

function SellItemScreen({ user, notify, refresh }: ScreenProps) {
  const [form, setForm] = useState({
    name: "",
    brand: "",
    category: "Accessories",
    description: "",
    price: "2500",
    quantity: "1",
    condition: "New" as Product["condition"]
  });
  const [loading, setLoading] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      const price = Number(form.price);
      const quantity = Number(form.quantity);
      await marketplaceApi.createProduct(user.id, {
        name: form.name,
        brand: form.brand,
        category: form.category,
        description: form.description,
        price,
        quantity,
        condition: form.condition,
        status: quantity > 0 ? "listed" : "draft",
        color: makeProductColor(form.name + form.brand)
      });
      notify("success", "Product listed in your inventory.");
      setForm({ ...form, name: "", brand: "", description: "", quantity: "1" });
      await refresh();
    } catch (error) {
      notify("error", error instanceof Error ? error.message : "Could not list product.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="split-layout">
      <form className="work-panel form-grid" onSubmit={submit}>
        <h2>Add item for sale</h2>
        <label>
          Product name
          <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
        </label>
        <label>
          Brand
          <input value={form.brand} onChange={(event) => setForm({ ...form, brand: event.target.value })} required />
        </label>
        <label>
          Category
          <select value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}>
            {categories.filter((item) => item !== "All").map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
        </label>
        <label>
          Condition
          <select value={form.condition} onChange={(event) => setForm({ ...form, condition: event.target.value as Product["condition"] })}>
            <option>New</option>
            <option>Like New</option>
            <option>Used</option>
          </select>
        </label>
        <label>
          Price
          <input type="number" min="1" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} required />
        </label>
        <label>
          Quantity
          <input type="number" min="0" value={form.quantity} onChange={(event) => setForm({ ...form, quantity: event.target.value })} required />
        </label>
        <label className="wide">
          Description
          <textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} rows={5} required />
        </label>
        <button className="primary-button wide" disabled={loading}>
          <Package size={18} />
          Publish listing
        </button>
      </form>

      <aside className="work-panel selling-preview">
        <ProductVisual
          large
          product={{
            name: form.name || "Product preview",
            brand: form.brand || "Brand",
            category: form.category,
            color: makeProductColor(form.name + form.brand)
          }}
        />
        <div>
          <p className="eyebrow">Listing preview</p>
          <h2>{form.name || "Product preview"}</h2>
          <strong>{formatCurrency(Number(form.price) || 0)}</strong>
        </div>
      </aside>
    </section>
  );
}

function InventoryScreen({ inventory, notify, refresh, user }: { inventory: InventoryItem[]; notify: Notify; refresh: Refresh; user: User }) {
  const [editing, setEditing] = useState<InventoryItem | null>(null);
  const [quantity, setQuantity] = useState("0");
  const [status, setStatus] = useState<ProductStatus>("listed");

  const openEdit = (item: InventoryItem) => {
    setEditing(item);
    setQuantity(String(item.quantity));
    setStatus(item.status);
  };

  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (!editing) return;

    try {
      await marketplaceApi.updateProduct(user.id, editing.productId, { quantity: Number(quantity), status });
      notify("success", "Inventory item updated.");
      setEditing(null);
      await refresh();
    } catch (error) {
      notify("error", error instanceof Error ? error.message : "Inventory update failed.");
    }
  };

  const remove = async (item: InventoryItem) => {
    if (!window.confirm(`Remove ${item.productName} from your inventory?`)) return;

    try {
      await marketplaceApi.removeProduct(user.id, item.productId);
      notify("success", "Item removed from inventory.");
      await refresh();
    } catch (error) {
      notify("error", error instanceof Error ? error.message : "Could not remove item.");
    }
  };

  return (
    <section className="work-panel">
      <TableHeader icon={<ClipboardList />} title="Inventory management" action={`${inventory.length} items`} />
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Product</th>
              <th>Brand</th>
              <th>Available</th>
              <th>Reserved</th>
              <th>Sold</th>
              <th>Status</th>
              <th>Updated</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {inventory.map((item) => (
              <tr key={item.productId}>
                <td>{item.productName}</td>
                <td>{item.brand}</td>
                <td>{item.quantity}</td>
                <td>{item.reserved}</td>
                <td>{item.sold}</td>
                <td><StatusPill status={item.status} /></td>
                <td>{formatDate(item.updatedAt)}</td>
                <td className="table-actions">
                  <button className="ghost-button compact" onClick={() => openEdit(item)}>Edit</button>
                  <button className="icon-button danger" onClick={() => remove(item)} title="Remove item" aria-label={`Remove ${item.productName}`}>
                    <Trash2 size={16} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {editing && (
        <Modal title={`Edit ${editing.productName}`} onClose={() => setEditing(null)}>
          <form className="modal-form" onSubmit={save}>
            <label>
              Quantity
              <input type="number" min="0" value={quantity} onChange={(event) => setQuantity(event.target.value)} required />
            </label>
            <label>
              Status
              <select value={status} onChange={(event) => setStatus(event.target.value as ProductStatus)}>
                <option value="listed">listed</option>
                <option value="draft">draft</option>
                <option value="sold">sold</option>
              </select>
            </label>
            <button className="primary-button">Save changes</button>
          </form>
        </Modal>
      )}
    </section>
  );
}

function WalletScreen({ snapshot, notify, refresh, user }: { snapshot: MarketplaceSnapshot; notify: Notify; refresh: Refresh; user: User }) {
  const [amount, setAmount] = useState("1000");
  const [loading, setLoading] = useState(false);
  const purchased = snapshot.purchases.filter((purchase) => purchase.buyerId === user.id);
  const sold = snapshot.purchases.filter((purchase) => purchase.sellerId === user.id);

  const deposit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      await marketplaceApi.deposit(user.id, Number(amount));
      notify("success", "Deposit added to wallet.");
      await refresh();
    } catch (error) {
      notify("error", error instanceof Error ? error.message : "Deposit failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="screen-stack">
      <div className="split-layout">
        <div className="wallet-hero">
          <p className="eyebrow">Current cash balance</p>
          <h2>{formatCurrency(snapshot.currentUser.balance)}</h2>
          <span>Verified account wallet</span>
        </div>
        <form className="work-panel deposit-panel" onSubmit={deposit}>
          <h2>Deposit cash</h2>
          <label>
            Amount
            <input type="number" min="1" value={amount} onChange={(event) => setAmount(event.target.value)} required />
          </label>
          <button className="primary-button" disabled={loading}>
            <CircleDollarSign size={18} />
            Add funds
          </button>
        </form>
      </div>

      <div className="two-column">
        <HistoryPanel title="Purchased items" rows={purchased} userId={user.id} />
        <HistoryPanel title="Sold items" rows={sold} userId={user.id} />
      </div>

      <TransactionsTable transactions={snapshot.transactions} title="Wallet transactions" />
    </section>
  );
}

function ReportsScreen({ report }: { report: ReportSummary }) {
  return (
    <section className="screen-stack">
      <div className="report-grid">
        <ReportCard label="Revenue" value={formatCurrency(report.totalRevenue)} />
        <ReportCard label="Transactions" value={report.totalTransactions.toString()} />
        <ReportCard label="Active listings" value={report.activeListings.toString()} />
        <ReportCard label="Low stock" value={report.lowStockItems.toString()} />
      </div>

      <section className="work-panel">
        <TableHeader icon={<BarChart3 />} title="Sales by category" action={`${report.topCategories.length} categories`} />
        <div className="category-report">
          {report.topCategories.length === 0 ? (
            <InlineState icon={<BarChart3 />} label="No category sales yet." />
          ) : (
            report.topCategories.map((item) => (
              <div className="bar-row" key={item.category}>
                <span>{item.category}</span>
                <div className="bar-track">
                  <div style={{ width: `${Math.min(100, Math.max(12, item.count * 20))}%` }} />
                </div>
                <strong>{formatCurrency(item.revenue)}</strong>
              </div>
            ))
          )}
        </div>
      </section>

      <TransactionsTable transactions={report.recentTransactions} title="Recent report transactions" />
    </section>
  );
}

function CsvImportScreen({ user, notify, refresh }: ScreenProps) {
  const [rawCsv, setRawCsv] = useState("name,brand,price,quantity,category,description\nUSB-C Hub,Anker,1800,7,Accessories,Seven-port hub with HDMI and power delivery");
  const [rows, setRows] = useState<CsvImportRow[]>([]);
  const validRows = rows.filter((row) => row.valid);

  const parse = () => {
    const parsed = parseProductCsv(rawCsv);
    setRows(parsed);
    notify(parsed.some((row) => !row.valid) ? "error" : "success", `Parsed ${parsed.length} CSV rows.`);
  };

  const importRows = async () => {
    try {
      const imported = await marketplaceApi.importProducts(user.id, validRows);
      notify("success", `Imported ${imported.length} products into inventory.`);
      await refresh();
    } catch (error) {
      notify("error", error instanceof Error ? error.message : "CSV import failed.");
    }
  };

  const readFile = async (file?: File) => {
    if (!file) return;
    setRawCsv(await file.text());
  };

  return (
    <section className="screen-stack">
      <div className="work-panel import-panel">
        <div>
          <TableHeader icon={<Upload />} title="Product CSV upload" action="name, brand, price, quantity, category" />
          <textarea value={rawCsv} onChange={(event) => setRawCsv(event.target.value)} rows={8} />
          <div className="button-row">
            <label className="file-button">
              <Upload size={18} />
              Upload file
              <input type="file" accept=".csv,text/csv" onChange={(event) => readFile(event.target.files?.[0])} />
            </label>
            <button className="ghost-button" onClick={parse}>Preview rows</button>
            <button className="primary-button" onClick={importRows} disabled={validRows.length === 0}>
              <FileSpreadsheet size={18} />
              Import valid rows
            </button>
          </div>
        </div>
      </div>

      {rows.length > 0 && (
        <section className="work-panel">
          <TableHeader icon={<FileSpreadsheet />} title="CSV preview" action={`${validRows.length}/${rows.length} valid`} />
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Brand</th>
                  <th>Price</th>
                  <th>Quantity</th>
                  <th>Category</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, index) => (
                  <tr key={`${row.name}-${index}`}>
                    <td>{row.name || "-"}</td>
                    <td>{row.brand || "-"}</td>
                    <td>{formatCurrency(row.price)}</td>
                    <td>{row.quantity}</td>
                    <td>{row.category || "-"}</td>
                    <td>{row.valid ? <span className="success-text">Valid</span> : row.errors.join(", ")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </section>
  );
}

function StorePortalScreen({ listings, notify }: { listings: Listing[]; notify: Notify }) {
  const [token, setToken] = useState("store_live_cairo_8841");
  const partnerListings = useMemo(() => listings.slice(0, 4), [listings]);

  return (
    <section className="screen-stack">
      <div className="store-portal">
        <div>
          <p className="eyebrow">External store interface</p>
          <h2>Partner catalog channel</h2>
          <div className="token-box">
            <span>Access token</span>
            <strong>{token}</strong>
          </div>
          <div className="button-row">
            <button className="primary-button" onClick={() => setToken(`store_live_${Math.random().toString(16).slice(2, 10)}`)}>
              <KeyRound size={18} />
              Generate token
            </button>
            <button className="ghost-button" onClick={() => notify("success", "Partner catalog synchronized.")}>
              <Database size={18} />
              Sync catalog
            </button>
          </div>
        </div>

        <div className="endpoint-list">
          <Endpoint method="GET" path="/products/search" />
          <Endpoint method="POST" path="/orders/begin-purchase" />
          <Endpoint method="POST" path="/orders/confirm-purchase" />
        </div>
      </div>

      <section className="work-panel">
        <TableHeader icon={<Store />} title="Partner storefront preview" action={`${partnerListings.length} synced items`} />
        <div className="mini-catalog">
          {partnerListings.map((listing) => (
            <article key={listing.id}>
              <ProductVisual product={listing} />
              <div>
                <strong>{listing.name}</strong>
                <span>{listing.brand}</span>
                <b>{formatCurrency(listing.price)}</b>
              </div>
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}

type Notify = (type: Toast["type"], message: string) => void;
type Refresh = () => Promise<void>;
type ScreenProps = { user: User; notify: Notify; refresh: Refresh };

function ProductVisual({ product, large = false }: { product: Pick<Product, "category" | "name" | "brand" | "color">; large?: boolean }) {
  const icon = product.category === "Laptops" ? <Laptop /> : product.category === "Audio" ? <Headphones /> : product.category === "Cameras" ? <Camera /> : <Package />;

  return (
    <div className={`product-visual ${large ? "large" : ""}`} style={{ "--accent": product.color } as CSSProperties}>
      <div className="visual-icon">{icon}</div>
      <div>
        <span>{product.brand}</span>
        <strong>{product.name}</strong>
      </div>
    </div>
  );
}

function TableHeader({ icon, title, action }: { icon: ReactElement; title: string; action?: string }) {
  return (
    <div className="table-header">
      <div>
        {icon}
        <h2>{title}</h2>
      </div>
      {action && <span>{action}</span>}
    </div>
  );
}

function InlineState({ icon, label }: { icon: ReactElement; label: string }) {
  return (
    <div className="inline-state">
      {icon}
      <span>{label}</span>
    </div>
  );
}

function Modal({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <section className="modal-card">
        <header>
          <h2>{title}</h2>
          <button className="icon-button" onClick={onClose} aria-label="Close">
            <X size={18} />
          </button>
        </header>
        {children}
      </section>
    </div>
  );
}

function StatusPill({ status }: { status: ProductStatus }) {
  return <span className={`status-pill ${status}`}>{status}</span>;
}

function HistoryPanel({ title, rows, userId }: { title: string; rows: Array<{ id: string; productName: string; amount: number; purchasedAt: string; buyerId: string }>; userId: string }) {
  return (
    <section className="work-panel">
      <TableHeader icon={<PackageCheck />} title={title} action={`${rows.length} records`} />
      <div className="simple-list">
        {rows.length === 0 ? (
          <InlineState icon={<Package />} label="No records yet." />
        ) : (
          rows.map((row) => (
            <div key={row.id} className="list-row">
              <span>{row.productName}</span>
              <strong>{formatCurrency(row.amount)}</strong>
              <small>{row.buyerId === userId ? "Bought" : "Sold"} / {formatDate(row.purchasedAt)}</small>
            </div>
          ))
        )}
      </div>
    </section>
  );
}

function TransactionsTable({ transactions, title }: { transactions: Transaction[]; title: string }) {
  return (
    <section className="work-panel">
      <TableHeader icon={<Database />} title={title} action={`${transactions.length} records`} />
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Type</th>
              <th>Description</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((transaction) => (
              <tr key={transaction.id}>
                <td>{transaction.type}</td>
                <td>{transaction.description}</td>
                <td>{formatCurrency(transaction.amount)}</td>
                <td><span className="success-text">{transaction.status}</span></td>
                <td>{formatDate(transaction.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function ReportCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="report-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function Endpoint({ method, path }: { method: string; path: string }) {
  return (
    <div className="endpoint">
      <span>{method}</span>
      <code>{path}</code>
    </div>
  );
}
