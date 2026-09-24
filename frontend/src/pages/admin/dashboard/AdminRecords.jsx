import React, { useEffect, useMemo, useState } from "react";
import api from "../../../services/api";
import "./AdminParkingAreas.css";

const CONFIG = {
  notifications: {
    title: "Notifications",
    url: "/api/admin/notifications",
    fields: [
      ["id", "Notification ID"],
      ["userId", "User ID"],
      ["type", "Type"],
      ["channel", "Channel"],
      ["status", "Status"],
      ["subject", "Subject"],
      ["createdAt", "Created"]
    ]
  },
  activity: {
    title: "Activity Logs",
    url: "/api/admin/activity-logs",
    fields: [
      ["id", "Log ID"],
      ["adminId", "Admin ID"],
      ["adminEmail", "Admin email"],
      ["action", "Action"],
      ["entityType", "Entity type"],
      ["entityId", "Entity ID"],
      ["createdAt", "Created"]
    ]
  },  wallets: {
    title: "Wallets",
    url: "/api/admin/wallets",
    fields: [
      ["id", "Wallet ID"],
      ["userId", "User ID"],
      ["userName", "Customer"],
      ["userEmail", "Email"],
      ["balance", "Balance"],
      ["updatedAt", "Updated"]
    ]
  },
  "monthly-passes": {
    title: "Monthly Passes",
    url: "/api/admin/monthly-passes",
    fields: [
      ["id", "Pass ID"],
      ["userId", "User ID"],
      ["parkingAreaId", "Parking area"],
      ["parkingSlotId", "Slot ID"],
      ["vehicleType", "Vehicle type"],
      ["vehicleNumber", "Vehicle number"],
      ["status", "Status"],
      ["createdAt", "Created"]
    ]
  },
  users: {
    title: "Users",
    url: "/api/admin/users",
    fields: [
      ["id", "User ID"],
      ["name", "Name"],
      ["email", "Email"],
      ["phone", "Phone"],
      ["role", "Role"],
      ["authProvider", "Provider"],
      ["emailVerified", "Email verified"],
      ["enabled", "Enabled"],
      ["createdAt", "Joined"]
    ]
  },  bookings: {
    title: "Bookings",
    url: "/api/admin/bookings",
    fields: [
      ["id", "Booking ID"],
      ["userId", "User ID"],
      ["parkingAreaId", "Parking area"],
      ["parkingSlotId", "Slot ID"],
      ["vehicleNumber", "Vehicle"],
      ["startTime", "Start"],
      ["endTime", "End"],
      ["status", "Status"]
    ]
  },
  payments: {
    title: "Payments",
    url: "/api/admin/payments",
    fields: [
      ["id", "Payment ID"],
      ["userId", "User ID"],
      ["bookingId", "Booking ID"],
      ["amount", "Amount"],
      ["paymentMethod", "Method"],
      ["status", "Status"],
      ["createdAt", "Created"]
    ]
  },
  refunds: {
    title: "Refunds",
    url: "/api/admin/refunds",
    fields: [
      ["id", "Payment ID"],
      ["userId", "User ID"],
      ["amount", "Amount"],
      ["refundAmount", "Refund amount"],
      ["refundReason", "Reason"],
      ["status", "Status"],
      ["refundRequestedAt", "Requested"]
    ]
  }
};

