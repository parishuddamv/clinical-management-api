package com.clinicos.staff.service;

public class StaffSeatLimitExceededException extends RuntimeException {

    private final Long requestId;
    private final long currentSeats;
    private final long seatLimit;

    public StaffSeatLimitExceededException(
            String message,
            Long requestId,
            long currentSeats,
            long seatLimit) {
        super(message);
        this.requestId = requestId;
        this.currentSeats = currentSeats;
        this.seatLimit = seatLimit;
    }

    public Long getRequestId() {
        return requestId;
    }

    public long getCurrentSeats() {
        return currentSeats;
    }

    public long getSeatLimit() {
        return seatLimit;
    }
}