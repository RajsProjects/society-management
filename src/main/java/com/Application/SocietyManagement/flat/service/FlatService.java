package com.Application.SocietyManagement.flat.service;

import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.flat.dto.CreateFlatRequest;
import com.Application.SocietyManagement.flat.dto.FlatResponse;
import com.Application.SocietyManagement.flat.entity.Flat;
import com.Application.SocietyManagement.flat.repository.FlatRepository;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FlatService {

    private final FlatRepository flatRepository;

    public FlatResponse create(CreateFlatRequest request) {
        String societyId = TenantContext.getSocietyId();

        if (flatRepository.existsByFlatNumberAndSocietyId(
                request.getFlatNumber(), societyId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Flat number already exists: " + request.getFlatNumber());
        }

        Flat flat = Flat.builder()
                .societyId(societyId)
                .block(request.getBlock())
                .floor(request.getFloor())
                .flatNumber(request.getFlatNumber())
                .ownerName(request.getOwnerName())
                .ownerEmail(request.getOwnerEmail())
                .ownerPhone(request.getOwnerPhone())
                .occupied(request.isOccupied())
                .type(request.getType())
                .areaSqFt(request.getAreaSqFt())
                .build();

        return FlatResponse.from(flatRepository.save(flat));
    }

    public PagedResponse<FlatResponse> getAll(String block,
                                              Boolean occupied,
                                              int page, int size) {
        String societyId = TenantContext.getSocietyId();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("block").ascending()
                        .and(Sort.by("flatNumber").ascending()));

        Page<Flat> result;

        if (block != null && occupied != null) {
            result = flatRepository.findBySocietyIdAndBlock(
                    societyId, block, pageable);
        } else if (block != null) {
            result = flatRepository.findBySocietyIdAndBlock(
                    societyId, block, pageable);
        } else if (occupied != null) {
            result = flatRepository.findBySocietyIdAndOccupied(
                    societyId, occupied, pageable);
        } else {
            result = flatRepository.findBySocietyId(societyId, pageable);
        }

        return PagedResponse.<FlatResponse>builder()
                .content(result.getContent().stream()
                        .map(FlatResponse::from).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    public FlatResponse getById(String flatId) {
        String societyId = TenantContext.getSocietyId();
        Flat flat = flatRepository.findByIdAndSocietyId(flatId, societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Flat not found"));
        return FlatResponse.from(flat);
    }

    public FlatResponse update(String flatId, CreateFlatRequest request) {
        String societyId = TenantContext.getSocietyId();
        Flat flat = flatRepository.findByIdAndSocietyId(flatId, societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Flat not found"));

        flat.setBlock(request.getBlock());
        flat.setFloor(request.getFloor());
        flat.setFlatNumber(request.getFlatNumber());
        flat.setOwnerName(request.getOwnerName());
        flat.setOwnerEmail(request.getOwnerEmail());
        flat.setOwnerPhone(request.getOwnerPhone());
        flat.setOccupied(request.isOccupied());
        flat.setType(request.getType());
        flat.setAreaSqFt(request.getAreaSqFt());

        return FlatResponse.from(flatRepository.save(flat));
    }

    public void delete(String flatId) {
        String societyId = TenantContext.getSocietyId();
        Flat flat = flatRepository.findByIdAndSocietyId(flatId, societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Flat not found"));
        flatRepository.delete(flat);
    }
}