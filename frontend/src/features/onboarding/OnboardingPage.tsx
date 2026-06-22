import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { apiFetch } from "@/lib/api";
import { useLang } from "@/lang/LanguageProvider";
import { useAuth } from "@/auth/AuthProvider";
import type { ScoreResponse } from "@/score/types";
import {
  Globe,
  Landmark,
  Percent,
  Clock,
  TrendingUp,
  Users,
  CalendarRange,
  Truck,
  Smartphone,
  Check,
  ChevronLeft,
  ChevronRight,
  Building2,
  Languages,
  Sparkles
} from "lucide-react";

// ---------------------------------------------------------------------------
// Types & Constants
// ---------------------------------------------------------------------------

type InterestRateBand = "below_15" | "15_to_20" | "above_20" | "not_sure";

interface OnboardingPayload {
  businessName: string;
  q1FxYes: boolean;
  q2LoansYes: boolean;
  q2bInterestRate?: string;
  q3CreditSalesYes: boolean;
  q4FixedCostsYes: boolean;
  q5ConcentrationYes: boolean;
  q6CashTimingYes: boolean;
  q7SupplierDepYes: boolean;
  q8InformalCreditYes: boolean;
}

type StepId =
  | "lang"
  | "name"
  | "q1"
  | "q2"
  | "q2b"
  | "q3"
  | "q4"
  | "q5"
  | "q6"
  | "q7"
  | "q8"
  | "done";

const TOTAL_Q = 8;

const Q_NUMBERED: StepId[] = ["q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8"];
const Q_ALL: StepId[] = ["q1", "q2", "q2b", "q3", "q4", "q5", "q6", "q7", "q8"];

// ---------------------------------------------------------------------------
// Score Gauge SVG Path Helpers (matching ScoreGauge.tsx style)
// ---------------------------------------------------------------------------

const BAND_VARIANT: Record<string, "good" | "watch" | "act" | "urgent"> = {
  all_good: "good",
  watch_out: "watch",
  act_now: "act",
  urgent: "urgent",
};

const ARC_COLOR: Record<string, string> = {
  all_good: "hsl(var(--band-good))",
  watch_out: "hsl(var(--band-watch))",
  act_now: "hsl(var(--band-act))",
  urgent: "hsl(var(--band-urgent))",
};

function toArcPath(score: number): string {
  const R = 48;
  const cx = 60;
  const cy = 60;
  const startAngle = -225; // degrees, measured clockwise from positive-x axis
  const sweepAngle = 270;
  const fraction = Math.min(Math.max(score, 0), 100) / 100;
  const endAngle = startAngle + sweepAngle * fraction;

  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const x1 = cx + R * Math.cos(toRad(startAngle));
  const y1 = cy + R * Math.sin(toRad(startAngle));
  const x2 = cx + R * Math.cos(toRad(endAngle));
  const y2 = cy + R * Math.sin(toRad(endAngle));
  const largeArc = sweepAngle * fraction > 180 ? 1 : 0;

  return `M ${x1} ${y1} A ${R} ${R} 0 ${largeArc} 1 ${x2} ${y2}`;
}

