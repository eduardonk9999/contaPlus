package com.contaplus.api.cashregister;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreService;
import com.contaplus.api.user.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final CashMovementRepository cashMovementRepository;
    private final StoreService storeService;

    CashRegisterService(CashRegisterRepository cashRegisterRepository,
                        CashMovementRepository cashMovementRepository,
                        StoreService storeService) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.storeService = storeService;
    }

    @Transactional
    public CashRegister abrir(UUID storeId, User user, Integer openingAmountCents) {
        Store store = storeService.buscarPorId(storeId);

        if (cashRegisterRepository.existsByStore_IdAndStatus(storeId, CashRegisterStatus.OPEN)) {
            throw new CashRegisterAlreadyOpenException("Já existe um caixa aberto para esta loja");
        }

        CashRegister cashRegister = new CashRegister(store, user, openingAmountCents);

        CashMovement openingMovement = new CashMovement(
            cashRegister, user, CashMovementType.OPENING, openingAmountCents, "Abertura de caixa"
        );
        cashRegister.addMovement(openingMovement);

        return cashRegisterRepository.save(cashRegister);
    }

    @Transactional
    public CashRegister fechar(UUID cashRegisterId, User user, Integer closingAmountCents, String notes) {
        CashRegister cashRegister = buscarPorId(cashRegisterId);

        if (!cashRegister.isOpen()) {
            throw new CashRegisterClosedException("Este caixa já está fechado");
        }

        int expectedAmount = cashRegister.calculateExpectedAmount();

        CashMovement closingMovement = new CashMovement(
            cashRegister, user, CashMovementType.CLOSING, closingAmountCents, "Fechamento de caixa"
        );
        cashRegister.addMovement(closingMovement);

        cashRegister.close(user, closingAmountCents, expectedAmount, notes);

        return cashRegisterRepository.save(cashRegister);
    }

    @Transactional
    public CashMovement registrarSangria(UUID cashRegisterId, User user, Integer amountCents, String reason) {
        CashRegister cashRegister = buscarPorId(cashRegisterId);

        if (!cashRegister.isOpen()) {
            throw new CashRegisterClosedException("Não é possível realizar sangria em caixa fechado");
        }

        CashMovement movement = new CashMovement(
            cashRegister, user, CashMovementType.WITHDRAWAL, amountCents, reason
        );
        cashRegister.addMovement(movement);
        cashRegisterRepository.save(cashRegister);

        return movement;
    }

    @Transactional
    public CashMovement registrarSuprimento(UUID cashRegisterId, User user, Integer amountCents, String reason) {
        CashRegister cashRegister = buscarPorId(cashRegisterId);

        if (!cashRegister.isOpen()) {
            throw new CashRegisterClosedException("Não é possível realizar suprimento em caixa fechado");
        }

        CashMovement movement = new CashMovement(
            cashRegister, user, CashMovementType.SUPPLY, amountCents, reason
        );
        cashRegister.addMovement(movement);
        cashRegisterRepository.save(cashRegister);

        return movement;
    }

    public CashRegister buscarPorId(UUID id) {
        return cashRegisterRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("CashRegister", id));
    }

    public CashRegister buscarCaixaAberto(UUID storeId) {
        return cashRegisterRepository.findByStore_IdAndStatus(storeId, CashRegisterStatus.OPEN)
            .orElseThrow(() -> new NoCashRegisterOpenException("Não há caixa aberto para esta loja"));
    }

    public List<CashRegister> listarPorStore(UUID storeId) {
        return cashRegisterRepository.findByStore_IdOrderByOpenedAtDesc(storeId);
    }

    public List<CashMovement> listarMovimentos(UUID cashRegisterId) {
        buscarPorId(cashRegisterId);
        return cashMovementRepository.findByCashRegister_IdOrderByOccurredAtDesc(cashRegisterId);
    }

    public CashRegisterReport gerarRelatorio(UUID cashRegisterId) {
        CashRegister cashRegister = buscarPorId(cashRegisterId);
        List<CashMovement> movements = listarMovimentos(cashRegisterId);

        int totalSupply = 0;
        int totalWithdrawal = 0;
        int totalSales = 0;
        int totalRefunds = 0;

        for (CashMovement m : movements) {
            switch (m.getType()) {
                case SUPPLY -> totalSupply += m.getAmountCents();
                case WITHDRAWAL -> totalWithdrawal += m.getAmountCents();
                case SALE -> totalSales += m.getAmountCents();
                case REFUND -> totalRefunds += m.getAmountCents();
                default -> {}
            }
        }

        return new CashRegisterReport(
            cashRegister.getId(),
            cashRegister.getStatus(),
            cashRegister.getOpeningAmountCents(),
            cashRegister.getClosingAmountCents(),
            cashRegister.getExpectedAmountCents(),
            cashRegister.getDifferenceCents(),
            totalSupply,
            totalWithdrawal,
            totalSales,
            totalRefunds,
            movements.size(),
            cashRegister.getOpenedAt(),
            cashRegister.getClosedAt()
        );
    }

    public record CashRegisterReport(
        UUID id,
        CashRegisterStatus status,
        Integer openingAmountCents,
        Integer closingAmountCents,
        Integer expectedAmountCents,
        Integer differenceCents,
        Integer totalSupplyCents,
        Integer totalWithdrawalCents,
        Integer totalSalesCents,
        Integer totalRefundsCents,
        Integer movementCount,
        java.time.OffsetDateTime openedAt,
        java.time.OffsetDateTime closedAt
    ) {}
}
