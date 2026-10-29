package com.clinicos.common.service;

import com.clinicos.common.dto.BookingRequestDto;
import com.clinicos.common.dto.PublicDoctorDto;
import com.clinicos.common.entity.BookingRequest;
import com.clinicos.common.entity.ClinicProfile;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.BookingRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private final BookingRequestRepository repo = mock(BookingRequestRepository.class);
    private final ClinicProfileService profiles = mock(ClinicProfileService.class);
    private BookingService service;
    private final LocalDate today = LocalDate.of(2026, 10, 30);
    private final List<PublicDoctorDto> doctors = List.of(PublicDoctorDto.builder().id(5L).name("Dr. Neha Mehta").build());

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        ObjectProvider<BookingRequestRepository> p = mock(ObjectProvider.class);
        when(p.getObject()).thenReturn(repo);
        service = new BookingService(p, profiles);
        service.setClock(Clock.fixed(today.atTime(9, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant(), ZoneId.of("Asia/Kolkata")));
        when(profiles.bookableBySlug("sunrise")).thenReturn(ClinicProfile.builder().clinicId("C1").bookingSlug("sunrise").bookingEnabled(true).build());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private BookingRequestDto form() {
        return BookingRequestDto.builder().patientName(" Asha  Rao ").phone("+91 98765-43210").preferredDate(today.plusDays(1))
                .preferredTime("morning").doctorId(5L).consent(true).build();
    }

    @Test
    void validRequestIsStoredForTheClinicWithAReference() {
        BookingRequestDto result = service.request("sunrise", form(), "1.2.3.4", doctors);

        ArgumentCaptor<BookingRequest> saved = ArgumentCaptor.forClass(BookingRequest.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getClinicId()).isEqualTo("C1");
        assertThat(saved.getValue().getPatientName()).isEqualTo("Asha Rao");
        assertThat(saved.getValue().getPhone()).isEqualTo("9876543210");
        assertThat(saved.getValue().getPreferredTime()).isEqualTo("MORNING");
        assertThat(saved.getValue().getDoctorName()).isEqualTo("Dr. Neha Mehta");
        assertThat(result.getReference()).matches("BK-[A-Z2-9]{6}");
        assertThat(result.getPhone()).isNull();
    }

    @Test
    void spamBotsAreQuietlyIgnored() {
        BookingRequestDto result = service.request("sunrise", form().toBuilder().website("http://spam").build(), "1.2.3.4", doctors);
        assertThat(result.getReference()).isEqualTo("RECEIVED");
        verify(repo, never()).save(any());
    }

    @Test
    void validation() {
        assertThatThrownBy(() -> service.request("sunrise", form().toBuilder().phone("12345").build(), null, doctors)).hasMessageContaining("mobile");
        assertThatThrownBy(() -> service.request("sunrise", form().toBuilder().consent(false).build(), null, doctors)).hasMessageContaining("agree");
        assertThatThrownBy(() -> service.request("sunrise", form().toBuilder().preferredDate(today.minusDays(1)).build(), null, doctors)).hasMessageContaining("date");
        assertThatThrownBy(() -> service.request("sunrise", form().toBuilder().preferredDate(today.plusDays(61)).build(), null, doctors)).hasMessageContaining("date");
        assertThatThrownBy(() -> service.request("sunrise", form().toBuilder().preferredTime("night").build(), null, doctors)).hasMessageContaining("morning");
        assertThatThrownBy(() -> service.request("sunrise", form().toBuilder().doctorId(99L).build(), null, doctors)).hasMessageContaining("doctor");
    }

    @Test
    void floodingIsLimited() {
        when(repo.countByClinicIdAndPhoneAndCreatedAtAfter(eq("C1"), eq("9876543210"), any())).thenReturn(3L);
        assertThatThrownBy(() -> service.request("sunrise", form(), "1.2.3.4", doctors))
                .isInstanceOfSatisfying(RegistrationException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void confirmingNeedsTheBookedAppointment() {
        when(repo.findByIdAndClinicId(1L, "C1")).thenReturn(Optional.of(BookingRequest.builder().id(1L).clinicId("C1").status(BookingRequest.Status.NEW).build()));
        assertThatThrownBy(() -> service.update("C1", 1L, "CONFIRMED", null, "desk@x.in")).hasMessageContaining("Book the appointment");
        BookingRequestDto done = service.update("C1", 1L, "confirmed", 77L, "desk@x.in");
        assertThat(done.getStatus()).isEqualTo("CONFIRMED");
        assertThat(done.getAppointmentId()).isEqualTo(77L);
    }

    @Test
    void anotherClinicsRequestIsNotFound() {
        when(repo.findByIdAndClinicId(1L, "C2")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update("C2", 1L, "DECLINED", null, "x"))
                .isInstanceOfSatisfying(RegistrationException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
