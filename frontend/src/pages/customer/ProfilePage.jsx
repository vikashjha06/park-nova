import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./ProfilePage.css";

function getInitials(name) {
  if (!name) return "PN";

  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join("");
}

function formatMemberSince(value) {
  if (!value) return "Not available";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Not available";
  }

  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(date);
}

function formatPhone(value) {
  if (!value) return "Not provided";

  const phone = String(value).replace(/\s+/g, "");

  if (/^\d{10}$/.test(phone)) {
    return `+91 ${phone.slice(0, 5)} ${phone.slice(5)}`;
  }

  return value;
}

export default function ProfilePage() {
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [editPhone, setEditPhone] = useState("");
  const [saving, setSaving] = useState(false);
  const [editError, setEditError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    let active = true;

    async function loadProfile() {
      try {
        setLoading(true);
        setError("");

        const response = await api.get("/api/users/me");

        if (active) {
          setProfile(response.data);
        }
      } catch (err) {
        if (!active) return;

        const status = err?.response?.status;

        if (status === 401 || status === 403) {
          localStorage.removeItem("parkNovaToken");
          localStorage.removeItem("parkNovaUser");
          navigate("/login", { replace: true });
          return;
        }

        setError(
          err?.response?.data?.message ||
            "We could not load your profile right now."
        );
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadProfile();

    const handleEdit = () => {
    setEditName(profile?.name || "");
    setEditPhone(profile?.phone || "");
    setEditError("");
    setSuccessMessage("");
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    setEditName(profile?.name || "");
    setEditPhone(profile?.phone || "");
    setEditError("");
    setIsEditing(false);
  };

  const handleSaveProfile = async (event) => {
    event.preventDefault();

    const cleanName = editName.trim();
    const cleanPhone = editPhone.replace(/[\s-]/g, "");

    if (cleanName.length < 2) {
      setEditError("Name must contain at least 2 characters.");
      return;
    }

    if (!/^\d{10}$/.test(cleanPhone)) {
      setEditError("Phone number must contain exactly 10 digits.");
      return;
    }

    try {
      setSaving(true);
      setEditError("");
      setSuccessMessage("");

      const response = await api.patch("/api/users/me", {
        name: cleanName,
        phone: cleanPhone,
      });

      const updatedProfile = response.data.profile;

      setProfile(updatedProfile);
      setEditName(updatedProfile.name || "");
      setEditPhone(updatedProfile.phone || "");
      setIsEditing(false);

      setSuccessMessage(
        response.data.message || "Profile updated successfully"
      );

      try {
        const storedUser = JSON.parse(
          localStorage.getItem("parkNovaUser") || "{}"
        );

        localStorage.setItem(
          "parkNovaUser",
          JSON.stringify({
            ...storedUser,
            name: updatedProfile.name,
            email: updatedProfile.email,
            role: updatedProfile.role,
            emailVerified: updatedProfile.emailVerified,
            userId: updatedProfile.userId,
          })
        );
      } catch {
        // Backend update already succeeded.
      }
    } catch (err) {
      const status = err?.response?.status;

      if (status === 401 || status === 403) {
        localStorage.removeItem("parkNovaToken");
        localStorage.removeItem("parkNovaUser");
        navigate("/login", { replace: true });
        return;
      }

      setEditError(
        err?.response?.data?.message ||
          "We could not update your profile right now."
      );
    } finally {
      setSaving(false);
    }
  };

  return () => {
      active = false;
    };
  }, [navigate]);

  const initials = useMemo(
    () => getInitials(profile?.name),
    [profile?.name]
  );

  const handleLogout = () => {
    localStorage.removeItem("parkNovaToken");
    localStorage.removeItem("parkNovaUser");
    navigate("/login", { replace: true });
  };

  const handleEdit = () => {
    setEditName(profile?.name || "");
    setEditPhone(profile?.phone || "");
    setEditError("");
    setSuccessMessage("");
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    setEditName(profile?.name || "");
    setEditPhone(profile?.phone || "");
    setEditError("");
    setIsEditing(false);
  };

  const handleSaveProfile = async (event) => {
    event.preventDefault();

    const cleanName = editName.trim();
    const cleanPhone = editPhone.replace(/[\s-]/g, "");

    if (cleanName.length < 2) {
      setEditError("Name must contain at least 2 characters.");
      return;
    }

    if (!/^\d{10}$/.test(cleanPhone)) {
      setEditError("Phone number must contain exactly 10 digits.");
      return;
    }

    try {
      setSaving(true);
      setEditError("");
      setSuccessMessage("");

      const response = await api.patch("/api/users/me", {
        name: cleanName,
        phone: cleanPhone,
      });

      const updatedProfile = response.data.profile;

      setProfile(updatedProfile);
      setEditName(updatedProfile.name || "");
      setEditPhone(updatedProfile.phone || "");
      setIsEditing(false);

      setSuccessMessage(
        response.data.message || "Profile updated successfully"
      );

      try {
        const storedUser = JSON.parse(
          localStorage.getItem("parkNovaUser") || "{}"
        );

        localStorage.setItem(
          "parkNovaUser",
          JSON.stringify({
            ...storedUser,
            name: updatedProfile.name,
            email: updatedProfile.email,
            role: updatedProfile.role,
            emailVerified: updatedProfile.emailVerified,
            userId: updatedProfile.userId,
          })
        );
      } catch {
        // Backend update already succeeded.
      }
    } catch (err) {
      const status = err?.response?.status;

      if (status === 401 || status === 403) {
        localStorage.removeItem("parkNovaToken");
        localStorage.removeItem("parkNovaUser");
        navigate("/login", { replace: true });
        return;
      }

      setEditError(
        err?.response?.data?.message ||
          "We could not update your profile right now."
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <main className="pn-profile-page">
      <header className="pn-profile-header">
        <Link to="/" className="pn-profile-brand" aria-label="Park Nova home">
          <img src="/images/park-nova-logo.png" alt="Park Nova" />
        </Link>

        <nav className="pn-profile-header-actions" aria-label="Profile navigation">
          <Link to="/" className="pn-profile-home-button">
            <span aria-hidden="true">←</span>
            Home
          </Link>

          <button
            type="button"
            className="pn-profile-logout-button"
            onClick={handleLogout}
          >
            Log out
          </button>
        </nav>
      </header>

      <section className="pn-profile-shell">
        <div className="pn-profile-intro">
          <div>
            <p className="pn-profile-eyebrow">
              <span />
              MY ACCOUNT
            </p>

            <h1>My Profile</h1>

            <p className="pn-profile-subtitle">
              Your Park Nova account details, verification and membership
              information in one place.
            </p>
          </div>

          {!loading && profile && (
            <div
              className={`pn-profile-account-pill ${
                profile.enabled ? "is-active" : "is-disabled"
              }`}
            >
              <span />
              {profile.enabled ? "Account Active" : "Account Disabled"}
            </div>
          )}
        </div>

        {loading && (
          <section className="pn-profile-loading" aria-live="polite">
            <div className="pn-profile-loading-spinner" />
            <div>
              <strong>Loading your profile</strong>
              <p>Getting your Park Nova account information...</p>
            </div>
          </section>
        )}

        {!loading && error && (
          <section className="pn-profile-error" role="alert">
            <div className="pn-profile-error-icon">!</div>

            <div>
              <h2>Profile unavailable</h2>
              <p>{error}</p>

              <button
                type="button"
                onClick={() => window.location.reload()}
              >
                Try again
              </button>
            </div>
          </section>
        )}

        {!loading && !error && profile && (
          <div className="pn-profile-layout">
            <aside className="pn-profile-summary-card">
              <div className="pn-profile-avatar">
                <span>{initials}</span>
                {profile.emailVerified && (
                  <span
                    className="pn-profile-avatar-check"
                    title="Verified account"
                    aria-label="Verified account"
                  >
                    ✓
                  </span>
                )}
              </div>

              <div className="pn-profile-summary-copy">
                <p className="pn-profile-summary-label">
                  PARK NOVA MEMBER
                </p>

                <h2>{profile.name || "Park Nova User"}</h2>

                <p className="pn-profile-summary-email">
                  {profile.email || "Email not available"}
                </p>
              </div>

              <div className="pn-profile-summary-divider" />

              <div className="pn-profile-summary-status">
                <div>
                  <span>Account type</span>
                  <strong>{profile.role || "USER"}</strong>
                </div>

                <div>
                  <span>Member since</span>
                  <strong>{formatMemberSince(profile.createdAt)}</strong>
                </div>
              </div>

              <div className="pn-profile-quick-links">
                <Link to="/bookings">
                  <span className="pn-profile-link-icon">B</span>
                  <span>
                    <strong>My Bookings</strong>
                    <small>View parking activity</small>
                  </span>
                  <span className="pn-profile-arrow">→</span>
                </Link>

                <Link to="/wallet">
                  <span className="pn-profile-link-icon">W</span>
                  <span>
                    <strong>Wallet</strong>
                    <small>Balance & transactions</small>
                  </span>
                  <span className="pn-profile-arrow">→</span>
                </Link>

                <Link to="/monthly-pass">
                  <span className="pn-profile-link-icon">P</span>
                  <span>
                    <strong>Monthly Pass</strong>
                    <small>Manage your parking pass</small>
                  </span>
                  <span className="pn-profile-arrow">→</span>
                </Link>
              </div>
            </aside>

            <section className="pn-profile-details">
              <div className="pn-profile-section-heading">
                <div>
                  <p>PERSONAL INFORMATION</p>
                  <h2>Account details</h2>
                </div>

                <div className="pn-profile-heading-actions">
                  {!isEditing && (
                    <button
                      type="button"
                      className="pn-profile-edit-button"
                      onClick={handleEdit}
                    >
                      <span aria-hidden="true">✎</span>
                      Edit Profile
                    </button>
                  )}

                  <span className="pn-profile-secure-label">
                    <span>✓</span>
                    Secure account
                  </span>
                </div>
              </div>

              <div className="pn-profile-detail-grid">
                <article className="pn-profile-detail-card">
                  <div className="pn-profile-detail-icon">N</div>

                  <div>
                    <span>Full name</span>

                    {isEditing ? (
                      <input
                        className="pn-profile-edit-input"
                        type="text"
                        value={editName}
                        maxLength={80}
                        autoComplete="name"
                        onChange={(event) => setEditName(event.target.value)}
                        aria-label="Full name"
                      />
                    ) : (
                      <strong>{profile.name || "Not provided"}</strong>
                    )}

                    <small>Your registered account name</small>
                  </div>
                </article>

                <article className="pn-profile-detail-card">
                  <div className="pn-profile-detail-icon">@</div>

                  <div>
                    <span>Email address</span>
                    <strong>{profile.email || "Not provided"}</strong>

                    <small
                      className={
                        profile.emailVerified
                          ? "pn-profile-verified-text"
                          : "pn-profile-unverified-text"
                      }
                    >
                      {profile.emailVerified
                        ? "✓ Email verified"
                        : "Email verification pending"}
                    </small>
                  </div>
                </article>

                <article className="pn-profile-detail-card">
                  <div className="pn-profile-detail-icon">T</div>

                  <div>
                    <span>Phone number</span>

                    {isEditing ? (
                      <input
                        className="pn-profile-edit-input"
                        type="tel"
                        inputMode="numeric"
                        value={editPhone}
                        maxLength={10}
                        autoComplete="tel"
                        onChange={(event) =>
                          setEditPhone(
                            event.target.value.replace(/\D/g, "").slice(0, 10)
                          )
                        }
                        aria-label="Phone number"
                      />
                    ) : (
                      <strong>{formatPhone(profile.phone)}</strong>
                    )}

                    <small>Registered contact number</small>
                  </div>
                </article>

                <article className="pn-profile-detail-card">
                  <div className="pn-profile-detail-icon">A</div>

                  <div>
                    <span>Sign-in method</span>
                    <strong>
                      {profile.authProvider === "GOOGLE"
                        ? "Google"
                        : "Email & Password"}
                    </strong>
                    <small>
                      {profile.authProvider === "GOOGLE"
                        ? "Connected with Google"
                        : "Park Nova secure login"}
                    </small>
                  </div>
                </article>
              </div>

              {isEditing && (
                <form
                  className="pn-profile-edit-panel"
                  onSubmit={handleSaveProfile}
                >
                  <div>
                    <strong>Edit your information</strong>
                    <span>
                      Name and phone number can be updated. Your verified email
                      and account role are protected.
                    </span>
                  </div>

                  <div className="pn-profile-edit-actions">
                    <button
                      type="button"
                      className="pn-profile-cancel-button"
                      onClick={handleCancelEdit}
                      disabled={saving}
                    >
                      Cancel
                    </button>

                    <button
                      type="submit"
                      className="pn-profile-save-button"
                      disabled={saving}
                    >
                      {saving ? "Saving..." : "Save Changes"}
                    </button>
                  </div>

                  {editError && (
                    <p className="pn-profile-edit-error" role="alert">
                      {editError}
                    </p>
                  )}
                </form>
              )}

              {!isEditing && successMessage && (
                <div className="pn-profile-success-message" role="status">
                  <span>✓</span>
                  {successMessage}
                </div>
              )}

              <div className="pn-profile-security-card">
                <div className="pn-profile-security-icon">✓</div>

                <div className="pn-profile-security-copy">
                  <p>ACCOUNT SECURITY</p>
                  <h3>
                    {profile.emailVerified
                      ? "Your email is verified"
                      : "Verify your email address"}
                  </h3>

                  <span>
                    {profile.emailVerified
                      ? "Your verified email helps keep your Park Nova account and parking activity secure."
                      : "Complete email verification to strengthen your Park Nova account security."}
                  </span>
                </div>

                <div
                  className={`pn-profile-verification-badge ${
                    profile.emailVerified ? "is-verified" : ""
                  }`}
                >
                  {profile.emailVerified ? "VERIFIED" : "PENDING"}
                </div>
              </div>

              <div className="pn-profile-id-row">
                <div>
                  <span>Park Nova Account</span>
                  <strong>
                    {profile.enabled ? "Ready to park" : "Account unavailable"}
                  </strong>
                </div>

                <Link to="/parking" className="pn-profile-parking-button">
                  Find Parking
                  <span>→</span>
                </Link>
              </div>
            </section>
          </div>
        )}
      </section>
    </main>
  );
}