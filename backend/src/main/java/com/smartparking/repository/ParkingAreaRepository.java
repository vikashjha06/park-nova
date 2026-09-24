package com.smartparking.repository;

import com.smartparking.model.ParkingArea;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ParkingAreaRepository
        extends MongoRepository<ParkingArea, String> {

    List<ParkingArea> findByActiveTrue();

    List<ParkingArea> findByCityIgnoreCase(String city);
}