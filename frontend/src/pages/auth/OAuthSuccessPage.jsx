import React, {
  useEffect,
  useRef,
  useState,
} from "react";
import {
  Link,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import AuthLayout from "../../components/auth/AuthLayout";
import api from "../../services/api";

export default function OAuthSuccessPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const started = useRef(false);

  const [error, setError] = useState("");

  useEffect(() => {
    if (started.current) return;
    started.current = true;

    const finishGoogleLogin = async () => {
      const token = searchParams.get("token");

      if (!token) {
        setError(
          "Google sign-in token was not returned."
        );
        return;
      }

      try {
        localStorage.setItem(
          "parkNovaToken",
          token
        );

        /*
         * Use the authenticated profile endpoint
         * to obtain trusted current user details.
         */
        const response = await api.get(
          "/api/users/me"
        );

        const user =
          response.data?.user ||
          response.data;

        const role =
          String(
            user?.role || "USER"
          )
            .replace(/^ROLE_/i, "")
            .toUpperCase();

        localStorage.setItem(
          "parkNovaUser",
          JSON.stringify({
            userId:
              user?.id ||
              user?.userId ||
              "",
            name: user?.name || "",
            email: user?.email || "",
            role,
            emailVerified:
              user?.emailVerified ??
              true,
          })
        );

        if (role === "ADMIN") {
          navigate("/admin", {
            replace: true,
          });
        } else {
          navigate("/", {
            replace: true,
          });
        }
      } catch (err) {
        localStorage.removeItem(
          "parkNovaToken"
        );
        localStorage.removeItem(
          "parkNovaUser"
        );

        setError(
          err?.response?.data?.message ||
            "Google sign-in could not be completed."
        );
      }
    };

    finishGoogleLogin();
  }, [navigate, searchParams]);

  return (
    <AuthLayout
      eyebrow="GOOGLE SIGN IN"
      title={
        error
          ? "Sign in failed"
          : "Signing you in"
      }
      description="Park Nova is securely completing your Google sign-in."
      footerText="Prefer another method?"
      footerLinkText="Sign in"
      footerLinkTo="/login"
    >
      <div className="auth-form">
        {error ? (
          <>
            <div
              className="auth-message auth-message-error"
              role="alert"
            >
              {error}
            </div>

            <Link
              className="auth-submit auth-action-link"
              to="/login"
            >
              Back to Sign In
              <span>-&gt;</span>
            </Link>
          </>
        ) : (
          <div
            className="auth-message auth-message-success"
            role="status"
          >
            Completing Google sign-in...
          </div>
        )}
      </div>
    </AuthLayout>
  );
}