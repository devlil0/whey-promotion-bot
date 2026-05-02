package com.devlil0.whey_promotion_bot.controller;

import com.devlil0.whey_promotion_bot.entity.ProductOffer;
import com.devlil0.whey_promotion_bot.repository.ProductOfferRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductOfferRepository repository;

    public ProductController(ProductOfferRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProductOffer> listAll() {
        return repository.findAll();
    }

    @GetMapping("/available")
    public List<ProductOffer> listAvailable() {
        return repository.findByAvailableTrue();
    }

    @GetMapping("/by-store")
    public List<ProductOffer> listByStore(@RequestParam String store) {
        return repository.findByStore(store);
    }
}
