package com.example.productapi.service;

import com.example.productapi.exception.ProductNotFoundException;
import com.example.productapi.model.Product;
import com.example.productapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Cacheable(value = "products")
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.debug("Fetching all products from database");
        return productRepository.findAll();
    }

    @Override
    @Cacheable(value = "product", key = "#id")
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        log.debug("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true)
    })
    public Product createProduct(Product product) {
        log.debug("Creating product: {}", product.getName());
        return productRepository.save(product);
    }

    @Override
    @Caching(
            put = { @CachePut(value = "product", key = "#id") },
            evict = { @CacheEvict(value = "products", allEntries = true) }
    )
    public Product updateProduct(Long id, Product product) {
        log.debug("Updating product with id: {}", id);
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        existing.setName(product.getName());
        existing.setPrice(product.getPrice());
        existing.setDescription(product.getDescription());
        existing.setCategory(product.getCategory());
        existing.setStockQuantity(product.getStockQuantity());
        existing.setBrand(product.getBrand());
        existing.setSku(product.getSku());

        return productRepository.save(existing);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "products", allEntries = true)
    })
    public void deleteProduct(Long id) {
        log.debug("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Override
    @Cacheable(value = "productsByCategory", key = "#category")
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        log.debug("Fetching products for category: {}", category);
        return productRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> searchProductsByName(String name) {
        log.debug("Searching products with name: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name);
    }
}
