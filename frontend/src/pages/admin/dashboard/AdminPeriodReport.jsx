import React, { useEffect, useMemo, useState } from "react";
import api from "../../../services/api";
import "./AdminPeriodReport.css";

const sources = [
  ["bookings", "/api/admin/bookings"],
  ["payments", "/api/admin/payments"],
  ["passes", "/api/admin/monthly-passes"],
  ["users", "/api/admin/users"],
  ["notifications", "/api/admin/notifications"],
  ["wallets", "/api/admin/wallets"]
];

const formatMoney = value =>
  `₹${Number(value || 0).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`;

const datePart = value => String(value || "").slice(0, 10);
const sum = (items, field) =>
  items.reduce((total, item) => total + Number(item[field] || 0), 0);

function countBy(items, field) {
  return items.reduce((result, item) => {
    const key = String(item[field] || "UNKNOWN");
    result[key] = (result[key] || 0) + 1;
    return result;
  }, {});
}

function errorText(error) {
  return error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message || "Could not load report.";
}

export default function AdminPeriodReport({ refreshSignal }) {
  const [mode, setMode] = useState("overall");
  const [day, setDay] = useState(() => new Date().toISOString().slice(0, 10));
  const [month, setMonth] = useState(() => new Date().toISOString().slice(0, 7));
  const [records, setRecords] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    Promise.all(sources.map(async ([key, url]) => {
      const response = await api.get(url);
      if (!Array.isArray(response.data)) {
        throw new Error(`Invalid ${key} response.`);
      }
      return [key, response.data];
    }))
      .then(results => {
        if (active) setRecords(Object.fromEntries(results));
      })
      .catch(err => {
        if (active) setError(errorText(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, [refreshSignal]);

  const report = useMemo(() => {
    if (!records) return null;

    const inPeriod = value => {
      const date = datePart(value);
      if (!date) return false;
      if (mode === "overall") return true;
      if (mode === "day") return date === day;
      return date.startsWith(month + "-");
    };

    const bookings = records.bookings.filter(x => inPeriod(x.createdAt));
    const payments = records.payments.filter(x => inPeriod(x.createdAt));
    const passes = records.passes.filter(x => inPeriod(x.createdAt));
    const users = records.users.filter(x => inPeriod(x.createdAt));
    const notifications = records.notifications.filter(
      x => inPeriod(x.createdAt)
    );
    const wallets = records.wallets.filter(x => inPeriod(x.createdAt));

    const revenuePayments = payments.filter(x =>
      ["SUCCESS", "REFUND_PENDING", "REFUNDED"].includes(
        String(x.status || "").toUpperCase()
      )
    );
    const refunds = records.payments.filter(x =>
      String(x.status || "").toUpperCase() === "REFUNDED" &&
      inPeriod(x.refundedAt)
    );

    const gross = sum(revenuePayments, "amount");
    const refunded = sum(refunds, "refundAmount");

    return {
      bookings,
      payments,
      passes,
      users,
      notifications,
      wallets,
      refunds,
      gross,
      refunded,
      net: gross - refunded,
      bookingsByStatus: countBy(bookings, "status"),
      paymentsByStatus: countBy(payments, "status"),
      passesByStatus: countBy(passes, "status")
    };
  }, [records, mode, day, month]);

  const title = mode === "overall"
    ? "Overall report"
    : mode === "day"
      ? `Daily report · ${day}`
      : `Monthly report · ${month}`;

  const metrics = report ? [
    ["Bookings created", report.bookings.length],
    ["Payments created", report.payments.length],
    ["Gross payments", formatMoney(report.gross)],
    ["Refunds completed", report.refunds.length],
    ["Refunded amount", formatMoney(report.refunded)],
    ["Net", formatMoney(report.net)],
    ["Monthly passes created", report.passes.length],
    ["Users joined", report.users.length],
    ["Notifications", report.notifications.length],
    ["Wallets created", report.wallets.length]
  ] : [];

  return (
    <div className="pn-report">
      <header className="pn-report-heading">
        <div>
          <span>PARK NOVA ANALYTICS</span>
          <h2>Reports</h2>
          <p>Choose a day, month or the full history.</p>
        </div>
      </header>

      <div className="pn-report-controls">
        <label>
          Report type
          <select value={mode} onChange={e => setMode(e.target.value)}>
            <option value="day">Daily</option>
            <option value="month">Monthly</option>
            <option value="overall">Overall</option>
          </select>
        </label>

        {mode === "day" && (
          <label>
            Date
            <input
              type="date"
              value={day}
              onChange={e => setDay(e.target.value)}
            />
          </label>
        )}

        {mode === "month" && (
          <label>
            Month
            <input
              type="month"
              value={month}
              onChange={e => setMonth(e.target.value)}
            />
          </label>
        )}
      </div>

      {error && <p className="pn-report-error" role="alert">{error}</p>}
      {loading && <p>Loading report...</p>}

      {!loading && report && (
        <>
          <h3>{title}</h3>
          <p className="pn-report-note">
            Counts use each record's creation date. Refunds use their
            completion date. Status counts show the records' current status.
          </p>

          <div className="pn-report-grid">
            {metrics.map(([label, value]) => (
              <article key={label} className="pn-report-card">
                <span>{label}</span>
                <strong>{value}</strong>
              </article>
            ))}
          </div>

          <div className="pn-report-sections">
            {[
              ["Bookings by status", report.bookingsByStatus],
              ["Payments by status", report.paymentsByStatus],
              ["Monthly passes by status", report.passesByStatus]
            ].map(([label, values]) => (
              <section className="pn-report-card" key={label}>
                <h4>{label}</h4>
                {Object.keys(values).length === 0
                  ? <p>No records in this period.</p>
                  : Object.entries(values).map(([status, count]) => (
                    <div className="pn-report-row" key={status}>
                      <span>{status}</span><strong>{count}</strong>
                    </div>
                  ))}
              </section>
            ))}
          </div>
        </>
      )}
    </div>
  );
}