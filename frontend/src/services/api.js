import axios from "axios";

const api = axios.create({
  baseURL:
    import.meta.env.VITE_API_BASE_URL ||
    "http://localhost:8080",

  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    const token =
      localStorage.getItem(
        "parkNovaToken"
      );

    if (token) {
      config.headers.Authorization =
        `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,

  (error) => {
    const status =
      error?.response?.status;

    const hadToken =
      Boolean(
        localStorage.getItem(
          "parkNovaToken"
        )
      );

    if (status === 401 && hadToken) {
      localStorage.removeItem(
        "parkNovaToken"
      );

      localStorage.removeItem(
        "parkNovaUser"
      );

      const currentPath =
        window.location.pathname;

      const adminAuthPaths = [
        "/admin/login",
        "/admin/verify-login",
      ];

      const customerAuthPaths = [
        "/login",
        "/register",
        "/verify-email",
        "/forgot-password",
        "/reset-password",
        "/oauth-success",
      ];

      const isAdminPath =
        currentPath === "/admin" ||
        currentPath.startsWith("/admin/");

      const isAdminAuthPath =
        adminAuthPaths.includes(
          currentPath
        );

      const isCustomerAuthPath =
        customerAuthPaths.includes(
          currentPath
        );

      if (
        isAdminPath &&
        !isAdminAuthPath
      ) {
        window.location.replace(
          "/admin/login?session=expired"
        );
      } else if (
        !isAdminAuthPath &&
        !isCustomerAuthPath
      ) {
        window.location.replace(
          "/login?session=expired"
        );
      }
    }

    return Promise.reject(error);
  }
);

export default api;