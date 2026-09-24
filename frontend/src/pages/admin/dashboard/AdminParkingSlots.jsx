import React, { useEffect, useState } from "react";
import api from "../../../services/api";
import "./AdminParkingAreas.css";

const base = "/api/admin/parking-slots";
const errorMessage = e =>
  e?.response?.data?.message || e?.response?.data?.error ||
  e?.message || "Request failed.";

export default function AdminParkingSlots({ refreshSignal }) {
  const [areas, setAreas] = useState([]);
  const [areaId, setAreaId] = useState("");
  const [slots, setSlots] = useState([]);
  const [form, setForm] = useState({
    slotNumber: "", vehicleType: "CAR",
    floor: "", zone: "", sensorId: ""
  });
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    api.get("/api/admin/parking-areas")
      .then(response => {
        const list = Array.isArray(response.data) ? response.data : [];
        setAreas(list);
        setAreaId(current => current || list[0]?.id || "");
      })
      .catch(e => setError(errorMessage(e)));
  }, []);

  async function load(id) {
    if (!id) { setSlots([]); return; }
    setLoading(true);
    try {
      const response = await api.get(
        `${base}/area/${encodeURIComponent(id)}`
      );
      setSlots(Array.isArray(response.data) ? response.data : []);
      setError("");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(areaId); }, [areaId, refreshSignal]);

  async function create(event) {
    event.preventDefault();
    if (!areaId) return;
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api.post(base, {
        parkingAreaId: areaId,
        slotNumber: form.slotNumber.trim(),
        vehicleType: form.vehicleType,
        floor: form.floor.trim(),
        zone: form.zone.trim(),
        sensorId: form.sensorId.trim()
      });
      setForm({
        slotNumber: "", vehicleType: "CAR",
        floor: "", zone: "", sensorId: ""
      });
      await load(areaId);
      setNotice("Parking slot created.");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function change(slot, kind, value) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api.patch(
        `${base}/${encodeURIComponent(slot.id)}/${kind}`,
        null,
        { params: { [kind === "active" ? "active" : "status"]: value } }
      );
      await load(areaId);
      setNotice("Slot updated.");
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="pn-areas">
      <div className="pn-areas-heading">
        <div>
          <small>PARKING NETWORK</small>
          <h2>Parking Slots</h2>
          <p>Manage slots inside each parking area.</p>
        </div>
      </div>

      {error && <p className="pn-areas-error" role="alert">{error}</p>}
      {notice && <p className="pn-areas-success" role="status">{notice}</p>}

      <div className="pn-areas-form">
        <label>
          <span>Parking area</span>
          <select
            value={areaId}
            onChange={e => setAreaId(e.target.value)}
            aria-label="Parking area"
          >
            {areas.length === 0 && <option value="">No areas available</option>}
            {areas.map(area => (
              <option key={area.id} value={area.id}>
                {area.name} — {area.city}
              </option>
            ))}
          </select>
        </label>

        <form onSubmit={create}>
          <h3>Add slot</h3>
          <div className="pn-areas-fields">
            <label>
              <span>Slot number</span>
              <input
                required
                value={form.slotNumber}
                onChange={e => setForm({ ...form, slotNumber: e.target.value })}
                placeholder="A-01"
                disabled={busy}
              />
            </label>
            <label>
              <span>Vehicle type</span>
              <select
                value={form.vehicleType}
                onChange={e => setForm({ ...form, vehicleType: e.target.value })}
                disabled={busy}
              >
                <option value="CAR">Car</option>
                <option value="BIKE">Bike</option>
                <option value="EV">EV</option>
              </select>
            </label>
            {[
              ["floor", "Floor"],
              ["zone", "Zone"],
              ["sensorId", "Sensor ID"]
            ].map(([key, label]) => (
              <label key={key}>
                <span>{label}</span>
                <input
                  value={form[key]}
                  onChange={e => setForm({ ...form, [key]: e.target.value })}
                  disabled={busy}
                />
              </label>
            ))}
          </div>
          <button className="pn-areas-primary" disabled={busy || !areaId}>
            {busy ? "Saving..." : "Add slot"}
          </button>
        </form>
      </div>

      {loading ? <p>Loading slots...</p> :
        slots.length === 0 ? <p>No slots in this area.</p> : (
          <div className="pn-areas-grid">
            {slots.map(slot => (
              <article className="pn-areas-card" key={slot.id}>
                <div className="pn-areas-card-top">
                  <h3>{slot.slotNumber}</h3>
                  <strong>{slot.active ? "Active" : "Disabled"}</strong>
                </div>
                <p>Vehicle: {slot.vehicleType}</p>
                <p>Floor: {slot.floor || "—"} · Zone: {slot.zone || "—"}</p>
                <p>Status: {slot.status}</p>
                <div className="pn-areas-actions">
                  <select
                    aria-label={`Status for ${slot.slotNumber}`}
                    value={slot.status || "AVAILABLE"}
                    disabled={busy}
                    onChange={e => change(slot, "status", e.target.value)}
                  >
                    <option value="AVAILABLE">Available</option>
                    <option value="OCCUPIED">Occupied</option>
                    <option value="BOOKED">Booked</option>
                    <option value="MAINTENANCE">Maintenance</option>
                  </select>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => change(slot, "active", !slot.active)}
                  >
                    {slot.active ? "Disable" : "Enable"}
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
    </div>
  );
}