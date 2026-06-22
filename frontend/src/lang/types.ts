import type { en } from "./en";

/** The canonical dictionary shape is inferred from the English source of truth. */
export type Dictionary = typeof en;

/** Swahili (and any non-default language) may be partial; missing keys fall back to English. */
export type DeepPartial<T> = {
  [P in keyof T]?: T[P] extends object ? DeepPartial<T[P]> : T[P];
};

export type LanguageCode = "en" | "sw";
