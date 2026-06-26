import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "@/features/auth/LoginPage";
import RegisterPage from "@/features/auth/RegisterPage";
import ForgotPasswordPage from "@/features/auth/ForgotPasswordPage";
import ResetPasswordPage from "@/features/auth/ResetPasswordPage";
import VerifyEmailPage from "@/features/auth/VerifyEmailPage";
import DashboardPage from "@/features/dashboard/DashboardPage";
import ModuleDetailPage from "@/features/dashboard/modules/ModuleDetailPage";
import AlertsPage from "@/features/alerts/AlertsPage";
import DeadlinesPage from "@/features/dashboard/DeadlinesPage";
import InsightsPage from "@/features/dashboard/InsightsPage";
import SettingsPage from "@/features/settings/SettingsPage";
import AdminPage from "@/features/admin/AdminPage";
import OnboardingPage from "@/features/onboarding/OnboardingPage";
import { ProtectedRoute } from "@/features/auth/ProtectedRoute";
import { RequireAdmin } from "@/features/admin/RequireAdmin";
import { RequireOnboarded } from "@/features/onboarding/RequireOnboarded";
import AppLayout from "@/layout/AppLayout";
import AdminLayout from "@/layout/AdminLayout";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot" element={<ForgotPasswordPage />} />
      <Route path="/reset" element={<ResetPasswordPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/onboarding" element={<OnboardingPage />} />

        {/* Client routes */}
        <Route element={<RequireOnboarded />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/modules/:code" element={<ModuleDetailPage />} />
            <Route path="/deadlines" element={<DeadlinesPage />} />
            <Route path="/insights" element={<InsightsPage />} />
            <Route path="/alerts" element={<AlertsPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Route>
        </Route>

        {/* Admin routes — own layout, no onboarding gate */}
        <Route element={<RequireAdmin />}>
          <Route element={<AdminLayout />}>
            <Route path="/admin" element={<AdminPage />} />
            <Route path="/admin/settings" element={<SettingsPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
