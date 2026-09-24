package com.smartparking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.smartparking.model.Booking;
import com.smartparking.model.BookingStatus;

public interface BookingRepository
        extends MongoRepository<Booking, String> {

    List<Booking> findByUserId(
            String userId
    );

    List<Booking> findByUserIdAndStatus(
            String userId,
            BookingStatus status
    );

    List<Booking> findByParkingAreaId(
            String parkingAreaId
    );

    List<Booking> findByParkingSlotId(
            String parkingSlotId
    );

    List<Booking> findByStatus(
            BookingStatus status
    );

    /*
     * Finds unpaid PENDING bookings whose
     * payment hold has already expired.
     */
    List<Booking> findByStatusAndPaymentExpiresAtBefore(
            BookingStatus status,
            LocalDateTime paymentExpiresAt
    );

    /*
     * OVERLAP RULE:
     *
     * existing.startTime < requestedEnd
     * AND
     * existing.endTime > requestedStart
     *
     * Only PENDING, CONFIRMED and ACTIVE
     * bookings block the selected interval.
     *
     * CANCELLED and COMPLETED bookings do not
     * block future reservations.
     */
    @Query(value = """
            {
              'parkingSlotId': ?0,
              'status': {
                '$in': ['PENDING', 'CONFIRMED', 'ACTIVE']
              },
              'startTime': { '$lt': ?2 },
              'endTime': { '$gt': ?1 }
            }
            """,
            exists = true)
    boolean existsOverlappingBooking(
            String parkingSlotId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /*
     * Confirmed bookings whose scheduled start time
     * has arrived.
     */
    List<Booking> findByStatusAndStartTimeLessThanEqual(
            BookingStatus status,
            LocalDateTime startTime
    );

    /*
     * Active/confirmed bookings whose scheduled
     * end time has arrived.
     */
    List<Booking> findByStatusAndEndTimeLessThanEqual(
            BookingStatus status,
            LocalDateTime endTime
    );
}