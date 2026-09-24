import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import AuthLayout from "../../components/auth/AuthLayout";
import api from "../../services/api";

const getErrorMessage = (error) =>
  error?.response?.data?.message ||
  "Unable to create your account. Please try again.";

export default function RegisterPage() {
  const navigate = useNavigate();

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [form, setForm] = useState({
    name: "",
    email: "",
    phone: "",
    password: "",
    confirmPassword: "",
  });

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));

    setError("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (form.password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }

    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      await api.post("/api/auth/register", {
        name: form.name.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
        password: form.password,
      });

      navigate(
        `/login?registered=1&email=${encodeURIComponent(
          form.email.trim()
        )}`,
        { replace: true }
      );
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    const baseUrl =
      import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

    window.location.href =
      `${baseUrl}/oauth2/authorization/google`;
  };

  return (
    <AuthLayout
      eyebrow="JOIN PARK NOVA"
      title="Create your account"
      description="One account for parking discovery, bookings, payments and smart access."
      footerText="Already have an account?"
      footerLinkText="Sign in"
      footerLinkTo="/login"
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        {error && (
          <div className="auth-message auth-message-error" role="alert">
            {error}
          </div>
        )}

        <div className="auth-field">
          <label htmlFor="register-name">Full name</label>

          <div className="auth-input-wrap">
            <span className="auth-field-icon">U</span>

            <input
              id="register-name"
              type="text"
              name="name"
              value={form.name}
              onChange={handleChange}
              placeholder="Enter your full name"
              autoComplete="name"
              required
              disabled={loading}
            />
          </div>
        </div>

        <div className="auth-field">
          <label htmlFor="register-email">Email address</label>

          <div className="auth-input-wrap">
            <span className="auth-field-icon">@</span>

            <input
              id="register-email"
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              placeholder="you@example.com"
              autoComplete="email"
              required
              disabled={loading}
            />
          </div>
        </div>

        <div className="auth-field">
          <label htmlFor="register-phone">Phone number</label>

          <div className="auth-input-wrap">
            <span className="auth-field-icon">#</span>

            <input
              id="register-phone"
              type="tel"
              name="phone"
              value={form.phone}
              onChange={handleChange}
              placeholder="Enter your phone number"
              autoComplete="tel"
              required
              disabled={loading}
            />
          </div>
        </div>

        <div className="auth-form-grid">
          <div className="auth-field">
            <label htmlFor="register-password">Password</label>

            <div className="auth-input-wrap">
              <span className="auth-field-icon">*</span>

              <input
                id="register-password"
                type={showPassword ? "text" : "password"}
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="Minimum 8 characters"
                autoComplete="new-password"
                minLength={8}
                required
                disabled={loading}
              />
            </div>
          </div>

          <div className="auth-field">
            <label htmlFor="confirm-password">
              Confirm password
            </label>

            <div className="auth-input-wrap">
              <span className="auth-field-icon">*</span>

              <input
                id="confirm-password"
                type={showPassword ? "text" : "password"}
                name="confirmPassword"
                value={form.confirmPassword}
                onChange={handleChange}
                placeholder="Repeat password"
                autoComplete="new-password"
                minLength={8}
                required
                disabled={loading}
              />
            </div>
          </div>
        </div>

        <button
          className="auth-show-password"
          type="button"
          onClick={() =>
            setShowPassword((current) => !current)
          }
        >
          {showPassword ? "Hide passwords" : "Show passwords"}
        </button>

        <label className="auth-check">
          <input type="checkbox" required disabled={loading} />

          <span>
            I agree to the Park Nova terms and privacy requirements.
          </span>
        </label>

        <button
          className="auth-submit"
          type="submit"
          disabled={loading}
        >
          {loading ? "Creating Account..." : "Create Account"}
          {!loading && <span>-&gt;</span>}
        </button>

        <div className="auth-divider">
          <span>or sign up with</span>
        </div>

        <button
          className="auth-google"
          type="button"
          onClick={handleGoogleLogin}
          disabled={loading}
        >
          <span className="google-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path
                fill="#4285F4"
                d="M21.6 12.23c0-.71-.06-1.4-.18-2.07H12v3.92h5.38a4.6 4.6 0 0 1-2 3.02v2.54h3.24c1.9-1.75 2.98-4.33 2.98-7.41Z"
              />
              <path
                fill="#34A853"
                d="M12 22c2.7 0 4.97-.9 6.63-2.36l-3.24-2.54c-.9.6-2.05.96-3.39.96-2.61 0-4.82-1.76-5.61-4.13H3.04v2.62A10 10 0 0 0 12 22Z"
              />
              <path
                fill="#FBBC05"
                d="M6.39 13.93A6.02 6.02 0 0 1 6.07 12c0-.67.12-1.32.32-1.93V7.45H3.04A10 10 0 0 0 2 12c0 1.61.39 3.14 1.04 4.55l3.35-2.62Z"
              />
              <path
                fill="#EA4335"
                d="M12 5.94c1.47 0 2.79.51 3.83 1.5l2.87-2.88A9.64 9.64 0 0 0 12 2a10 10 0 0 0-8.96 5.45l3.35 2.62C7.18 7.7 9.39 5.94 12 5.94Z"
              />
            </svg>
          </span>

          Continue with Google
        </button>
      </form>
    </AuthLayout>
  );
}