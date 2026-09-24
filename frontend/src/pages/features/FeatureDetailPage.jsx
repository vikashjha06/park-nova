import React from "react";
import { Link, Navigate, useParams } from "react-router-dom";
import { featureDetails } from "./featureDetails";
import "./FeatureDetailPage.css";

function SearchVisual() {
  return (
    <div className="fd-live-board">
      <div className="fd-board-top">
        <div>
          <span className="fd-mini-label">CITY CENTER PARKING</span>
          <strong>Live availability</strong>
        </div>
        <span className="fd-live-pill">
          <i />
          LIVE
        </span>
      </div>

      <div className="fd-search-box">
        <span>⌕</span>
        <p>Search parking area</p>
        <b>Find</b>
      </div>

      <div className="fd-slot-grid">
        <div className="fd-slot available">
          <span>A-01</span>
          <small>AVAILABLE</small>
        </div>

        <div className="fd-slot occupied">
          <span>A-02</span>
          <small>OCCUPIED</small>
        </div>

        <div className="fd-slot available">
          <span>A-03</span>
          <small>AVAILABLE</small>
        </div>

        <div className="fd-slot booked">
          <span>A-04</span>
          <small>BOOKED</small>
        </div>
      </div>

      <div className="fd-board-footer">
        <span><i className="green-dot" /> Available</span>
        <strong>2 spaces ready</strong>
      </div>
    </div>
  );
}

function BookingVisual() {
  return (
    <div className="fd-booking-board">
      <div className="fd-ticket-head">
        <span className="fd-mini-label">RESERVATION PREVIEW</span>
        <span className="fd-status-tag">READY</span>
      </div>

      <div className="fd-booking-slot">
        <small>SELECTED SLOT</small>
        <strong>A-01</strong>
        <span>CAR</span>
      </div>

      <div className="fd-booking-route">
        <div>
          <span>01</span>
          <p>Select</p>
        </div>
        <i />
        <div>
          <span>02</span>
          <p>Details</p>
        </div>
        <i />
        <div>
          <span>03</span>
          <p>Confirm</p>
        </div>
      </div>

      <div className="fd-confirm-line">
        <span>City Center Parking</span>
        <strong>Ready to reserve</strong>
      </div>
    </div>
  );
}

function PaymentVisual() {
  return (
    <div className="fd-payment-board">
      <div className="fd-payment-head">
        <div>
          <span className="fd-mini-label">PARK NOVA CHECKOUT</span>
          <strong>Payment summary</strong>
        </div>

        <div className="fd-shield">✓</div>
      </div>

      <div className="fd-payment-item">
        <span>Parking reservation</span>
        <strong>Confirmed details</strong>
      </div>

      <div className="fd-payment-item">
        <span>Slot</span>
        <strong>A-01</strong>
      </div>

      <div className="fd-payment-total">
        <span>STATUS</span>
        <strong>READY FOR PAYMENT</strong>
      </div>

      <div className="fd-secure-line">
        <span>✓</span>
        Clear booking-linked checkout
      </div>
    </div>
  );
}

function JourneyVisual() {
  return (
    <div className="fd-journey-board">
      <div className="fd-journey-head">
        <div>
          <span className="fd-mini-label">MY PARK NOVA</span>
          <strong>Parking journey</strong>
        </div>
        <span className="fd-profile-dot">PN</span>
      </div>

      <div className="fd-journey-card active">
        <span className="fd-journey-icon">P</span>
        <div>
          <small>CURRENT BOOKING</small>
          <strong>City Center Parking</strong>
          <p>Slot A-01</p>
        </div>
        <b>ACTIVE</b>
      </div>

      <div className="fd-activity-row">
        <div>
          <small>BOOKINGS</small>
          <strong>Organized</strong>
        </div>
        <div>
          <small>PAYMENTS</small>
          <strong>Tracked</strong>
        </div>
      </div>

      <div className="fd-timeline">
        <span className="done">✓</span>
        <i />
        <span className="done">✓</span>
        <i />
        <span>3</span>
      </div>
    </div>
  );
}

function FeatureVisual({ type }) {
  if (type === "search") return <SearchVisual />;
  if (type === "booking") return <BookingVisual />;
  if (type === "payment") return <PaymentVisual />;
  return <JourneyVisual />;
}

export default function FeatureDetailPage() {
  const { featureId } = useParams();
  const feature = featureDetails[featureId];

  if (!feature) {
    return <Navigate to="/" replace />;
  }

  return (
    <main className={`feature-detail-page fd-${feature.visualType}`}>
      <div className="fd-grid-background" />

      <header className="fd-header">
        <Link className="fd-brand" to="/">
          <img src="/images/park-nova-logo.png" alt="Park Nova" />

          <div>
            <strong>PARK NOVA</strong>
            <span>PARK SMART LIVE BETTER</span>
          </div>
        </Link>

        <Link className="fd-back" to="/#features">
          <span>&lt;</span>
          Back to Features
        </Link>
      </header>

      <section className="fd-hero">
        <div className="fd-copy">
          <div className="fd-number">{feature.number}</div>

          <p className="fd-eyebrow">{feature.eyebrow}</p>

          <h1>
            {feature.title}
            <br />
            <span>{feature.accent}</span>
          </h1>

          <p className="fd-description">
            {feature.description}
          </p>

          <div className="fd-actions">
            <Link className="fd-primary" to="/#find-parking">
              {feature.cta}
              <span>-&gt;</span>
            </Link>

            <Link className="fd-secondary" to="/">
              Home
            </Link>
          </div>

          <div className="fd-stats">
            {feature.stats.map((stat) => (
              <div key={stat.label}>
                <strong>{stat.value}</strong>
                <span>{stat.label}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="fd-visual-wrap">
          <div className="fd-orbit orbit-one" />
          <div className="fd-orbit orbit-two" />

          <div className="fd-visual-label">
            <span>INTERACTIVE VIEW</span>
            <strong>{feature.cardTitle}</strong>
          </div>

          <FeatureVisual type={feature.visualType} />

          <p className="fd-visual-caption">
            {feature.cardText}
          </p>
        </div>
      </section>

      <section className="fd-process">
        <div className="fd-process-heading">
          <div>
            <p className="fd-eyebrow">HOW IT WORKS</p>
            <h2>
              Designed around a
              <span> clear parking journey.</span>
            </h2>
          </div>

          <p>
            Each stage focuses on the information you need
            without unnecessary complexity.
          </p>
        </div>

        <div className="fd-step-grid">
          {feature.steps.map((step) => (
            <article className="fd-step" key={step.number}>
              <span>{step.number}</span>
              <div className="fd-step-line" />
              <h3>{step.title}</h3>
              <p>{step.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="fd-bottom-cta">
        <div>
          <p className="fd-eyebrow">PARK NOVA</p>
          <h2>Ready for smarter parking?</h2>
        </div>

        <Link to="/register">
          Create Account
          <span>-&gt;</span>
        </Link>
      </section>
    </main>
  );
}
