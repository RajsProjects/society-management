package com.Application.SocietyManagement.Flat.controller;

import com.Application.SocietyManagement.Flat.dto.CreateFlatRequest;
import com.Application.SocietyManagement.Flat.dto.FlatResponse;
import com.Application.SocietyManagement.Flat.service.FlatService;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/flats")
@RequiredArgsConstructor
@Tag(name = "Flats", description = "Flat and unit management")
public class FlatController {

    private final FlatService flatService;

    @Operation(summary = "Create flat",
            description = "Creates a new flat/unit. Admin only.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FlatResponse> create(
            @RequestBody @Valid CreateFlatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(flatService.create(request));
    }

    @Operation(summary = "List flats",
            description = "Returns all flats. Filter by block or occupancy.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'RESIDENT')")
    public ResponseEntity<PagedResponse<FlatResponse>> getAll(
            @RequestParam(required = false) String block,
            @RequestParam(required = false) Boolean occupied,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                flatService.getAll(block, occupied, page, size));
    }

    @Operation(summary = "Get flat by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'RESIDENT')")
    public ResponseEntity<FlatResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(flatService.getById(id));
    }

    @Operation(summary = "Update flat",
            description = "Updates flat details. Admin only.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FlatResponse> update(
            @PathVariable String id,
            @RequestBody @Valid CreateFlatRequest request) {
        return ResponseEntity.ok(flatService.update(id, request));
    }

    @Operation(summary = "Delete flat",
            description = "Deletes a flat. Admin only.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        flatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
