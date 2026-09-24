import React, { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./PaymentPage.css";

const money = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value || 0));

export default function PaymentPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const draft = location.state?.paymentDraft || null;

  const [method, setMethod] = useState("UPI");
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState("");
  const [payment, setPayment] = useState(null);

  useEffect(() => {
    if (!draft) {
      navigate("/bookings", { replace: true });
    }
  }, [draft, navigate]);

  if (!draft) return null;

  const handleAuthFailure = (err) => {
    const status = err?.response?.status;

    if (status === 401 || status === 403) {
      localStorage.removeItem("parkNovaToken");
      localStorage.removeItem("parkNovaUser");
      navigate("/login", { replace: true });
      return true;
    }

    return false;
  };

  const pay = async () => {
    try {
      setProcessing(true);
      setError("");

      const createResponse = await api.post("/api/payments", {
        paymentType: draft.paymentType,
        referenceId: draft.referenceId,
        paymentMethod: method,
      });

      const created =
        createResponse.data?.payment || createResponse.data;

      if (!created?.id) {
        throw new Error("Payment ID was not returned.");
      }

      /*
       * Wallet payments can already be SUCCESS after creation.
       * Do not call the success endpoint a second time.
       */
      if (created.status === "SUCCESS") {
        setPayment(created);
        return;
      }

      const successResponse = await api.patch(
        `/api/payments/${created.id}/success`
      );

      const completed =
        successResponse.data?.payment ||
        successResponse.data ||
        created;

      setPayment(completed);
    } catch (err) {
      if (handleAuthFailure(err)) return;

      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Payment could not be completed."
      );
    } finally {
      setProcessing(false);
    }
  };

  const finish = () => {
    if (draft.paymentType === "MONTHLY_PASS") {
      navigate("/monthly-pass", { replace: true });
    } else {
      navigate("/bookings", { replace: true });
    }
  };

  return (
    <main className="pn-payment-page">
      <header className="pn-payment-header">
        <Link to="/" className="pn-payment-brand">
          <img src="/images/park-nova-logo.png" alt="Park Nova" />
        </Link>

        <Link
          to={
            draft.paymentType === "MONTHLY_PASS"
              ? "/monthly-pass"
              : "/bookings"
          }
        >
          ← Back
        </Link>
      </header>

      <section className="pn-payment-shell">
        {!payment ? (
          <>
            <div className="pn-payment-title">
              <p>SECURE CHECKOUT</p>
              <h1>
                Complete <em>Payment</em>
              </h1>
              <span>
                Review your payment and choose a payment method.
              </span>
            </div>

            {error && (
              <div className="pn-payment-error" role="alert">
                {error}
              </div>
            )}

            <div className="pn-payment-layout">
              <section className="pn-payment-card">
                <div className="pn-payment-card-head">
                  <div>
                    <p>PAYMENT DETAILS</p>
                    <h2>Order summary</h2>
                  </div>

                  <span>Secure</span>
                </div>

                <div className="pn-payment-summary">
                  <div>
                    <span>Payment for</span>
                    <strong>
                      {draft.paymentType === "MONTHLY_PASS"
                        ? "Monthly Parking Pass"
                        : "Parking Booking"}
                    </strong>
                  </div>

                  {draft.booking?.vehicleNumber && (
                    <div>
                      <span>Vehicle</span>
                      <strong>
                        {draft.booking.vehicleNumber}
                      </strong>
                    </div>
                  )}

                  <div>
                    <span>Reference</span>
                    <strong>
                      #{String(draft.referenceId)
                        .slice(-8)
                        .toUpperCase()}
                    </strong>
                  </div>

                  <div className="pn-payment-total">
                    <span>Total amount</span>
                    <strong>{money(draft.amount)}</strong>
                  </div>
                </div>
              </section>

              <section className="pn-payment-method-card">
                <p>PAYMENT METHOD</p>
                <h2>How would you like to pay?</h2>

                <div className="pn-payment-methods">
                  {[
                    ["UPI", "UPI", "Fast digital payment"],
                    ["CARD", "Card", "Debit or credit card"],
                    ["WALLET", "Park Nova Wallet", "Use wallet balance"],
                  ].map(([value, title, subtitle]) => (
                    <button
                      type="button"
                      key={value}
                      className={
                        method === value ? "is-selected" : ""
                      }
                      onClick={() => setMethod(value)}
                    >
                      <span className="pn-payment-radio">
                        {method === value && <i />}
                      </span>

                      <span>
                        <strong>{title}</strong>
                        <small>{subtitle}</small>
                      </span>
                    </button>
                  ))}
                </div>

                <div className="pn-payment-note">
                  <span>✓</span>
                  <p>
                    Payment is processed through Park Nova's
                    backend payment workflow.
                  </p>
                </div>

                <button
                  type="button"
                  className="pn-payment-pay"
                  onClick={pay}
                  disabled={processing}
                >
                  {processing
                    ? "Processing..."
                    : `Pay ${money(draft.amount)}`}
                  {!processing && <span>→</span>}
                </button>
              </section>
            </div>
          </>
        ) : (
          <section className="pn-payment-success">
            <div className="pn-payment-success-icon">✓</div>

            <p>PAYMENT COMPLETE</p>
            <h1>Payment successful</h1>

            <span>
              Your{" "}
              {draft.paymentType === "MONTHLY_PASS"
                ? "monthly pass payment"
                : "parking booking payment"}{" "}
              has been completed successfully.
            </span>

            <div className="pn-payment-success-details">
              <div>
                <small>Amount</small>
                <strong>{money(draft.amount)}</strong>
              </div>

              <div>
                <small>Method</small>
                <strong>{method}</strong>
              </div>

              <div>
                <small>Status</small>
                <strong>{payment.status || "SUCCESS"}</strong>
              </div>
            </div>

            <button type="button" onClick={finish}>
              Continue
              <span>→</span>
            </button>
          </section>
        )}
      </section>
    </main>
  );
}