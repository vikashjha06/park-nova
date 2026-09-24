import React, { useState } from "react";
import {
  Link,
  useSearchParams,
} from "react-router-dom";
import AuthLayout from "../../components/auth/AuthLayout";
import api from "../../services/api";

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";

  const [form, setForm] = useState({
    password: "",
    confirmPassword: "",
  });

  const [showPassword, setShowPassword] =
    useState(false);

  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");

  const change = (event) => {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));

    setError("");
  };

  const submit = async (event) => {
    event.preventDefault();

    if (!token) {
      setError(
        "This password reset link is invalid."
      );
      return;
    }

    if (form.password.length < 8) {
      setError(
        "Password must be at least 8 characters."
      );
      return;
    }

    if (
      form.password !== form.confirmPassword
    ) {
      setError("Passwords do not match.");
      return;
    }

    try {
      setLoading(true);
      setError("");
      setSuccess("");

      const response = await api.post(
        "/api/auth/reset-password",
        {
          token,
          newPassword: form.password,
        }
      );

      setSuccess(
        response.data?.message ||
          "Password reset successfully."
      );

      setForm({
        password: "",
        confirmPassword: "",
      });
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          "Unable to reset your password. The link may be invalid or expired."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      eyebrow="SECURE RESET"
      title="Create a new password"
      description="Choose a new password for your Park Nova account."
      footerText="Return to your account?"
      footerLinkText="Sign in"
      footerLinkTo="/login"
    >
      <form className="auth-form" onSubmit={submit}>
        {!token && (
          <div className="auth-message auth-message-error">
            This password reset link is invalid.
          </div>
        )}

        {error && (
          <div
            className="auth-message auth-message-error"
            role="alert"
          >
            {error}
          </div>
        )}

        {success && (
          <div
            className="auth-message auth-message-success"
            role="status"
          >
            {success}
          </div>
        )}

        {!success && token && (
          <>
            <div className="auth-field">
              <label htmlFor="reset-password">
                New password
              </label>

              <div className="auth-input-wrap">
                <span className="auth-field-icon">
                  *
                </span>

                <input
                  id="reset-password"
                  name="password"
                  type={
                    showPassword
                      ? "text"
                      : "password"
                  }
                  value={form.password}
                  onChange={change}
                  placeholder="Minimum 8 characters"
                  autoComplete="new-password"
                  minLength={8}
                  required
                  disabled={loading}
                />
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="reset-confirm">
                Confirm new password
              </label>

              <div className="auth-input-wrap">
                <span className="auth-field-icon">
                  *
                </span>

                <input
                  id="reset-confirm"
                  name="confirmPassword"
                  type={
                    showPassword
                      ? "text"
                      : "password"
                  }
                  value={form.confirmPassword}
                  onChange={change}
                  placeholder="Repeat new password"
                  autoComplete="new-password"
                  minLength={8}
                  required
                  disabled={loading}
                />
              </div>
            </div>

            <button
              className="auth-show-password"
              type="button"
              onClick={() =>
                setShowPassword(
                  (current) => !current
                )
              }
            >
              {showPassword
                ? "Hide passwords"
                : "Show passwords"}
            </button>

            <button
              className="auth-submit"
              type="submit"
              disabled={loading}
            >
              {loading
                ? "Resetting Password..."
                : "Reset Password"}

              {!loading && <span>-&gt;</span>}
            </button>
          </>
        )}

        {success && (
          <Link
            className="auth-submit auth-action-link"
            to="/login"
          >
            Continue to Sign In
            <span>-&gt;</span>
          </Link>
        )}

        {!token && (
          <Link
            className="auth-submit auth-action-link"
            to="/forgot-password"
          >
            Request New Reset Link
            <span>-&gt;</span>
          </Link>
        )}
      </form>
    </AuthLayout>
  );
}