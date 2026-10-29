package com.clinicos.emr.controller;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.emr.dto.FavouriteDrugDto;
import com.clinicos.emr.service.FavouriteDrugService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The signed-in doctor's favourite medicines and dose templates. */
@RestController
@RequestMapping("/api/v1/emr/favourite-drugs")
@RequiredArgsConstructor
public class FavouriteDrugController {

    private final FavouriteDrugService favourites;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FavouriteDrugDto>>> list(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(favourites.list(clinicId(auth), auth.getName())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FavouriteDrugDto>> save(@Valid @RequestBody FavouriteDrugDto body, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Saved to favourites", favourites.save(clinicId(auth), auth.getName(), body)));
    }

    @PostMapping("/{id}/used")
    public ResponseEntity<ApiResponse<Void>> used(@PathVariable Long id, Authentication auth) {
        favourites.markUsed(clinicId(auth), auth.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication auth) {
        favourites.delete(clinicId(auth), auth.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Removed from favourites", null));
    }

    private static String clinicId(Authentication auth) {
        return (String) auth.getDetails();
    }
}
