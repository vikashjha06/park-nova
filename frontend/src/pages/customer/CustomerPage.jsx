import React from "react";
import { Link } from "react-router-dom";

export default function CustomerPage({ eyebrow, title, description }) {
  let user = null;

  try {
    user = JSON.parse(localStorage.getItem("parkNovaUser") || "null");
  } catch {
    user = null;
  }

  return (
    <main className="customer-page">
      <header className="customer-page-header">
        <Link to="/" className="customer-page-logo">
          <img
            src="/images/park-nova-logo.png"
            alt="Park Nova"
          />
        </Link>

        <Link to="/" className="customer-home-link">
          Back to Home
        </Link>
      </header>

      <section className="customer-page-content">
        <p className="eyebrow">{eyebrow}</p>

        <h1>{title}</h1>

        <p>{description}</p>

        {user?.name && (
          <div className="customer-page-user">
            Signed in as <strong>{user.name}</strong>
          </div>
        )}
      </section>
    </main>
  );
}