function toTrackPath(): string {
  const R = 48;
  const cx = 60;
  const cy = 60;
  const startAngle = -225;
  const endAngle = startAngle + 270;
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const x1 = cx + R * Math.cos(toRad(startAngle));
  const y1 = cy + R * Math.sin(toRad(startAngle));
  const x2 = cx + R * Math.cos(toRad(endAngle));
  const y2 = cy + R * Math.sin(toRad(endAngle));
  return `M ${x1} ${y1} A ${R} ${R} 0 1 1 ${x2} ${y2}`;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function OnboardingPage() {
  const { t, lang, setLang } = useLang();
  const { refreshUser } = useAuth();
  const navigate = useNavigate();

  // Form state
  const [businessName, setBusinessName] = useState("");
  const [q1, setQ1] = useState<boolean | null>(null);
  const [q2, setQ2] = useState<boolean | null>(null);
  const [q2b, setQ2b] = useState<InterestRateBand | null>(null);
  const [q3, setQ3] = useState<boolean | null>(null);
  const [q4, setQ4] = useState<boolean | null>(null);
  const [q5, setQ5] = useState<boolean | null>(null);
  const [q6, setQ6] = useState<boolean | null>(null);
  const [q7, setQ7] = useState<boolean | null>(null);
  const [q8, setQ8] = useState<boolean | null>(null);

  const [score, setScore] = useState<ScoreResponse | null>(null);
  const [stepId, setStepId] = useState<StepId>("lang");

  // Sequence is dynamic: Q2b only appears when Q2 = Yes
  const getSequence = (): StepId[] => {
    const seq: StepId[] = ["lang", "name", "q1", "q2"];
    if (q2 === true) seq.push("q2b");
    return [...seq, "q3", "q4", "q5", "q6", "q7", "q8", "done"];
  };

  const sequence = getSequence();
  const currentIdx = sequence.indexOf(stepId);

  const isQuestionStep = Q_ALL.includes(stepId);
  // 1-based question number; Q2b returns 2 (same question group)
  const questionNumber = stepId === "q2b" ? 2 : Q_NUMBERED.indexOf(stepId) + 1;

  const progressValue = isQuestionStep
    ? (Math.max(0, Q_NUMBERED.indexOf(stepId === "q2b" ? "q2" : stepId)) / TOTAL_Q) * 100
    : stepId === "done"
    ? 100
    : 0;

  // Navigation
  const goNext = () => {
    const next = sequence[currentIdx + 1];
    if (next) setStepId(next);
  };

  const goBack = () => {
    const prev = sequence[currentIdx - 1];
    if (prev) setStepId(prev);
  };

  // Submit
  const submitMutation = useMutation({
    mutationFn: (payload: OnboardingPayload) =>
      apiFetch<ScoreResponse>("/onboarding", {
        method: "POST",
        body: JSON.stringify(payload),
      }),
    onSuccess: async (data) => {
      setScore(data);
      await refreshUser();
      setStepId("done");
    },
  });

  const handleSubmit = () => {
    submitMutation.mutate({
      businessName: businessName.trim(),
      q1FxYes: q1 ?? false,
      q2LoansYes: q2 ?? false,
      q2bInterestRate: q2 === true ? (q2b ?? "not_sure") : undefined,
      q3CreditSalesYes: q3 ?? false,
      q4FixedCostsYes: q4 ?? false,
      q5ConcentrationYes: q5 ?? false,
      q6CashTimingYes: q6 ?? false,
      q7SupplierDepYes: q7 ?? false,
      q8InformalCreditYes: q8 ?? false,
    });
  };

  const canAdvance = (): boolean => {
    switch (stepId) {
      case "lang":   return true;
      case "name":   return businessName.trim().length > 0;
      case "q1":     return q1 !== null;
      case "q2":     return q2 !== null;
      case "q2b":    return q2b !== null;
      case "q3":     return q3 !== null;
      case "q4":     return q4 !== null;
      case "q5":     return q5 !== null;
      case "q6":     return q6 !== null;
      case "q7":     return q7 !== null;
      case "q8":     return q8 !== null;
      default:       return false;
    }
  };

  // Helper to map current steps to visual roadmap groups
  const getStepGroup = (step: StepId): number => {
    if (step === "lang") return 0;
    if (step === "name") return 1;
    if (step === "done") return 3;
    return 2;
  };

  // Helper to resolve specific topic icon for each question
  const getQuestionIcon = (step: StepId) => {
    switch (step) {
      case "q1": return <Globe className="h-6 w-6 text-primary" />;
      case "q2": return <Landmark className="h-6 w-6 text-primary" />;
      case "q2b": return <Percent className="h-6 w-6 text-primary" />;
      case "q3": return <Clock className="h-6 w-6 text-primary" />;
      case "q4": return <TrendingUp className="h-6 w-6 text-primary" />;
      case "q5": return <Users className="h-6 w-6 text-primary" />;
      case "q6": return <CalendarRange className="h-6 w-6 text-primary" />;
      case "q7": return <Truck className="h-6 w-6 text-primary" />;
      case "q8": return <Smartphone className="h-6 w-6 text-primary" />;
      default: return null;
    }
  };

  // Helper to resolve the Risk module translation label
  const getQuestionCategory = (step: StepId): string => {
    switch (step) {
      case "q1": return t.modules.names.FX;
      case "q2":
      case "q2b":
      case "q8": return t.modules.names.CREDIT;
      case "q3":
      case "q6": return t.modules.names.LIQUIDITY;
      case "q4": return t.modules.names.COMMODITY;
      case "q5":
      case "q7": return t.modules.names.COUNTERPARTY;
      default: return "";
    }
  };

  // ---------------------------------------------------------------------------
  // Redesigned Yes / No Choice Cards
  // ---------------------------------------------------------------------------

  const YesNoButtons = ({
    value,
    onChange,
  }: {
    value: boolean | null;
    onChange: (v: boolean) => void;
  }) => (
    <div className="grid grid-cols-2 gap-4 mt-6">
      {[
        { val: true, text: t.onboarding.yes },
        { val: false, text: t.onboarding.no }
      ].map(({ val, text }) => {
        const isSelected = value === val;
        return (
          <button
            key={String(val)}
            type="button"
            onClick={() => onChange(val)}
            className={[
              "relative rounded-xl border-2 p-5 flex flex-col text-left transition-all duration-200 group focus:outline-none focus:ring-2 focus:ring-primary/20",
              isSelected
                ? "border-primary bg-[#ff4f00]/5 text-[#201515]"
                : "border-border/60 bg-card/40 text-foreground hover:border-primary/40 hover:bg-card",
            ].join(" ")}
          >
            <div className="flex items-center justify-between w-full mb-1">
              <span className={`text-lg font-bold ${isSelected ? 'text-[#ff4f00]' : 'text-[#201515]'}`}>
                {text}
              </span>
              <span className={[
                "h-5 w-5 rounded-full border flex items-center justify-center transition-all duration-200 shrink-0",
                isSelected
                  ? "border-primary bg-primary text-white"
                  : "border-border group-hover:border-primary/40",
              ].join(" ")}>
                {isSelected && <Check className="h-3 w-3 stroke-[3]" />}
              </span>
            </div>
            <span className="text-xs text-muted-foreground mt-1.5 leading-normal">
              {val 
                ? (lang === "en" ? "Matches my business operations" : "Inahusu uendeshaji wangu") 
                : (lang === "en" ? "Not applicable to me" : "Haifanyi kazi hapa")}
            </span>
          </button>
        );
      })}
    </div>
  );

  // ---------------------------------------------------------------------------
  // Step content renders
  // ---------------------------------------------------------------------------

  const renderStep = () => {
    // Language selection step
    if (stepId === "lang") {
      return (
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
              <Languages className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-[#201515]">{t.onboarding.lang.title}</h1>
              <p className="text-sm text-muted-foreground mt-0.5">{t.onboarding.lang.subtitle}</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 mt-6">
            {(["en", "sw"] as const).map((code) => {
              const isSelected = lang === code;
              return (
                <button
                  key={code}
                  type="button"
                  onClick={() => setLang(code)}
                  className={[
                    "relative rounded-xl border-2 p-6 flex flex-col items-center justify-center text-center transition-all duration-200 group focus:outline-none focus:ring-2 focus:ring-primary/20",
                    isSelected
                      ? "border-primary bg-[#ff4f00]/5 text-[#201515]"
                      : "border-border/60 bg-card/40 text-foreground hover:border-primary/40 hover:bg-card",
                  ].join(" ")}
                >
                  <span className="text-lg font-bold text-[#201515]">
                    {code === "en" ? t.onboarding.lang.english : t.onboarding.lang.swahili}
                  </span>
                  <span className="text-xs text-muted-foreground mt-1">
                    {code === "en" ? "English interface" : "Kiolesura cha Kiswahili"}
                  </span>
                  {isSelected && (
                    <span className="absolute top-3 right-3 h-5 w-5 rounded-full bg-primary text-white flex items-center justify-center">
                      <Check className="h-3 w-3 stroke-[3]" />
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      );
    }

    // Business Name step
    if (stepId === "name") {
      return (
        <div className="space-y-6">
          <div className="flex items-center gap-3">
            <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
              <Building2 className="h-6 w-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-[#201515]">{t.onboarding.name.title}</h1>
              <p className="text-sm text-muted-foreground mt-0.5">{lang === "en" ? "Let's set up your profile." : "Wacha tuweke wasifu wako."}</p>
            </div>
          </div>
          <div className="space-y-4 pt-2">
            <div className="space-y-2">
              <Label htmlFor="businessName" className="text-sm font-semibold text-[#201515]">
                {t.onboarding.name.businessName}
              </Label>
              <Input
                id="businessName"
                value={businessName}
                onChange={(e) => setBusinessName(e.target.value)}
                autoComplete="organization"
                placeholder={t.onboarding.name.businessName}
                className="rounded-xl border border-border bg-white h-12 px-4 focus-visible:ring-2 focus-visible:ring-primary focus-visible:border-primary transition-all text-base text-[#201515]"
              />
              <p className="text-xs text-muted-foreground mt-1 leading-normal">
                {lang === "en" ? "This will appear on your Riskily SME dashboard." : "Hii itaonekana kwenye dashibodi yako ya Riskily SME."}
              </p>
            </div>
          </div>
        </div>
      );
    }

    // Yes/No Question steps (Q1 - Q8)
    type QKey = "q1" | "q2" | "q3" | "q4" | "q5" | "q6" | "q7" | "q8";
    const yesNoMap: Record<
      QKey,
      { q: { question: string; hint?: string }; getter: boolean | null; setter: (v: boolean) => void }
    > = {
      q1: { q: t.onboarding.q1, getter: q1, setter: setQ1 },
      q2: {
        q: t.onboarding.q2,
        getter: q2,
        setter: (v) => {
          setQ2(v);
          if (!v) setQ2b(null);
        },
      },
      q3: { q: t.onboarding.q3, getter: q3, setter: setQ3 },
      q4: { q: t.onboarding.q4, getter: q4, setter: setQ4 },
      q5: { q: t.onboarding.q5, getter: q5, setter: setQ5 },
      q6: { q: t.onboarding.q6, getter: q6, setter: setQ6 },
      q7: { q: t.onboarding.q7, getter: q7, setter: setQ7 },
      q8: { q: t.onboarding.q8, getter: q8, setter: setQ8 },
    };

    if (stepId in yesNoMap) {
      const { q, getter, setter } = yesNoMap[stepId as QKey];
      return (
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-lg border border-border bg-card/60 flex items-center justify-center text-[#201515] shrink-0">
              {getQuestionIcon(stepId)}
            </div>
            <div className="flex flex-col gap-0.5">
              <div className="flex items-center gap-2">
                <Badge variant="outline" className="border-border bg-card/60 text-muted-foreground text-[10px] font-bold tracking-wider uppercase rounded px-2.5 py-0.5">
                  {getQuestionCategory(stepId)}
                </Badge>
                <Badge variant="primary" className="text-[10px] font-bold tracking-wide rounded px-2 py-0.5 bg-[#ff4f00] text-white border-none">
                  Q{questionNumber} {t.onboarding.questionOf} {TOTAL_Q}
                </Badge>
              </div>
            </div>
          </div>
          <h1 className="text-xl font-bold leading-snug text-[#201515] pt-1">{q.question}</h1>
          {q.hint && <p className="text-sm text-muted-foreground leading-relaxed">{q.hint}</p>}
          <YesNoButtons value={getter} onChange={setter} />
        </div>
      );
    }

    // Interest rate band step (Q2b)
    if (stepId === "q2b") {
      const opts = t.onboarding.q2b.options;
      const bands: { key: InterestRateBand; label: string }[] = [
        { key: "below_15",  label: opts.below_15 },
        { key: "15_to_20",  label: opts["15_to_20"] },
        { key: "above_20",  label: opts.above_20 },
        { key: "not_sure",  label: opts.not_sure },
      ];
      return (
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-lg border border-border bg-card/60 flex items-center justify-center text-[#201515] shrink-0">
              {getQuestionIcon(stepId)}
            </div>
            <div className="flex flex-col gap-0.5">
              <div className="flex items-center gap-2">
                <Badge variant="outline" className="border-border bg-card/60 text-muted-foreground text-[10px] font-bold tracking-wider uppercase rounded px-2.5 py-0.5">
                  {getQuestionCategory(stepId)}
                </Badge>
                <Badge variant="primary" className="text-[10px] font-bold tracking-wide rounded px-2 py-0.5 bg-[#ff4f00] text-white border-none">
                  Q2 {t.onboarding.questionOf} {TOTAL_Q}
                </Badge>
              </div>
            </div>
          </div>
          <h1 className="text-xl font-bold leading-snug text-[#201515] pt-1">{t.onboarding.q2b.question}</h1>
          <p className="text-sm text-muted-foreground leading-relaxed">{t.onboarding.q2b.hint}</p>
          <div className="grid grid-cols-1 gap-3 mt-6 sm:grid-cols-2">
            {bands.map(({ key, label }) => {
              const isSelected = q2b === key;
              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => setQ2b(key)}
                  className={[
                    "relative rounded-xl border-2 p-5 text-left transition-all duration-200 group focus:outline-none focus:ring-2 focus:ring-primary/20 flex items-center justify-between",
                    isSelected
                      ? "border-primary bg-[#ff4f00]/5 text-[#201515]"
                      : "border-border/60 bg-card/40 text-foreground hover:border-primary/40 hover:bg-card",
                  ].join(" ")}
                >
                  <div className="flex flex-col">
                    <span className="text-base font-bold text-[#201515]">{label}</span>
                    <span className="text-xs text-muted-foreground mt-0.5">
                      {key === "below_15" && (lang === "en" ? "Low rate bracket" : "Kiwango cha chini")}
                      {key === "15_to_20" && (lang === "en" ? "Standard market rate" : "Kiwango cha kawaida")}
                      {key === "above_20" && (lang === "en" ? "High cost funding" : "Gharama ya juu")}
                      {key === "not_sure" && (lang === "en" ? "Estimate later" : "Utashughulikia baadaye")}
                    </span>
                  </div>
                  <span className={[
                    "h-5 w-5 rounded-full border flex items-center justify-center transition-all duration-200 shrink-0 ml-3",
                    isSelected
                      ? "border-primary bg-primary text-white"
                      : "border-border group-hover:border-primary/40",
                  ].join(" ")}>
                    {isSelected && <Check className="h-3 w-3 stroke-[3]" />}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      );
    }

    // Done step showing calculated Business Health Score Gauge
    if (stepId === "done") {
      const bandKey = (score?.overallBand ?? "all_good") as keyof typeof t.score.band;
      const scoreVal = score?.overallHealth ?? 0;
      const bandLabel = t.score.band[bandKey];
      const bandDescription = t.score.bandDescription[bandKey];
      const arcColor = ARC_COLOR[bandKey] || "hsl(var(--primary))";

      return (
        <div className="space-y-6 text-center py-4">
          <div className="inline-flex items-center justify-center h-16 w-16 rounded-full bg-primary/10 text-primary mb-2">
            <Sparkles className="h-8 w-8" />
          </div>
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-[#201515]">{t.onboarding.done.title}</h1>
            <p className="text-sm text-muted-foreground mt-1.5">{t.onboarding.done.subtitle}</p>
          </div>

          <div className="flex flex-col items-center gap-6 py-6 bg-card/45 rounded-2xl border border-border/40 max-w-sm mx-auto">
            {/* SVG Gauge */}
            <div className="relative flex shrink-0 items-center justify-center" aria-hidden="true">
              <svg width="140" height="140" viewBox="0 0 120 120">
                {/* Track */}
                <path
                  d={toTrackPath()}
                  fill="none"
                  stroke="hsl(var(--muted))"
                  strokeWidth="8"
                  strokeLinecap="round"
                />
                {/* Arc */}
                {scoreVal > 0 && (
                  <path
                    d={toArcPath(scoreVal)}
                    fill="none"
                    stroke={arcColor}
                    strokeWidth="8"
                    strokeLinecap="round"
                  />
                )}
              </svg>
              {/* Score label centred inside gauge */}
              <div className="absolute flex flex-col items-center">
                <span className="font-display text-4xl font-extrabold leading-none text-[#201515]">
                  {scoreVal}
                </span>
                <span className="text-xs text-muted-foreground mt-0.5">/100</span>
              </div>
            </div>

            {/* Text info */}
            <div className="flex flex-col gap-1 px-6">
              <div className="inline-flex items-center justify-center">
                <Badge variant={BAND_VARIANT[bandKey]} className="px-3 py-1 text-xs font-semibold rounded-full">
                  {bandLabel}
                </Badge>
              </div>
              <p className="text-sm text-muted-foreground mt-2 max-w-[240px] leading-relaxed">
                {bandDescription}
              </p>
            </div>
          </div>

          <Button 
            className="w-full h-12 rounded-xl text-base font-bold bg-primary text-primary-foreground hover:bg-primary/90 mt-4 transition-all hover:scale-[1.01]" 
            size="lg" 
            onClick={() => navigate("/dashboard")}
          >
            {t.onboarding.done.continue}
          </Button>
        </div>
      );
    }

    return null;
  };

  // ---------------------------------------------------------------------------
  // Shell UI Layout
  // ---------------------------------------------------------------------------

  const showProgress = isQuestionStep;
  const showNav = stepId !== "done";
  const showBack = currentIdx > 0 && stepId !== "lang";
  const isLastQuestion = stepId === "q8";

  return (
    <div className="min-h-screen bg-[#fffefb] flex flex-col lg:grid lg:grid-cols-12 text-[#201515]">
      {/* Left Panel: Brand info & dynamic help */}
      <div className="hidden lg:flex lg:col-span-5 bg-[#f8f4f0] text-[#201515] p-12 flex-col justify-between relative overflow-hidden border-r border-border/30">
        {/* Subtle grid pattern background */}
        <div className="absolute inset-0 opacity-[0.04] pointer-events-none bg-[radial-gradient(#201515_1px,transparent_1px)] [background-size:16px_16px]"></div>
        
        {/* App Branding */}
        <div className="flex items-center gap-3 z-10">
          <div className="h-9 w-9 rounded-lg bg-[#ff4f00] flex items-center justify-center text-white font-black text-xl">
            R
          </div>
          <span className="font-semibold text-xl tracking-tight text-[#201515]">{t.common.appName}</span>
        </div>

        {/* Stepper Roadmap */}
        <div className="my-auto py-12 space-y-8 z-10">
          {[
            { label: t.onboarding.roadmap.lang, idx: 0 },
            { label: t.onboarding.roadmap.name, idx: 1 },
            { label: t.onboarding.roadmap.diagnostic, idx: 2 },
            { label: t.onboarding.roadmap.done, idx: 3 },
          ].map((s) => {
            const currentStepGroup = getStepGroup(stepId);
            const isActive = currentStepGroup === s.idx;
            const isCompleted = currentStepGroup > s.idx;
            
            return (
              <div key={s.idx} className="flex items-center gap-3 group">
                <span className={[
                  "h-6 w-6 rounded-full flex items-center justify-center font-bold text-xs transition-all duration-300 shrink-0",
                  isActive
                    ? "bg-[#ff4f00] text-white ring-4 ring-[#ff4f00]/15"
                    : isCompleted
                    ? "border border-primary bg-white text-[#ff4f00]"
                    : "border border-border/80 bg-white text-muted-foreground/60",
                ].join(" ")}>
                  {isCompleted ? <Check className="h-3 w-3 stroke-[3]" /> : s.idx + 1}
                </span>
                <span className={[
                  "font-medium text-sm transition-colors duration-300",
                  isActive ? "text-[#201515] font-semibold" : "text-muted-foreground/80 group-hover:text-foreground",
                ].join(" ")}>
                  {s.label}
                </span>
              </div>
            );
          })}
        </div>

        {/* Dynamic Helpful Guideline Panel */}
        <div className="mt-auto bg-white border border-border/50 rounded-2xl p-6 z-10 max-w-md shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-wider text-[#ff4f00] mb-2">
            {t.onboarding.whyMattersTitle}
          </p>
          <p className="text-sm text-[#201515]/90 leading-relaxed font-normal">
            {t.onboarding.whyItMatters[stepId as keyof typeof t.onboarding.whyItMatters] || 
             (lang === "en" ? "Every response helps paint an accurate risk intelligence picture for your business." : "Kila jibu husaidia kuchora picha sahihi ya akili ya hatari kwa biashara yako.")}
          </p>
        </div>
      </div>

      {/* Right Panel: Questionnaire */}
      <div className="flex-1 lg:col-span-7 flex flex-col justify-between p-6 sm:p-12 lg:p-16 min-h-screen bg-[#fffefb]">
        {/* Top Header Row (Mobile roadmap & Language switcher info) */}
        <div className="w-full flex items-center justify-between gap-4 mb-6">
          <div className="lg:hidden flex items-center gap-2">
            <div className="h-7 w-7 rounded bg-[#ff4f00] flex items-center justify-center text-[#fffefb] font-bold text-sm">
              R
            </div>
            <span className="font-bold text-sm text-[#201515]">{t.common.appName.split(" ")[0]}</span>
          </div>

          <div className="text-right ml-auto">
            <span className="text-xs text-muted-foreground uppercase tracking-wider font-semibold">
              {t.onboarding.diagnosticProgress}
            </span>
          </div>
        </div>

        {/* Form Container */}
        <div className="max-w-xl w-full mx-auto my-auto py-8">
          {showProgress && (
            <div className="mb-8">
              <div className="flex justify-between items-center text-xs text-muted-foreground mb-2">
                <span className="font-medium text-primary">
                  {t.onboarding.stepLabel} {questionNumber} {t.onboarding.questionOf} {TOTAL_Q}
                </span>
                <span>{Math.round(progressValue)}% {lang === "en" ? "complete" : "imekamilika"}</span>
              </div>
              <Progress value={progressValue} className="h-1.5 bg-[#f8f4f0]" />
            </div>
          )}

          <div className="transition-all duration-300">
            {renderStep()}
          </div>
        </div>

        {/* Bottom Navigation */}
        <div className="max-w-xl w-full mx-auto pt-6 mt-8 border-t border-border/40">
          {/* Mobile-only "Why this matters" brief accordion */}
          {stepId !== "done" && (
            <div className="lg:hidden mb-6 p-4 rounded-xl bg-[#f8f4f0] border border-border/30">
              <p className="text-xs font-bold uppercase text-primary mb-1">{t.onboarding.whyMattersTitle}</p>
              <p className="text-xs text-muted-foreground">
                {t.onboarding.whyItMatters[stepId as keyof typeof t.onboarding.whyItMatters] || 
                 (lang === "en" ? "Every response helps paint an accurate risk intelligence picture for your business." : "Kila jibu husaidia kuchora picha sahihi ya akili ya hatari kwa biashara yako.")}
              </p>
            </div>
          )}

          {showNav && (
            <div className="space-y-4">
              <div className="flex gap-4 items-center justify-between">
                {showBack ? (
                  <Button 
                    variant="outline" 
                    onClick={goBack} 
                    disabled={submitMutation.isPending}
                    className="rounded-xl border border-border/80 text-foreground bg-white hover:bg-[#f8f4f0] h-12 px-6 font-semibold transition-all flex items-center gap-2"
                  >
                    <ChevronLeft className="h-4 w-4" />
                    {t.onboarding.back}
                  </Button>
                ) : (
                  <div />
                )}

                {isLastQuestion ? (
                  <Button
                    onClick={handleSubmit}
                    disabled={!canAdvance() || submitMutation.isPending}
                    className="rounded-xl bg-primary text-primary-foreground hover:bg-primary/95 h-12 px-8 font-bold min-w-36 transition-all hover:scale-[1.01] flex items-center justify-center gap-2"
                  >
                    {submitMutation.isPending ? (
                      <span className="h-5 w-5 border-2 border-primary-foreground/35 border-t-primary-foreground rounded-full animate-spin" />
                    ) : (
                      <>
                        {t.onboarding.finish}
                        <ChevronRight className="h-4 w-4" />
                      </>
                    )}
                  </Button>
                ) : (
                  <Button
                    onClick={goNext}
                    disabled={!canAdvance()}
                    className="rounded-xl bg-primary text-primary-foreground hover:bg-primary/95 h-12 px-8 font-bold min-w-36 transition-all hover:scale-[1.01] flex items-center justify-center gap-2 ml-auto"
                  >
                    {t.onboarding.next}
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                )}
              </div>
              
              {submitMutation.isError && (
                <div className="p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-sm text-destructive mt-3">
                  {(submitMutation.error as Error).message}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
