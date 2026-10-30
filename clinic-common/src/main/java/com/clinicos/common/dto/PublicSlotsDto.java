package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Times a patient can ask for on the public booking page. No patient details, only whether each
 * time is free. exact = false when the clinic has no doctor hours set, so the page asks for
 * morning, afternoon or evening instead.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicSlotsDto {
    private String date;
    private boolean exact;
    private List<Slot> slots;
    private String reason;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Slot {
        private String time;
        private boolean free;
    }
}
