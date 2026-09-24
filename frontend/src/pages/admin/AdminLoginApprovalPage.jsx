import React, { useEffect, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api from "../../services/api";
import "./AdminAuth.css";

export default function AdminLoginApprovalPage() {
  const [searchParams] = useSearchParams();
  const sentRef = useRef(false);

  const [state, setState] = useState({
    loading: true,
    status: "",
    message: "Confirming admin login...",
  });

  useEffect(() => {
    if (sentRef.current) return;
    sentRef.current = true;

    const token = searchParams.get("token");
    const action = String(
      searchParams.get("action") || ""
    ).toLowerCase();

    if (
      !token ||
      !["approve", "deny"].includes(action)
    ) {
      setState({
        loading: false,
        status: "error",
        message:
          "This admin login confirmation link is invalid.",
      });
      return;
    }

    const submitDecision = async () => {
      try {
        const response = await api.get(
          "/api/auth/admin/decision",
          {
            params: {
              token,
              action,
            },
          }
        );

        if (action === "approve") {
          setState({
            loading: false,
            status: "approved",
            message:
              "Login approved. Returning to Park Nova...",
          });

          /*
           * The JWT remains restricted to the original
           * waiting browser. This email tab only approves
           * the request.
           */
          setTimeout(() => {
            try {
              if (window.opener && !window.opener.closed) {
                window.opener.focus();
              }

              window.close();
            } catch {
              // Browser may block closing externally opened tabs.
            }
          }, 350);

          return;
        }

        setState({
          loading: false,
          status: "denied",
          message:
            response?.data?.message ||
            "Admin login request denied.",
        });
      } catch (error) {
        setState({
          loading: false,
          status: "error",
          message:
            error?.response?.data?.message ||
            error?.response?.data?.error ||
            "This login confirmation link is invalid, expired, or already used.",
        });
      }
    };

    submitDecision();
  }, [searchParams]);

  return (
    <main className="pn-admin-auth-page">
      <section className="pn-admin-auth-card pn-admin-decision-card">
        <div className="pn-admin-auth-logo">
          <img
            src="/images/park-nova-logo.png"
            alt="Park Nova"
          />
        </div>

        {state.loading ? (
          <>
            <div className="pn-admin-decision-loader" />

            <h1>Confirming login</h1>

            <p>
              Securely confirming your Park Nova
              administrator login...
            </p>
          </>
        ) : (
          <>
            <div
              className={`pn-admin-decision-icon ${state.status}`}
            >
              {state.status === "approved"
                ? "✓"
                : state.status === "denied"
                  ? "×"
                  : "!"}
            </div>

            <h1>
              {state.status === "approved"
                ? "Approved"
                : state.status === "denied"
                  ? "Login denied"
                  : "Request unavailable"}
            </h1>

            <p>{state.message}</p>

            {state.status === "approved" && (
              <button
                type="button"
                className="pn-admin-auth-primary pn-admin-return-button"
                onClick={() => {
                  try {
                    if (
                      window.opener &&
                      !window.opener.closed
                    ) {
                      window.opener.focus();
                    }

                    window.close();
                  } catch {
                    // Browser can block window.close().
                  }
                }}
              >
                Return to Admin Dashboard
              </button>
            )}

            {state.status === "denied" && (
              <div className="pn-admin-auth-alert error">
                No admin session has been created.
                You can close this page.
              </div>
            )}
          </>
        )}

        <div className="pn-admin-auth-security">
          <span>◆</span>
          Park Nova secure administrator verification
        </div>
      </section>
    </main>
  );
}