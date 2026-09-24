package com.smartparking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.smartparking.model.ParkingSlot;
import com.smartparking.model.ParkingSlotStatus;
import com.smartparking.model.VehicleType;

public interface ParkingSlotRepository
        extends MongoRepository<ParkingSlot, String> {

    List<ParkingSlot> findByParkingAreaId(
            String parkingAreaId
    );

    List<ParkingSlot> findByParkingAreaIdAndStatus(
            String parkingAreaId,
            ParkingSlotStatus status
    );

    List<ParkingSlot> findByParkingAreaIdAndVehicleType(
            String parkingAreaId,
            VehicleType vehicleType
    );

    List<ParkingSlot> findByParkingAreaIdAndActiveTrue(
            String parkingAreaId
    );

    List<ParkingSlot>
    findByParkingAreaIdAndVehicleTypeAndActiveTrue(
            String parkingAreaId,
            VehicleType vehicleType
    );

    List<ParkingSlot>
    findByParkingAreaIdAndStatusAndActiveTrue(
            String parkingAreaId,
            ParkingSlotStatus status
    );

    List<ParkingSlot>
    findByParkingAreaIdAndVehicleTypeAndStatusAndActiveTrue(
            String parkingAreaId,
            VehicleType vehicleType,
            ParkingSlotStatus status
    );

    Optional<ParkingSlot>
    findByParkingAreaIdAndSlotNumber(
            String parkingAreaId,
            String slotNumber
    );

    boolean existsByParkingAreaIdAndSlotNumber(
            String parkingAreaId,
            String slotNumber
    );

    long countByParkingAreaIdAndStatusAndActiveTrue(
            String parkingAreaId,
            ParkingSlotStatus status
    );

    long countByParkingAreaIdAndVehicleTypeAndStatusAndActiveTrue(
            String parkingAreaId,
            VehicleType vehicleType,
            ParkingSlotStatus status
    );
}