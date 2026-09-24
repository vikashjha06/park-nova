import React, { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import AuthLayout from "../../components/auth/AuthLayout";
import api from "../../services/api";

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const requestStarted = useRef(false);

  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState(
    "Verifying your email address..."
  );

  useEffect(() => {
    if (requestStarted.current) {
      return;
    }

    requestStarted.current = true;

    const verifyEmail = async () => {
      if (!token) {
        setStatus("error");
        setMessage("This verification link is invalid.");
        return;
      }

      try {
        const response = await api.get("/api/auth/verify-email", {
          params: { token },
        });

        setStatus("success");
        setMessage(
          response.data?.message || "Email verified successfully."
        );
      } catch (error) {
        setStatus("error");
        setMessage(
          error?.response?.data?.message ||
            "Email verification failed. The link may be invalid or expired."
        );
      }
    };

    verifyEmail();
  }, [token]);

  return (
    <AuthLayout
      eyebrow="EMAIL VERIFICATION"
      title={
        status === "success"
          ? "Email verified"
          : status === "error"
          ? "Verification failed"
          : "Verifying your email"
      }
      description="Park Nova verifies your email to keep your account secure."
      footerText="Already verified?"
      footerLinkText="Sign in"
      footerLinkTo="/login"
    >
      <div className="auth-form">
        <div
          className={`auth-message ${
            status === "error"
              ? "auth-message-error"
              : "auth-message-success"
          }`}
          role="status"
        >
          {message}
        </div>

        {status === "loading" && (
          <p className="auth-security-note">
            Please wait while we verify your account.
          </p>
        )}

        {status === "success" && (
          <Link className="auth-submit auth-action-link" to="/login">
            Continue to Sign In
            <span>-&gt;</span>
          </Link>
        )}

        {status === "error" && (
          <>
            <p className="auth-security-note">
              If your verification link expired, you can request a new
              verification email.
            </p>

            <Link className="auth-submit auth-action-link" to="/login">
              Back to Sign In
              <span>-&gt;</span>
            </Link>
          </>
        )}
      </div>
    </AuthLayout>
  );
}