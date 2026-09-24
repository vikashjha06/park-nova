import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./MonthlyPassPage.css";

const money = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value || 0));

const dateTime = (value) => {
  if (!value) return "—";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
};

const todayString = () => {
  const now = new Date();
  const offset = now.getTimezoneOffset();
  const local = new Date(now.getTime() - offset * 60000);

  return local.toISOString().split("T")[0];
};

export default function MonthlyPassPage() {
  const navigate = useNavigate();

  const [passes, setPasses] = useState([]);
  const [areas, setAreas] = useState([]);
  const [slots, setSlots] = useState([]);

  const [parkingAreaId, setParkingAreaId] = useState("");
  const [vehicleType, setVehicleType] = useState("CAR");
  const [passStartDate, setPassStartDate] = useState("");
  const [dailyStartTime, setDailyStartTime] = useState("");
  const [dailyEndTime, setDailyEndTime] = useState("");
  const [parkingSlotId, setParkingSlotId] = useState("");
  const [vehicleNumber, setVehicleNumber] = useState("");

  const [quote, setQuote] = useState(null);

  const [loading, setLoading] = useState(true);
  const [slotLoading, setSlotLoading] = useState(false);
  const [quoteLoading, setQuoteLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [cancelId, setCancelId] = useState("");

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

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

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");

      const [passResponse, areaResponse] = await Promise.all([
        api.get("/api/monthly-passes/my"),
        api.get("/api/parking/areas"),
      ]);

      setPasses(
        Array.isArray(passResponse.data)
          ? passResponse.data
          : []
      );

      const areaList = Array.isArray(areaResponse.data)
        ? areaResponse.data
        : [];

      setAreas(areaList);

      setParkingAreaId((current) => {
        if (current) return current;

        const firstActive =
          areaList.find((area) => area.active !== false) ||
          areaList[0];

        return firstActive?.id || "";
      });
    } catch (err) {
      if (authFailure(err)) return;

      setError(
        err?.response?.data?.message ||
          "Unable to load monthly passes."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  /*
   * Monthly-pass slots are intentionally loaded only after
   * area + vehicle + start date + daily time window exist.
   *
   * Backend checks the entire one-month recurring reservation.
   */
  useEffect(() => {
    const loadAvailableSlots = async () => {
      setSlots([]);
      setParkingSlotId("");
      setQuote(null);

      if (
        !parkingAreaId ||
        !vehicleType ||
        !passStartDate ||
        !dailyStartTime ||
        !dailyEndTime
      ) {
        return;
      }

      if (dailyStartTime === dailyEndTime) {
        return;
      }

      try {
        setSlotLoading(true);
        setError("");

        const response = await api.post(
          "/api/monthly-passes/available-slots",
          {
            parkingAreaId,
            vehicleType,
            passStartDate,
            dailyStartTime,
            dailyEndTime,
          }
        );

        const availableSlots = Array.isArray(response.data)
          ? response.data
          : [];

        setSlots(availableSlots);

        if (availableSlots.length > 0) {
          setParkingSlotId(availableSlots[0].id);
        }
      } catch (err) {
        if (authFailure(err)) return;

        setSlots([]);
        setParkingSlotId("");

        setError(
          err?.response?.data?.message ||
            "Unable to check monthly-pass slot availability."
        );
      } finally {
        setSlotLoading(false);
      }
    };

    loadAvailableSlots();
  }, [
    parkingAreaId,
    vehicleType,
    passStartDate,
    dailyStartTime,
    dailyEndTime,
  ]);

  const areaMap = useMemo(
    () =>
      Object.fromEntries(
        areas.map((area) => [area.id, area])
      ),
    [areas]
  );

  const sortedPasses = useMemo(
    () =>
      [...passes].sort(
        (a, b) =>
          new Date(b.createdAt || 0) -
          new Date(a.createdAt || 0)
      ),
    [passes]
  );

  const cleanVehicle = vehicleNumber
    .toUpperCase()
    .replace(/\s+/g, "")
    .trim();

  const requestBody = () => ({
    parkingAreaId,
    parkingSlotId,
    vehicleType,
    vehicleNumber: cleanVehicle,
    passStartDate,
    dailyStartTime,
    dailyEndTime,
  });

  const getQuote = async () => {
    if (
      !parkingAreaId ||
      !parkingSlotId ||
      !passStartDate ||
      cleanVehicle.length < 4 ||
      !dailyStartTime ||
      !dailyEndTime
    ) {
      setQuote(null);
      return;
    }

    try {
      setQuoteLoading(true);
      setError("");
      setSuccess("");
      setQuote(null);

      const response = await api.post(
        "/api/monthly-passes/quote",
        requestBody()
      );

      setQuote(response.data);
    } catch (err) {
      if (authFailure(err)) return;

      setQuote(null);

      setError(
        err?.response?.data?.message ||
          "Unable to calculate monthly pass price."
      );
    } finally {
      setQuoteLoading(false);
    }
  };

  useEffect(() => {
    setQuote(null);
  }, [
    parkingAreaId,
    vehicleType,
    passStartDate,
    dailyStartTime,
    dailyEndTime,
    parkingSlotId,
    vehicleNumber,
  ]);

  const createPass = async (event) => {
    event.preventDefault();

    if (!parkingAreaId) {
      setError("Select a parking area.");
      return;
    }

    if (!passStartDate) {
      setError("Select the monthly pass start date.");
      return;
    }

    if (!dailyStartTime || !dailyEndTime) {
      setError("Select daily start and end time.");
      return;
    }

    if (dailyStartTime === dailyEndTime) {
      setError("Daily start and end time cannot be the same.");
      return;
    }

    if (!parkingSlotId) {
      setError(
        "No available compatible slot is selected for this date and time."
      );
      return;
    }

    if (cleanVehicle.length < 4) {
      setError("Enter a valid vehicle number.");
      return;
    }

    if (!quote) {
      setError("Calculate the monthly pass price first.");
      return;
    }

    try {
      setCreating(true);
      setError("");
      setSuccess("");

      const response = await api.post(
        "/api/monthly-passes",
        requestBody()
      );

      const pass =
        response.data?.monthlyPass || response.data;

      if (!pass?.id) {
        throw new Error(
          "Monthly pass ID was not returned."
        );
      }

      if (
        pass.status === "PENDING" &&
        Number(pass.pricePaid) > 0
      ) {
        navigate("/payment", {
          state: {
            paymentDraft: {
              paymentType: "MONTHLY_PASS",
              referenceId: pass.id,
              amount: pass.pricePaid,
              monthlyPass: pass,
            },
          },
        });

        return;
      }

      setSuccess("Monthly pass created successfully.");
      await loadData();
    } catch (err) {
      if (authFailure(err)) return;

      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Unable to create monthly pass."
      );
    } finally {
      setCreating(false);
    }
  };

  const cancelPass = async (pass) => {
    try {
      setCancelId(pass.id);
      setError("");
      setSuccess("");

      await api.patch(
        `/api/monthly-passes/${pass.id}/cancel`
      );

      setSuccess("Monthly pass cancelled successfully.");

      navigate("/payments", {
        replace: true,
        state: {
          refundTarget: {
            paymentType: "MONTHLY_PASS",
            referenceId: pass.id,
          },
        },
      });

      return;
    } catch (err) {
      if (authFailure(err)) return;

      setError(
        err?.response?.data?.message ||
          "Unable to cancel this monthly pass."
      );
    } finally {
      setCancelId("");
    }
  };

  const payPass = (pass) => {
    navigate("/payment", {
      state: {
        paymentDraft: {
          paymentType: "MONTHLY_PASS",
          referenceId: pass.id,
          amount: pass.pricePaid,
          monthlyPass: pass,
        },
      },
    });
  };

  const availabilityReady =
    parkingAreaId &&
    vehicleType &&
    passStartDate &&
    dailyStartTime &&
    dailyEndTime &&
    dailyStartTime !== dailyEndTime;

  return (
    <main className="pn-pass-page">
      <header className="pn-pass-header">
        <Link to="/" className="pn-pass-brand">
          <img
            src="/images/park-nova-logo.png"
            alt="Park Nova"
          />
        </Link>

        <nav>
          <Link to="/parking">Find Parking</Link>
          <Link to="/bookings">Bookings</Link>
          <Link to="/wallet">Wallet</Link>
          <Link to="/profile">Profile</Link>
        </nav>
      </header>

      <section className="pn-pass-shell">
        <div className="pn-pass-title">
          <div>
            <p>MONTHLY PARKING</p>

            <h1>
              Monthly <em>Pass</em>
            </h1>

            <span>
              Choose your start date and recurring daily parking
              window. Park Nova shows only slots available for
              that one-month reservation.
            </span>
          </div>

          <button
            type="button"
            onClick={loadData}
            disabled={loading}
          >
            Refresh
          </button>
        </div>

        {error && (
          <div className="pn-pass-alert error">
            {error}
          </div>
        )}

        {success && (
          <div className="pn-pass-alert success">
            ✓ {success}
          </div>
        )}

        <section className="pn-pass-create">
          <div className="pn-pass-create-copy">
            <p>NEW PASS</p>

            <h2>Build your monthly parking plan</h2>

            <span>
              Select the location, vehicle, start date and daily
              parking window first. Only compatible slots with no
              conflicting reservation during your pass period are
              shown.
            </span>

            <div className="pn-pass-feature-grid">
              <div>
                <strong>30</strong>
                <span>Days pricing</span>
              </div>

              <div>
                <strong>1</strong>
                <span>Month reserved window</span>
              </div>
            </div>
          </div>

          <form
            className="pn-pass-form"
            onSubmit={createPass}
          >
            <label>
              <span>Parking area</span>

              <select
                value={parkingAreaId}
                onChange={(e) =>
                  setParkingAreaId(e.target.value)
                }
                required
              >
                <option value="">
                  Select parking area
                </option>

                {areas
                  .filter(
                    (area) => area.active !== false
                  )
                  .map((area) => (
                    <option
                      key={area.id}
                      value={area.id}
                    >
                      {area.name}
                      {area.city
                        ? ` — ${area.city}`
                        : ""}
                    </option>
                  ))}
              </select>
            </label>

            <label>
              <span>Vehicle type</span>

              <select
                value={vehicleType}
                onChange={(e) =>
                  setVehicleType(e.target.value)
                }
              >
                <option value="CAR">Car</option>
                <option value="BIKE">Bike</option>
                <option value="EV">EV</option>
              </select>
            </label>

            <label>
              <span>Pass start date</span>

              <input
                type="date"
                min={todayString()}
                value={passStartDate}
                onChange={(e) =>
                  setPassStartDate(e.target.value)
                }
                required
              />
            </label>

            <label>
              <span>Daily start time</span>

              <input
                type="time"
                value={dailyStartTime}
                onChange={(e) =>
                  setDailyStartTime(e.target.value)
                }
                required
              />
            </label>

            <label>
              <span>Daily end time</span>

              <input
                type="time"
                value={dailyEndTime}
                onChange={(e) =>
                  setDailyEndTime(e.target.value)
                }
                required
              />
            </label>

            <label>
              <span>Available compatible slot</span>

              <select
                value={parkingSlotId}
                onChange={(e) =>
                  setParkingSlotId(e.target.value)
                }
                disabled={
                  !availabilityReady ||
                  slotLoading ||
                  slots.length === 0
                }
                required
              >
                <option value="">
                  {!availabilityReady
                    ? "Select date and time first"
                    : slotLoading
                      ? "Checking availability..."
                      : slots.length === 0
                        ? "No slots available for this period"
                        : "Select available slot"}
                </option>

                {slots.map((slot) => (
                  <option
                    key={slot.id}
                    value={slot.id}
                  >
                    {slot.slotNumber}
                    {slot.floor
                      ? ` • ${slot.floor}`
                      : ""}
                    {slot.zone
                      ? ` • ${slot.zone}`
                      : ""}
                  </option>
                ))}
              </select>
            </label>

            <label className="pn-pass-vehicle">
              <span>Vehicle number</span>

              <input
                value={vehicleNumber}
                onChange={(e) =>
                  setVehicleNumber(
                    e.target.value.toUpperCase()
                  )
                }
                placeholder="PB10AB1234"
                maxLength={15}
                required
              />
            </label>

            <button
              type="button"
              onClick={getQuote}
              disabled={
                quoteLoading ||
                !parkingAreaId ||
                !parkingSlotId ||
                !passStartDate ||
                cleanVehicle.length < 4 ||
                !dailyStartTime ||
                !dailyEndTime
              }
            >
              {quoteLoading
                ? "Calculating..."
                : "Calculate Monthly Price"}
            </button>

            {quote && (
              <div className="pn-pass-quote">
                <div>
                  <span>Daily parking</span>
                  <strong>
                    {quote.dailyHours} hrs
                  </strong>
                </div>

                <div>
                  <span>Hourly rate</span>
                  <strong>
                    {money(quote.hourlyRate)}
                  </strong>
                </div>

                <div>
                  <span>30-day normal price</span>
                  <strong>
                    {money(
                      quote.normalMonthlyPrice
                    )}
                  </strong>
                </div>

                <div>
                  <span>Monthly discount</span>
                  <strong>
                    {quote.discountPercent}%
                  </strong>
                </div>

                <div>
                  <span>You save</span>
                  <strong>
                    {money(quote.discountAmount)}
                  </strong>
                </div>

                <div>
                  <span>Final pass price</span>
                  <strong>
                    {money(quote.finalPrice)}
                  </strong>
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={creating || !quote}
            >
              {creating
                ? "Creating pass..."
                : `Continue to Payment${
                    quote
                      ? ` • ${money(
                          quote.finalPrice
                        )}`
                      : ""
                  }`}

              {!creating && <span>→</span>}
            </button>
          </form>
        </section>

        <section className="pn-pass-list">
          <div className="pn-pass-list-head">
            <div>
              <p>YOUR PASSES</p>
              <h2>Monthly pass history</h2>
            </div>

            <span>
              {passes.length}{" "}
              {passes.length === 1
                ? "pass"
                : "passes"}
            </span>
          </div>

          {loading ? (
            <div className="pn-pass-state">
              <div className="pn-pass-spinner" />
              <strong>Loading passes...</strong>
            </div>
          ) : sortedPasses.length === 0 ? (
            <div className="pn-pass-state">
              <div className="pn-pass-state-icon">
                P
              </div>

              <strong>No monthly passes yet</strong>

              <span>
                Create your first monthly parking pass.
              </span>
            </div>
          ) : (
            <div className="pn-pass-grid">
              {sortedPasses.map((pass) => {
                const area =
                  areaMap[pass.parkingAreaId];

                return (
                  <article
                    className="pn-pass-card"
                    key={pass.id}
                  >
                    <div className="pn-pass-card-top">
                      <div>
                        <span>MONTHLY PASS</span>

                        <strong>
                          #
                          {String(pass.id)
                            .slice(-8)
                            .toUpperCase()}
                        </strong>
                      </div>

                      <i
                        className={`status-${String(
                          pass.status
                        ).toLowerCase()}`}
                      >
                        {pass.status}
                      </i>
                    </div>

                    <div className="pn-pass-card-location">
                      <small>
                        PARKING LOCATION
                      </small>

                      <strong>
                        {area?.name ||
                          `Area ${String(
                            pass.parkingAreaId
                          ).slice(-6)}`}
                      </strong>

                      <span>
                        {area?.city ||
                          "Park Nova parking"}
                      </span>
                    </div>

                    <div className="pn-pass-card-details">
                      <div>
                        <span>Vehicle</span>
                        <strong>
                          {pass.vehicleNumber}
                        </strong>
                        <small>
                          {pass.vehicleType}
                        </small>
                      </div>

                      <div>
                        <span>Daily window</span>

                        <strong>
                          {pass.dailyStartTime || "—"} –{" "}
                          {pass.dailyEndTime || "—"}
                        </strong>

                        <small>
                          {pass.dailyHours
                            ? `${pass.dailyHours} hrs/day`
                            : ""}
                        </small>
                      </div>

                      <div>
                        <span>Price</span>

                        <strong>
                          {money(pass.pricePaid)}
                        </strong>

                        <small>
                          {Number(
                            pass.discountPercent || 0
                          )}
                          % off
                        </small>
                      </div>

                      <div>
                        <span>Starts</span>
                        <strong>
                          {dateTime(pass.startDate)}
                        </strong>
                      </div>

                      <div>
                        <span>Expires</span>
                        <strong>
                          {dateTime(pass.expiryDate)}
                        </strong>
                      </div>
                    </div>

                    <div className="pn-pass-actions">
                      {pass.status === "PENDING" &&
                        Number(pass.pricePaid) >
                          0 && (
                          <button
                            className="primary"
                            type="button"
                            onClick={() =>
                              payPass(pass)
                            }
                          >
                            Pay now
                          </button>
                        )}

                      {["PENDING", "SCHEDULED"].includes(
                        pass.status
                      ) && (
                        <button
                          type="button"
                          disabled={
                            cancelId === pass.id
                          }
                          onClick={() =>
                            cancelPass(pass)
                          }
                        >
                          {cancelId === pass.id
                            ? "Cancelling..."
                            : "Cancel Pass"}
                        </button>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>
      </section>
    </main>
  );
}