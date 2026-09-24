import React, { useEffect, useState } from "react";
import api from "../../../services/api";
import "./AdminParkingAreas.css";

const url = "/api/admin/parking-areas";
const blank = {
  name: "", address: "", city: "", totalSlots: "1",
  latitude: "", longitude: "", googleMapUrl: "", googlePlaceId: ""
};
const message = (e) =>
  e?.response?.data?.message || e?.response?.data?.error ||
  e?.message || "Request failed.";

export default function AdminParkingAreas({ refreshSignal }) {
  const [areas, setAreas] = useState([]);
  const [form, setForm] = useState(blank);
  const [editingId, setEditingId] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function load() {
    setLoading(true);
    try {
      const result = await api.get(url);
      if (!Array.isArray(result.data)) throw new Error("Invalid area list.");
      setAreas(result.data);
      setError("");
    } catch (e) {
      setError(message(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [refreshSignal]);

  function open(area = null) {
    setError("");
    setSuccess("");
    setEditingId(area?.id || null);
    setForm(area
      ? Object.fromEntries(Object.keys(blank).map(
          key => [key, String(area[key] ?? blank[key])]
        ))
      : { ...blank });
    setFormOpen(true);
  }

  function close() {
    setFormOpen(false);
    setEditingId(null);
    setForm({ ...blank });
  }

  async function save(event) {
    event.preventDefault();
    const latitude = Number(form.latitude);
    const longitude = Number(form.longitude);
    const totalSlots = Number(form.totalSlots);

    if (!Number.isFinite(latitude) || latitude < -90 || latitude > 90 ||
        !Number.isFinite(longitude) || longitude < -180 || longitude > 180 ||
        !Number.isInteger(totalSlots) || totalSlots < 1) {
      setError("Enter valid coordinates and at least one total slot.");
      return;
    }

    let mapsUrl;
    try {
      mapsUrl = new URL(form.googleMapUrl.trim());
      if (mapsUrl.protocol !== "https:" ||
          !/(^|\.)google\.[a-z.]+$|(^|\.)goo\.gl$/.test(mapsUrl.hostname)) {
        throw new Error();
      }
    } catch {
      setError("Enter a valid HTTPS Google Maps URL.");
      return;
    }

    const payload = {
      name: form.name.trim(),
      address: form.address.trim(),
      city: form.city.trim(),
      totalSlots,
      latitude,
      longitude,
      googleMapUrl: form.googleMapUrl.trim(),
      googlePlaceId: form.googlePlaceId.trim()
    };

    setBusy(true);
    setError("");
    try {
      if (editingId) {
        await api.put(`${url}/${encodeURIComponent(editingId)}`, payload);
      } else {
        await api.post(url, payload);
      }
      const text = editingId ? "Area updated." : "Area created.";
      close();
      await load();
      setSuccess(text);
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }

  async function changeStatus(area) {
    if (!window.confirm(`${area.active ? "Disable" : "Enable"} ${area.name}?`)) return;
    setBusy(true);
    setError("");
    try {
      await api.patch(
        `${url}/${encodeURIComponent(area.id)}/status`,
        null,
        { params: { active: !area.active } }
      );
      await load();
      setSuccess(area.active ? "Area disabled." : "Area enabled.");
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }

  const filtered = areas.filter(area =>
    [area.name, area.address, area.city].some(value =>
      String(value || "").toLowerCase().includes(search.toLowerCase().trim())
    )
  );

  return (
    <div className="pn-areas">
      <div className="pn-areas-heading">
        <div>
          <small>PARKING NETWORK</small>
          <h2>Parking Areas</h2>
          <p>Manage locations and exact coordinates for customer directions.</p>
        </div>
        <button className="pn-areas-primary" onClick={() => open()}>
          + Add parking area
        </button>
      </div>

      {error && <p className="pn-areas-error" role="alert">{error}</p>}
      {success && <p className="pn-areas-success" role="status">{success}</p>}

      {formOpen && (
        <form className="pn-areas-form" onSubmit={save}>
          <h3>{editingId ? "Edit parking area" : "Create parking area"}</h3>
          <div className="pn-areas-fields">
            {[
              ["name", "Area name"], ["address", "Address"],
              ["city", "City"], ["totalSlots", "Total slots"],
              ["latitude", "Latitude"], ["longitude", "Longitude"],
              ["googleMapUrl", "Google Maps URL"],
              ["googlePlaceId", "Google Place ID (optional)"]
            ].map(([key, label]) => (
              <label key={key}>
                <span>{label}</span>
                <input
                  value={form[key]}
                  onChange={e => setForm({ ...form, [key]: e.target.value })}
                  type={["totalSlots", "latitude", "longitude"].includes(key)
                    ? "number" : key === "googleMapUrl" ? "url" : "text"}
                  step={key === "totalSlots" ? "1"
                    : ["latitude", "longitude"].includes(key) ? "any" : undefined}
                  min={key === "totalSlots" ? "1" : undefined}
                  required={key !== "googlePlaceId"}
                  disabled={busy}
                />
              </label>
            ))}
          </div>
          {form.latitude && form.longitude && (
            <a
              href={`https://www.google.com/maps?q=${encodeURIComponent(
                `${form.latitude},${form.longitude}`
              )}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              Preview location on Google Maps ↗
            </a>
          )}
          <div className="pn-areas-actions">
            <button type="button" onClick={close} disabled={busy}>Cancel</button>
            <button className="pn-areas-primary" disabled={busy}>
              {busy ? "Saving..." : editingId ? "Save changes" : "Create area"}
            </button>
          </div>
        </form>
      )}

      <div className="pn-areas-toolbar">
        <input
          aria-label="Search parking areas"
          placeholder="Search name, city or address"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <span>{filtered.length} areas</span>
      </div>

      {loading ? <p>Loading parking areas...</p> :
        filtered.length === 0 ? <p>No parking areas found.</p> : (
          <div className="pn-areas-grid">
            {filtered.map(area => (
              <article className="pn-areas-card" key={area.id}>
                <div className="pn-areas-card-top">
                  <div>
                    <small>{area.city}</small>
                    <h3>{area.name}</h3>
                  </div>
                  <strong>{area.active ? "Active" : "Disabled"}</strong>
                </div>
                <p>{area.address}</p>
                <p>Capacity: {area.totalSlots} slots</p>
                <p>Location: {area.latitude}, {area.longitude}</p>
                <div className="pn-areas-actions">
                  <button onClick={() => open(area)} disabled={busy}>Edit</button>
                  <button onClick={() => changeStatus(area)} disabled={busy}>
                    {area.active ? "Disable" : "Enable"}
                  </button>
                  <a
                    href={`https://www.google.com/maps?q=${encodeURIComponent(
                      `${area.latitude},${area.longitude}`
                    )}`}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    View map ↗
                  </a>
                </div>
              </article>
            ))}
          </div>
        )}
    </div>
  );
}