import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./ParkingPage.css";

function getAvailabilityTone(available, total) {
  if (!total || available <= 0) return "full";

  const ratio = available / total;

  if (ratio <= 0.15) return "low";
  return "available";
}

function vehicleLabel(type) {
  if (!type) return "Vehicle";

  return type
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function hasValidCoordinates(area) {
  const latitude = Number(area?.latitude);
  const longitude = Number(area?.longitude);

  return (
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    latitude >= -90 &&
    latitude <= 90 &&
    longitude >= -180 &&
    longitude <= 180 &&
    !(latitude === 0 && longitude === 0)
  );
}

function getParkingDestination(area) {
  if (hasValidCoordinates(area)) {
    return `${Number(area.latitude)},${Number(area.longitude)}`;
  }

  return [area?.address, area?.city]
    .filter(Boolean)
    .join(", ")
    .trim();
}

function getViewMapUrl(area) {
  if (hasValidCoordinates(area)) {
    const destination = getParkingDestination(area);

    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
      destination
    )}`;
  }

  if (area?.googleMapUrl?.trim()) {
    return area.googleMapUrl.trim();
  }

  const destination = getParkingDestination(area);

  if (!destination) return "";

  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
    destination
  )}`;
}

function getDirectionsUrl(area) {
  const destination = getParkingDestination(area);

  if (!destination) return "";

  let url =
    `https://www.google.com/maps/dir/?api=1` +
    `&destination=${encodeURIComponent(destination)}` +
    `&travelmode=driving`;

  if (area?.googlePlaceId?.trim()) {
    url += `&destination_place_id=${encodeURIComponent(
      area.googlePlaceId.trim()
    )}`;
  }

  return url;
}

