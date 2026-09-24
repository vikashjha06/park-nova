import React, { useEffect, useState } from "react";
import api from "../../../services/api";
import "./AdminParkingAreas.css";
import AdminPeriodReport from "./AdminPeriodReport";

const errorText = e =>
  e?.response?.data?.message || e?.response?.data?.error ||
  e?.message || "Request failed.";

function display(value) {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

export default function AdminInsights({ kind, refreshSignal }) {
  const [data, setData] = useState(null);
  const [form, setForm] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  async function load() {
    setLoading(true);
    try {
      const response = await api.get(
        kind === "reports"
          ? "/api/admin/reports/summary"
          : "/api/admin/settings"
      );
      setData(response.data);
      if (kind === "settings") {
        setForm({
          systemName: response.data?.systemName || "",
          supportEmail: response.data?.supportEmail || "",
          maintenanceMode: Boolean(response.data?.maintenanceMode),
          bookingEnabled: Boolean(response.data?.bookingEnabled)
        });
      }
      setError("");
    } catch (e) {
      setError(errorText(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [kind, refreshSignal]);

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const response = await api.put("/api/admin/settings", {
        systemName: form.systemName.trim(),
        supportEmail: form.supportEmail.trim(),
        maintenanceMode: form.maintenanceMode,
        bookingEnabled: form.bookingEnabled
      });
      setData(response.data);
      setNotice("Settings saved.");
    } catch (e) {
      setError(errorText(e));
    } finally {
      setSaving(false);
    }
  }

  function entries(value) {
    if (!value || typeof value !== "object") return null;
    return Object.entries(value).map(([key, item]) => (
      <article className="pn-areas-card" key={key}>
        <small>{key.replace(/([A-Z])/g, " $1").toUpperCase()}</small>
        {item !== null && typeof item === "object"
          ? <div>{entries(item)}</div>
          : <h3>{display(item)}</h3>}
      </article>
    ));
  }

  if (kind === "reports") {
    return <AdminPeriodReport refreshSignal={refreshSignal} />;
  }
  return (
    <div className="pn-areas">
      <div className="pn-areas-heading">
        <div>
          <small>PARK NOVA ADMIN</small>
          <h2>{kind === "reports" ? "Reports" : "Settings"}</h2>
          <p>
            {kind === "reports"
              ? "Live summary from the backend."
              : "Manage system-level configuration."}
          </p>
        </div>
      </div>

      {error && <p className="pn-areas-error" role="alert">{error}</p>}
      {notice && <p className="pn-areas-success" role="status">{notice}</p>}

      {loading ? <p>Loading...</p> :
        kind === "reports"
          ? <div className="pn-areas-grid">{entries(data)}</div>
          : form && (
            <form className="pn-areas-form" onSubmit={save}>
              <div className="pn-areas-fields">
                <label>
                  <span>System name</span>
                  <input
                    value={form.systemName}
                    required
                    disabled={saving}
                    onChange={e =>
                      setForm({ ...form, systemName: e.target.value })
                    }
                  />
                </label>
                <label>
                  <span>Support email</span>
                  <input
                    type="email"
                    value={form.supportEmail}
                    required
                    disabled={saving}
                    onChange={e =>
                      setForm({ ...form, supportEmail: e.target.value })
                    }
                  />
                </label>
                <label>
                  <span>
                    <input
                      type="checkbox"
                      checked={form.maintenanceMode}
                      disabled={saving}
                      onChange={e =>
                        setForm({
                          ...form,
                          maintenanceMode: e.target.checked
                        })
                      }
                    />
                    Maintenance mode
                  </span>
                </label>
                <label>
                  <span>
                    <input
                      type="checkbox"
                      checked={form.bookingEnabled}
                      disabled={saving}
                      onChange={e =>
                        setForm({
                          ...form,
                          bookingEnabled: e.target.checked
                        })
                      }
                    />
                    Booking enabled
                  </span>
                </label>
              </div>
              <button className="pn-areas-primary" disabled={saving}>
                {saving ? "Saving..." : "Save settings"}
              </button>
            </form>
          )}
    </div>
  );
}