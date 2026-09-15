package com.clinicos.billing.service;

import com.clinicos.billing.dto.CreateInvoiceRequest;
import com.clinicos.billing.entity.Invoice;
import com.clinicos.billing.repository.InvoiceRepository;
import com.clinicos.billing.subscription.model.FeatureCode;
import com.clinicos.billing.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceAccessTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void create_enforcesSubscriptionAndPermissionBeforePersist() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setPatientId(101L);
        request.setTotalAmount(new BigDecimal("350.00"));
        request.setInvoiceDate(LocalDate.now());
        request.setDueDate(LocalDate.now().plusDays(7));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invoiceService.create("CLINIC_A", request, authentication);

        verify(subscriptionService).enforceAccess(authentication, FeatureCode.BASIC_BILLING, "MANAGE_BILLING");
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertEquals("CLINIC_A", captor.getValue().getClinicId());
    }
}

