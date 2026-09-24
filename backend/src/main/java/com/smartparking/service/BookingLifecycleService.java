package com.smartparking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.smartparking.model.Booking;
import com.smartparking.model.BookingStatus;
import com.smartparking.repository.BookingRepository;

@Service
public class BookingLifecycleService {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public BookingLifecycleService(
            BookingRepository bookingRepository,
            BookingService bookingService) {

        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    /*
     * Automatic server-side booking lifecycle.
     *
     * CONFIRMED -> ACTIVE
     * ACTIVE -> COMPLETED
     *
     * Runs every 15 seconds.
     */
    @Scheduled(fixedDelay = 15000)
    public void updateBookingLifecycle() {

        LocalDateTime now = LocalDateTime.now();

        startDueBookings(now);
        completeDueBookings(LocalDateTime.now());
    }

    private void startDueBookings(LocalDateTime now) {

        List<Booking> dueBookings =
                bookingRepository
                        .findByStatusAndStartTimeLessThanEqual(
                                BookingStatus.CONFIRMED,
                                now
                        );

        for (Booking booking : dueBookings) {

            /*
             * If the whole reservation period has
             * already passed, don't occupy the slot.
             */
            if (booking.getEndTime() != null
                    && !booking.getEndTime().isAfter(now)) {
                continue;
            }

            try {

                bookingService.startBooking(
                        booking.getId(),
                        booking.getUserId()
                );

                System.out.println(
                        "[BOOKING LIFECYCLE] Auto-started booking "
                                + booking.getId()
                );

            } catch (Exception exception) {

                System.err.println(
                        "[BOOKING LIFECYCLE] Auto-start failed for "
                                + booking.getId()
                                + ": "
                                + exception.getMessage()
                );
            }
        }
    }

    private void completeDueBookings(LocalDateTime now) {

        List<Booking> dueBookings =
                bookingRepository
                        .findByStatusAndEndTimeLessThanEqual(
                                BookingStatus.ACTIVE,
                                now
                        );

        for (Booking booking : dueBookings) {

            try {

                bookingService.completeBooking(
                        booking.getId(),
                        booking.getUserId()
                );

                System.out.println(
                        "[BOOKING LIFECYCLE] Auto-completed booking "
                                + booking.getId()
                );

            } catch (Exception exception) {

                System.err.println(
                        "[BOOKING LIFECYCLE] Auto-complete failed for "
                                + booking.getId()
                                + ": "
                                + exception.getMessage()
                );
            }
        }

        /*
         * Recovery:
         * backend may have been offline for the
         * complete booking interval.
         */
        List<Booking> missedBookings =
                bookingRepository
                        .findByStatusAndEndTimeLessThanEqual(
                                BookingStatus.CONFIRMED,
                                now
                        );

        for (Booking booking : missedBookings) {

            try {

                booking.setStatus(
                        BookingStatus.COMPLETED
                );

                booking.setUpdatedAt(
                        LocalDateTime.now()
                );

                bookingRepository.save(booking);

                System.out.println(
                        "[BOOKING LIFECYCLE] Recovered expired booking "
                                + booking.getId()
                );

            } catch (Exception exception) {

                System.err.println(
                        "[BOOKING LIFECYCLE] Recovery failed for "
                                + booking.getId()
                                + ": "
                                + exception.getMessage()
                );
            }
        }
    }
}