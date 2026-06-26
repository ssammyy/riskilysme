import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/auth/AuthProvider";

/** Gates child routes behind authentication and email verification. */
export function ProtectedRoute() {
  const { status, user } = useAuth();

  if (status === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-muted border-t-primary" />
      </div>
    );
  }

  if (status === "anonymous") return <Navigate to="/login" replace />;

  // Regular users must verify their email before accessing the app.
  // Admins are seeded with emailVerified = true and bypass this gate.
  if (user && !user.emailVerified && user.role !== "admin") {
    return <Navigate to="/verify-email" replace />;
  }

  return <Outlet />;
}
