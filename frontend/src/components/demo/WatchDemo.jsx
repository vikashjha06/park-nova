import React, { useEffect, useState } from "react";
import "./WatchDemo.css";

const stages = [
  {
    id: "search",
    label: "Search",
    title: "Find Parking",
    subtitle: "Searching nearby parking areas",
  },
  {
    id: "availability",
    label: "Availability",
    title: "City Center Parking",
    subtitle: "Live parking availability",
  },
  {
    id: "reserve",
    label: "Reserve",
    title: "Select Your Slot",
    subtitle: "Choose an available parking space",
  },
  {
    id: "payment",
    label: "Payment",
    title: "Secure Payment",
    subtitle: "Confirm your parking reservation",
  },
  {
    id: "access",
    label: "Access",
    title: "Smart Access",
    subtitle: "Booking confirmed",
  },
  {
    id: "park",
    label: "Park",
    title: "You're All Set",
    subtitle: "Welcome to Park Nova",
  },
];

function SearchStage() {
  return (
    <div className="wd-screen-content wd-search-screen">
      <div className="wd-greeting">GOOD AFTERNOON</div>

      <h3>
        Find Your
        <br />
        Parking Space
      </h3>

      <div className="wd-search-box">
        <span className="wd-search-icon" />
        Search location, mall or parking area
      </div>

      <div className="wd-quick-row">
        <div>
          <span className="wd-quick-icon">N</span>
          <small>Nearby</small>
        </div>

        <div>
          <span className="wd-quick-icon">M</span>
          <small>Mall</small>
        </div>

        <div>
          <span className="wd-quick-icon">O</span>
          <small>Office</small>
        </div>
      </div>

      <div className="wd-location-card">
        <div>
          <small>0.5 km away</small>
          <strong>City Center Parking</strong>
          <span>Open 24/7</span>
        </div>

        <span className="wd-card-arrow">-&gt;</span>
      </div>
    </div>
  );
}

function AvailabilityStage() {
  return (
    <div className="wd-screen-content">
      <div className="wd-mini-map">
        <span className="wd-map-road road-a" />
        <span className="wd-map-road road-b" />
        <span className="wd-map-road road-c" />

        <span className="wd-map-pin pin-one">P</span>
        <span className="wd-map-pin pin-two">P</span>
        <span className="wd-user-dot" />
      </div>

      <div className="wd-parking-summary">
        <small>0.5 km away</small>
        <h3>City Center Parking</h3>

        <div className="wd-summary-grid">
          <div>
            <strong>120</strong>
            <span>Total Slots</span>
          </div>

          <div>
            <strong>87</strong>
            <span>Available</span>
          </div>

          <div>
            <strong>Rs 40</strong>
            <span>Per Hour</span>
          </div>
        </div>

        <button type="button">
          View Available Slots
        </button>
      </div>
    </div>
  );
}

function ReserveStage() {
  const slots = [
    ["A-01", "available"],
    ["A-02", "available"],
    ["A-03", "booked"],
    ["A-04", "available"],
    ["A-05", "available"],
    ["A-06", "available"],
    ["B-01", "available"],
    ["B-02", "available"],
    ["B-03", "booked"],
  ];

  return (
    <div className="wd-screen-content wd-slot-screen">
      <small className="wd-screen-label">
        FLOOR 1
      </small>

      <h3>Select Your Parking Slot</h3>

      <div className="wd-floor-tabs">
        <span className="active">Floor 1</span>
        <span>Floor 2</span>
        <span>Floor 3</span>
      </div>

      <div className="wd-slot-grid">
        {slots.map(([slot, status]) => (
          <div
            key={slot}
            className={[
              "wd-slot",
              status,
              slot === "A-01" ? "selected" : "",
            ].join(" ")}
          >
            <span className="wd-car-symbol" />
            <strong>{slot}</strong>
          </div>
        ))}
      </div>

      <div className="wd-slot-legend">
        <span><i className="available" /> Available</span>
        <span><i className="booked" /> Booked</span>
      </div>
    </div>
  );
}

function PaymentStage() {
  return (
    <div className="wd-screen-content wd-payment-screen">
      <small className="wd-screen-label">
        BOOKING DETAILS
      </small>

      <div className="wd-booking-card">
        <div className="wd-booking-car">
          <span className="wd-car-symbol large" />
        </div>

        <div>
          <strong>A-01</strong>
          <span>City Center Parking</span>
          <small>Floor 1</small>
        </div>
      </div>

      <div className="wd-detail-list">
        <div>
          <span>Date</span>
          <strong>19 Sep 2026</strong>
        </div>

        <div>
          <span>Duration</span>
          <strong>2 Hours</strong>
        </div>

        <div>
          <span>Vehicle</span>
          <strong>MH 12 AB 1234</strong>
        </div>
      </div>

      <div className="wd-total">
        <span>Total Amount</span>
        <strong>Rs 80</strong>
      </div>

      <button className="wd-pay-button" type="button">
        Pay Securely
      </button>
    </div>
  );
}

function AccessStage() {
  return (
    <div className="wd-screen-content wd-access-screen">
      <div className="wd-success-check">
        <span />
      </div>

      <small>PAYMENT VERIFIED</small>
      <h3>Booking Confirmed</h3>

      <p>
        Your parking slot is reserved and ready.
      </p>

      <div className="wd-qr">
        <div className="wd-qr-pattern" />
      </div>

      <div className="wd-access-details">
        <strong>A-01</strong>
        <span>Gate Access Ready</span>
      </div>

      <div className="wd-booking-id">
        Booking ID
        <strong>PN726519</strong>
      </div>
    </div>
  );
}

