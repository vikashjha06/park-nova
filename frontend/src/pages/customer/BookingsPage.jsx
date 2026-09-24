import React, { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./BookingsPage.css";

const money = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value || 0));

const dateTime = (value) => {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return date.toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
};

const localInput = (date) => {
  const pad = (n) => String(n).padStart(2, "0");

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate()
  )}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

export default function BookingsPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const draft = location.state?.bookingDraft || null;

  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [actionId, setActionId] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [createdBooking, setCreatedBooking] = useState(null);

  const now = useMemo(() => new Date(), []);

  const [vehicleNumber, setVehicleNumber] = useState("");
  const [startTime, setStartTime] = useState(
    localInput(new Date(now.getTime() + 15 * 60 * 1000))
  );
  const [endTime, setEndTime] = useState(
    localInput(new Date(now.getTime() + 75 * 60 * 1000))
  );

  const authFailure = (err) => {
    const status = err?.response?.status;

    if (status === 401 || status === 403) {
      localStorage.removeItem("parkNovaToken");
      localStorage.removeItem("parkNovaUser");
      navigate("/login", { replace: true });
      return true;
    }

    return false;
  };

  const loadBookings = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get("/api/bookings/my");
      setBookings(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      if (authFailure(err)) return;

      setError(
        err?.response?.data?.message ||
          "Unable to load your bookings."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBookings();
  }, []);

  const createBooking = async (event) => {
    event.preventDefault();

    if (!draft) return;

    const cleanVehicle = vehicleNumber
      .toUpperCase()
      .replace(/\s+/g, "")
      .trim();

    if (cleanVehicle.length < 4) {
      setError("Enter a valid vehicle number.");
      return;
    }

    const start = new Date(startTime);
    const end = new Date(endTime);

    if (
      Number.isNaN(start.getTime()) ||
      Number.isNaN(end.getTime()) ||
      end <= start
    ) {
      setError("End time must be later than start time.");
      return;
    }

    if (start < new Date(Date.now() - 60000)) {
      setError("Start time cannot be in the past.");
      return;
    }

    const bookingDurationMinutes =
      (end.getTime() - start.getTime()) / (1000 * 60);

    if (bookingDurationMinutes < 30) {
      setError("Minimum booking duration is 30 minutes.");
      return;
    }

    try {
      setCreating(true);
      setError("");
      setSuccess("");

      const response = await api.post("/api/bookings", {
        parkingAreaId: draft.parkingAreaId,
        parkingSlotId: draft.parkingSlotId,
        vehicleNumber: cleanVehicle,
        vehicleType: draft.vehicleType,
        startTime,
        endTime,
      });

      const booking = response.data?.booking || response.data;

      // Paid booking: go directly to payment.
      // Do not make the customer return to booking history
      // and click a second "Pay now" button.
      if (
        booking?.status === "PENDING" &&
        Number(booking?.totalAmount) > 0
      ) {
        navigate("/payment", {
          replace: true,
          state: {
            paymentDraft: {
              paymentType: "BOOKING",
              referenceId: booking.id,
              amount: booking.totalAmount,
              booking,
            },
          },
        });
        return;
      }

      // Zero-cost / pass-covered booking does not need payment.
      setCreatedBooking(booking);
      setSuccess(
        response.data?.message || "Booking created successfully."
      );

      await loadBookings();
    } catch (err) {
      if (authFailure(err)) return;

      setError(
        err?.response?.data?.message ||
          "Unable to create booking. Please check the selected slot and time."
      );
    } finally {
      setCreating(false);
    }
  };

  const runBookingAction = async (booking, action) => {
    try {
      setActionId(`${booking.id}:${action}`);
      setError("");
      setSuccess("");

      const response = await api.patch(
        `/api/bookings/${booking.id}/${action}`
      );

      setSuccess(
        response.data?.message ||
          `Booking ${action} action completed.`
      );

      if (action === "cancel") {
        navigate("/payments", {
          replace: true,
          state: {
            refundTarget: {
              paymentType: "BOOKING",
              referenceId: booking.id,
            },
          },
        });
        return;
      }

      await loadBookings();
    } catch (err) {
      if (authFailure(err)) return;

      setError(
        err?.response?.data?.message ||
          `Unable to ${action} this booking.`
      );
    } finally {
      setActionId("");
    }
  };

  const goToPayment = (booking) => {
    navigate("/payment", {
      state: {
        paymentDraft: {
          paymentType: "BOOKING",
          referenceId: booking.id,
          amount: booking.totalAmount,
          booking,
        },
      },
    });
  };

  const sortedBookings = useMemo(
    () =>
      [...bookings].sort(
        (a, b) =>
          new Date(b.createdAt || 0) -
          new Date(a.createdAt || 0)
      ),
    [bookings]
  );

  return (
    <main className="pn-bookings-page">
      <header className="pn-bookings-header">
        <Link to="/" className="pn-bookings-brand">
          <img src="/images/park-nova-logo.png" alt="Park Nova" />
        </Link>

        <nav>
          <Link to="/parking">Find Parking</Link>
          <Link to="/wallet">Wallet</Link>
          <Link to="/monthly-pass">Monthly Pass</Link>
          <Link to="/profile">Profile</Link>
        </nav>
      </header>

      <section className="pn-bookings-shell">
        <div className="pn-bookings-title">
          <div>
            <p>PARKING JOURNEY</p>
            <h1>
              My <em>Bookings</em>
            </h1>
            <span>
              Reserve your selected parking slot and manage your
              parking journey.
            </span>
          </div>

          <Link to="/parking">+ Find Parking</Link>
        </div>

        {error && (
          <div className="pn-bookings-alert error" role="alert">
            <span>{error}</span>
            <button type="button" onClick={() => setError("")}>
              ×
            </button>
          </div>
        )}

        {success && (
          <div className="pn-bookings-alert success" role="status">
            <span>✓ {success}</span>
            <button type="button" onClick={() => setSuccess("")}>
              ×
            </button>
          </div>
        )}

        {draft && !createdBooking && (
          <section className="pn-booking-create">
            <div className="pn-booking-create-copy">
              <p>NEW RESERVATION</p>
              <h2>Complete your booking</h2>
              <span>
                Confirm your vehicle and parking time. The final
                parking amount is calculated by Park Nova's backend.
              </span>

              <div className="pn-booking-selected">
                <div>
                  <small>PARKING LOCATION</small>
                  <strong>{draft.parkingAreaName}</strong>
                  <span>
                    {[draft.parkingAreaAddress, draft.parkingAreaCity]
                      .filter(Boolean)
                      .join(", ")}
                  </span>
                </div>

                <div>
                  <small>SELECTED SLOT</small>
                  <strong>{draft.slotNumber}</strong>
                  <span>
                    {draft.vehicleType} · {draft.floor || "Parking"} ·
                    Zone {draft.zone || "—"}
                  </span>
                </div>
              </div>
            </div>

            <form
              className="pn-booking-form"
              onSubmit={createBooking}
            >
              <label>
                <span>Vehicle number</span>
                <input
                  value={vehicleNumber}
                  onChange={(event) =>
                    setVehicleNumber(event.target.value.toUpperCase())
                  }
                  placeholder="PB10AB1234"
                  maxLength={15}
                  autoComplete="off"
                  required
                />
              </label>

              <label>
                <span>Vehicle type</span>
                <input
                  value={draft.vehicleType || ""}
                  readOnly
                />
              </label>

              <label>
                <span>Start time</span>
                <input
                  type="datetime-local"
                  value={startTime}
                  onChange={(event) =>
                    setStartTime(event.target.value)
                  }
                  required
                />
              </label>

              <label>
                <span>End time · minimum 30 minutes</span>
                <input
                  type="datetime-local"
                  value={endTime}
                  min={
                    startTime
                      ? localInput(
                          new Date(
                            new Date(startTime).getTime() +
                              30 * 60 * 1000
                          )
                        )
                      : undefined
                  }
                  onChange={(event) =>
                    setEndTime(event.target.value)
                  }
                  required
                />
              </label>

              <button type="submit" disabled={creating}>
                {creating ? "Creating booking..." : "Reserve Slot"}
                {!creating && <span>→</span>}
              </button>
            </form>
          </section>
        )}

        {createdBooking && (
          <section className="pn-booking-created">
            <div className="pn-booking-created-icon">✓</div>

            <div>
              <p>BOOKING CREATED</p>
              <h2>
                Slot reserved successfully
              </h2>
              <span>
                Booking status:{" "}
                <strong>{createdBooking.status}</strong>
              </span>
            </div>

            {createdBooking.status === "PENDING" &&
            Number(createdBooking.totalAmount) > 0 ? (
              <button
                type="button"
                onClick={() => goToPayment(createdBooking)}
              >
                Pay {money(createdBooking.totalAmount)}
                <span>→</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={() => {
                  setCreatedBooking(null);
                  navigate("/bookings", { replace: true });
                }}
              >
                View booking
              </button>
            )}
          </section>
        )}

        <section className="pn-bookings-list-section">
          <div className="pn-bookings-section-head">
            <div>
              <p>YOUR ACTIVITY</p>
              <h2>Booking history</h2>
            </div>

            <button
              type="button"
              onClick={loadBookings}
              disabled={loading}
            >
              ↻ Refresh
            </button>
          </div>

          {loading ? (
            <div className="pn-bookings-state">
              <div className="pn-bookings-spinner" />
              <strong>Loading bookings...</strong>
            </div>
          ) : sortedBookings.length === 0 ? (
            <div className="pn-bookings-state">
              <div className="pn-bookings-state-icon">P</div>
              <strong>No bookings yet</strong>
              <span>
                Find an available parking slot to create your first
                booking.
              </span>
              <Link to="/parking">Find Parking</Link>
            </div>
          ) : (
            <div className="pn-bookings-grid">
              {sortedBookings.map((booking) => (
                <article
                  className="pn-booking-card"
                  key={booking.id}
                >
                  <div className="pn-booking-card-top">
                    <div>
                      <span>BOOKING</span>
                      <strong>
                        #{String(booking.id).slice(-8).toUpperCase()}
                      </strong>
                    </div>

                    <i
                      className={`status-${String(
                        booking.status
                      ).toLowerCase()}`}
                    >
                      {booking.status}
                    </i>
                  </div>

                  <div className="pn-booking-card-body">
                    <div>
                      <span>Vehicle</span>
                      <strong>{booking.vehicleNumber}</strong>
                      <small>{booking.vehicleType}</small>
                    </div>

                    <div>
                      <span>Amount</span>
                      <strong>{money(booking.totalAmount)}</strong>
                      <small>Backend calculated</small>
                    </div>

                    <div>
                      <span>Starts</span>
                      <strong>{dateTime(booking.startTime)}</strong>
                    </div>

                    <div>
                      <span>Ends</span>
                      <strong>{dateTime(booking.endTime)}</strong>
                    </div>
                  </div>

                  {booking.paymentExpiresAt &&
                    booking.status === "PENDING" && (
                      <div className="pn-booking-hold">
                        Payment hold expires{" "}
                        {dateTime(booking.paymentExpiresAt)}
                      </div>
                    )}

                  <div className="pn-booking-card-actions">
                    {booking.status === "PENDING" &&
                      Number(booking.totalAmount) > 0 && (
                        <button
                          className="primary"
                          type="button"
                          onClick={() => goToPayment(booking)}
                        >
                          Pay now
                        </button>
                      )}


                    {["PENDING", "CONFIRMED"].includes(
                      booking.status
                    ) && (
                      <button
                        type="button"
                        disabled={
                          actionId === `${booking.id}:cancel`
                        }
                        onClick={() =>
                          runBookingAction(booking, "cancel")
                        }
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </section>
    </main>
  );
}