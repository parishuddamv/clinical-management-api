package com.clinicos.emr.dto;

import com.clinicos.emr.entity.FavouriteDrug;
import com.clinicos.emr.entity.PrescriptionItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavouriteDrugDto {
    private Long id;
    @NotBlank(message = "Medicine name is required")
    @Size(max = 200)
    private String drugName;
    @Size(max = 200)
    private String genericName;
    @Size(max = 50)
    private String strength;
    @Size(max = 50)
    private String form;
    @Size(max = 100)
    private String dosage;
    @Size(max = 50)
    private String frequency;
    @Size(max = 50)
    private String duration;
    @Size(max = 20)
    private String route;
    private PrescriptionItem.FoodTiming beforeAfterFood;
    @Size(max = 500)
    private String specialInstructions;
    private Integer useCount;

    public static FavouriteDrugDto from(FavouriteDrug f) {
        return FavouriteDrugDto.builder().id(f.getId()).drugName(f.getDrugName()).genericName(f.getGenericName())
                .strength(f.getStrength()).form(f.getForm()).dosage(f.getDosage()).frequency(f.getFrequency())
                .duration(f.getDuration()).route(f.getRoute()).beforeAfterFood(f.getBeforeAfterFood())
                .specialInstructions(f.getSpecialInstructions()).useCount(f.getUseCount()).build();
    }
}
