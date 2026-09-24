import React, { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./PaymentsPage.css";

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

export default function PaymentsPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const refundTarget =
    location.state?.refundTarget || null;

  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [refundPayment, setRefundPayment] =
    useState(null);

  const [reason, setReason] = useState("");
  const [destination, setDestination] =
    useState("WALLET");

  const authFailure = (err) => {
    const status = err?.response?.status;

    if (status === 401 || status === 403) {
      localStorage.removeItem("parkNovaToken");
      localStorage.removeItem("parkNovaUser");

      navigate("/login", {
        replace: true,
      });

      return true;
    }

    return false;
  };

  const loadPayments = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get(
        "/api/payments/my"
      );

      setPayments(
        Array.isArray(response.data)
          ? response.data
          : []
      );
    } catch (err) {
      if (authFailure(err)) {
        return;
      }

      setError(
        err?.response?.data?.message ||
          "Unable to load payment history."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPayments();
  }, []);

  const sortedPayments = useMemo(() => {
    return [...payments].sort((a, b) => {
      const first =
        new Date(a?.createdAt || 0).getTime();

      const second =
        new Date(b?.createdAt || 0).getTime();

      return second - first;
    });
  }, [payments]);

  const requestRefund = async (event) => {
    event.preventDefault();

    if (!refundPayment) {
      return;
    }

    const cleanReason = reason.trim();

    if (!cleanReason) {
      setError(
        "Please enter a refund reason."
      );
      return;
    }

    try {
      setActionId(refundPayment.id);
      setError("");
      setSuccess("");

      await api.post(
        `/api/payments/${refundPayment.id}/refund`,
        {
          reason: cleanReason,
          destination,
        }
      );

      const completedResponse =
        await api.patch(
          `/api/payments/${refundPayment.id}/refund/complete`
        );

      const completedRefund =
        completedResponse.data?.payment ||
        completedResponse.data;

      setSuccess(
        destination === "WALLET"
          ? `Refund completed. ${money(
              completedRefund?.refundAmount ??
                refundPayment.amount
            )} has been credited to your Park Nova Wallet.`
          : "Refund completed successfully to the original payment method."
      );

      setRefundPayment(null);
      setReason("");
      setDestination("WALLET");

      await loadPayments();
    } catch (err) {
      if (authFailure(err)) {
        return;
      }

      setError(
        err?.response?.data?.message ||
          "Unable to request refund."
      );
    } finally {
      setActionId("");
    }
  };

  return (
    <main className="pn-payments-page">
      <header className="pn-payments-header">
        <Link
          to="/"
          className="pn-payments-brand"
        >
          <img
            src="/images/park-nova-logo.png"
            alt="Park Nova"
          />
        </Link>

        <nav>
          <Link to="/parking">
            Find Parking
          </Link>

          <Link to="/bookings">
            Bookings
          </Link>

          <Link to="/wallet">
            Wallet
          </Link>

          <Link to="/monthly-pass">
            Monthly Pass
          </Link>

          <Link to="/profile">
            Profile
          </Link>
        </nav>
      </header>

      <section className="pn-payments-shell">
        <div className="pn-payments-title">
          <div>
            <p>MONEY ACTIVITY</p>

            <h1>
              Payments & <em>Refunds</em>
            </h1>

            <span>
              View your Park Nova transactions
              and request eligible refunds.
            </span>
          </div>

          <button
            type="button"
            onClick={loadPayments}
            disabled={loading}
          >
            {loading
              ? "Loading..."
              : "Refresh"}
          </button>
        </div>

        {error && (
          <div className="pn-payments-alert error">
            <span>{error}</span>

            <button
              type="button"
              onClick={() => setError("")}
            >
              ×
            </button>
          </div>
        )}

        {success && (
          <div className="pn-payments-alert success">
            <span>✓ {success}</span>

            <button
              type="button"
              onClick={() => setSuccess("")}
            >
              ×
            </button>
          </div>
        )}

        {refundPayment && (
          <section className="pn-refund-panel">
            <div className="pn-refund-head">
              <div>
                <p>REFUND REQUEST</p>

                <h2>
                  {money(
                    refundPayment.amount
                  )}
                </h2>
              </div>

              <button
                type="button"
                onClick={() => {
                  setRefundPayment(null);
                  setReason("");
                  setDestination(
                    "WALLET"
                  );
                }}
              >
                ×
              </button>
            </div>

            <form
              onSubmit={requestRefund}
            >
              <label>
                <span>
                  Refund destination
                </span>

                <select
                  value={destination}
                  onChange={(event) =>
                    setDestination(
                      event.target.value
                    )
                  }
                >
                  <option value="WALLET">
                    Park Nova Wallet
                  </option>

                  <option value="ORIGINAL_METHOD">
                    Original Payment Method
                  </option>
                </select>
              </label>

              <label>
                <span>Reason</span>

                <textarea
                  value={reason}
                  onChange={(event) =>
                    setReason(
                      event.target.value
                    )
                  }
                  placeholder="Tell us why you are requesting this refund..."
                  rows="4"
                  required
                />
              </label>

              <button
                className="pn-refund-submit"
                type="submit"
                disabled={
                  actionId ===
                  refundPayment.id
                }
              >
                {actionId ===
                refundPayment.id
                  ? "Submitting..."
                  : "Request Refund"}
              </button>
            </form>

            <small>
              Refund eligibility is
              verified by the Park Nova
              backend before the request
              is accepted.
            </small>
          </section>
        )}

        {loading ? (
          <div className="pn-payments-state">
            <strong>
              Loading payments...
            </strong>
          </div>
        ) : sortedPayments.length ===
          0 ? (
          <div className="pn-payments-state">
            <strong>
              No payments yet
            </strong>

            <span>
              Your completed parking and
              monthly-pass payments will
              appear here.
            </span>
          </div>
        ) : (
          <section className="pn-payment-history">
            {sortedPayments.map(
              (payment) => {
                const canRequestRefund =
                  Boolean(refundTarget) &&
                  payment.paymentType ===
                    refundTarget.paymentType &&
                  payment.referenceId ===
                    refundTarget.referenceId &&
                  payment.status ===
                    "SUCCESS" &&
                  payment.referenceStatus ===
                    "CANCELLED";

                return (
                  <article
                    className="pn-payment-history-card"
                    key={payment.id}
                  >
                    <div className="pn-payment-card-head">
                      <div>
                        <small>
                          {
                            payment.paymentType
                          }
                        </small>

                        <strong>
                          {money(
                            payment.amount
                          )}
                        </strong>
                      </div>

                      <span
                        className={`pn-payment-status status-${String(
                          payment.status ||
                            ""
                        ).toLowerCase()}`}
                      >
                        {String(
                          payment.status ||
                            ""
                        ).replaceAll(
                          "_",
                          " "
                        )}
                      </span>
                    </div>

                    <div className="pn-payment-details">
                      <div>
                        <span>
                          Payment method
                        </span>

                        <strong>
                          {payment.paymentMethod ||
                            "—"}
                        </strong>
                      </div>

                      <div>
                        <span>
                          Paid / created
                        </span>

                        <strong>
                          {dateTime(
                            payment.createdAt
                          )}
                        </strong>
                      </div>

                      <div>
                        <span>
                          Transaction
                        </span>

                        <strong>
                          {payment.transactionId ||
                            "—"}
                        </strong>
                      </div>

                      <div>
                        <span>
                          Reference
                        </span>

                        <strong>
                          {payment.referenceId
                            ? `#${String(
                                payment.referenceId
                              )
                                .slice(-8)
                                .toUpperCase()}`
                            : "—"}
                        </strong>
                      </div>
                    </div>

                    {[
                      "REFUND_PENDING",
                      "REFUNDED",
                    ].includes(
                      payment.status
                    ) && (
                      <div className="pn-refund-info">
                        <div>
                          <span>
                            Refund amount
                          </span>

                          <strong>
                            {money(
                              payment.refundAmount ??
                                payment.amount
                            )}
                          </strong>
                        </div>

                        <div>
                          <span>
                            Destination
                          </span>

                          <strong>
                            {String(
                              payment.refundDestination ||
                                "—"
                            ).replaceAll(
                              "_",
                              " "
                            )}
                          </strong>
                        </div>

                        <div>
                          <span>
                            Requested
                          </span>

                          <strong>
                            {dateTime(
                              payment.refundRequestedAt
                            )}
                          </strong>
                        </div>

                        {payment.status ===
                          "REFUNDED" && (
                          <div>
                            <span>
                              Refunded
                            </span>

                            <strong>
                              {dateTime(
                                payment.refundedAt
                              )}
                            </strong>
                          </div>
                        )}

                        {payment.refundReason && (
                          <div className="full">
                            <span>
                              Reason
                            </span>

                            <strong>
                              {
                                payment.refundReason
                              }
                            </strong>
                          </div>
                        )}
                      </div>
                    )}

                    <div className="pn-payment-actions">
                      {canRequestRefund && (
                        <button
                          type="button"
                          onClick={() => {
                            setError("");
                            setSuccess("");
                            setReason("");
                            setDestination(
                              "WALLET"
                            );
                            setRefundPayment(
                              payment
                            );
                          }}
                        >
                          Request Refund
                        </button>
                      )}

                      {payment.status ===
                        "REFUND_PENDING" && (
                        <span>
                          Refund processing
                        </span>
                      )}

                      {payment.status ===
                        "REFUNDED" && (
                        <span>
                          Refund completed ✓
                        </span>
                      )}
                    </div>
                  </article>
                );
              }
            )}
          </section>
        )}
      </section>
    </main>
  );
}