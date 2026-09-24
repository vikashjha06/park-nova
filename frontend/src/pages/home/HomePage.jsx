import React, { useEffect, useState } from "react";
import WatchDemo from "../../components/demo/WatchDemo";
import { Link } from "react-router-dom";

const Icon = ({ children }) => (
  <span className="pn-icon" aria-hidden="true">{children}</span>
);

function Navbar() {
  const [open, setOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);

  const [user, setUser] = useState(() => {
    try {
      const token = localStorage.getItem("parkNovaToken");
      const savedUser = localStorage.getItem("parkNovaUser");

      return token && savedUser
        ? JSON.parse(savedUser)
        : null;
    } catch {
      return null;
    }
  });

  useEffect(() => {
    const close = () => {
      if (window.innerWidth > 900) setOpen(false);
    };

    window.addEventListener("resize", close);
    return () => window.removeEventListener("resize", close);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("parkNovaToken");
    localStorage.removeItem("parkNovaUser");

    setUser(null);
    setProfileOpen(false);
    setOpen(false);

    window.location.href = "/";
  };

  const firstName =
    user?.name?.trim()?.split(/\s+/)[0] || "Account";

  return (
    <header className="navbar">
      <div className="nav-container">
        <a className="brand" href="#home" aria-label="Park Nova home">
          <img src="/images/park-nova-logo.png" alt="Park Nova" />
        </a>

        <nav className="desktop-nav" aria-label="Main navigation">
          <a href="#home">Home</a>

          {user ? (
            <>
              <Link to="/parking">Find Parking</Link>
              <Link to="/bookings">My Bookings</Link>
              <Link to="/wallet">Wallet</Link>
              <Link to="/monthly-pass">My Pass</Link>
            </>
          ) : (
            <>
              <a href="#find-parking">Find Parking</a>
              <a href="#how-it-works">How It Works</a>
              <a href="#features">Features</a>
              <a href="#contact">Contact</a>
            </>
          )}
        </nav>

        <div className="nav-actions">
          {user ? (
            <div className="customer-profile desktop-action">
              <button
                type="button"
                className="customer-profile-button"
                onClick={() =>
                  setProfileOpen((current) => !current)
                }
                aria-expanded={profileOpen}
              >
                <span className="customer-avatar">
                  {firstName.charAt(0).toUpperCase()}
                </span>

                <span className="customer-profile-copy">
                  <small>Welcome</small>
                  <strong>{firstName}</strong>
                </span>

                <span
                  className={`customer-chevron ${
                    profileOpen ? "open" : ""
                  }`}
                >
                  ▾
                </span>
              </button>

              {profileOpen && (
                <div className="customer-dropdown">
                  <div className="customer-dropdown-head">
                    <span className="customer-avatar customer-avatar-large">
                      {firstName.charAt(0).toUpperCase()}
                    </span>

                    <div>
                      <strong>{user.name}</strong>
                      <span>{user.email}</span>
                    </div>
                  </div>

                  <div className="customer-dropdown-links">
                    <Link
                      to="/profile"
                      onClick={() => setProfileOpen(false)}
                    >
                      My Profile
                    </Link>

                    <Link
                      to="/bookings"
                      onClick={() => setProfileOpen(false)}
                    >
                      My Bookings
                    </Link>

                    <Link
                      to="/wallet"
                      onClick={() => setProfileOpen(false)}
                    >
                      Wallet & Transactions
                    </Link>

                    <Link
                      to="/monthly-pass"
                      onClick={() => setProfileOpen(false)}
                    >
                      Monthly Pass
                    </Link>

                    <Link
                      to="/payments"
                      onClick={() => setProfileOpen(false)}
                    >
                      Payments & Refunds
                    </Link>
                  </div>

                  <button
                    type="button"
                    className="customer-logout"
                    onClick={handleLogout}
                  >
                    Logout
                  </button>
                </div>
              )}
            </div>
          ) : (
            <>
              <Link
                className="btn btn-outline desktop-action"
                to="/login"
              >
                Login
              </Link>

              <Link
                className="btn btn-primary desktop-action"
                to="/register"
              >
                Sign Up
              </Link>
            </>
          )}

          <button
            className={`menu-button ${open ? "active" : ""}`}
            type="button"
            aria-label="Toggle menu"
            aria-expanded={open}
            onClick={() => setOpen((value) => !value)}
          >
            <span />
            <span />
            <span />
          </button>
        </div>
      </div>

      <div className={`mobile-menu ${open ? "open" : ""}`}>
        <a href="#home" onClick={() => setOpen(false)}>
          Home
        </a>

        {user ? (
          <>
            <div className="mobile-customer">
              <span className="customer-avatar">
                {firstName.charAt(0).toUpperCase()}
              </span>

              <div>
                <strong>{user.name}</strong>
                <span>{user.email}</span>
              </div>
            </div>

            <Link to="/parking" onClick={() => setOpen(false)}>
              Find Parking
            </Link>

            <Link to="/bookings" onClick={() => setOpen(false)}>
              My Bookings
            </Link>

            <Link to="/wallet" onClick={() => setOpen(false)}>
              Wallet
            </Link>

            <Link to="/monthly-pass" onClick={() => setOpen(false)}>
              My Pass
            </Link>

            <Link to="/profile" onClick={() => setOpen(false)}>
              My Profile
            </Link>

            <Link to="/payments" onClick={() => setOpen(false)}>
              Payments & Refunds
            </Link>

            <div className="mobile-menu-actions">
              <button
                className="btn btn-outline customer-mobile-logout"
                type="button"
                onClick={handleLogout}
              >
                Logout
              </button>
            </div>
          </>
        ) : (
          <>
            <a
              href="#find-parking"
              onClick={() => setOpen(false)}
            >
              Find Parking
            </a>

            <a
              href="#how-it-works"
              onClick={() => setOpen(false)}
            >
              How It Works
            </a>

            <a href="#features" onClick={() => setOpen(false)}>
              Features
            </a>

            <a href="#contact" onClick={() => setOpen(false)}>
              Contact
            </a>

            <div className="mobile-menu-actions">
              <Link
                className="btn btn-outline"
                to="/login"
                onClick={() => setOpen(false)}
              >
                Login
              </Link>

              <Link
                className="btn btn-primary"
                to="/register"
                onClick={() => setOpen(false)}
              >
                Sign Up
              </Link>
            </div>
          </>
        )}
      </div>
    </header>
  );
}

const features = [
  {
    icon: "⌕",
    title: "Find Parking",
    text: "Search nearby parking areas and check available spaces in real time.",
  },
  {
    icon: "▣",
    title: "Book Instantly",
    text: "Choose your parking slot and reserve it in just a few simple steps.",
  },
  {
    icon: "▰",
    title: "Secure Payment",
    text: "Use convenient payment options with a clear and reliable checkout flow.",
  },
  {
    icon: "◇",
    title: "Park with Confidence",
    text: "Manage bookings, parking sessions and account activity from one place.",
  },
];

function FeatureCard({ icon, title, text, index }) {
  const featureRoutes = {
    "Find Parking": "/features/find-parking",
    "Book Instantly": "/features/book-instantly",
    "Secure Payment": "/features/secure-payment",
    "Park with Confidence": "/features/park-with-confidence",
  };

  return (
    <article
      className="feature-card reveal-card"
      style={{ "--delay": `${index * 90}ms` }}
    >
      <Icon>{icon}</Icon>
      <h3>{title}</h3>
      <p>{text}</p>
      <Link to={featureRoutes[title]} className="learn-link">
        Learn More <span>→</span>
      </Link>
    </article>
  );
}

function HomePage() {
  const [demoOpen, setDemoOpen] = useState(false);

  return (
    <>
      <Navbar />

      <main>
        <section className="hero" id="home">
          <div className="hero-glow hero-glow-one" />
          <div className="hero-glow hero-glow-two" />

          <div className="page-container hero-grid">
            <div className="hero-content">
              <p className="eyebrow">WELCOME TO PARK NOVA</p>

              <h1>
                Park Smart
                <br />
                Live <span>Better</span>
              </h1>

              <p className="hero-description">
                Find, book, and manage your parking space easily.
                Smarter parking for a cleaner, greener tomorrow.
              </p>

              <div className="hero-actions">
                <a className="btn btn-primary btn-large" href="#find-parking">
                  Find Parking <span>→</span>
                </a>

                <button
                  className="watch-demo"
                  type="button"
                  onClick={() => setDemoOpen(true)}
                >
                  <span className="play-button">▶</span>
                  <span>
                    <strong>Watch Demo</strong>
                  </span>
                </button>
              </div>

              <div className="hero-stats">
                <div>
                  <strong>Real-Time</strong>
                  <span>Availability</span>
                </div>
                <div>
                  <strong>Fast</strong>
                  <span>Booking</span>
                </div>
                <div>
                  <strong>24/7</strong>
                  <span>Smart Access</span>
                </div>
                <div>
                  <strong>Secure</strong>
                  <span>Payments</span>
                </div>
              </div>
            </div>

            <div className="hero-visual" aria-hidden="true">
              <div className="visual-orbit orbit-one" />
              <div className="visual-orbit orbit-two" />

              <div className="parking-sign">
                <span>P</span>
              </div>

              <div className="smart-card">
                <span className="live-dot" />
                <p>SMART PARKING</p>
                <strong>Space available</strong>

                <div className="parking-lines">
                  <span />
                  <span />
                  <span />
                </div>
              </div>

              <div className="road">
                <span className="road-line line-one" />
                <span className="road-line line-two" />
                <span className="road-line line-three" />
              </div>

              <div className="hero-car">
                <div className="car-roof" />
                <div className="car-body">
                  <span className="headlight left" />
                  <span className="headlight right" />
                </div>
                <span className="wheel wheel-left" />
                <span className="wheel wheel-right" />
              </div>

              <div className="eco-copy">
                <span className="eco-leaf">●</span>
                <p>
                  A Smoother
                  <br />
                  Greener
                  <br />
                  Tomorrow
                </p>
              </div>
            </div>
          </div>
        </section>

        <section className="features-section" id="features">
          <div className="page-container">
            <div className="section-heading">
              <div>
                <p className="eyebrow">SMARTER PARKING</p>
                <h2>Everything you need to park with ease.</h2>
              </div>

              <p>
                A clean parking experience designed around availability,
                speed, security and convenience.
              </p>
            </div>

            <div className="feature-grid">
              {features.map((feature, index) => (
                <FeatureCard
                  key={feature.title}
                  {...feature}
                  index={index}
                />
              ))}
            </div>
          </div>
        </section>

        <section className="find-section" id="find-parking">
          <div className="page-container find-panel">
            <div>
              <p className="eyebrow">READY WHEN YOU ARE</p>
              <h2>Find your next parking space.</h2>
              <p>
                Sign in to view live parking availability and reserve
                the right slot for your vehicle.
              </p>
            </div>

            <Link
              to={
                localStorage.getItem("parkNovaToken") &&
                localStorage.getItem("parkNovaUser")
                  ? "/parking"
                  : "/login"
              }
              className="btn btn-primary btn-large"
            >
              Start Parking <span>→</span>
            </Link>
          </div>
        </section>

        <section className="how-section" id="how-it-works">
          <div className="page-container">
            <div className="section-heading compact">
              <div>
                <p className="eyebrow">HOW IT WORKS</p>
                <h2>From search to parked in four simple steps.</h2>
              </div>
            </div>

            <div className="steps-grid">
              {[
                ["01", "Find", "Choose a parking area and check availability."],
                ["02", "Reserve", "Select the right slot and booking time."],
                ["03", "Pay", "Complete your payment securely."],
                ["04", "Park", "Start your session and manage it from Park Nova."],
              ].map(([number, title, text]) => (
                <article className="step-card" key={number}>
                  <span>{number}</span>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </article>
              ))}
            </div>
          </div>
        </section>
      </main>

      <footer id="contact">
        <div className="page-container footer-inner">
          <img src="/images/park-nova-logo.png" alt="Park Nova" />
          <p>Park Smart Live Better</p>
          <span>© {new Date().getFullYear()} Park Nova</span>
        </div>
      </footer>

      <WatchDemo open={demoOpen} onClose={() => setDemoOpen(false)} />
    </>
  );
}

export default HomePage;