function show(value) {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

function errorText(error) {
  return error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    "Request failed.";
}

export default function AdminRecords({ kind, refreshSignal }) {
  const config = CONFIG[kind];
  const [rows, setRows] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [selected, setSelected] = useState(null);

  async function load() {
    setLoading(true);
    try {
      const response = await api.get(config.url);
      if (!Array.isArray(response.data)) {
        throw new Error("Invalid admin API response.");
      }
      setRows(response.data);
      setError("");
    } catch (err) {
      setError(errorText(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    setSelected(null);
    setSearch("");
    load();
  }, [kind, refreshSignal]);

  const filtered = useMemo(() =>
    rows.filter(row =>
      config.fields.some(([key]) =>
        show(row[key]).toLowerCase().includes(search.toLowerCase().trim())
      )
    ), [rows, search, kind]);

  async function changeUserStatus(row) {
    if (String(row.role).toUpperCase().replace(/^ROLE_/, "") === "ADMIN") {
      setError("Administrator accounts cannot be blocked.");
      return;
    }
    const nextEnabled = !row.enabled;
    if (!window.confirm(
      `${nextEnabled ? "Unblock" : "Block"} ${row.name || row.email}?`
    )) return;

    setBusyId(row.id);
    setError("");
    setNotice("");
    try {
      await api.patch(
        `/api/admin/users/${encodeURIComponent(row.id)}/status`,
        null,
        { params: { enabled: nextEnabled } }
      );
      await load();
      setSelected(null);
      setNotice(nextEnabled ? "User unblocked." : "User blocked.");
    } catch (err) {
      setError(errorText(err));
    } finally {
      setBusyId("");
    }
  }

  async function showWallet(row) {
    setBusyId(row.id);
    setError("");
    try {
      const response = await api.get(
        `/api/admin/wallets/${encodeURIComponent(row.userId)}`
      );
      setSelected(response.data);
    } catch (err) {
      setError(errorText(err));
    } finally {
      setBusyId("");
    }
  }
  async function cancelActivePass(row) {
    if (row.status !== "ACTIVE") return;

    if (!window.confirm(
      `Cancel active monthly pass ${row.id}? This does not issue a refund.`
    )) return;

    setBusyId(row.id);
    setError("");
    setNotice("");
    try {
      await api.patch(
        `/api/admin/monthly-passes/${encodeURIComponent(row.id)}/cancel`
      );
      await load();
      setSelected(null);
      setNotice("Monthly pass cancelled.");
    } catch (err) {
      setError(errorText(err));
    } finally {
      setBusyId("");
    }
  }
  async function completeRefund(row) {
    if (row.status !== "REFUND_PENDING") return;
    if (!window.confirm(
      `Complete the pending refund for payment ${row.id}?`
    )) return;

    setBusyId(row.id);
    setError("");
    setNotice("");
    try {
      await api.patch(
        `/api/admin/refunds/${encodeURIComponent(row.id)}/complete`
      );
      await load();
      setSelected(null);
      setNotice("Refund completed.");
    } catch (err) {
      setError(errorText(err));
    } finally {
      setBusyId("");
    }
  }

  return (
    <div className="pn-areas">
      <div className="pn-areas-heading">
        <div>
          <small>ADMIN OPERATIONS</small>
          <h2>{config.title}</h2>
          <p>Records loaded from the Park Nova backend.</p>
        </div>
      </div>

      {error && <p className="pn-areas-error" role="alert">{error}</p>}
      {notice && <p className="pn-areas-success" role="status">{notice}</p>}

      <div className="pn-areas-toolbar">
        <input
          aria-label={`Search ${config.title}`}
          placeholder="Search records"
          value={search}
          onChange={event => setSearch(event.target.value)}
        />
        <span>{filtered.length} records</span>
      </div>

      {loading ? <p>Loading {config.title.toLowerCase()}...</p> :
        filtered.length === 0 ? <p>No records found.</p> : (
          <div className="pn-areas-grid">
            {filtered.map(row => (
              <article className="pn-areas-card" key={row.id}>
                <div className="pn-areas-card-top">
                  <h3>{row.status || config.title.slice(0, -1)}</h3>
                  <small>{row.id}</small>
                </div>

                {config.fields.slice(1).map(([key, label]) => (
                  <p key={key}>
                    <strong>{label}:</strong> {show(row[key])}
                  </p>
                ))}

                <div className="pn-areas-actions">
                  <button type="button" onClick={() => setSelected(row)}>
                    Details
                  </button>
                  {kind === "wallets" && (
                    <button
                      type="button"
                      disabled={Boolean(busyId)}
                      onClick={() => showWallet(row)}
                    >
                      Transactions
                    </button>
                  )}
                  {kind === "users" &&
                    String(row.role).toUpperCase().replace(/^ROLE_/, "") !== "ADMIN" && (
                    <button
                      type="button"
                      disabled={Boolean(busyId)}
                      onClick={() => changeUserStatus(row)}
                    >
                      {row.enabled ? "Block user" : "Unblock user"}
                    </button>
                  )}                  {kind === "monthly-passes" && row.status === "ACTIVE" && (
                    <button
                      type="button"
                      disabled={Boolean(busyId)}
                      onClick={() => cancelActivePass(row)}
                    >
                      {busyId === row.id ? "Cancelling..." : "Cancel pass"}
                    </button>
                  )}                  {kind === "refunds" && row.status === "REFUND_PENDING" && (
                    <button
                      type="button"
                      disabled={Boolean(busyId)}
                      onClick={() => completeRefund(row)}
                    >
                      {busyId === row.id ? "Completing..." : "Complete refund"}
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}

      {selected && (
        <div className="pn-areas-form" style={{ marginTop: 20 }}>
          <div className="pn-areas-card-top">
            <h3>{config.title} details</h3>
            <button type="button" onClick={() => setSelected(null)}>
              Close
            </button>
          </div>
          {Object.entries(selected).map(([key, value]) => (
            <p key={key}>
              <strong>{key}:</strong> {show(value)}
            </p>
          ))}
        </div>
      )}
    </div>
  );
}