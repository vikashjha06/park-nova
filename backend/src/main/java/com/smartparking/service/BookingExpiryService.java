package com.smartparking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.smartparking.model.Booking;
import com.smartparking.model.BookingStatus;
import com.smartparking.repository.BookingRepository;

@Service
public class BookingExpiryService {

    private final BookingRepository bookingRepository;

    public BookingExpiryService(
            BookingRepository bookingRepository) {

        this.bookingRepository =
                bookingRepository;
    }

    /*
     * Check expired payment holds once per minute.
     */
    @Scheduled(fixedDelay = 60000)
    public void expirePendingBookings() {

        LocalDateTime now =
                LocalDateTime.now();

        List<Booking> expiredBookings =
                bookingRepository
                        .findByStatusAndPaymentExpiresAtBefore(
                                BookingStatus.PENDING,
                                now
                        );

        for (Booking booking : expiredBookings) {

            booking.setStatus(
                    BookingStatus.CANCELLED
            );

            booking.setPaymentExpiresAt(
                    null
            );

            booking.setUpdatedAt(now);

            bookingRepository.save(
                    booking
            );
        }
    }
}