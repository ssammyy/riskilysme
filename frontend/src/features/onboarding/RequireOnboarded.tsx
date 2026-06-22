import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/auth/AuthProvider";

/** Sends authenticated-but-not-yet-onboarded users into the onboarding flow. */
export function RequireOnboarded() {
  const { user } = useAuth();
  if (user && !user.onboardingCompleted) return <Navigate to="/onboarding" replace />;
  return <Outlet />;
}
