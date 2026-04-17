package com.clinicos.emr.dto;

import com.clinicos.emr.entity.DrugMaster.DrugForm;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugResponse {
    private Long id;
    private String drugCode;
    private String brandName;
    private String genericName;
    private String strength;
    private DrugForm form;
    private String manufacturer;
    private String category;
    private String scheduleType;
    private BigDecimal unitPrice;
}

