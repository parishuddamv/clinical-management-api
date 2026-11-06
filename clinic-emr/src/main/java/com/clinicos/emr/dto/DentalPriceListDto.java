package com.clinicos.emr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DentalPriceListDto {
    /** { procedures: [ { code, name, price, sittings, lab, wholeMouth } ] }; null when the clinic hasn't saved one. */
    private JsonNode data;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
