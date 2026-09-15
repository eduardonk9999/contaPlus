package com.contaplus.api.customer;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreService;
import com.contaplus.api.transaction.Transaction;
import com.contaplus.api.transaction.TransactionRepository;
import com.contaplus.api.transaction.TransactionType;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final StoreService storeService;

    CustomerService(CustomerRepository customerRepository,
                    TransactionRepository transactionRepository,
                    StoreService storeService) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.storeService = storeService;
    }

    @Transactional
    public Customer criar(CriarCustomerRequest request) {
        Store store = storeService.buscarPorId(request.storeId());

        Customer customer = new Customer(
            store,
            request.name(),
            request.phone(),
            request.email(),
            request.cpfCnpj(),
            request.notes()
        );

        return customerRepository.save(customer);
    }

    public Customer buscarPorId(UUID id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    public List<Customer> listarPorStore(UUID storeId, boolean includeInactive) {
        if (includeInactive) {
            return customerRepository.findByStore_Id(storeId);
        }
        return customerRepository.findByStore_IdAndActiveTrue(storeId);
    }

    public List<Customer> buscarPorNome(UUID storeId, String nome) {
        return customerRepository.findByStore_IdAndNameContainingIgnoreCase(storeId, nome);
    }

    public Page<Customer> listarPorStorePaginado(UUID storeId, boolean includeInactive, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return customerRepository.findByStore_IdAndNameContainingIgnoreCaseAndActiveTrue(storeId, search, pageable);
        }
        if (includeInactive) {
            return customerRepository.findByStore_Id(storeId, pageable);
        }
        return customerRepository.findByStore_IdAndActiveTrue(storeId, pageable);
    }

    @Transactional
    public Customer atualizar(UUID id, AtualizarCustomerRequest request) {
        Customer customer = buscarPorId(id);

        customer.update(
            request.name(),
            request.phone(),
            request.email(),
            request.cpfCnpj(),
            request.notes()
        );

        return customerRepository.save(customer);
    }

    @Transactional
    public void desativar(UUID id) {
        Customer customer = buscarPorId(id);
        customer.deactivate();
        customerRepository.save(customer);
    }

    public List<Transaction> buscarHistoricoCompras(UUID customerId) {
        buscarPorId(customerId);
        return transactionRepository.findByCustomer_IdAndTypeOrderByOccurredAtDesc(customerId, TransactionType.SALE);
    }

    public record CriarCustomerRequest(
        UUID storeId,
        String name,
        String phone,
        String email,
        String cpfCnpj,
        String notes
    ) {}

    public record AtualizarCustomerRequest(
        String name,
        String phone,
        String email,
        String cpfCnpj,
        String notes
    ) {}
}
