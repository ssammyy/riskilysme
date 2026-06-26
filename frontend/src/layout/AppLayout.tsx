import { useState, type ComponentType } from "react";
import { Link, Navigate, Outlet, useLocation } from "react-router-dom";
import {
  LayoutDashboard, Bell, Settings, Menu, X, LogOut, Calendar, Sparkles,
  ArrowLeftRight, Droplets, Users, Package, CreditCard, Scale, Globe,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { useAuth } from "@/auth/AuthProvider";
import { useLang } from "@/lang/LanguageProvider";

interface NavEntry {
  to: string;
  label: string;
  icon: ComponentType<{ className?: string }>;
  exact?: boolean;
}

const MODULE_NAV: NavEntry[] = [
  { to: "/modules/fx",           label: "Foreign Exchange",    icon: ArrowLeftRight },
  { to: "/modules/liquidity",    label: "Cash Flow",           icon: Droplets },
  { to: "/modules/counterparty", label: "Customers & Suppliers", icon: Users },
  { to: "/modules/commodity",    label: "Commodity Prices",    icon: Package },
  { to: "/modules/credit",       label: "Credit & Loans",      icon: CreditCard },
  { to: "/modules/regulatory",   label: "Compliance",          icon: Scale },
  { to: "/modules/macro",        label: "Economic Conditions", icon: Globe },
];

/**
 * The single page-level layout wrapper.
 * Persistent left sidebar on lg+, off-canvas drawer on mobile.
 */
export default function AppLayout() {
  const { t, lang, setLang } = useLang();
  const { user, logout } = useAuth();
  const location = useLocation();
  const [drawerOpen, setDrawerOpen] = useState(false);

  if (user?.role === "admin") return <Navigate to="/admin" replace />;

  const primaryNav: NavEntry[] = [
    { to: "/",          label: t.nav.dashboard, icon: LayoutDashboard, exact: true },
    { to: "/deadlines", label: t.nav.deadlines, icon: Calendar },
    { to: "/insights",  label: t.nav.insights,  icon: Sparkles },
    { to: "/alerts",    label: t.nav.alerts,    icon: Bell },
    { to: "/settings",  label: t.nav.settings,  icon: Settings },
  ];

  const isActive = ({ to, exact }: NavEntry) =>
    exact ? location.pathname === to : location.pathname.startsWith(to);

  const activeEntry =
    MODULE_NAV.find((m) => location.pathname.startsWith(m.to)) ??
    primaryNav.find(isActive);
  const pageTitle = activeEntry?.label ?? t.common.appName;

  const NavLink = ({ item, onClick }: { item: NavEntry; onClick?: () => void }) => {
    const active = isActive(item);
    const Icon = item.icon;
    return (
      <Link
        to={item.to}
        onClick={onClick}
        className={cn(
          "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all relative group",
          active
            ? "bg-primary/5 text-primary font-semibold"
            : "text-muted-foreground hover:bg-card hover:text-[#201515]",
        )}
      >
        {active && (
          <span className="absolute left-0 top-2 bottom-2 w-1 rounded-r bg-primary" />
        )}
        <Icon className={cn(
          "h-4 w-4 shrink-0 transition-colors",
          active ? "text-primary" : "text-muted-foreground group-hover:text-foreground",
        )} />
        <span className="truncate">{item.label}</span>
      </Link>
    );
  };

  const SidebarContent = (
    <div className="flex h-full flex-col">
      {/* Workspace card */}
      <div className="p-4">
        <div className="flex items-center gap-3 rounded-xl border border-border/40 bg-card p-3 shadow-sm select-none">
          <div className="h-9 w-9 rounded-lg bg-primary flex items-center justify-center text-white font-black text-xl shrink-0">
            {t.common.appName.charAt(0)}
          </div>
          <div className="min-w-0 flex-1">
            <h2 className="text-sm font-bold text-[#201515] truncate leading-tight">
              {user?.firstName ? `${user.firstName}'s Co.` : t.common.appName}
            </h2>
            <div className="flex items-center gap-1.5 mt-0.5">
              <Badge
                variant="outline"
                className={cn(
                  "text-[9px] font-bold uppercase tracking-wider px-1.5 py-0 rounded",
                  user?.subscriptionTier === "standard"
                    ? "bg-primary/5 text-primary border-primary/20"
                    : "bg-muted text-muted-foreground border-border/50",
                )}
              >
                {user?.subscriptionTier === "standard" ? t.admin.tierStandard : t.admin.tierBasic}
              </Badge>
            </div>
          </div>
        </div>
      </div>

      <Separator className="bg-border/30" />

      <nav className="flex flex-1 flex-col gap-0.5 px-3 py-3 overflow-y-auto">
        {/* Primary nav */}
        {primaryNav.map((item) => (
          <NavLink key={item.to} item={item} onClick={() => setDrawerOpen(false)} />
        ))}

        {/* Risk Modules section */}
        <div className="mt-4 mb-1 px-3">
          <p className="text-[10px] font-bold uppercase tracking-wider text-muted-foreground/60">
            {t.nav.riskModules}
          </p>
        </div>
        {MODULE_NAV.map((item) => (
          <NavLink key={item.to} item={item} onClick={() => setDrawerOpen(false)} />
        ))}
      </nav>

      {/* User footer */}
      <div className="mt-auto border-t border-border/30 p-4 bg-card/45 select-none">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="h-9 w-9 rounded-full bg-primary/10 border border-primary/20 text-primary flex items-center justify-center font-bold text-xs shrink-0">
              {(user?.firstName || "U").charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-[#201515] truncate leading-tight">
                {user?.firstName || "User"}
              </p>
              <p className="text-[10px] text-muted-foreground truncate mt-0.5">{user?.email}</p>
            </div>
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => logout()}
            className="h-8 w-8 rounded-lg hover:bg-destructive/10 hover:text-destructive shrink-0 text-muted-foreground"
            title={t.dashboard.signOut}
          >
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );

  return (
    <div className="flex min-h-screen bg-background">
      {/* Desktop sidebar */}
      <aside className="hidden w-64 shrink-0 border-r border-border bg-card/45 lg:flex lg:flex-col sticky top-0 h-screen max-h-screen">
        {SidebarContent}
      </aside>

      {/* Mobile drawer */}
      {drawerOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="absolute inset-0 bg-[#201515]/20 backdrop-blur-sm"
            onClick={() => setDrawerOpen(false)}
            aria-hidden
          />
          <aside className="absolute left-0 top-0 h-full w-64 border-r border-border bg-background shadow-xl flex flex-col">
            <div className="flex justify-end p-2 border-b border-border/30">
              <Button variant="ghost" size="icon" onClick={() => setDrawerOpen(false)} aria-label={t.nav.menu}>
                <X className="h-5 w-5" />
              </Button>
            </div>
            <div className="flex-1 overflow-y-auto">{SidebarContent}</div>
          </aside>
        </div>
      )}

      {/* Main column */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b border-border/30 bg-background/85 px-6 backdrop-blur-md">
          <Button
            variant="ghost"
            size="icon"
            className="lg:hidden h-9 w-9 rounded-lg border border-border/50 bg-white shadow-sm shrink-0"
            onClick={() => setDrawerOpen(true)}
            aria-label={t.nav.menu}
          >
            <Menu className="h-4 w-4 text-foreground" />
          </Button>

          <div className="flex items-center gap-2 text-sm select-none">
            <span className="text-muted-foreground font-medium">Riskily</span>
            <span className="text-muted-foreground/40 font-semibold">/</span>
            <span className="font-display font-medium text-[#201515]">{pageTitle}</span>
          </div>

          <div className="ml-auto flex items-center gap-2">
            <div className="flex items-center rounded-lg border border-border/40 p-0.5 bg-card/60">
              <button
                onClick={() => setLang("en")}
                className={cn(
                  "rounded-md px-2.5 py-1 text-[10px] font-bold transition-all",
                  lang === "en" ? "bg-white text-primary shadow-sm" : "text-muted-foreground hover:text-foreground",
                )}
              >
                EN
              </button>
              <button
                onClick={() => setLang("sw")}
                className={cn(
                  "rounded-md px-2.5 py-1 text-[10px] font-bold transition-all",
                  lang === "sw" ? "bg-white text-primary shadow-sm" : "text-muted-foreground hover:text-foreground",
                )}
              >
                SW
              </button>
            </div>

            {user && (
              <>
                <Separator orientation="vertical" className="mx-1 h-5 bg-border/40" />
                <Badge
                  variant="outline"
                  className="hidden border-border/40 bg-card/60 text-muted-foreground text-[10px] font-bold uppercase tracking-wider rounded-md px-2 py-0.5 sm:inline"
                >
                  {user.subscriptionTier === "standard" ? t.admin.tierStandard : t.admin.tierBasic}
                </Badge>
              </>
            )}
          </div>
        </header>

        <div className="flex-1 overflow-y-auto">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
