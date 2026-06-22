export type AlertSeverity = "URGENT" | "ACT_NOW" | "WATCH_OUT";

export interface AlertItem {
  id: number;
  severity: AlertSeverity;
  moduleCode: string | null;
  title: string;
  body: string;
  isRead: boolean;
  createdAt: string;
}

export interface AlertsPageData {
  alerts: AlertItem[];
  monthlyUsed: number;
  monthlyCap: number; // Int.MAX_VALUE sentinel (2147483647) means Standard (unlimited)
}
