package com.clinicos.emr.ai;

import com.clinicos.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** AI assistant endpoints. Under /emr, so the gateway's VISITS permission applies. */
@RestController
@RequestMapping("/api/v1/emr")
@RequiredArgsConstructor
public class AiController {

    private final PatientSummaryService summaries;

    /** Lets the screens show AI buttons only where the AI assistant is switched on. */
    @GetMapping("/ai/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {
        Map<String, Object> body = Map.of("enabled", summaries.available());
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    @PostMapping("/patients/{patientId}/ai-summary")
    public ResponseEntity<ApiResponse<PatientSummaryResponse>> summarise(@PathVariable Long patientId,
                                                                         @RequestBody(required = false) PatientSummaryRequest body,
                                                                         Authentication auth) {
        String clinicId = (String) auth.getDetails();
        return ResponseEntity.ok(ApiResponse.success(summaries.summarise(clinicId, patientId, body)));
    }
}
