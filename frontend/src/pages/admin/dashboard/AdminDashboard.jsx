import React, { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import api from "../../../services/api";
import "./AdminDashboard.css";
import AdminParkingAreas from "./AdminParkingAreas";
import AdminParkingSlots from "./AdminParkingSlots";
import AdminPricing from "./AdminPricing";
import AdminRecords from "./AdminRecords";
import AdminInsights from "./AdminInsights";

const MENU = [
  ["dashboard", "Dashboard", "▦"],
  ["parking-areas", "Parking Areas", "P"],
  ["parking-slots", "Parking Slots", "S"],
  ["pricing", "Pricing", "₹"],
  ["bookings", "Bookings", "B"],
  ["payments", "Payments", "₹"],
  ["refunds", "Refunds", "R"],
  ["wallets", "Wallets", "W"],
  ["monthly-passes", "Monthly Passes", "M"],
  ["users", "Users", "U"],
  ["notifications", "Notifications", "N"],
  ["reports", "Reports", "A"],
  ["activity", "Activity Logs", "L"],
  ["settings", "Settings", "⚙"],
];

const money = (value) =>
  `₹${Number(value || 0).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;

const dateText = (value) => {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : date.toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
      });
};

const getStatusClass = (status) => {
  const value = String(status || "").toUpperCase();

  if (["SUCCESS", "COMPLETED", "AVAILABLE", "ACTIVE"].includes(value)) {
    return "success";
  }

  if (["PENDING", "CONFIRMED", "BOOKED", "REFUND_PENDING"].includes(value)) {
    return "pending";
  }

  if (["FAILED", "CANCELLED", "DENIED"].includes(value)) {
    return "danger";
  }

  if (["REFUNDED", "OCCUPIED"].includes(value)) {
    return "warning";
  }

  return "neutral";
};

export default function AdminDashboard() {
  const navigate = useNavigate();
  const location = useLocation();
  const isAreas = location.pathname === "/admin/parking-areas";
  const isSlots = location.pathname === "/admin/parking-slots";
  const isPricing = location.pathname === "/admin/pricing";
  const insightKind = ["reports", "settings"]
    .find(key => location.pathname === `/admin/${key}`);
  const recordKind = ["bookings", "payments", "refunds", "wallets", "monthly-passes", "users", "notifications", "activity"]
    .find(key => location.pathname === `/admin/${key}`);
  const [refreshSignal, setRefreshSignal] = useState(0);

  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [stats, setStats] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const admin = useMemo(() => {
    try {
      return JSON.parse(localStorage.getItem("parkNovaUser") || "{}");
    } catch {
      return {};
    }
  }, []);

  const loadDashboard = async (manual = false) => {
    try {
      manual ? setRefreshing(true) : setLoading(true);
      setError("");

      const [dashboardResponse, bookingsResponse, paymentsResponse] =
        await Promise.all([
          api.get("/api/admin/dashboard"),
          api.get("/api/admin/bookings"),
          api.get("/api/admin/payments"),
        ]);

      setStats(dashboardResponse?.data || {});
      setBookings(
        Array.isArray(bookingsResponse?.data)
          ? bookingsResponse.data.slice(0, 5)
          : []
      );
      setPayments(
        Array.isArray(paymentsResponse?.data)
          ? paymentsResponse.data.slice(0, 5)
          : []
      );
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          "Unable to load admin dashboard."
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    if (!isAreas && !isSlots && !isPricing && !recordKind && !insightKind) loadDashboard();
  }, [isAreas]);

  const logout = () => {
    localStorage.removeItem("parkNovaToken");
    localStorage.removeItem("parkNovaUser");
    navigate("/admin/login", { replace: true });
  };

  const goToModule = (key) => {
    if (key === "dashboard") {
      navigate("/admin");
      setSidebarOpen(false);
      return;
    }

    navigate(`/admin/${key}`);
    setSidebarOpen(false);
  };

  const slotTotal = Number(stats?.totalParkingSlots || 0);

  const slotPercent = (value) => {
    if (!slotTotal) return 0;
    return Math.min(100, (Number(value || 0) / slotTotal) * 100);
  };

  const cards = [
    {
      label: "Total Users",
      value: stats?.totalUsers ?? 0,
      sub: `${stats?.enabledUsers ?? 0} enabled`,
      icon: "U",
    },
    {
      label: "Parking Areas",
      value: stats?.totalParkingAreas ?? 0,
      sub: `${stats?.activeParkingAreas ?? 0} active`,
      icon: "P",
    },
    {
      label: "Total Slots",
      value: stats?.totalParkingSlots ?? 0,
      sub: `${stats?.availableSlots ?? 0} available`,
      icon: "S",
    },
    {
      label: "Active Bookings",
      value: stats?.activeBookings ?? 0,
      sub: `${stats?.totalBookings ?? 0} total bookings`,
      icon: "B",
    },
    {
      label: "Net Revenue",
      value: money(stats?.netRevenue),
      sub: `${stats?.successfulPayments ?? 0} successful payments`,
      icon: "₹",
    },
    {
      label: "Refunded",
      value: money(stats?.totalRefunded),
      sub: `${stats?.refundedPayments ?? 0} refunded payments`,
      icon: "R",
    },
  ];

  return (
    <div className="pn-admin-shell">
      {sidebarOpen && (
        <button
          className="pn-admin-overlay"
          aria-label="Close menu"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <aside className={`pn-admin-sidebar ${sidebarOpen ? "open" : ""}`}>
        <div className="pn-admin-brand">
          <img src="/images/park-nova-logo.png" alt="Park Nova" />
          <div>
            <strong>ADMIN CONSOLE</strong>
            <span>Park Smart Live Better</span>
          </div>
        </div>

        <nav className="pn-admin-nav">
          {MENU.map(([key, label, icon]) => (
            <button
              key={key}
              type="button"
              className={(isAreas ? key === "parking-areas" : isSlots ? key === "parking-slots" : isPricing ? key === "pricing" : recordKind ? key === recordKind : insightKind ? key === insightKind : key === "dashboard") ? "active" : ""}
              onClick={() => goToModule(key)}
            >
              <span className="pn-admin-nav-icon">{icon}</span>
              <span>{label}</span>
            </button>
          ))}
        </nav>

        <div className="pn-admin-sidebar-bottom">
          <div className="pn-admin-mini-profile">
            <div className="pn-admin-avatar">
              {(admin?.name || "A").charAt(0).toUpperCase()}
            </div>
            <div>
              <strong>{admin?.name || "Administrator"}</strong>
              <span>{admin?.email || "Park Nova Admin"}</span>
            </div>
          </div>

          <button
            type="button"
            className="pn-admin-logout"
            onClick={logout}
          >
            <span>↪</span>
            Logout
          </button>
        </div>
      </aside>

      <main className="pn-admin-main">
        <header className="pn-admin-topbar">
          <div className="pn-admin-topbar-left">
            <button
              type="button"
              className="pn-admin-menu-button"
              aria-label="Open menu"
              onClick={() => setSidebarOpen(true)}
            >
              ☰
            </button>

            <div>
              <span className="pn-admin-eyebrow">PARK NOVA ADMIN</span>
              <h1>{isAreas ? "Parking Areas" : isSlots ? "Parking Slots" : isPricing ? "Pricing" : recordKind ? recordKind[0].toUpperCase() + recordKind.slice(1) : insightKind ? insightKind[0].toUpperCase() + insightKind.slice(1) : "Dashboard Overview"}</h1>
            </div>
          </div>

          <div className="pn-admin-top-actions">
            <button
              type="button"
              className="pn-admin-refresh"
              onClick={() => (isAreas || isSlots || isPricing || recordKind || insightKind) ? setRefreshSignal(n => n + 1) : loadDashboard(true)}
              disabled={refreshing}
            >
              {refreshing ? "Refreshing..." : "Refresh"}
            </button>

            <div className="pn-admin-top-profile">
              <div className="pn-admin-avatar">
                {(admin?.name || "A").charAt(0).toUpperCase()}
              </div>
              <div>
                <strong>{admin?.name || "Administrator"}</strong>
                <span>Administrator</span>
              </div>
            </div>
          </div>
        </header>

        <div className="pn-admin-content">
          {isAreas ? <AdminParkingAreas refreshSignal={refreshSignal} /> : isSlots ? <AdminParkingSlots refreshSignal={refreshSignal} /> : isPricing ? <AdminPricing refreshSignal={refreshSignal} /> : recordKind ? <AdminRecords kind={recordKind} refreshSignal={refreshSignal} /> : insightKind ? <AdminInsights kind={insightKind} refreshSignal={refreshSignal} /> : <>
          <section className="pn-admin-welcome">
            <div>
              <span className="pn-admin-live">
                <i />
                SYSTEM OVERVIEW
              </span>
              <h2>
                Welcome back, {admin?.name?.split(" ")?.[0] || "Admin"}
              </h2>
              <p>
                Monitor Park Nova operations, parking activity and
                financial performance from one secure workspace.
              </p>
            </div>

            <div className="pn-admin-welcome-mark">PN</div>
          </section>

          {error && (
            <div className="pn-admin-dashboard-error" role="alert">
              <div>
                <strong>Dashboard data unavailable</strong>
                <span>{error}</span>
              </div>
              <button type="button" onClick={() => (isAreas || isSlots || isPricing || recordKind || insightKind) ? setRefreshSignal(n => n + 1) : loadDashboard(true)}>
                Retry
              </button>
            </div>
          )}

          {loading ? (
            <div className="pn-admin-loading">
              <span />
              Loading live dashboard data...
            </div>
          ) : (
            <>
              <section className="pn-admin-stat-grid">
                {cards.map((card) => (
                  <article className="pn-admin-stat-card" key={card.label}>
                    <div className="pn-admin-stat-head">
                      <span>{card.label}</span>
                      <div className="pn-admin-stat-icon">{card.icon}</div>
                    </div>

                    <strong className="pn-admin-stat-value">
                      {card.value}
                    </strong>

                    <span className="pn-admin-stat-sub">{card.sub}</span>
                  </article>
                ))}
              </section>

              <section className="pn-admin-dashboard-grid">
                <article className="pn-admin-panel">
                  <div className="pn-admin-panel-heading">
                    <div>
                      <span>LIVE CAPACITY</span>
                      <h3>Parking Occupancy</h3>
                    </div>
                    <strong>{slotTotal} slots</strong>
                  </div>

                  <div className="pn-admin-capacity-list">
                    {[
                      ["Available", stats?.availableSlots, "available"],
                      ["Booked", stats?.bookedSlots, "booked"],
                      ["Occupied", stats?.occupiedSlots, "occupied"],
                      ["Disabled", stats?.disabledSlots, "disabled"],
                    ].map(([label, value, type]) => (
                      <div className="pn-admin-capacity-row" key={label}>
                        <div>
                          <span>{label}</span>
                          <strong>{value ?? 0}</strong>
                        </div>

                        <div className="pn-admin-progress">
                          <span
                            className={type}
                            style={{
                              width: `${slotPercent(value)}%`,
                            }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </article>

                <article className="pn-admin-panel">
                  <div className="pn-admin-panel-heading">
                    <div>
                      <span>BOOKING FLOW</span>
                      <h3>Booking Status</h3>
                    </div>
                    <strong>{stats?.totalBookings ?? 0} total</strong>
                  </div>

                  <div className="pn-admin-status-grid">
                    <div>
                      <span>Pending</span>
                      <strong>{stats?.pendingBookings ?? 0}</strong>
                    </div>
                    <div>
                      <span>Confirmed</span>
                      <strong>{stats?.confirmedBookings ?? 0}</strong>
                    </div>
                    <div>
                      <span>Active</span>
                      <strong>{stats?.activeBookings ?? 0}</strong>
                    </div>
                    <div>
                      <span>Completed</span>
                      <strong>{stats?.completedBookings ?? 0}</strong>
                    </div>
                    <div>
                      <span>Cancelled</span>
                      <strong>{stats?.cancelledBookings ?? 0}</strong>
                    </div>
                  </div>
                </article>

                <article className="pn-admin-panel pn-admin-revenue-panel">
                  <div className="pn-admin-panel-heading">
                    <div>
                      <span>FINANCIAL SUMMARY</span>
                      <h3>Revenue & Payments</h3>
                    </div>
                  </div>

                  <div className="pn-admin-revenue-main">
                    <span>Net Revenue</span>
                    <strong>{money(stats?.netRevenue)}</strong>
                  </div>

                  <div className="pn-admin-finance-grid">
                    <div>
                      <span>Gross Revenue</span>
                      <strong>{money(stats?.grossRevenue)}</strong>
                    </div>
                    <div>
                      <span>Total Refunded</span>
                      <strong>{money(stats?.totalRefunded)}</strong>
                    </div>
                    <div>
                      <span>Successful</span>
                      <strong>{stats?.successfulPayments ?? 0}</strong>
                    </div>
                    <div>
                      <span>Pending</span>
                      <strong>{stats?.pendingPayments ?? 0}</strong>
                    </div>
                    <div>
                      <span>Failed</span>
                      <strong>{stats?.failedPayments ?? 0}</strong>
                    </div>
                    <div>
                      <span>Refund Pending</span>
                      <strong>{stats?.refundPendingPayments ?? 0}</strong>
                    </div>
                  </div>
                </article>
              </section>

              <section className="pn-admin-table-grid">
                <article className="pn-admin-panel pn-admin-table-panel">
                  <div className="pn-admin-panel-heading">
                    <div>
                      <span>RECENT ACTIVITY</span>
                      <h3>Latest Bookings</h3>
                    </div>
                    <button
                      type="button"
                      onClick={() => goToModule("bookings")}
                    >
                      View all
                    </button>
                  </div>

                  {bookings.length === 0 ? (
                    <div className="pn-admin-empty">
                      No booking activity yet.
                    </div>
                  ) : (
                    <div className="pn-admin-table-wrap">
                      <table>
                        <thead>
                          <tr>
                            <th>Booking</th>
                            <th>Status</th>
                            <th>Created</th>
                          </tr>
                        </thead>
                        <tbody>
                          {bookings.map((booking) => (
                            <tr key={booking.id}>
                              <td>
                                <strong>
                                  #{String(booking.id || "").slice(-7)}
                                </strong>
                                <span>
                                  {booking.parkingAreaName ||
                                    booking.parkingAreaId ||
                                    "Parking booking"}
                                </span>
                              </td>
                              <td>
                                <span
                                  className={`pn-admin-status ${getStatusClass(
                                    booking.status
                                  )}`}
                                >
                                  {booking.status || "UNKNOWN"}
                                </span>
                              </td>
                              <td>{dateText(booking.createdAt)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </article>

                <article className="pn-admin-panel pn-admin-table-panel">
                  <div className="pn-admin-panel-heading">
                    <div>
                      <span>TRANSACTIONS</span>
                      <h3>Latest Payments</h3>
                    </div>
                    <button
                      type="button"
                      onClick={() => goToModule("payments")}
                    >
                      View all
                    </button>
                  </div>

                  {payments.length === 0 ? (
                    <div className="pn-admin-empty">
                      No payment activity yet.
                    </div>
                  ) : (
                    <div className="pn-admin-table-wrap">
                      <table>
                        <thead>
                          <tr>
                            <th>Payment</th>
                            <th>Amount</th>
                            <th>Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {payments.map((payment) => (
                            <tr key={payment.id}>
                              <td>
                                <strong>
                                  #{String(payment.id || "").slice(-7)}
                                </strong>
                                <span>{dateText(payment.createdAt)}</span>
                              </td>
                              <td>{money(payment.amount)}</td>
                              <td>
                                <span
                                  className={`pn-admin-status ${getStatusClass(
                                    payment.status
                                  )}`}
                                >
                                  {payment.status || "UNKNOWN"}
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </article>
              </section>

              <section className="pn-admin-system-strip">
                <div>
                  <span className="online-dot" />
                  <p>
                    <strong>Park Nova system operational</strong>
                    <span>Live database connection active</span>
                  </p>
                </div>

                <div>
                  <span>Verified users</span>
                  <strong>{stats?.verifiedUsers ?? 0}</strong>
                </div>

                <div>
                  <span>Total payments</span>
                  <strong>{stats?.totalPayments ?? 0}</strong>
                </div>
              </section>
            </>
          )}
          </>}
        </div>
      </main>
    </div>
  );
}