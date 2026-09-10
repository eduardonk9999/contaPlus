package com.contaplus.api.sale;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.product.Product;
import com.contaplus.api.product.ProductRepository;
import com.contaplus.api.stock.StockService;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreService;
import com.contaplus.api.transaction.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SaleService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final StoreService storeService;
    private final StockService stockService;

    SaleService(TransactionRepository transactionRepository,
                ProductRepository productRepository,
                StoreService storeService,
                StockService stockService) {
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
        this.storeService = storeService;
        this.stockService = stockService;
    }

    @Transactional
    public Transaction confirmarVenda(ConfirmarVendaRequest request) {
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
            TransactionType.SALE,
            request.source() != null ? request.source() : TransactionSource.MANUAL,
            request.description() != null ? request.description() : "",
            request.idempotencyKey(),
            occurredAt
        );

        if (request.originalInput() != null) {
            transaction.setOriginalInput(request.originalInput());
        }

        // First, validate all products and create items
        List<Product> productsToUpdate = new ArrayList<>();
        for (ItemVendaRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.productId()));

            if (!product.getStoreId().equals(request.storeId())) {
                throw new IllegalArgumentException(
                    "Product " + itemReq.productId() + " does not belong to store " + request.storeId()
                );
            }

            if (!product.getActive()) {
                throw new IllegalArgumentException("Product " + product.getName() + " is not active");
            }

            TransactionItem item = new TransactionItem(transaction, product, itemReq.quantity());
            transaction.addItem(item);
            productsToUpdate.add(product);
        }

        // Save the transaction first so it has a valid ID for stock movements
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Now create stock movements (transaction is persisted)
        for (int i = 0; i < request.items().size(); i++) {
            ItemVendaRequest itemReq = request.items().get(i);
            Product product = productsToUpdate.get(i);

            stockService.registrarVenda(store, product, savedTransaction, itemReq.quantity(), occurredAt);
        }

        return savedTransaction;
    }

    public Transaction buscarPorId(UUID id) {
        return transactionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    public List<Transaction> listarVendasPorStore(UUID storeId) {
        return transactionRepository.findByStore_IdAndTypeOrderByOccurredAtDesc(storeId, TransactionType.SALE);
    }

    public record ConfirmarVendaRequest(
        UUID storeId,
        UUID idempotencyKey,
        String description,
        List<ItemVendaRequest> items,
        TransactionSource source,
        String originalInput,
        OffsetDateTime occurredAt
    ) {}

    public record ItemVendaRequest(
        UUID productId,
        BigDecimal quantity
    ) {}
}
