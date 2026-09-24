package com.contaplus.api.report;

import com.contaplus.api.security.StoreAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reports")
@Tag(name = "Relatórios", description = "Relatórios e dashboard")
public class ReportController {

    private final ReportService reportService;
    private final StoreAuthorizationService storeAuth;

    ReportController(ReportService reportService, StoreAuthorizationService storeAuth) {
        this.reportService = reportService;
        this.storeAuth = storeAuth;
    }

    @GetMapping("/sales")
    public ReportService.SalesReport vendasPorPeriodo(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        storeAuth.validateStoreAccess(storeId);
        return reportService.vendasPorPeriodo(storeId, startDate, endDate);
    }

    @GetMapping("/top-products")
    public List<ReportService.TopSellingProduct> produtosMaisVendidos(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        storeAuth.validateStoreAccess(storeId);
        return reportService.produtosMaisVendidos(storeId, startDate, endDate, limit);
    }

    @GetMapping("/margins")
    public List<ReportService.ProductMarginReport> margemPorProduto(@RequestParam UUID storeId) {
        storeAuth.validateStoreAccess(storeId);
        return reportService.margemPorProduto(storeId);
    }

    @GetMapping("/low-stock")
    public List<ReportService.LowStockProduct> estoqueBaixo(@RequestParam UUID storeId) {
        storeAuth.validateStoreAccess(storeId);
        return reportService.produtosEstoqueBaixo(storeId);
    }

    @GetMapping("/stock-movements")
    public List<ReportService.StockMovementReport> movimentacoesEstoque(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        storeAuth.validateStoreAccess(storeId);
        return reportService.movimentacoesEstoque(storeId, startDate, endDate);
    }

    @GetMapping("/dashboard")
    public ReportService.DashboardReport dashboard(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        storeAuth.validateStoreAccess(storeId);
        return reportService.dashboard(storeId, startDate, endDate);
    }

    @GetMapping("/sales-by-category")
    public List<ReportService.CategorySalesReport> vendasPorCategoria(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        storeAuth.validateStoreAccess(storeId);
        return reportService.vendasPorCategoria(storeId, startDate, endDate);
    }
}
