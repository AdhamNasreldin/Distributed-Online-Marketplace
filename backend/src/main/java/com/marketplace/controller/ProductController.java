package com.marketplace.controller;

import com.marketplace.exception.AppException;
import com.marketplace.model.CsvImportRow;
import com.marketplace.model.Listing;
import com.marketplace.model.Product;
import com.marketplace.service.MarketplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private MarketplaceService marketplaceService;

    @GetMapping("/search")
    public List<Listing> searchListings(
            @RequestParam(required = false, defaultValue = "") String userId,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "All") String category) {
        return marketplaceService.searchListings(userId, query, category);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@RequestBody CreateProductRequest request) {
        if (request.getProduct() == null) {
            throw new AppException(400, "Product payload is required.");
        }
        return marketplaceService.createProduct(
            request.getUserId() != null ? request.getUserId() : "",
            request.getProduct()
        );
    }

    @PatchMapping("/{productId}")
    public Product updateProduct(@PathVariable String productId, @RequestBody UpdateProductRequest request) {
        return marketplaceService.updateProduct(
            request.getUserId() != null ? request.getUserId() : "",
            productId,
            request.getUpdates() != null ? request.getUpdates() : new Product()
        );
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable String productId, @RequestBody DeleteProductRequest request) {
        marketplaceService.removeProduct(
            request.getUserId() != null ? request.getUserId() : "",
            productId
        );
    }

    @PostMapping("/import-csv")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Product> importCsv(@RequestBody ImportCsvRequest request) {
        return marketplaceService.importProducts(
            request.getUserId() != null ? request.getUserId() : "",
            request.getRows() != null ? request.getRows() : List.of()
        );
    }

    // DTOs
    public static class CreateProductRequest {
        private String userId;
        private Product product;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Product getProduct() { return product; }
        public void setProduct(Product product) { this.product = product; }
    }

    public static class UpdateProductRequest {
        private String userId;
        private Product updates;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public Product getUpdates() { return updates; }
        public void setUpdates(Product updates) { this.updates = updates; }
    }

    public static class DeleteProductRequest {
        private String userId;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class ImportCsvRequest {
        private String userId;
        private List<CsvImportRow> rows;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public List<CsvImportRow> getRows() { return rows; }
        public void setRows(List<CsvImportRow> rows) { this.rows = rows; }
    }
}
