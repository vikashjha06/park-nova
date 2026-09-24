import React, { useEffect, useState } from "react";
import api from "../../../services/api";
import "./AdminParkingAreas.css";

const endpoint = "/api/admin/pricing";
const empty = {
  vehicleType: "CAR",
  hourlyRate: "0",
  peakHourlyRate: "0",
  monthlyPassPrice: "0",
  monthlyPassDiscountPercent: "0"
};
const errorText = e =>
  e?.response?.data?.message || e?.response?.data?.error ||
  e?.message || "Request failed.";

export default function AdminPricing({ refreshSignal }) {
  const [areas, setAreas] = useState([]);
  const [areaId, setAreaId] = useState("");
  const [plans, setPlans] = useState([]);
  const [form, setForm] = useState({ ...empty });
  const [editingId, setEditingId] = useState(null);
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
      .catch(e => setError(errorText(e)));
  }, []);

  async function load(id) {
    if (!id) { setPlans([]); return; }
    setLoading(true);
    try {
      const response = await api.get(
        `${endpoint}/area/${encodeURIComponent(id)}`
      );
      if (!Array.isArray(response.data)) throw new Error("Invalid pricing response.");
      setPlans(response.data);
      setError("");
    } catch (e) {
      setError(errorText(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(areaId); }, [areaId, refreshSignal]);

  function edit(plan) {
    setEditingId(plan.id);
    setForm({
      vehicleType: plan.vehicleType || "CAR",
      hourlyRate: String(plan.hourlyRate ?? 0),
      peakHourlyRate: String(plan.peakHourlyRate ?? 0),
      monthlyPassPrice: String(plan.monthlyPassPrice ?? 0),
      monthlyPassDiscountPercent:
        String(plan.monthlyPassDiscountPercent ?? 0)
    });
    setError("");
    setNotice("");
  }

  function reset() {
    setEditingId(null);
    setForm({ ...empty });
  }

  async function save(event) {
    event.preventDefault();
    if (!areaId) return;

    const numeric = [
      "hourlyRate",
      "peakHourlyRate",
      "monthlyPassPrice",
      "monthlyPassDiscountPercent"
    ];
    if (numeric.some(key =>
      form[key] === "" ||
      !Number.isFinite(Number(form[key])) ||
      Number(form[key]) < 0
    ) || Number(form.monthlyPassDiscountPercent) > 100) {
      setError("Rates must be non-negative; discount must be between 0 and 100.");
      return;
    }

    const payload = {
      parkingAreaId: areaId,
      vehicleType: form.vehicleType,
      hourlyRate: Number(form.hourlyRate),
      peakHourlyRate: Number(form.peakHourlyRate),
      monthlyPassPrice: Number(form.monthlyPassPrice),
      monthlyPassDiscountPercent:
        Number(form.monthlyPassDiscountPercent)
    };

    setBusy(true);
    setError("");
    setNotice("");
    try {
      if (editingId) {
        await api.put(
          `${endpoint}/${encodeURIComponent(editingId)}`, payload
        );
      } else {
        await api.post(endpoint, payload);
      }
      const text = editingId ? "Pricing updated." : "Pricing created.";
      reset();
      await load(areaId);
      setNotice(text);
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }

  async function toggle(plan) {
    if (!window.confirm(
      `${plan.active ? "Disable" : "Enable"} ${plan.vehicleType} pricing?`
    )) return;

    setBusy(true);
    setError("");
    setNotice("");
    try {
      await api.patch(
        `${endpoint}/${encodeURIComponent(plan.id)}/active`,
        null,
        { params: { active: !plan.active } }
      );
      await load(areaId);
      setNotice(plan.active ? "Pricing disabled." : "Pricing enabled.");
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="pn-areas">
      <div className="pn-areas-heading">
        <div>
          <small>PARKING NETWORK</small>
          <h2>Pricing & Monthly Pass Discount</h2>
          <p>Set rates separately for each area and vehicle type.</p>
        </div>
      </div>

      {error && <p className="pn-areas-error" role="alert">{error}</p>}
      {notice && <p className="pn-areas-success" role="status">{notice}</p>}

      <div className="pn-areas-form">
        <label>
          <span>Parking area</span>
          <select
            aria-label="Parking area"
            value={areaId}
            onChange={e => { setAreaId(e.target.value); reset(); }}
          >
            {areas.length === 0 && <option value="">No areas available</option>}
            {areas.map(area =>
              <option key={area.id} value={area.id}>
                {area.name} — {area.city}
              </option>
            )}
          </select>
        </label>

        <form onSubmit={save}>
          <h3>{editingId ? "Edit pricing" : "Create pricing"}</h3>
          <div className="pn-areas-fields">
            <label>
              <span>Vehicle type</span>
              <select
                value={form.vehicleType}
                disabled={busy}
                onChange={e =>
                  setForm({ ...form, vehicleType: e.target.value })
                }
              >
                <option value="CAR">Car</option>
                <option value="BIKE">Bike</option>
                <option value="EV">EV</option>
              </select>
            </label>

            {[
              ["hourlyRate", "Hourly rate (₹)"],
              ["peakHourlyRate", "Peak hourly rate (₹)"],
              ["monthlyPassPrice", "Monthly pass price (₹)"],
              ["monthlyPassDiscountPercent", "Monthly pass discount (%)"]
            ].map(([key, label]) =>
              <label key={key}>
                <span>{label}</span>
                <input
                  type="number"
                  min="0"
                  max={key === "monthlyPassDiscountPercent" ? "100" : undefined}
                  step="0.01"
                  required
                  disabled={busy}
                  value={form[key]}
                  onChange={e =>
                    setForm({ ...form, [key]: e.target.value })
                  }
                />
              </label>
            )}
          </div>
          <div className="pn-areas-actions">
            {editingId && (
              <button type="button" onClick={reset} disabled={busy}>
                Cancel edit
              </button>
            )}
            <button className="pn-areas-primary" disabled={busy || !areaId}>
              {busy ? "Saving..." : editingId ? "Save changes" : "Create pricing"}
            </button>
          </div>
        </form>
      </div>

      {loading ? <p>Loading pricing...</p> :
        plans.length === 0 ? <p>No pricing plans in this area.</p> : (
          <div className="pn-areas-grid">
            {plans.map(plan =>
              <article className="pn-areas-card" key={plan.id}>
                <div className="pn-areas-card-top">
                  <h3>{plan.vehicleType}</h3>
                  <strong>{plan.active ? "Active" : "Disabled"}</strong>
                </div>
                <p>Hourly: ₹{plan.hourlyRate}</p>
                <p>Peak hourly: ₹{plan.peakHourlyRate}</p>
                <p>Monthly pass base: ₹{plan.monthlyPassPrice}</p>
                <p>
                  Monthly pass discount:
                  {" "}{plan.monthlyPassDiscountPercent ?? 0}%
                </p>
                <div className="pn-areas-actions">
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => edit(plan)}
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => toggle(plan)}
                  >
                    {plan.active ? "Disable" : "Enable"}
                  </button>
                </div>
              </article>
            )}
          </div>
        )}
    </div>
  );
}