package com.smartparking.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.smartparking.model.MonthlyPass;
import com.smartparking.model.MonthlyPassStatus;
import com.smartparking.model.VehicleType;

@Repository
public interface MonthlyPassRepository
        extends MongoRepository<MonthlyPass, String> {

    List<MonthlyPass> findByUserId(
            String userId
    );

    List<MonthlyPass> findByUserIdAndStatus(
            String userId,
            MonthlyPassStatus status
    );

    Optional<MonthlyPass>
    findFirstByUserIdAndParkingAreaIdAndVehicleTypeAndVehicleNumberAndStatus(
            String userId,
            String parkingAreaId,
            VehicleType vehicleType,
            String vehicleNumber,
            MonthlyPassStatus status
    );

    List<MonthlyPass>
    findByStatusAndExpiryDateBefore(
            MonthlyPassStatus status,
            LocalDateTime expiryDate
    );

    /*
     * ACTIVE recurring reservations for one physical slot.
     */
    List<MonthlyPass>
    findByParkingSlotIdAndStatus(
            String parkingSlotId,
            MonthlyPassStatus status
    );

    /*
     * Used when validating duplicate/pending reservations.
     */
    List<MonthlyPass>
    findByParkingSlotIdAndStatusIn(
            String parkingSlotId,
            List<MonthlyPassStatus> statuses
    );
}