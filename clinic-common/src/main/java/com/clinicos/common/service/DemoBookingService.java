package com.clinicos.common.service;

import com.clinicos.common.entity.DemoBooking;
import com.clinicos.common.repository.DemoBookingRepository;
import com.clinicos.common.security.RegistrationAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DemoBookingService {

    private final ObjectProvider<DemoBookingRepository> repositoryProvider;
    private final RegistrationAccess access;

    private DemoBookingRepository getRepository() {
        DemoBookingRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new RuntimeException("DemoBookingRepository not available");
        }
        return repository;
    }

    @Transactional(readOnly = true)
    public Page<DemoBooking> getPendingBookings(
            String authorization,
            int page,
            int size) {

        access.requireSuperAdmin(authorization);
        validatePage(page, size);

        return getRepository().findByStatus(
                DemoBooking.DemoBookingStatus.PENDING,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.asc("demoDate"),
                                Sort.Order.asc("demoTime"),
                                Sort.Order.desc("createdAt")
                        )
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<DemoBooking> getConfirmedBookings(
            String authorization,
            int page,
            int size) {

        access.requireSuperAdmin(authorization);
        validatePage(page, size);

        return getRepository().findByStatus(
                DemoBooking.DemoBookingStatus.CONFIRMED,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.asc("demoDate"),
                                Sort.Order.asc("demoTime"),
                                Sort.Order.desc("createdAt")
                        )
                )
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<DemoBooking> getBookingsByDate(
            String authorization,
            LocalDate date) {

        access.requireSuperAdmin(authorization);

        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        return getRepository().findByDemoDateOrderByDemoTimeAsc(date);
    }

    @Transactional(readOnly = true)
    public DemoBooking getBooking(
            String authorization,
            Long id) {

        access.requireSuperAdmin(authorization);

        return getRepository().findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Demo booking not found: " + id));
    }

    @Transactional
    public DemoBooking confirmBooking(
            String authorization,
            Long id,
            String demoLink) {

        var admin = access.requireSuperAdmin(authorization);

        DemoBooking booking = getRepository().findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Demo booking not found: " + id));

        if (booking.getStatus() != DemoBooking.DemoBookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending demo bookings can be confirmed."
            );
        }

        booking.confirm(demoLink, admin.getEmail());

        return getRepository().save(booking);
    }

    @Transactional
    public DemoBooking cancelBooking(
            String authorization,
            Long id) {

        var admin = access.requireSuperAdmin(authorization);

        DemoBooking booking = getRepository().findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Demo booking not found: " + id));

        if (booking.getStatus() != DemoBooking.DemoBookingStatus.PENDING
                && booking.getStatus() != DemoBooking.DemoBookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only pending or confirmed demo bookings can be cancelled."
            );
        }

        booking.cancel();

        return getRepository().save(booking);
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0.");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100.");
        }
    }
}
