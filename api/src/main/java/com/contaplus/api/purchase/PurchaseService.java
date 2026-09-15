package com.contaplus.api.purchase;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.product.Product;
import com.contaplus.api.product.ProductRepository;
import com.contaplus.api.stock.StockService;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreService;
import com.contaplus.api.transaction.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PurchaseService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final StoreService storeService;
    private final StockService stockService;

    PurchaseService(TransactionRepository transactionRepository,
                    ProductRepository productRepository,
                    StoreService storeService,
                    StockService stockService) {
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
        this.storeService = storeService;
        this.stockService = stockService;
    }

    @Transactional
    public Transaction registrarCompra(RegistrarCompraRequest request) {
        Store store = storeService.buscarPorId(request.storeId());

        Optional<Transaction> existente = transactionRepository
            .findByStore_IdAndIdempotencyKey(request.storeId(), request.idempotencyKey());

        if (existente.isPresent()) {
            return existente.get();
        }

        OffsetDateTime occurredAt = request.occurredAt() != null
            ? request.occurredAt()
            : OffsetDateTime.now();

        Transaction transaction = new Transaction(
            store,
            TransactionType.PURCHASE,
            request.source() != null ? request.source() : TransactionSource.MANUAL,
            request.description() != null ? request.description() : "",
            request.idempotencyKey(),
            occurredAt
        );

        List<Product> productsToUpdate = new ArrayList<>();
        for (ItemCompraRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.productId()));

            if (!product.getStoreId().equals(request.storeId())) {
                throw new IllegalArgumentException(
                    "Product " + itemReq.productId() + " does not belong to store " + request.storeId()
                );
            }

            TransactionItem item = new TransactionItem(transaction, product, itemReq.quantity());

            if (itemReq.unitCostCents() != null) {
                item.overrideCost(itemReq.unitCostCents());
            }

            transaction.addItem(item);
            productsToUpdate.add(product);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        for (int i = 0; i < request.items().size(); i++) {
            ItemCompraRequest itemReq = request.items().get(i);
            Product product = productsToUpdate.get(i);

            stockService.registrarCompra(store, product, savedTransaction, itemReq.quantity(), occurredAt);

            if (itemReq.unitCostCents() != null) {
                product.updateCost(itemReq.unitCostCents());
                productRepository.save(product);
            }
        }

        return savedTransaction;
    }

    public Transaction buscarPorId(UUID id) {
        return transactionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    public List<Transaction> listarComprasPorStore(UUID storeId) {
        return transactionRepository.findByStore_IdAndTypeOrderByOccurredAtDesc(storeId, TransactionType.PURCHASE);
    }

    public Page<Transaction> listarComprasPorStorePaginado(UUID storeId, Pageable pageable) {
        return transactionRepository.findByStore_IdAndType(storeId, TransactionType.PURCHASE, pageable);
    }

    public record RegistrarCompraRequest(
        UUID storeId,
        UUID idempotencyKey,
        String description,
        List<ItemCompraRequest> items,
        TransactionSource source,
        OffsetDateTime occurredAt
    ) {}

    public record ItemCompraRequest(
        UUID productId,
        BigDecimal quantity,
        Integer unitCostCents
    ) {}
}
