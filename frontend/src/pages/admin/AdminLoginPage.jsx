import React, { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./AdminAuth.css";

const POLL_INTERVAL_MS = 2000;
const APPROVAL_SIGNAL_KEY = "parkNovaAdminApprovalSignal";

export default function AdminLoginPage() {
  const navigate = useNavigate();

  const pollTimerRef = useRef(null);
  const pollingRef = useRef(false);
  const activeRequestRef = useRef({
    requestId: "",
    pollToken: "",
  });

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [loading, setLoading] = useState(false);
  const [waiting, setWaiting] = useState(false);

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const stopPolling = () => {
    if (pollTimerRef.current) {
      clearTimeout(pollTimerRef.current);
      pollTimerRef.current = null;
    }

    pollingRef.current = false;
  };

  const clearActiveRequest = () => {
    activeRequestRef.current = {
      requestId: "",
      pollToken: "",
    };
  };

  const finishApprovedLogin = (data) => {
    const token = data?.token;

    const user = {
      userId: data?.userId,
      id: data?.userId,
      name: data?.name,
      email: data?.email,
      role: data?.role,
      emailVerified: data?.emailVerified,
    };

    if (!token || !user.email || !user.role) {
      throw new Error("Approved login response is incomplete.");
    }

    const role = String(user.role)
      .replace(/^ROLE_/i, "")
      .toUpperCase();

    if (role !== "ADMIN") {
      throw new Error(
        "This account is not authorized for admin access."
      );
    }

    localStorage.setItem("parkNovaToken", token);
    localStorage.setItem(
      "parkNovaUser",
      JSON.stringify(user)
    );

    stopPolling();
    clearActiveRequest();

    navigate("/admin", {
      replace: true,
    });
  };

  const scheduleNextPoll = () => {
    if (!activeRequestRef.current.requestId) {
      return;
    }

    clearTimeout(pollTimerRef.current);

    pollTimerRef.current = setTimeout(() => {
      checkApproval();
    }, POLL_INTERVAL_MS);
  };

  const checkApproval = async () => {
    const {
      requestId,
      pollToken,
    } = activeRequestRef.current;

    if (
      !requestId ||
      !pollToken ||
      pollingRef.current
    ) {
      return;
    }

    pollingRef.current = true;

    try {
      const response = await api.post(
        "/api/auth/admin/poll",
        {
          requestId,
          pollToken,
        }
      );

      const data = response?.data || {};

      const status = String(
        data.status || ""
      ).toUpperCase();

      if (status === "APPROVED") {
        if (!data.token) {
          throw new Error(
            "Approved login response did not contain a session token."
          );
        }

        finishApprovedLogin(data);
        return;
      }

      if (status === "DENIED") {
        stopPolling();
        clearActiveRequest();

        setWaiting(false);
        setMessage("");
        setError(
          "Login denied from the approval email."
        );

        return;
      }

      if (status === "EXPIRED") {
        stopPolling();
        clearActiveRequest();

        setWaiting(false);
        setMessage("");
        setError(
          "This admin login request has expired. Please sign in again."
        );

        return;
      }
    } catch (err) {
      const status = err?.response?.status;

      const serverMessage =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        "";

      if (
        status === 400 ||
        status === 404 ||
        status === 410
      ) {
        stopPolling();
        clearActiveRequest();

        setWaiting(false);
        setMessage("");
        setError(
          serverMessage ||
            "This admin login request is no longer valid. Please sign in again."
        );

        return;
      }

      console.error(
        "Admin approval polling failed:",
        err
      );
    } finally {
      pollingRef.current = false;

      if (activeRequestRef.current.requestId) {
        scheduleNextPoll();
      }
    }
  };

  const startPolling = (
    requestId,
    pollToken
  ) => {
    stopPolling();

    activeRequestRef.current = {
      requestId,
      pollToken,
    };

    checkApproval();
  };

  useEffect(() => {
    const handleStorage = (event) => {
      if (
        event.key !== APPROVAL_SIGNAL_KEY ||
        !event.newValue ||
        !activeRequestRef.current.requestId
      ) {
        return;
      }

      try {
        const signal = JSON.parse(
          event.newValue
        );

        if (
          signal?.requestId &&
          signal.requestId !==
            activeRequestRef.current.requestId
        ) {
          return;
        }
      } catch {
        // Even if the signal cannot be parsed,
        // immediately check the server.
      }

      clearTimeout(pollTimerRef.current);
      checkApproval();
    };

    window.addEventListener(
      "storage",
      handleStorage
    );

    return () => {
      window.removeEventListener(
        "storage",
        handleStorage
      );

      stopPolling();
    };
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (loading || waiting) {
      return;
    }

    setError("");
    setMessage("");
    setLoading(true);

    localStorage.removeItem(
      "parkNovaToken"
    );

    localStorage.removeItem(
      "parkNovaUser"
    );

    try {
      const response = await api.post(
        "/api/auth/admin/login",
        {
          email: email.trim(),
          password,
        }
      );

      const data = response?.data || {};

      if (
        !data.requestId ||
        !data.pollToken
      ) {
        throw new Error(
          "Admin approval request was not created."
        );
      }

      setWaiting(true);
      setPassword("");

      setMessage(
        "Approval email sent. Confirm this login from your registered admin email."
      );

      startPolling(
        data.requestId,
        data.pollToken
      );
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          "Invalid email or password"
      );
    } finally {
      setLoading(false);
    }
  };

  const cancelWaiting = () => {
    stopPolling();
    clearActiveRequest();

    setWaiting(false);
    setMessage("");
    setError("");
  };

  return (
    <main className="pn-admin-auth-page">
      <section className="pn-admin-auth-card">
        <Link
          to="/"
          className="pn-admin-auth-logo"
          aria-label="Park Nova home"
        >
          <img
            src="/images/park-nova-logo.png"
            alt="Park Nova"
          />
        </Link>

        <div className="pn-admin-auth-badge">
          <span />
          SECURE ADMIN ACCESS
        </div>

        {!waiting ? (
          <>
            <h1>Admin Dashboard</h1>

            <p className="pn-admin-auth-copy">
              Sign in with your administrator account.
              Every login requires confirmation from the
              registered admin email.
            </p>

            <form
              onSubmit={handleSubmit}
              className="pn-admin-auth-form"
            >
              <label>
                <span>Admin email</span>

                <input
                  type="email"
                  autoComplete="username"
                  value={email}
                  onChange={(event) =>
                    setEmail(
                      event.target.value
                    )
                  }
                  placeholder="admin@example.com"
                  required
                  disabled={loading}
                />
              </label>

              <label>
                <span>Password</span>

                <input
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) =>
                    setPassword(
                      event.target.value
                    )
                  }
                  placeholder="Enter your password"
                  required
                  disabled={loading}
                />
              </label>

              {error && (
                <div
                  className="pn-admin-auth-alert error"
                  role="alert"
                >
                  {error}
                </div>
              )}

              <button
                type="submit"
                className="pn-admin-auth-primary"
                disabled={loading}
              >
                {loading
                  ? "Verifying..."
                  : "Continue Secure Login"}
              </button>
            </form>
          </>
        ) : (
          <div className="pn-admin-waiting">
            <div className="pn-admin-waiting-icon">
              <span />
            </div>

            <div className="pn-admin-auth-badge centered">
              EMAIL APPROVAL REQUIRED
            </div>

            <h1>
              Waiting for approval
            </h1>

            <p>
              We sent a login confirmation to the
              registered admin email. Choose{" "}
              <strong>I am Login</strong> to approve
              this browser or{" "}
              <strong>I am Not Login</strong> to deny
              it.
            </p>

            <div className="pn-admin-waiting-status">
              <span className="pn-admin-waiting-pulse" />
              Waiting for email approval...
            </div>

            {message && (
              <div className="pn-admin-auth-alert info">
                {message}
              </div>
            )}

            {error && (
              <div
                className="pn-admin-auth-alert error"
                role="alert"
              >
                {error}
              </div>
            )}

            <button
              type="button"
              className="pn-admin-auth-secondary"
              onClick={cancelWaiting}
            >
              Cancel login request
            </button>
          </div>
        )}

        <div className="pn-admin-auth-security">
          <span>◆</span>
          Admin session is created only after email
          approval.
        </div>
      </section>
    </main>
  );
}