export interface UserSummary {
  id: number;
  email: string;
  firstName: string | null;
  role: "user" | "admin";
  subscriptionTier: "basic" | "standard";
  language: "en" | "sw";
  onboardingCompleted: boolean;
  emailVerified: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: UserSummary;
}

export interface RegisterPayload {
  email: string;
  password: string;
  firstName?: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}
