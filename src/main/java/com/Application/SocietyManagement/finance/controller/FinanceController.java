package com.Application.SocietyManagement.finance.controller;

import com.Application.SocietyManagement.finance.dto.*;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.finance.service.MaintenanceBillService;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/finance/bills")
@RequiredArgsConstructor
@Tag(name = "Finance", description = "Maintenance billing and payments")
public class FinanceController {

    private final MaintenanceBillService billService;

    @Operation(summary = "Create bill", description = "Generates a maintenance bill for an apartment. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bill created"),
            @ApiResponse(responseCode = "400", description = "Validation failed or apartment mismatch"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Bill already exists for this month")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MaintenanceBillDto> createBill(
            @RequestBody @Valid CreateBillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billService.createBill(request));
    }

    @Operation(
            summary = "List bills",
            description = "Admins see all bills. Residents see only their own. Optionally filter by status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bills retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid status filter")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<PagedResponse<MaintenanceBillDto>> getBills(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Filter by status: PENDING, PAID, OVERDUE")
            @RequestParam(required = false) BillStatus status,
            @Parameter(description = "Page number, 0-indexed")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(billService.getBills(currentUser, status, page, size));
    }

    @Operation(
            summary = "Pay bill",
            description = "Simulates a UPI payment for a bill. Resident must own the bill. Full payment only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment successful"),
            @ApiResponse(responseCode = "400", description = "Partial payment attempted"),
            @ApiResponse(responseCode = "403", description = "Bill not owned by resident or bill is overdue"),
            @ApiResponse(responseCode = "404", description = "Bill not found"),
            @ApiResponse(responseCode = "409", description = "Bill already paid")
    })
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<Map<String, String>> payBill(
            @Parameter(description = "Bill ID") @PathVariable String id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid PayBillRequest request) {
        return ResponseEntity.ok(billService.payBill(id, currentUser, request));
    }
}