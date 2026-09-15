package com.contaplus.api.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reports")
public class ReportController {

    private final ReportService reportService;

    ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    public ReportService.SalesReport vendasPorPeriodo(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return reportService.vendasPorPeriodo(storeId, startDate, endDate);
    }

    @GetMapping("/top-products")
    public List<ReportService.TopSellingProduct> produtosMaisVendidos(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return reportService.produtosMaisVendidos(storeId, startDate, endDate, limit);
    }

    @GetMapping("/margins")
    public List<ReportService.ProductMarginReport> margemPorProduto(@RequestParam UUID storeId) {
        return reportService.margemPorProduto(storeId);
    }

    @GetMapping("/low-stock")
    public List<ReportService.LowStockProduct> estoqueBaixo(@RequestParam UUID storeId) {
        return reportService.produtosEstoqueBaixo(storeId);
    }

    @GetMapping("/stock-movements")
    public List<ReportService.StockMovementReport> movimentacoesEstoque(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return reportService.movimentacoesEstoque(storeId, startDate, endDate);
    }

    @GetMapping("/dashboard")
    public ReportService.DashboardReport dashboard(
            @RequestParam UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate
    ) {
        return reportService.dashboard(storeId, startDate, endDate);
    }
}
