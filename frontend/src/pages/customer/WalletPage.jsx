import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./WalletPage.css";

const money = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value || 0));

const dateTime = (value) => {
  if (!value) return "—";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return date.toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
};

export default function WalletPage() {
  const navigate = useNavigate();

  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const authFailure = (err) => {
    const status = err?.response?.status;

    if (status === 401 || status === 403) {
      localStorage.removeItem("parkNovaToken");
      localStorage.removeItem("parkNovaUser");
      navigate("/login", { replace: true });
      return true;
    }

    return false;
  };

  const loadWallet = async () => {
    try {
      setLoading(true);
      setError("");

      const [walletResponse, transactionsResponse] =
        await Promise.all([
          api.get("/api/wallet"),
          api.get("/api/wallet/transactions"),
        ]);

      setWallet(walletResponse.data || null);

      setTransactions(
        Array.isArray(transactionsResponse.data)
          ? transactionsResponse.data
          : []
      );
    } catch (err) {
      if (authFailure(err)) return;

      setError(
        err?.response?.data?.message ||
          "Unable to load your wallet."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWallet();
  }, []);

  const sortedTransactions = useMemo(
    () =>
      [...transactions].sort(
        (a, b) =>
          new Date(b.createdAt || 0) -
          new Date(a.createdAt || 0)
      ),
    [transactions]
  );

  const totalCredits = useMemo(
    () =>
      transactions
        .filter((item) => item.type === "CREDIT")
        .reduce((sum, item) => sum + Number(item.amount || 0), 0),
    [transactions]
  );

  const totalDebits = useMemo(
    () =>
      transactions
        .filter((item) => item.type === "DEBIT")
        .reduce((sum, item) => sum + Number(item.amount || 0), 0),
    [transactions]
  );

  return (
    <main className="pn-wallet-page">
      <header className="pn-wallet-header">
        <Link to="/" className="pn-wallet-brand">
          <img src="/images/park-nova-logo.png" alt="Park Nova" />
        </Link>

        <nav>
          <Link to="/parking">Find Parking</Link>
          <Link to="/bookings">Bookings</Link>
          <Link to="/monthly-pass">Monthly Pass</Link>
          <Link to="/profile">Profile</Link>
        </nav>
      </header>

      <section className="pn-wallet-shell">
        <div className="pn-wallet-title">
          <div>
            <p>PAYMENTS</p>
            <h1>
              Wallet & <em>Transactions</em>
            </h1>
            <span>
              Track your Park Nova wallet balance and payment
              activity in one place.
            </span>
          </div>

          <button
            type="button"
            onClick={loadWallet}
            disabled={loading}
          >
            Refresh
          </button>
        </div>

        {error && (
          <div className="pn-wallet-error" role="alert">
            {error}
          </div>
        )}

        {loading ? (
          <div className="pn-wallet-loading">
            <div />
            <strong>Loading wallet...</strong>
          </div>
        ) : (
          <>
            <section className="pn-wallet-overview">
              <article className="pn-wallet-balance">
                <div className="pn-wallet-balance-head">
                  <div>
                    <span>AVAILABLE BALANCE</span>
                    <strong>{money(wallet?.balance)}</strong>
                  </div>

                  <div className="pn-wallet-mark">PN</div>
                </div>

                <p>
                  Your wallet balance can be used when WALLET is
                  selected during a supported Park Nova payment.
                </p>

                <div className="pn-wallet-id">
                  <span>WALLET</span>
                  <strong>
                    •••• {String(wallet?.id || "0000").slice(-4)}
                  </strong>
                </div>
              </article>

              <div className="pn-wallet-stats">
                <article>
                  <span>Total credits</span>
                  <strong>{money(totalCredits)}</strong>
                  <small>Recorded wallet credits</small>
                </article>

                <article>
                  <span>Total debits</span>
                  <strong>{money(totalDebits)}</strong>
                  <small>Recorded wallet spending</small>
                </article>

                <article>
                  <span>Transactions</span>
                  <strong>{transactions.length}</strong>
                  <small>Wallet ledger entries</small>
                </article>
              </div>
            </section>

            <section className="pn-wallet-history">
              <div className="pn-wallet-history-head">
                <div>
                  <p>WALLET LEDGER</p>
                  <h2>Transaction history</h2>
                </div>

                <span>
                  {transactions.length}{" "}
                  {transactions.length === 1
                    ? "transaction"
                    : "transactions"}
                </span>
              </div>

              {sortedTransactions.length === 0 ? (
                <div className="pn-wallet-empty">
                  <div>₹</div>
                  <strong>No wallet transactions yet</strong>
                  <span>
                    Wallet credits, debits and eligible refunds will
                    appear here.
                  </span>
                </div>
              ) : (
                <div className="pn-wallet-transactions">
                  {sortedTransactions.map((item) => {
                    const isCredit = item.type === "CREDIT";

                    return (
                      <article
                        className="pn-wallet-transaction"
                        key={item.id}
                      >
                        <div
                          className={`pn-wallet-transaction-icon ${
                            isCredit ? "credit" : "debit"
                          }`}
                        >
                          {isCredit ? "+" : "−"}
                        </div>

                        <div className="pn-wallet-transaction-main">
                          <strong>
                            {item.description ||
                              `${item.type} transaction`}
                          </strong>

                          <span>
                            {dateTime(item.createdAt)}
                            {item.referenceType
                              ? ` · ${item.referenceType}`
                              : ""}
                          </span>

                          {item.transactionId && (
                            <small>
                              Transaction: {item.transactionId}
                            </small>
                          )}
                        </div>

                        <div className="pn-wallet-transaction-amount">
                          <strong
                            className={
                              isCredit ? "credit" : "debit"
                            }
                          >
                            {isCredit ? "+" : "−"}
                            {money(item.amount)}
                          </strong>

                          <span>
                            Balance {money(item.balanceAfter)}
                          </span>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>
          </>
        )}
      </section>
    </main>
  );
}