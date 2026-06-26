import { Link } from "react-router-dom";
import { ArrowLeft, Calendar } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useLang } from "@/lang/LanguageProvider";
import { useAuth } from "@/auth/AuthProvider";
import { DeadlinesGrid } from "./DeadlinesGrid";

export default function DeadlinesPage() {
  const { t } = useLang();
  const { user } = useAuth();
  const tier = user?.subscriptionTier ?? "basic";

  return (
    <main className="min-h-screen bg-background px-6 py-8 sm:px-8">
      <div className="mx-auto max-w-3xl flex flex-col gap-6">
        {/* Header */}
        <div className="flex flex-col gap-4 border-b border-border/20 pb-4">
          <Button variant="ghost" size="sm" asChild className="self-start -ml-2 text-muted-foreground hover:text-[#201515]">
            <Link to="/">
              <ArrowLeft className="h-4 w-4 mr-1.5" />
              {t.dashboard.backToDashboard}
            </Link>
          </Button>
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
              <Calendar className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h1 className="font-display text-2xl font-bold tracking-tight text-[#201515]">
                {t.deadlines.title}
              </h1>
              <p className="text-xs text-muted-foreground mt-0.5">
                {t.deadlines.pageSubtitle}
              </p>
            </div>
          </div>
        </div>

        {/* Full grid — subscriptionTier controls the cap + upgrade prompt */}
        <DeadlinesGrid subscriptionTier={tier} />
      </div>
    </main>
  );
}
