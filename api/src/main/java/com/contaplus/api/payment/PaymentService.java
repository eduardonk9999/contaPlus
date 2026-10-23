package com.contaplus.api.payment;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.transaction.Transaction;
import com.contaplus.api.transaction.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TransactionRepository transactionRepository;

    PaymentService(PaymentRepository paymentRepository,
                   PaymentMethodRepository paymentMethodRepository,
                   TransactionRepository transactionRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Payment registrarPagamento(UUID transactionId, UUID paymentMethodId, Integer amountCents, Integer changeCents) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
            .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", paymentMethodId));

        if (!paymentMethod.getStoreId().equals(transaction.getStoreId())) {
            throw new IllegalArgumentException("A forma de pagamento não pertence a esta loja");
        }

        if (!paymentMethod.getActive()) {
            throw new IllegalArgumentException("A forma de pagamento está desativada");
        }

        if (changeCents != null && changeCents > 0 && !paymentMethod.getAcceptsChange()) {
            throw new IllegalArgumentException("Esta forma de pagamento não aceita troco");
        }

        int netAmount = amountCents - (changeCents != null ? changeCents : 0);
        if (netAmount <= 0) {
            throw new IllegalArgumentException("O valor pago precisa ser maior que o troco");
        }

        Payment payment = new Payment(transaction, paymentMethod, amountCents, changeCents);
        transaction.addPayment(payment);

        return paymentRepository.save(payment);
    }

    @Transactional
    public List<Payment> registrarPagamentos(UUID transactionId, List<PagamentoRequest> pagamentos) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction", transactionId));

        int totalPago = 0;
        for (PagamentoRequest pag : pagamentos) {
            PaymentMethod pm = paymentMethodRepository.findById(pag.paymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", pag.paymentMethodId()));

            if (!pm.getStoreId().equals(transaction.getStoreId())) {
                throw new IllegalArgumentException("A forma de pagamento '" + pm.getName() + "' não pertence a esta loja");
            }

            if (!pm.getActive()) {
                throw new IllegalArgumentException("A forma de pagamento '" + pm.getName() + "' está desativada");
            }

            int changeCents = pag.changeCents() != null ? pag.changeCents() : 0;

            if (changeCents > 0 && !pm.getAcceptsChange()) {
                throw new IllegalArgumentException("'" + pm.getName() + "' não aceita troco");
            }

            int netAmount = pag.amountCents() - changeCents;
            if (netAmount <= 0) {
                throw new IllegalArgumentException("O valor pago em '" + pm.getName() + "' precisa ser maior que o troco");
            }

            totalPago += netAmount;
        }

        if (totalPago < transaction.getTotalAmountCents()) {
            throw new IllegalArgumentException(
                "O valor pago (" + reais(totalPago) + ") não cobre o total da venda (" +
                reais(transaction.getTotalAmountCents()) + ")"
            );
        }

        for (PagamentoRequest pag : pagamentos) {
            PaymentMethod pm = paymentMethodRepository.findById(pag.paymentMethodId()).orElseThrow();
            Payment payment = new Payment(transaction, pm, pag.amountCents(), pag.changeCents());
            transaction.addPayment(payment);
            paymentRepository.save(payment);
        }

        transactionRepository.save(transaction);

        return transaction.getPayments();
    }

    public List<Payment> listarPorTransacao(UUID transactionId) {
        return paymentRepository.findByTransaction_Id(transactionId);
    }

    private static String reais(int centavos) {
        return String.format(Locale.of("pt", "BR"), "R$ %,.2f", centavos / 100.0);
    }

    public record PagamentoRequest(
        UUID paymentMethodId,
        Integer amountCents,
        Integer changeCents
    ) {}
}
