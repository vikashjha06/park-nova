import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";

import HomePage from "./pages/home/HomePage";

import LoginPage from "./pages/auth/LoginPage";
import RegisterPage from "./pages/auth/RegisterPage";
import VerifyEmailPage from "./pages/auth/VerifyEmailPage";
import ForgotPasswordPage from "./pages/auth/ForgotPasswordPage";
import ResetPasswordPage from "./pages/auth/ResetPasswordPage";
import OAuthSuccessPage from "./pages/auth/OAuthSuccessPage";

import FeatureDetailPage from "./pages/features/FeatureDetailPage";
import PrivacyPage from "./pages/legal/PrivacyPage";

import ProtectedRoute from "./components/routing/ProtectedRoute";

import ProfilePage from "./pages/customer/ProfilePage";
import ParkingPage from "./pages/customer/ParkingPage";
import BookingsPage from "./pages/customer/BookingsPage";
import WalletPage from "./pages/customer/WalletPage";
import MonthlyPassPage from "./pages/customer/MonthlyPassPage";
import PaymentPage from "./pages/customer/PaymentPage";
import PaymentsPage from "./pages/customer/PaymentsPage";

import AdminLoginPage from "./pages/admin/AdminLoginPage";
import AdminLoginApprovalPage from "./pages/admin/AdminLoginApprovalPage";
import AdminDashboard from "./pages/admin/dashboard/AdminDashboard";

function ComingSoon({ title }) {
  return (
    <main className="route-placeholder">
      <img
        src="/images/park-nova-logo.png"
        alt="Park Nova"
      />

      <p>PARK NOVA</p>

      <h1>{title}</h1>

      <a href="/">
        Back to Home
      </a>
    </main>
  );
}

function UserRoute({ children }) {
  return (
    <ProtectedRoute role="USER">
      {children}
    </ProtectedRoute>
  );
}

function AdminRoute({ children }) {
  return (
    <ProtectedRoute role="ADMIN">
      {children}
    </ProtectedRoute>
  );
}

function App() {
  return (
    <Routes>
      {/* =========================
          PUBLIC
         ========================= */}

      <Route
        path="/"
        element={<HomePage />}
      />

      <Route
        path="/features/:featureId"
        element={<FeatureDetailPage />}
      />

      <Route path="/privacy" element={<PrivacyPage />} />

      {/* =========================
          CUSTOMER AUTH
         ========================= */}

      <Route
        path="/login"
        element={<LoginPage />}
      />

      <Route
        path="/register"
        element={<RegisterPage />}
      />

      <Route
        path="/verify-email"
        element={<VerifyEmailPage />}
      />

      <Route
        path="/forgot-password"
        element={<ForgotPasswordPage />}
      />

      <Route
        path="/reset-password"
        element={<ResetPasswordPage />}
      />

      <Route
        path="/oauth-success"
        element={<OAuthSuccessPage />}
      />

      {/* =========================
          ADMIN AUTH
         ========================= */}

      <Route
        path="/admin/login"
        element={<AdminLoginPage />}
      />

      <Route
        path="/admin/verify-login"
        element={<AdminLoginApprovalPage />}
      />

      {/* =========================
          CUSTOMER - USER ONLY
         ========================= */}

      <Route
        path="/profile"
        element={
          <UserRoute>
            <ProfilePage />
          </UserRoute>
        }
      />

      <Route
        path="/parking"
        element={
          <UserRoute>
            <ParkingPage />
          </UserRoute>
        }
      />

      <Route
        path="/bookings"
        element={
          <UserRoute>
            <BookingsPage />
          </UserRoute>
        }
      />

      <Route
        path="/wallet"
        element={
          <UserRoute>
            <WalletPage />
          </UserRoute>
        }
      />

      <Route
        path="/payment"
        element={
          <UserRoute>
            <PaymentPage />
          </UserRoute>
        }
      />

      <Route
        path="/payments"
        element={
          <UserRoute>
            <PaymentsPage />
          </UserRoute>
        }
      />

      <Route
        path="/monthly-pass"
        element={
          <UserRoute>
            <MonthlyPassPage />
          </UserRoute>
        }
      />

      {/* =========================
          ADMIN - ADMIN ONLY
         ========================= */}

      <Route
        path="/admin"
        element={
          <AdminRoute>
            <AdminDashboard />
          </AdminRoute>
        }
      />

      <Route
        path="/admin/parking-areas"
        element={
          <AdminRoute>
            <AdminDashboard />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/parking-slots"
        element={
          <AdminRoute>
            <AdminDashboard />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/pricing"
        element={
          <AdminRoute>
            <AdminDashboard />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/bookings"
        element={<AdminRoute><AdminDashboard /></AdminRoute>}
      />
      <Route
        path="/admin/payments"
        element={<AdminRoute><AdminDashboard /></AdminRoute>}
      />
      <Route
        path="/admin/refunds"
        element={<AdminRoute><AdminDashboard /></AdminRoute>}
      />
      <Route
        path="/admin/wallets"
        element={<AdminRoute><AdminDashboard /></AdminRoute>}
      />
      <Route
        path="/admin/monthly-passes"
        element={<AdminRoute><AdminDashboard /></AdminRoute>}
      />
      <Route
        path="/admin/users"
        element={<AdminRoute><AdminDashboard /></AdminRoute>}
      />
      <Route
        path="/admin/notifications"
        element={<AdminRoute><AdminDashboard /></AdminRoute>}
      />
      <Route
        path="/admin/reports"
        element={<AdminRoute><AdminDashboard /></AdminRoute>}
      />
      <Route
        path="/admin/activity"
        element={<AdminRoute><AdminDashboard /></AdminRoute>}
      />
      <Route
        path="/admin/settings"
        element={<AdminRoute><AdminDashboard /></AdminRoute>}
      />
      {/* =========================
          FALLBACK
         ========================= */}

      <Route
        path="*"
        element={
          <Navigate
            to="/"
            replace
          />
        }
      />
    </Routes>
  );
}

export default App;