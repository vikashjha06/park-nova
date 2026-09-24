package com.smartparking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.smartparking.model.PricingPlan;
import com.smartparking.model.VehicleType;

public interface PricingPlanRepository
        extends MongoRepository<PricingPlan, String> {

    List<PricingPlan> findByParkingAreaIdAndActiveTrue(
            String parkingAreaId
    );

    Optional<PricingPlan>
    findByParkingAreaIdAndVehicleTypeAndActiveTrue(
            String parkingAreaId,
            VehicleType vehicleType
    );

    boolean existsByParkingAreaIdAndVehicleType(
            String parkingAreaId,
            VehicleType vehicleType
    );
}