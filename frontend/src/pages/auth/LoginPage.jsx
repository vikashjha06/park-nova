import React, { useEffect, useState } from "react";
import {
  Link,
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import AuthLayout from "../../components/auth/AuthLayout";
import api from "../../services/api";

const getErrorMessage = (error) =>
  error?.response?.data?.message ||
  error?.message ||
  "Unable to sign in. Please check your details and try again.";

const getOAuthErrorMessage = (code) => {
  switch (code) {
    case "google_email_missing":
      return "Google did not provide an email address.";

    case "google_user_not_found":
      return "Google sign-in could not find your Park Nova account.";

    case "account_disabled":
      return "This Park Nova account is disabled.";

    default:
      return code
        ? "Google sign-in could not be completed. Please try again."
        : "";
  }
};

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const registered =
    searchParams.get("registered") === "1";

  const registeredEmail =
    searchParams.get("email") || "";

  const oauthError =
    searchParams.get("error");

  const sessionExpired =
    searchParams.get("session") === "expired";

  const [showPassword, setShowPassword] =
    useState(false);

  const [loading, setLoading] =
    useState(false);

  const [resending, setResending] =
    useState(false);

  const [message, setMessage] =
    useState("");

  const [error, setError] =
    useState("");

  const [form, setForm] = useState({
    email: registeredEmail,
    password: "",
  });

  useEffect(() => {
    if (registered) {
      setMessage(
        "Account created successfully. Please verify your email before signing in."
      );
    } else if (sessionExpired) {
      setMessage(
        "Your session has expired. Please sign in again."
      );
    }
  }, [registered, sessionExpired]);

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

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await api.post(
        "/api/auth/login",
        {
          email: form.email.trim(),
          password: form.password,
        }
      );

      const data = response.data;

      if (!data?.token) {
        throw new Error(
          "Login token was not returned."
        );
      }

      const role = String(
        data.role || "USER"
      )
        .replace(/^ROLE_/i, "")
        .toUpperCase();

      localStorage.setItem(
        "parkNovaToken",
        data.token
      );

      localStorage.setItem(
        "parkNovaUser",
        JSON.stringify({
          userId: data.userId,
          name: data.name,
          email: data.email,
          role,
          emailVerified:
            data.emailVerified,
        })
      );

      if (role === "ADMIN") {
        navigate("/admin", {
          replace: true,
        });

        return;
      }

      const requestedPath =
        location.state?.from;

      const destination =
        requestedPath &&
        requestedPath !== "/admin"
          ? requestedPath
          : "/";

      navigate(destination, {
        replace: true,
      });
    } catch (requestError) {
      localStorage.removeItem(
        "parkNovaToken"
      );

      localStorage.removeItem(
        "parkNovaUser"
      );

      setError(
        getErrorMessage(requestError)
      );
    } finally {
      setLoading(false);
    }
  };

  const resendVerification = async () => {
    const email = form.email.trim();

    if (!email) {
      setError(
        "Enter your email address first."
      );

      return;
    }

    try {
      setResending(true);
      setError("");
      setMessage("");

      const response = await api.post(
        "/api/auth/resend-verification",
        null,
        {
          params: { email },
        }
      );

      setMessage(
        response.data?.message ||
          "Verification email sent successfully."
      );
    } catch (requestError) {
      setError(
        requestError?.response?.data?.message ||
          "Unable to resend the verification email."
      );
    } finally {
      setResending(false);
    }
  };

  const handleGoogleLogin = () => {
    const baseUrl =
      import.meta.env.VITE_API_BASE_URL ||
      "http://localhost:8080";

    window.location.href =
      `${baseUrl}/oauth2/authorization/google`;
  };

  const displayedOAuthError =
    getOAuthErrorMessage(oauthError);

  return (
    <AuthLayout
      eyebrow="WELCOME BACK"
      title="Sign in to Park Nova"
      description="Access your bookings, wallet and smart parking experience."
      footerText="New to Park Nova?"
      footerLinkText="Create account"
      footerLinkTo="/register"
    >
      <form
        className="auth-form"
        onSubmit={handleSubmit}
      >
        {(error || displayedOAuthError) && (
          <div
            className="auth-message auth-message-error"
            role="alert"
          >
            {error || displayedOAuthError}
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
          <label htmlFor="login-email">
            Email address
          </label>

          <div className="auth-input-wrap">
            <span className="auth-field-icon">
              @
            </span>

            <input
              id="login-email"
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              placeholder="you@example.com"
              autoComplete="email"
              required
              disabled={loading || resending}
            />
          </div>
        </div>

        <div className="auth-field">
          <div className="auth-label-row">
            <label htmlFor="login-password">
              Password
            </label>

            <Link to="/forgot-password">
              Forgot password?
            </Link>
          </div>

          <div className="auth-input-wrap">
            <span className="auth-field-icon">
              *
            </span>

            <input
              id="login-password"
              type={
                showPassword
                  ? "text"
                  : "password"
              }
              name="password"
              value={form.password}
              onChange={handleChange}
              placeholder="Enter your password"
              autoComplete="current-password"
              required
              disabled={loading || resending}
            />

            <button
              className="auth-password-toggle"
              type="button"
              onClick={() =>
                setShowPassword(
                  (current) => !current
                )
              }
              disabled={loading || resending}
            >
              {showPassword
                ? "Hide"
                : "Show"}
            </button>
          </div>
        </div>

        <label className="auth-check">
          <input
            type="checkbox"
            disabled={loading || resending}
          />

          <span>
            Keep me signed in on this device
          </span>
        </label>

        <button
          className="auth-submit"
          type="submit"
          disabled={loading || resending}
        >
          {loading
            ? "Signing In..."
            : "Sign In"}

          {!loading && <span>-&gt;</span>}
        </button>

        <button
          className="auth-google"
          type="button"
          onClick={resendVerification}
          disabled={loading || resending}
        >
          {resending
            ? "Sending Verification Email..."
            : "Resend Verification Email"}
        </button>

        <div className="auth-divider">
          <span>or continue with</span>
        </div>

        <button
          className="auth-google"
          type="button"
          onClick={handleGoogleLogin}
          disabled={loading || resending}
        >
          <span
            className="google-mark"
            aria-hidden="true"
          >
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

        <p className="auth-security-note">
          Your account is protected by Park Nova secure authentication.
        </p>
      </form>
    </AuthLayout>
  );
}