function ParkStage() {
  return (
    <div className="wd-screen-content wd-park-screen">
      <div className="wd-garage">
        <div className="wd-garage-light light-one" />
        <div className="wd-garage-light light-two" />

        <div className="wd-slot-sign">
          A-01
        </div>

        <div className="wd-demo-car">
          <div className="wd-demo-car-roof" />
          <div className="wd-demo-car-body">
            <span className="wd-demo-light left" />
            <span className="wd-demo-light right" />
          </div>
          <span className="wd-demo-wheel wheel-one" />
          <span className="wd-demo-wheel wheel-two" />
        </div>

        <div className="wd-garage-line" />
      </div>

      <div className="wd-park-success">
        <div className="wd-success-check small">
          <span />
        </div>

        <h3>You're All Set!</h3>
        <p>Park Smart. Live Better.</p>
      </div>
    </div>
  );
}

function StageContent({ stage }) {
  switch (stage) {
    case 0:
      return <SearchStage />;
    case 1:
      return <AvailabilityStage />;
    case 2:
      return <ReserveStage />;
    case 3:
      return <PaymentStage />;
    case 4:
      return <AccessStage />;
    default:
      return <ParkStage />;
  }
}

export default function WatchDemo({ open, onClose }) {
  const [stage, setStage] = useState(0);
  const [paused, setPaused] = useState(false);

  useEffect(() => {
    if (!open) {
      setStage(0);
      setPaused(false);
      return undefined;
    }

    const handleEscape = (event) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    document.body.classList.add("modal-open");
    window.addEventListener("keydown", handleEscape);

    return () => {
      document.body.classList.remove("modal-open");
      window.removeEventListener("keydown", handleEscape);
    };
  }, [open, onClose]);

  useEffect(() => {
    if (!open || paused) return undefined;

    const timer = window.setInterval(() => {
      setStage((current) => (
        current === stages.length - 1
          ? 0
          : current + 1
      ));
    }, 3200);

    return () => window.clearInterval(timer);
  }, [open, paused]);

  if (!open) return null;

  const current = stages[stage];

  return (
    <div
      className="wd-backdrop"
      onMouseDown={onClose}
    >
      <section
        className="wd-modal"
        role="dialog"
        aria-modal="true"
        aria-label="Park Nova interactive demo"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button
          className="wd-close"
          type="button"
          onClick={onClose}
          aria-label="Close demo"
        >
          X
        </button>

        <header className="wd-header">
          <img
            src="/images/park-nova-logo.png"
            alt="Park Nova"
          />

          <div className="wd-heading">
            <span>INTERACTIVE PRODUCT TOUR</span>
            <h2>
              See <strong>Park Nova</strong> in Action
            </h2>
            <p>
              Find, reserve, pay and park in one
              seamless smart-parking experience.
            </p>
          </div>

          <div className="wd-live">
            <i />
            LIVE DEMO
          </div>
        </header>

        <div className="wd-demo-layout">
          <aside className="wd-story">
            <span className="wd-story-number">
              0{stage + 1}
            </span>

            <div>
              <small>CURRENT STEP</small>
              <h3>{current.title}</h3>
              <p>{current.subtitle}</p>
            </div>

            <div className="wd-story-stats">
              <div>
                <strong>87</strong>
                <span>Available</span>
              </div>

              <div>
                <strong>24/7</strong>
                <span>Access</span>
              </div>

              <div>
                <strong>A-01</strong>
                <span>Selected</span>
              </div>
            </div>
          </aside>

          <div className="wd-phone-wrap">
            <div className="wd-phone">
              <div className="wd-phone-top">
                <span>15:48</span>

                <img
                  src="/images/park-nova-logo.png"
                  alt=""
                />

                <span className="wd-signal">
                  LIVE
                </span>
              </div>

              <div
                key={stage}
                className="wd-stage"
              >
                <StageContent stage={stage} />
              </div>

              <div className="wd-phone-nav">
                <span className="active">Home</span>
                <span>Bookings</span>
                <span>Wallet</span>
                <span>Profile</span>
              </div>
            </div>
          </div>

          <aside className="wd-side-panel">
            <div className="wd-status-card">
              <span className="wd-status-dot" />

              <small>SYSTEM STATUS</small>
              <strong>Connected</strong>
              <p>
                Live parking services are online.
              </p>
            </div>

            <div className="wd-secure-card">
              <span className="wd-shield">
                S
              </span>

              <div>
                <small>SECURE EXPERIENCE</small>
                <strong>
                  Smart access enabled
                </strong>
              </div>
            </div>

            <button
              className="wd-control"
              type="button"
              onClick={() => setPaused((value) => !value)}
            >
              {paused ? "Resume Demo" : "Pause Demo"}
            </button>
          </aside>
        </div>

        <footer className="wd-progress-area">
          <div className="wd-progress-line">
            <span
              style={{
                width:
                  `${(stage / (stages.length - 1)) * 100}%`,
              }}
            />
          </div>

          <div className="wd-progress-steps">
            {stages.map((item, index) => (
              <button
                key={item.id}
                type="button"
                className={[
                  index === stage ? "active" : "",
                  index < stage ? "complete" : "",
                ].join(" ")}
                onClick={() => setStage(index)}
              >
                <i />
                <span>{item.label}</span>
              </button>
            ))}
          </div>
        </footer>
      </section>
    </div>
  );
}
