import React from "react";
import { Link } from "react-router-dom";

const sectionStyle = { marginTop: "2rem" };

export default function PrivacyPage() {
  return (
    <main
      style={{
        minHeight: "100vh",
        background: "#09131f",
        color: "#edf5f8",
        padding: "clamp(24px, 5vw, 64px)",
        fontFamily: "inherit",
      }}
    >
      <article style={{ maxWidth: 800, margin: "0 auto", lineHeight: 1.7 }}>
        <Link to="/" style={{ color: "#35e3c1" }}>
          ← Park Nova home
        </Link>

        <h1 style={{ marginTop: 32 }}>Privacy Policy</h1>
        <p>Last updated: 25 September 2026</p>

        <section style={sectionStyle}>
          <h2>About Park Nova</h2>
          <p>
            Park Nova is a parking service with accounts, parking search,
            bookings, payments, wallet features and monthly passes.
            This page explains the information used to provide those features.
          </p>
        </section>

        <section style={sectionStyle}>
          <h2>Information you provide</h2>
          <p>
            When you use Park Nova, you may provide account and contact
            details, vehicle details, and information needed to make a
            booking or purchase a monthly pass.
          </p>
        </section>

        <section style={sectionStyle}>
          <h2>How information is used</h2>
          <p>
            Park Nova uses information to manage accounts, show parking
            availability, handle bookings, payments and passes, and send
            account or service emails. Administrators use service records
            to operate and support the parking service.
          </p>
        </section>

        <section style={sectionStyle}>
          <h2>Google sign-in</h2>
          <p>
            If you choose Google sign-in, Park Nova receives the account
            information needed to identify you and sign you in. Google
            handles the Google sign-in process.
          </p>
        </section>

        <section style={sectionStyle}>
          <h2>Questions about your information</h2>
          <p>
            For privacy questions or requests about your account, contact
            the Park Nova support email shown on the Google sign-in consent
            screen.
          </p>
        </section>
      </article>
    </main>
  );
}