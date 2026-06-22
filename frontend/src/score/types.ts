export type BandCode = "all_good" | "watch_out" | "act_now" | "urgent";

export type ModuleCode = "FX" | "LIQUIDITY" | "COUNTERPARTY" | "COMMODITY" | "CREDIT" | "REGULATORY" | "MACRO";

export interface ModuleScore {
  code: ModuleCode;
  exposure: number;
  pressure: number;
  health: number;
  band: BandCode;
  dataConfidence: "profile" | "live";
  isProvisional: boolean;
}

export interface ScoreResponse {
  overallHealth: number;
  overallBand: BandCode;
  modules: ModuleScore[];
  calculatedAt: string; // ISO-8601 from the backend
}
