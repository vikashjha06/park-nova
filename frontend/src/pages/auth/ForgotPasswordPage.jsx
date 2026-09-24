import React, { useState } from "react";
import { Link } from "react-router-dom";
import AuthLayout from "../../components/auth/AuthLayout";
import api from "../../services/api";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const submit = async (event) => {
    event.preventDefault();

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await api.post(
        "/api/auth/forgot-password",
        null,
        {
          params: {
            email: email.trim(),
          },
        }
      );

      setMessage(
        response.data?.message ||
          "If an eligible account exists, a password reset link has been sent."
      );
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          "Unable to process the password reset request."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      eyebrow="ACCOUNT RECOVERY"
      title="Forgot your password?"
      description="Enter your Park Nova account email and we'll send you a secure reset link."
      footerText="Remember your password?"
      footerLinkText="Sign in"
      footerLinkTo="/login"
    >
      <form className="auth-form" onSubmit={submit}>
        {error && (
          <div
            className="auth-message auth-message-error"
            role="alert"
          >
            {error}
          </div>
        )}

        {message && (
          <div
            className="auth-message auth-message-success"
            role="status"
          >
            {message}
          </div>
        )}

        <div className="auth-field">
          <label htmlFor="forgot-email">
            Email address
          </label>

          <div className="auth-input-wrap">
            <span className="auth-field-icon">@</span>

            <input
              id="forgot-email"
              type="email"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value);
                setError("");
              }}
              placeholder="you@example.com"
              autoComplete="email"
              required
              disabled={loading}
            />
          </div>
        </div>

        <button
          className="auth-submit"
          type="submit"
          disabled={loading}
        >
          {loading
            ? "Sending Reset Link..."
            : "Send Reset Link"}
          {!loading && <span>-&gt;</span>}
        </button>

        <p className="auth-security-note">
          For security, Park Nova uses the same response
          whether or not an eligible account exists.
        </p>

        <Link
          className="auth-action-link"
          to="/login"
        >
          Back to Sign In
        </Link>
      </form>
    </AuthLayout>
  );
}