import React from "react";
import { Link } from "react-router-dom";
import "./Auth.css";

export default function AuthLayout({
  eyebrow,
  title,
  description,
  children,
  footerText,
  footerLinkText,
  footerLinkTo,
}) {
  return (
    <main className="auth-page">
      <Link
        to="/"
        className="auth-home-link"
        aria-label="Back to Park Nova home"
      >
        <span className="auth-back-arrow">&lt;</span>
        <span>Back to Home</span>
      </Link>

      <section className="auth-shell">
        <aside className="auth-showcase">
          <div className="auth-showcase-glow glow-one" />
          <div className="auth-showcase-glow glow-two" />

          <Link to="/" className="auth-brand">
            <img
              src="/images/park-nova-logo.png"
              alt="Park Nova"
            />

            <div>
              <strong>PARK NOVA</strong>
              <span>Park Smart Live Better</span>
            </div>
          </Link>

          <div className="auth-showcase-copy">
            <span className="auth-kicker">
              SMART PARKING EXPERIENCE
            </span>

            <h2>
              Parking made
              <br />
              <strong>simple and smart.</strong>
            </h2>

            <p>
              Discover available spaces, reserve your slot,
              manage payments and access your parking journey
              from one secure platform.
            </p>
          </div>

          <div className="auth-showcase-card">
            <div className="auth-live-icon">
              <span />
            </div>

            <div>
              <small>LIVE AVAILABILITY</small>
              <strong>Find the right space faster</strong>
              <p>
                Real-time parking information when you need it.
              </p>
            </div>
          </div>

          <div className="auth-showcase-stats">
            <div>
              <strong>24/7</strong>
              <span>Smart Access</span>
            </div>

            <div>
              <strong>Live</strong>
              <span>Availability</span>
            </div>

            <div>
              <strong>Secure</strong>
              <span>Payments</span>
            </div>
          </div>
        </aside>

        <section className="auth-panel">
          <div className="auth-mobile-brand">
            <Link to="/">
              <img
                src="/images/park-nova-logo.png"
                alt="Park Nova"
              />
            </Link>
          </div>

          <div className="auth-form-wrap">
            <header className="auth-heading">
              <span>{eyebrow}</span>
              <h1>{title}</h1>
              <p>{description}</p>
            </header>

            {children}

            {footerText && (
              <p className="auth-switch">
                {footerText}{" "}
                <Link to={footerLinkTo}>
                  {footerLinkText}
                </Link>
              </p>
            )}
          </div>

          <footer className="auth-panel-footer">
            <span>Secure authentication</span>
            <span>Park Nova 2026</span>
          </footer>
        </section>
      </section>
    </main>
  );
}
