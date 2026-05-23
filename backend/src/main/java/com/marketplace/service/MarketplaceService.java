package com.marketplace.service;

import com.marketplace.config.EnvConfig;
import com.marketplace.db.Mappers;
import com.marketplace.db.ShardRouting;
import com.marketplace.exception.AppException;
import com.marketplace.model.*;
import com.marketplace.utils.Ids;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarketplaceService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static boolean verifyPassword(String passwordHash, String password) {
        if (passwordHash.startsWith("plain:")) {
            return passwordHash.substring("plain:".length()).equals(password);
        }
        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (Exception e) {
            return false;
        }
    }

    private Timestamp expiresInMinutes(int minutes) {
        return new Timestamp(System.currentTimeMillis() + (long) minutes * 60 * 1000);
    }

    public StoredUser findUserByEmail(String email) {
        for (String schema : ShardRouting.SHARD_SCHEMAS) {
            String sql = String.format("SELECT * FROM %s.users WHERE LOWER(email) = LOWER(?) LIMIT 1", schema);
            List<StoredUser> users = jdbcTemplate.query(sql, (rs, rowNum) -> Mappers.mapStoredUser(rs, schema), email);
            if (!users.isEmpty()) {
                return users.get(0);
            }
        }
        return null;
    }

    public StoredUser findUserById(String userId) {
        for (String schema : ShardRouting.SHARD_SCHEMAS) {
            String sql = String.format("SELECT * FROM %s.users WHERE id = ? LIMIT 1", schema);
            List<StoredUser> users = jdbcTemplate.query(sql, (rs, rowNum) -> Mappers.mapStoredUser(rs, schema), userId);
            if (!users.isEmpty()) {
                return users.get(0);
            }
        }
        return null;
    }

    public StoredUser lockUserById(String userId) {
        StoredUser existing = findUserById(userId);
        if (existing == null) {
            throw new AppException(404, "User was not found.");
        }
        String sql = String.format("SELECT * FROM %s.users WHERE id = ? FOR UPDATE", existing.getSchema());
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> Mappers.mapStoredUser(rs, existing.getSchema()), userId);
    }

    public List<StoredUser> allUsers() {
        List<StoredUser> all = new ArrayList<>();
        for (String schema : ShardRouting.SHARD_SCHEMAS) {
            String sql = String.format("SELECT * FROM %s.users", schema);
            all.addAll(jdbcTemplate.query(sql, (rs, rowNum) -> Mappers.mapStoredUser(rs, schema)));
        }
        return all;
    }

    public StoredProduct findProductById(String productId) {
        for (String schema : ShardRouting.SHARD_SCHEMAS) {
            String sql = String.format("SELECT * FROM %s.products WHERE id = ? LIMIT 1", schema);
            List<StoredProduct> products = jdbcTemplate.query(sql, (rs, rowNum) -> Mappers.mapStoredProduct(rs, schema), productId);
            if (!products.isEmpty()) {
                return products.get(0);
            }
        }
        return null;
    }

    public StoredProduct lockProductById(String productId) {
        StoredProduct existing = findProductById(productId);
        if (existing == null) {
            throw new AppException(404, "Product was not found.");
        }
        String sql = String.format("SELECT * FROM %s.products WHERE id = ? FOR UPDATE", existing.getSchema());
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> Mappers.mapStoredProduct(rs, existing.getSchema()), productId);
    }

    public List<StoredProduct> allProducts() {
        List<StoredProduct> all = new ArrayList<>();
        for (String schema : ShardRouting.SHARD_SCHEMAS) {
            String sql = String.format("SELECT * FROM %s.products", schema);
            all.addAll(jdbcTemplate.query(sql, (rs, rowNum) -> Mappers.mapStoredProduct(rs, schema)));
        }
        // Sort listed_at descending
        all.sort((a, b) -> b.getListedAt().compareTo(a.getListedAt()));
        return all;
    }

    public List<Listing> activeListings() {
        List<StoredProduct> products = allProducts();
        List<StoredUser> users = allUsers();
        Map<String, StoredUser> sellers = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<Listing> listings = new ArrayList<>();
        for (StoredProduct product : products) {
            if ("listed".equals(product.getStatus()) && product.getQuantity() > 0) {
                StoredUser seller = sellers.get(product.getOwnerId());
                String sellerName = seller != null ? seller.getFullName() : "Unknown Seller";
                String sellerEmail = seller != null ? seller.getEmail() : "unknown@marketplace.local";
                listings.add(new Listing(product, sellerName, sellerEmail));
            }
        }
        return listings;
    }

    public List<Product> userProducts(String userId) {
        List<StoredProduct> products = allProducts();
        return products.stream()
                .filter(p -> p.getOwnerId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<InventoryItem> buildInventory(List<Product> products) {
        return products.stream().map(product -> new InventoryItem(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getQuantity(),
                "listed".equals(product.getStatus()) ? Math.min(1, product.getQuantity()) : 0,
                product.getSoldCount(),
                product.getStatus(),
                product.getListedAt()
        )).collect(Collectors.toList());
    }

    public List<Transaction> userTransactions(String userId) {
        String sql = "SELECT * FROM core.transactions WHERE from_user_id = ? OR to_user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> Mappers.mapTransaction(rs), userId, userId);
    }

    public List<Purchase> userPurchases(String userId) {
        String sql = "SELECT * FROM core.purchases WHERE buyer_id = ? OR seller_id = ? ORDER BY purchased_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> Mappers.mapPurchase(rs), userId, userId);
    }

    public ReportSummary buildReport(String userId) {
        List<Product> products = userProducts(userId);
        List<Transaction> transactions = userTransactions(userId);

        double revenue = transactions.stream()
                .filter(t -> "sale".equals(t.getType()) && userId.equals(t.getToUserId()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        Map<String, ReportSummary.CategorySummary> categoryMap = new HashMap<>();
        for (Product product : products) {
            String cat = product.getCategory();
            ReportSummary.CategorySummary summary = categoryMap.computeIfAbsent(cat, k -> new ReportSummary.CategorySummary(cat, 0, 0.0));
            summary.setCount(summary.getCount() + product.getSoldCount());
            summary.setRevenue(summary.getRevenue() + product.getSoldCount() * product.getPrice());
        }

        List<ReportSummary.CategorySummary> topCategories = new ArrayList<>(categoryMap.values());
        topCategories.sort((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()));

        int activeListings = (int) products.stream().filter(p -> "listed".equals(p.getStatus())).count();
        int soldItems = products.stream().mapToInt(Product::getSoldCount).sum();
        int lowStockItems = (int) products.stream().filter(p -> p.getQuantity() <= 1).count();

        List<Transaction> recentTransactions = transactions.stream().limit(6).collect(Collectors.toList());

        return new ReportSummary(
                revenue,
                transactions.size(),
                activeListings,
                soldItems,
                lowStockItems,
                topCategories,
                recentTransactions
        );
    }

    private List<String> validateCsvRow(CsvImportRow row) {
        List<String> errors = new ArrayList<>();
        if (row.getName() == null || row.getName().trim().isEmpty()) {
            errors.add("Name is required");
        } else if (row.getName().trim().length() > 200) {
            errors.add("Name cannot exceed 200 characters");
        }
        if (row.getBrand() == null || row.getBrand().trim().isEmpty()) errors.add("Brand is required");
        if (row.getCategory() == null || row.getCategory().trim().isEmpty()) errors.add("Category is required");
        if (row.getPrice() <= 0) errors.add("Price must be positive");
        if (row.getQuantity() < 0) errors.add("Quantity must be a whole number");
        if (row.getDescription() != null && row.getDescription().trim().length() > 20000) {
            errors.add("Description cannot exceed 20000 characters");
        }
        return errors;
    }

    // Public API Methods

    public User login(String email, String password) {
        StoredUser user = findUserByEmail(email);
        if (user == null || !verifyPassword(user.getPasswordHash(), password)) {
            throw new AppException(401, "Invalid email or password.");
        }
        return user; // Return User object
    }

    @Transactional
    public AuthChallenge register(String fullName, String email, String password) {
        if (fullName == null || fullName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.length() < 4) {
            throw new AppException(400, "Full name, valid email, and 4+ character password are required.");
        }

        if (findUserByEmail(email) != null) {
            throw new AppException(409, "Email is already registered.");
        }

        String userId = Ids.makeId("u");
        String schema = ShardRouting.shardForKey(userId);
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(10));

        String sqlUser = String.format(
                "INSERT INTO %s.users (id, full_name, email, password_hash, balance, two_factor_enabled) " +
                "VALUES (?, ?, ?, ?, ?, true)", schema
        );
        jdbcTemplate.update(sqlUser, userId, fullName.trim(), email.trim().toLowerCase(), passwordHash, 5000.0);

        // Fetch registered user back
        String selectSql = String.format("SELECT * FROM %s.users WHERE id = ?", schema);
        User user = jdbcTemplate.queryForObject(selectSql, (rs, rowNum) -> Mappers.mapUser(rs), userId);

        String challengeId = Ids.makeId("auth");
        String demo2fa = EnvConfig.get("DEMO_2FA_CODE", "246810");

        String sqlChallenge = "INSERT INTO core.auth_challenges (id, user_id, code, purpose, expires_at) VALUES (?, ?, ?, 'register', ?)";
        jdbcTemplate.update(sqlChallenge, challengeId, userId, demo2fa, expiresInMinutes(10));

        return new AuthChallenge(challengeId, user, "A verification code was sent to the account email.");
    }

    @Transactional
    public User verifyAuthChallenge(String challengeId, String code) {
        String selectSql = "SELECT user_id FROM core.auth_challenges " +
                           "WHERE id = ? AND code = ? AND consumed_at IS NULL AND expires_at > now()";
        
        List<String> userIds = jdbcTemplate.query(selectSql, (rs, rowNum) -> rs.getString("user_id"), challengeId, code);
        if (userIds.isEmpty()) {
            throw new AppException(400, "Verification failed or expired.");
        }

        String userId = userIds.get(0);

        String updateSql = "UPDATE core.auth_challenges SET consumed_at = now() " +
                           "WHERE id = ? AND consumed_at IS NULL";
        int rowsUpdated = jdbcTemplate.update(updateSql, challengeId);
        if (rowsUpdated == 0) {
            throw new AppException(400, "Verification failed or expired.");
        }

        StoredUser user = findUserById(userId);
        if (user == null) {
            throw new AppException(404, "Verified user was not found.");
        }

        return user;
    }

    public MarketplaceSnapshot getSnapshot(String userId) {
        StoredUser user = findUserById(userId);
        if (user == null) {
            throw new AppException(404, "User was not found.");
        }

        List<Listing> listings = activeListings();
        List<Product> products = userProducts(userId);
        List<Transaction> transactions = userTransactions(userId);
        List<Purchase> purchases = userPurchases(userId);
        ReportSummary report = buildReport(userId);

        return new MarketplaceSnapshot(
                listings,
                buildInventory(products),
                transactions,
                purchases,
                report,
                user
        );
    }

    public List<Listing> searchListings(String userId, String query, String category) {
        String normalizedQuery = query != null ? query.trim().toLowerCase() : "";
        List<Listing> listings = activeListings();

        return listings.stream().filter(listing -> {
            boolean matchesCategory = "All".equals(category) || category == null || category.isEmpty() || listing.getCategory().equals(category);
            String searchable = (listing.getName() + " " + listing.getBrand() + " " + listing.getCategory()).toLowerCase();
            boolean matchesQuery = normalizedQuery.isEmpty() || searchable.contains(normalizedQuery);
            return matchesCategory && matchesQuery;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Product createProduct(String userId, Product product) {
        StoredUser user = findUserById(userId);
        if (user == null) {
            throw new AppException(404, "User was not found.");
        }

        if (product.getName() == null || product.getName().trim().isEmpty() ||
            product.getBrand() == null || product.getBrand().trim().isEmpty() ||
            product.getCategory() == null || product.getCategory().trim().isEmpty() ||
            product.getPrice() <= 0 || product.getQuantity() < 0) {
            throw new AppException(400, "Name, brand, category, positive price, and valid quantity are required.");
        }

        if (product.getName().trim().length() > 200) {
            throw new AppException(400, "Product title cannot exceed 200 characters.");
        }

        if (product.getDescription() != null && product.getDescription().trim().length() > 20000) {
            throw new AppException(400, "Product description cannot exceed 20000 characters.");
        }

        String productId = Ids.makeId("p");
        String schema = ShardRouting.shardForKey(userId);

        String desc = (product.getDescription() == null || product.getDescription().trim().isEmpty())
                ? product.getBrand() + " " + product.getName()
                : product.getDescription().trim();

        String status = product.getStatus() != null ? product.getStatus() : (product.getQuantity() > 0 ? "listed" : "draft");
        String condition = product.getCondition() != null ? product.getCondition() : "New";
        String color = product.getColor() != null ? product.getColor() : "#0f766e";

        String insertSql = String.format(
                "INSERT INTO %s.products (id, owner_id, name, brand, category, description, price, quantity, condition, status, color) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", schema
        );

        jdbcTemplate.update(insertSql, productId, userId, product.getName().trim(), product.getBrand().trim(),
                product.getCategory().trim(), desc, product.getPrice(), product.getQuantity(), condition, status, color);

        String selectSql = String.format("SELECT * FROM %s.products WHERE id = ?", schema);
        return jdbcTemplate.queryForObject(selectSql, (rs, rowNum) -> Mappers.mapProduct(rs), productId);
    }

    @Transactional
    public Product updateProduct(String userId, String productId, Product updates) {
        StoredProduct product = findProductById(productId);
        if (product == null || !product.getOwnerId().equals(userId)) {
            throw new AppException(404, "Product was not found in your inventory.");
        }

        if (updates.getName() != null && updates.getName().trim().length() > 200) {
            throw new AppException(400, "Product title cannot exceed 200 characters.");
        }

        if (updates.getDescription() != null && updates.getDescription().trim().length() > 20000) {
            throw new AppException(400, "Product description cannot exceed 20000 characters.");
        }

        List<String> fields = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (updates.getName() != null) { fields.add("name = ?"); values.add(updates.getName().trim()); }
        if (updates.getBrand() != null) { fields.add("brand = ?"); values.add(updates.getBrand().trim()); }
        if (updates.getCategory() != null) { fields.add("category = ?"); values.add(updates.getCategory().trim()); }
        if (updates.getDescription() != null) { fields.add("description = ?"); values.add(updates.getDescription().trim()); }
        if (updates.getPrice() > 0) { fields.add("price = ?"); values.add(updates.getPrice()); }
        if (updates.getQuantity() >= 0) { fields.add("quantity = ?"); values.add(updates.getQuantity()); }
        if (updates.getCondition() != null) { fields.add("condition = ?"); values.add(updates.getCondition()); }
        if (updates.getStatus() != null) { fields.add("status = ?"); values.add(updates.getStatus()); }
        if (updates.getColor() != null) { fields.add("color = ?"); values.add(updates.getColor()); }

        if (fields.isEmpty()) {
            return product;
        }

        String setSql = String.join(", ", fields);
        values.add(productId);
        values.add(userId);

        String updateSql = String.format(
                "UPDATE %s.products SET %s WHERE id = ? AND owner_id = ?",
                product.getSchema(), setSql
        );

        jdbcTemplate.update(updateSql, values.toArray());

        String selectSql = String.format("SELECT * FROM %s.products WHERE id = ?", product.getSchema());
        return jdbcTemplate.queryForObject(selectSql, (rs, rowNum) -> Mappers.mapProduct(rs), productId);
    }

    @Transactional
    public void removeProduct(String userId, String productId) {
        StoredProduct product = findProductById(productId);
        if (product == null || !product.getOwnerId().equals(userId)) {
            throw new AppException(404, "Product was not found in your inventory.");
        }

        String deleteSql = String.format("DELETE FROM %s.products WHERE id = ? AND owner_id = ?", product.getSchema());
        jdbcTemplate.update(deleteSql, productId, userId);
    }

    @Transactional
    public User deposit(String userId, double amount) {
        if (amount <= 0) {
            throw new AppException(400, "Deposit amount must be positive.");
        }

        // Lock user first
        StoredUser user = lockUserById(userId);

        if (user.getBalance() + amount > 1000000.0) {
            double maxAllowed = Math.max(0.0, 1000000.0 - user.getBalance());
            throw new AppException(400, String.format("Wallet balance cannot exceed 1,000,000. Maximum you can deposit is %.2f.", maxAllowed));
        }

        String updateSql = String.format("UPDATE %s.users SET balance = balance + ? WHERE id = ?", user.getSchema());
        jdbcTemplate.update(updateSql, amount, userId);

        String txId = Ids.makeId("tx");
        String insertTxSql = "INSERT INTO core.transactions (id, type, amount, to_user_id, description, status) " +
                             "VALUES (?, 'deposit', ?, ?, 'Wallet deposit verified', 'completed')";
        jdbcTemplate.update(insertTxSql, txId, amount, userId);

        // Fetch back updated user
        String selectSql = String.format("SELECT * FROM %s.users WHERE id = ?", user.getSchema());
        return jdbcTemplate.queryForObject(selectSql, (rs, rowNum) -> Mappers.mapUser(rs), userId);
    }

    @Transactional
    public PurchaseChallenge beginPurchase(String userId, String productId) {
        StoredUser buyer = findUserById(userId);
        if (buyer == null) {
            throw new AppException(404, "Buyer was not found.");
        }

        StoredProduct product = findProductById(productId);
        if (product == null || !"listed".equals(product.getStatus()) || product.getQuantity() <= 0 || product.getOwnerId().equals(userId)) {
            throw new AppException(400, "This item is not available for purchase.");
        }

        if (buyer.getBalance() < product.getPrice()) {
            throw new AppException(400, "Your wallet balance is not enough for this purchase.");
        }

        StoredUser seller = findUserById(product.getOwnerId());
        if (seller == null) {
            throw new AppException(404, "Seller was not found.");
        }

        String challengeId = Ids.makeId("buy");
        String demo2fa = EnvConfig.get("DEMO_2FA_CODE", "246810");

        String insertChallengeSql = "INSERT INTO core.purchase_challenges (id, buyer_id, seller_id, product_id, amount, code, expires_at) " +
                                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(insertChallengeSql, challengeId, userId, seller.getId(), product.getId(), product.getPrice(), demo2fa, expiresInMinutes(10));

        Listing listing = new Listing(product, seller.getFullName(), seller.getEmail());
        return new PurchaseChallenge(challengeId, listing, product.getPrice(), "Confirm this purchase with two-factor verification.");
    }

    @Transactional
    public Purchase confirmPurchase(String userId, String challengeId, String code) {
        // Lock and check purchase challenge
        String selectChallengeSql = "SELECT * FROM core.purchase_challenges " +
                                    "WHERE id = ? AND buyer_id = ? AND code = ? AND consumed_at IS NULL AND expires_at > now() " +
                                    "FOR UPDATE";
        
        List<Map<String, Object>> challenges = jdbcTemplate.queryForList(selectChallengeSql, challengeId, userId, code);
        if (challenges.isEmpty()) {
            throw new AppException(400, "Purchase verification failed or expired.");
        }

        Map<String, Object> challenge = challenges.get(0);
        String productId = (String) challenge.get("product_id");

        // Lock records across shards in strict transactional order to avoid deadlocks
        StoredProduct product = lockProductById(productId);
        StoredUser buyer = lockUserById(userId);
        StoredUser seller = lockUserById(product.getOwnerId());

        if (!"listed".equals(product.getStatus()) || product.getQuantity() <= 0) {
            throw new AppException(400, "The selected item is no longer available.");
        }

        if (buyer.getBalance() < product.getPrice()) {
            throw new AppException(400, "Your wallet balance is not enough for this purchase.");
        }

        // 1. Debit buyer
        String debitSql = String.format("UPDATE %s.users SET balance = balance - ? WHERE id = ?", buyer.getSchema());
        jdbcTemplate.update(debitSql, product.getPrice(), buyer.getId());

        // 2. Credit seller
        String creditSql = String.format("UPDATE %s.users SET balance = balance + ? WHERE id = ?", seller.getSchema());
        jdbcTemplate.update(creditSql, product.getPrice(), seller.getId());

        // 3. Decrement product quantity
        String productSql = String.format(
                "UPDATE %s.products SET quantity = quantity - 1, sold_count = sold_count + 1, " +
                "status = CASE WHEN quantity - 1 <= 0 THEN 'sold' ELSE status END " +
                "WHERE id = ?", product.getSchema()
        );
        jdbcTemplate.update(productSql, product.getId());

        // 4. Consume challenge
        String consumeSql = "UPDATE core.purchase_challenges SET consumed_at = now() WHERE id = ?";
        jdbcTemplate.update(consumeSql, challengeId);

        // 5. Insert purchase record
        String purchaseId = Ids.makeId("ord");
        String insertPurchaseSql = "INSERT INTO core.purchases (id, product_id, product_name, buyer_id, seller_id, amount, status, purchased_at) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, 'completed', now()) RETURNING *";
        
        Purchase purchase = jdbcTemplate.queryForObject(
                "INSERT INTO core.purchases (id, product_id, product_name, buyer_id, seller_id, amount, status, purchased_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'completed', now()) RETURNING *",
                (rs, rowNum) -> Mappers.mapPurchase(rs),
                purchaseId, product.getId(), product.getName(), buyer.getId(), seller.getId(), product.getPrice()
        );

        // 6. Insert transactions (Debit & Credit rows)
        String txDebitId = Ids.makeId("tx");
        String txCreditId = Ids.makeId("tx");

        String insertTxSql = "INSERT INTO core.transactions (id, type, amount, from_user_id, to_user_id, product_id, description, created_at, status) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'completed')";

        Timestamp purchasedAt = java.sql.Timestamp.from(java.time.Instant.parse(purchase.getPurchasedAt()));

        // Buyer purchase transaction
        jdbcTemplate.update(insertTxSql, txDebitId, "purchase", product.getPrice(), buyer.getId(), seller.getId(), product.getId(),
                "Purchased " + product.getName() + " from " + seller.getFullName(), purchasedAt);

        // Seller sale transaction
        jdbcTemplate.update(insertTxSql, txCreditId, "sale", product.getPrice(), buyer.getId(), seller.getId(), product.getId(),
                "Sold " + product.getName() + " to " + buyer.getFullName(), purchasedAt);

        return purchase;
    }

    @Transactional
    public List<Product> importProducts(String userId, List<CsvImportRow> rows) {
        StoredUser user = findUserById(userId);
        if (user == null) {
            throw new AppException(404, "User was not found.");
        }

        List<CsvImportRow> validRows = rows.stream()
                .filter(row -> row.isValid() != false && validateCsvRow(row).isEmpty())
                .collect(Collectors.toList());

        if (validRows.isEmpty()) {
            return new ArrayList<>();
        }

        String schema = ShardRouting.shardForKey(userId);
        List<Product> createdProducts = new ArrayList<>();

        for (CsvImportRow row : validRows) {
            String productId = Ids.makeId("p");
            String desc = (row.getDescription() == null || row.getDescription().trim().isEmpty())
                    ? row.getBrand() + " " + row.getName() + " imported from CSV."
                    : row.getDescription().trim();

            String status = row.getQuantity() > 0 ? "listed" : "draft";

            String insertSql = String.format(
                    "INSERT INTO %s.products (id, owner_id, name, brand, category, description, price, quantity, condition, status, color) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'New', ?, '#0f766e')", schema
            );

            jdbcTemplate.update(insertSql, productId, userId, row.getName().trim(), row.getBrand().trim(),
                    row.getCategory().trim(), desc, row.getPrice(), row.getQuantity(), status);

            String selectSql = String.format("SELECT * FROM %s.products WHERE id = ?", schema);
            createdProducts.add(jdbcTemplate.queryForObject(selectSql, (rs, rowNum) -> Mappers.mapProduct(rs), productId));
        }

        return createdProducts;
    }
}
