import React from "react";
import {
  Navigate,
  useLocation,
} from "react-router-dom";

const clearSession = () => {
  localStorage.removeItem(
    "parkNovaToken"
  );

  localStorage.removeItem(
    "parkNovaUser"
  );
};

const normalizeRole = (role) =>
  String(role || "")
    .replace(/^ROLE_/i, "")
    .toUpperCase();

const isTokenExpired = (token) => {
  try {
    const parts = token.split(".");

    if (parts.length !== 3) {
      return true;
    }

    const normalized = parts[1]
      .replace(/-/g, "+")
      .replace(/_/g, "/");

    const padded =
      normalized +
      "=".repeat(
        (4 - (normalized.length % 4)) % 4
      );

    const payload = JSON.parse(
      atob(padded)
    );

    if (!payload?.exp) {
      return true;
    }

    return payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
};

export default function ProtectedRoute({
  children,
  role,
}) {
  const location = useLocation();

  const requiredRole =
    normalizeRole(role);

  const token =
    localStorage.getItem(
      "parkNovaToken"
    );

  let user = null;

  try {
    user = JSON.parse(
      localStorage.getItem(
        "parkNovaUser"
      ) || "null"
    );
  } catch {
    user = null;
  }

  if (
    !token ||
    !user ||
    isTokenExpired(token)
  ) {
    clearSession();

    const loginPath =
      requiredRole === "ADMIN"
        ? "/admin/login?session=expired"
        : "/login?session=expired";

    return (
      <Navigate
        to={loginPath}
        replace
        state={{
          from: location.pathname,
        }}
      />
    );
  }

  const currentRole =
    normalizeRole(user.role);

  if (
    requiredRole &&
    currentRole !== requiredRole
  ) {
    if (currentRole === "ADMIN") {
      return (
        <Navigate
          to="/admin"
          replace
        />
      );
    }

    return (
      <Navigate
        to="/"
        replace
      />
    );
  }

  return children;
}