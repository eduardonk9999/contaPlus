package com.contaplus.api.stock;

import com.contaplus.api.product.Product;
import com.contaplus.api.product.ProductRepository;
import com.contaplus.api.store.Store;
import com.contaplus.api.transaction.Transaction;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class StockService {

    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;

    StockService(StockMovementRepository movementRepository, ProductRepository productRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public StockMovement registrarMovimento(Store store, Product product, Transaction transaction,
                                            StockMovementType type, BigDecimal quantityDelta,
                                            String reason, OffsetDateTime occurredAt) {
        BigDecimal quantityBefore = product.getStockQuantity();

        StockMovement movement = new StockMovement(
            store, product, transaction, type,
            quantityDelta, quantityBefore, reason, occurredAt
        );


        product.adjustStock(quantityDelta);
        productRepository.save(product);

        return movementRepository.save(movement);
    }

    @Transactional
    public StockMovement ajustarEstoque(Store store, Product product, BigDecimal novaQuantidade,
                                        String reason, OffsetDateTime occurredAt) {
        BigDecimal delta = novaQuantidade.subtract(product.getStockQuantity());

        return registrarMovimento(
            store, product, null,
            StockMovementType.ADJUSTMENT, delta,
            reason, occurredAt
        );
    }

    @Transactional
    public StockMovement registrarVenda(Store store, Product product, Transaction transaction,
                                        BigDecimal quantidadeVendida, OffsetDateTime occurredAt) {
        BigDecimal delta = quantidadeVendida.negate();

        return registrarMovimento(
            store, product, transaction,
            StockMovementType.SALE, delta,
            "Sale transaction: " + transaction.getId(), occurredAt
        );
    }

    @Transactional
    public StockMovement registrarCompra(Store store, Product product, Transaction transaction,
                                         BigDecimal quantidadeComprada, OffsetDateTime occurredAt) {
        return registrarMovimento(
            store, product, transaction,
            StockMovementType.PURCHASE, quantidadeComprada,
            "Purchase transaction: " + transaction.getId(), occurredAt
        );
    }

    public List<StockMovement> listarMovimentosPorProduto(UUID productId) {
        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public List<StockMovement> listarMovimentosPorStore(UUID storeId) {
        return movementRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }
}