export default function ParkingPage() {
  const navigate = useNavigate();

  const [areas, setAreas] = useState([]);
  const [selectedArea, setSelectedArea] = useState(null);
  const [slots, setSlots] = useState([]);
  const [selectedSlot, setSelectedSlot] = useState(null);

  const [search, setSearch] = useState("");
  const [vehicleFilter, setVehicleFilter] = useState("ALL");

  const [loadingAreas, setLoadingAreas] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [areaError, setAreaError] = useState("");
  const [slotError, setSlotError] = useState("");

  const handleAuthError = (error) => {
    const status = error?.response?.status;

    if (status === 401 || status === 403) {
      localStorage.removeItem("parkNovaToken");
      localStorage.removeItem("parkNovaUser");
      navigate("/login", { replace: true });
      return true;
    }

    return false;
  };

  const loadAreas = async () => {
    try {
      setLoadingAreas(true);
      setAreaError("");

      const response = await api.get("/api/parking/areas");

      const data = Array.isArray(response.data) ? response.data : [];
      const activeAreas = data.filter((area) => area.active !== false);

      setAreas(activeAreas);

      setSelectedArea((current) => {
        if (!activeAreas.length) return null;

        if (current) {
          return (
            activeAreas.find((area) => area.id === current.id) ||
            activeAreas[0]
          );
        }

        return activeAreas[0];
      });
    } catch (error) {
      if (handleAuthError(error)) return;

      setAreaError(
        error?.response?.data?.message ||
          "Unable to load parking areas right now."
      );
    } finally {
      setLoadingAreas(false);
    }
  };

  const loadSlots = async (areaId) => {
    if (!areaId) {
      setSlots([]);
      return;
    }

    try {
      setLoadingSlots(true);
      setSlotError("");
      setSelectedSlot(null);

      const response = await api.get(
        `/api/parking/areas/${areaId}/available-slots`
      );

      const data = Array.isArray(response.data) ? response.data : [];

      setSlots(
        data.filter(
          (slot) =>
            slot.active !== false &&
            String(slot.status || "").toUpperCase() === "AVAILABLE"
        )
      );
    } catch (error) {
      if (handleAuthError(error)) return;

      setSlots([]);
      setSlotError(
        error?.response?.data?.message ||
          "Unable to load available slots."
      );
    } finally {
      setLoadingSlots(false);
    }
  };

  useEffect(() => {
    loadAreas();
  }, []);

  useEffect(() => {
    if (selectedArea?.id) {
      loadSlots(selectedArea.id);
    }
  }, [selectedArea?.id]);

  const filteredAreas = useMemo(() => {
    const query = search.trim().toLowerCase();

    if (!query) return areas;

    return areas.filter((area) =>
      [area.name, area.address, area.city]
        .filter(Boolean)
        .some((value) =>
          String(value).toLowerCase().includes(query)
        )
    );
  }, [areas, search]);

  const vehicleTypes = useMemo(() => {
    return [
      "ALL",
      ...Array.from(
        new Set(
          slots
            .map((slot) => slot.vehicleType)
            .filter(Boolean)
        )
      ),
    ];
  }, [slots]);

  const filteredSlots = useMemo(() => {
    if (vehicleFilter === "ALL") return slots;

    return slots.filter(
      (slot) =>
        String(slot.vehicleType).toUpperCase() ===
        String(vehicleFilter).toUpperCase()
    );
  }, [slots, vehicleFilter]);

  useEffect(() => {
    if (
      vehicleFilter !== "ALL" &&
      !vehicleTypes.includes(vehicleFilter)
    ) {
      setVehicleFilter("ALL");
    }
  }, [vehicleFilter, vehicleTypes]);

  const selectArea = (area) => {
    setSelectedArea(area);
    setSelectedSlot(null);
    setVehicleFilter("ALL");

    if (window.innerWidth <= 760) {
      setTimeout(() => {
        document
          .getElementById("pn-parking-slots")
          ?.scrollIntoView({
            behavior: "smooth",
            block: "start",
          });
      }, 100);
    }
  };

  const refreshParking = async () => {
    await loadAreas();

    if (selectedArea?.id) {
      await loadSlots(selectedArea.id);
    }
  };

  const continueToBooking = () => {
    if (!selectedArea || !selectedSlot) return;

    navigate("/bookings", {
      state: {
        bookingDraft: {
          parkingAreaId: selectedArea.id,
          parkingAreaName: selectedArea.name,
          parkingAreaAddress: selectedArea.address,
          parkingAreaCity: selectedArea.city,
          parkingSlotId: selectedSlot.id,
          slotNumber: selectedSlot.slotNumber,
          vehicleType: selectedSlot.vehicleType,
          floor: selectedSlot.floor,
          zone: selectedSlot.zone,
        },
      },
    });
  };

  return (
    <main className="pn-parking-page">
      <header className="pn-parking-header">
        <Link
          to="/"
          className="pn-parking-brand"
          aria-label="Park Nova home"
        >
          <img
            src="/images/park-nova-logo.png"
            alt="Park Nova"
          />
        </Link>

        <nav className="pn-parking-nav">
          <Link to="/">Home</Link>
          <Link to="/bookings">My Bookings</Link>
          <Link to="/wallet">Wallet</Link>
          <Link to="/profile">Profile</Link>
        </nav>
      </header>

      <section className="pn-parking-shell">
        <div className="pn-parking-hero">
          <div>
            <p className="pn-parking-eyebrow">
              <span />
              LIVE PARKING
            </p>

            <h1>
              Find your <em>perfect spot.</em>
            </h1>

            <p className="pn-parking-subtitle">
              Check live parking availability, explore available
              slots and choose where you want to park.
            </p>
          </div>

          <button
            type="button"
            className="pn-parking-refresh"
            onClick={refreshParking}
            disabled={loadingAreas || loadingSlots}
          >
            <span>↻</span>
            {loadingAreas || loadingSlots
              ? "Refreshing..."
              : "Refresh availability"}
          </button>
        </div>

        <section className="pn-parking-search-panel">
          <div className="pn-parking-search-box">
            <span className="pn-parking-search-icon">
              ⌕
            </span>

            <input
              type="search"
              value={search}
              onChange={(event) =>
                setSearch(event.target.value)
              }
              placeholder="Search by parking name, address or city"
              aria-label="Search parking areas"
            />

            {search && (
              <button
                type="button"
                onClick={() => setSearch("")}
                aria-label="Clear search"
              >
                ×
              </button>
            )}
          </div>

          <div className="pn-parking-live">
            <span />
            Live availability
          </div>
        </section>

        {areaError && (
          <div className="pn-parking-error" role="alert">
            <div>
              <strong>Could not load parking areas</strong>
              <span>{areaError}</span>
            </div>

            <button
              type="button"
              onClick={loadAreas}
            >
              Try again
            </button>
          </div>
        )}

        {loadingAreas ? (
          <section className="pn-parking-loading">
            <div className="pn-parking-spinner" />
            <strong>Finding parking near you...</strong>
            <span>Checking live availability</span>
          </section>
        ) : (
          <div className="pn-parking-layout">
            <section className="pn-parking-area-section">
              <div className="pn-parking-section-title">
                <div>
                  <p>PARKING LOCATIONS</p>
                  <h2>Available parking areas</h2>
                </div>

                <span>
                  {filteredAreas.length}{" "}
                  {filteredAreas.length === 1
                    ? "location"
                    : "locations"}
                </span>
              </div>

              {filteredAreas.length === 0 ? (
                <div className="pn-parking-empty">
                  <div>⌕</div>
                  <strong>No parking areas found</strong>
                  <span>
                    Try another parking name, address or city.
                  </span>

                  {search && (
                    <button
                      type="button"
                      onClick={() => setSearch("")}
                    >
                      Clear search
                    </button>
                  )}
                </div>
              ) : (
                <div className="pn-parking-area-list">
                  {filteredAreas.map((area) => {
                    const isSelected =
                      selectedArea?.id === area.id;

                    const available =
                      Number(area.availableSlots) || 0;

                    const total =
                      Number(area.totalSlots) || 0;

                    const occupied = Math.max(
                      total - available,
                      0
                    );

                    const usedPercent = total
                      ? Math.min(
                          100,
                          Math.round(
                            (occupied / total) * 100
                          )
                        )
                      : 0;

                    const tone = getAvailabilityTone(
                      available,
                      total
                    );

                    return (
                      <article
                        key={area.id}
                        className={`pn-parking-area-card ${
                          isSelected ? "is-selected" : ""
                        }`}
                      >
                        <button
                          type="button"
                          className="pn-parking-area-main"
                          onClick={() => selectArea(area)}
                        >
                          <div className="pn-parking-area-icon">
                            P
                          </div>

                          <div className="pn-parking-area-copy">
                            <div className="pn-parking-area-top">
                              <div>
                                <h3>{area.name}</h3>
                                <p>
                                  <span>⌖</span>
                                  {[area.address, area.city]
                                    .filter(Boolean)
                                    .join(", ")}
                                </p>
                              </div>

                              <span
                                className={`pn-parking-availability ${tone}`}
                              >
                                {available > 0
                                  ? `${available} available`
                                  : "Full"}
                              </span>
                            </div>

                            <div className="pn-parking-capacity">
                              <div>
                                <span>
                                  Live capacity
                                </span>
                                <strong>
                                  {available} / {total} free
                                </strong>
                              </div>

                              <div className="pn-parking-capacity-track">
                                <span
                                  style={{
                                    width: `${usedPercent}%`,
                                  }}
                                />
                              </div>
                            </div>
                          </div>
                        </button>

                        <div className="pn-parking-area-footer">
                          <div>
                            <span>
                              {total} total spaces
                            </span>

                            <span className="pn-parking-dot">
                              •
                            </span>

                            <span>
                              {area.active
                                ? "Open"
                                : "Unavailable"}
                            </span>
                          </div>

                          <div className="pn-parking-map-actions">
                            {getViewMapUrl(area) && (
                              <a
                                className="pn-parking-map-link"
                                href={getViewMapUrl(area)}
                                target="_blank"
                                rel="noopener noreferrer"
                                aria-label={`View ${area.name} on Google Maps`}
                              >
                                <span aria-hidden="true">⌖</span>
                                View on Map
                              </a>
                            )}

                            {getDirectionsUrl(area) && (
                              <a
                                className="pn-parking-directions-link"
                                href={getDirectionsUrl(area)}
                                target="_blank"
                                rel="noopener noreferrer"
                                aria-label={`Get driving directions to ${area.name}`}
                              >
                                <span aria-hidden="true">↗</span>
                                Get Directions
                              </a>
                            )}
                          </div>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>

            <aside
              className="pn-parking-slots-panel"
              id="pn-parking-slots"
            >
              {!selectedArea ? (
                <div className="pn-parking-no-selection">
                  <div>P</div>
                  <strong>Select a parking area</strong>
                  <span>
                    Choose a location to see its live available
                    slots.
                  </span>
                </div>
              ) : (
                <>
                  <div className="pn-parking-selected-head">
                    <div>
                      <p>SELECTED LOCATION</p>
                      <h2>{selectedArea.name}</h2>
                      <span>
                        {[selectedArea.address, selectedArea.city]
                          .filter(Boolean)
                          .join(", ")}
                      </span>
                    </div>

                    <span className="pn-parking-selected-count">
                      {selectedArea.availableSlots || 0}
                      <small>FREE</small>
                    </span>
                  </div>

                  <div className="pn-parking-slot-toolbar">
                    <div>
                      <p>AVAILABLE SPACES</p>
                      <h3>Choose your slot</h3>
                    </div>

                    {vehicleTypes.length > 1 && (
                      <select
                        value={vehicleFilter}
                        onChange={(event) => {
                          setVehicleFilter(
                            event.target.value
                          );
                          setSelectedSlot(null);
                        }}
                        aria-label="Filter slots by vehicle type"
                      >
                        {vehicleTypes.map((type) => (
                          <option
                            value={type}
                            key={type}
                          >
                            {type === "ALL"
                              ? "All vehicles"
                              : vehicleLabel(type)}
                          </option>
                        ))}
                      </select>
                    )}
                  </div>

                  {slotError && (
                    <div
                      className="pn-parking-slot-error"
                      role="alert"
                    >
                      <span>{slotError}</span>

                      <button
                        type="button"
                        onClick={() =>
                          loadSlots(selectedArea.id)
                        }
                      >
                        Retry
                      </button>
                    </div>
                  )}

                  {loadingSlots ? (
                    <div className="pn-parking-slot-loading">
                      <div className="pn-parking-spinner" />
                      <span>
                        Checking available slots...
                      </span>
                    </div>
                  ) : filteredSlots.length === 0 ? (
                    <div className="pn-parking-slot-empty">
                      <div>×</div>
                      <strong>
                        No available slots
                      </strong>
                      <span>
                        Try refreshing availability or selecting
                        another parking area.
                      </span>
                    </div>
                  ) : (
                    <div className="pn-parking-slot-grid">
                      {filteredSlots.map((slot) => {
                        const active =
                          selectedSlot?.id === slot.id;

                        return (
                          <button
                            type="button"
                            key={slot.id}
                            className={`pn-parking-slot ${
                              active ? "is-selected" : ""
                            }`}
                            onClick={() =>
                              setSelectedSlot(slot)
                            }
                          >
                            <div className="pn-parking-slot-top">
                              <span>
                                {slot.zone || "ZONE"}
                              </span>
                              <i />
                            </div>

                            <strong>
                              {slot.slotNumber}
                            </strong>

                            <div className="pn-parking-slot-meta">
                              <span>
                                {vehicleLabel(
                                  slot.vehicleType
                                )}
                              </span>
                              <span>•</span>
                              <span>
                                {slot.floor || "Parking"}
                              </span>
                            </div>

                            <small>
                              {active
                                ? "Selected"
                                : "Available"}
                            </small>
                          </button>
                        );
                      })}
                    </div>
                  )}

                  <div className="pn-parking-booking-bar">
                    {selectedSlot ? (
                      <div>
                        <span>YOUR SELECTION</span>
                        <strong>
                          Slot {selectedSlot.slotNumber}
                        </strong>
                        <small>
                          {vehicleLabel(
                            selectedSlot.vehicleType
                          )}{" "}
                          · {selectedSlot.floor || "Parking"} ·
                          Zone {selectedSlot.zone || "—"}
                        </small>
                      </div>
                    ) : (
                      <div>
                        <span>YOUR SELECTION</span>
                        <strong>No slot selected</strong>
                        <small>
                          Select an available space to continue.
                        </small>
                      </div>
                    )}

                    <button
                      type="button"
                      disabled={!selectedSlot}
                      onClick={continueToBooking}
                    >
                      Continue
                      <span>→</span>
                    </button>
                  </div>
                </>
              )}
            </aside>
          </div>
        )}
      </section>
    </main>
  );
}