package com.contaplus.api.supplier;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final StoreService storeService;

    SupplierService(SupplierRepository supplierRepository, StoreService storeService) {
        this.supplierRepository = supplierRepository;
        this.storeService = storeService;
    }

    @Transactional
    public Supplier criar(CriarSupplierRequest request) {
        Store store = storeService.buscarPorId(request.storeId());

        Supplier supplier = new Supplier(
            store,
            request.name(),
            request.cnpj(),
            request.phone(),
            request.email(),
            request.address(),
            request.notes()
        );

        return supplierRepository.save(supplier);
    }

    public Supplier buscarPorId(UUID id) {
        return supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    public Page<Supplier> listarPorStorePaginado(UUID storeId, boolean includeInactive, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return supplierRepository.findByStore_IdAndNameContainingIgnoreCaseAndActiveTrue(storeId, search, pageable);
        }
        if (includeInactive) {
            return supplierRepository.findByStore_Id(storeId, pageable);
        }
        return supplierRepository.findByStore_IdAndActiveTrue(storeId, pageable);
    }

    @Transactional
    public Supplier atualizar(UUID id, AtualizarSupplierRequest request) {
        Supplier supplier = buscarPorId(id);

        supplier.update(
            request.name(),
            request.cnpj(),
            request.phone(),
            request.email(),
            request.address(),
            request.notes()
        );

        return supplierRepository.save(supplier);
    }

    @Transactional
    public void desativar(UUID id) {
        Supplier supplier = buscarPorId(id);
        supplier.deactivate();
        supplierRepository.save(supplier);
    }

    public record CriarSupplierRequest(
        UUID storeId,
        String name,
        String cnpj,
        String phone,
        String email,
        String address,
        String notes
    ) {}

    public record AtualizarSupplierRequest(
        String name,
        String cnpj,
        String phone,
        String email,
        String address,
        String notes
    ) {}
}
