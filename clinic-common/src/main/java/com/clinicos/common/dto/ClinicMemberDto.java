package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Who is signed in and which parts of the app they can use. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicMemberDto {
    private String email;
    private String fullName;
    private String role;
    private String roleLabel;
    /** The person who registered the clinic: sees everything. */
    private boolean owner;
    private List<String> pages;
}
