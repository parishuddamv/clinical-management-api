package com.clinicos.emr.service;

import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.exception.ResourceNotFoundException;
import com.clinicos.emr.dto.FavouriteDrugDto;
import com.clinicos.emr.entity.FavouriteDrug;
import com.clinicos.emr.repository.FavouriteDrugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Each doctor's favourite medicines and usual doses. Private to the doctor, within their clinic. */
@Service
@RequiredArgsConstructor
public class FavouriteDrugService {

    static final int MAX_PER_DOCTOR = 200;

    private final FavouriteDrugRepository favourites;

    @Transactional(readOnly = true)
    public List<FavouriteDrugDto> list(String clinicId, String doctor) {
        return favourites.findTop50ByClinicIdAndDoctorUserOrderByUseCountDescDrugNameAsc(clinicId, key(doctor))
                .stream().map(FavouriteDrugDto::from).toList();
    }

    /** Saves a favourite; saving the same medicine and strength again updates its dose template. */
    @Transactional
    public FavouriteDrugDto save(String clinicId, String doctor, FavouriteDrugDto input) {
        String drugName = trim(input.getDrugName());
        if (drugName == null) throw new RegistrationException(HttpStatus.BAD_REQUEST, "Medicine name is required.");
        String strength = trim(input.getStrength());
        FavouriteDrug f = favourites.findSame(clinicId, key(doctor), drugName, strength).orElseGet(() -> {
            if (favourites.countByClinicIdAndDoctorUser(clinicId, key(doctor)) >= MAX_PER_DOCTOR) {
                throw new RegistrationException(HttpStatus.CONFLICT, "You can keep up to " + MAX_PER_DOCTOR + " favourites. Remove some first.");
            }
            return FavouriteDrug.builder().clinicId(clinicId).doctorUser(key(doctor)).build();
        });
        f.setDrugName(drugName);
        f.setStrength(strength);
        f.setGenericName(trim(input.getGenericName()));
        f.setForm(trim(input.getForm()));
        f.setDosage(trim(input.getDosage()));
        f.setFrequency(trim(input.getFrequency()));
        f.setDuration(trim(input.getDuration()));
        f.setRoute(trim(input.getRoute()));
        f.setBeforeAfterFood(input.getBeforeAfterFood());
        f.setSpecialInstructions(trim(input.getSpecialInstructions()));
        return FavouriteDrugDto.from(favourites.save(f));
    }

    /** Counts a favourite as used, so the most-used ones come first. */
    @Transactional
    public void markUsed(String clinicId, String doctor, Long id) {
        FavouriteDrug f = find(clinicId, doctor, id);
        f.setUseCount(f.getUseCount() + 1);
        f.setLastUsedAt(LocalDateTime.now());
        favourites.save(f);
    }

    @Transactional
    public void delete(String clinicId, String doctor, Long id) {
        favourites.delete(find(clinicId, doctor, id));
    }

    private FavouriteDrug find(String clinicId, String doctor, Long id) {
        return favourites.findByIdAndClinicIdAndDoctorUser(id, clinicId, key(doctor))
                .orElseThrow(() -> new ResourceNotFoundException("Favourite not found: " + id));
    }

    private static String key(String doctor) {
        if (doctor == null || doctor.isBlank()) throw new RegistrationException(HttpStatus.UNAUTHORIZED, "Please sign in again.");
        return doctor.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
