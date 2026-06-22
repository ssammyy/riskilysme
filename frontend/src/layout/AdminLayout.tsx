import { useState, type ComponentType } from "react";
import { Link, Navigate, Outlet, useLocation } from "react-router-dom";
import { ShieldCheck, LayoutDashboard, Settings, Menu, X, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import { useAuth } from "@/auth/AuthProvider";

interface NavEntry {
  to: string;
  label: string;
  icon: ComponentType<{ className?: string }>;
  exact?: boolean;
}

export default function AdminLayout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [drawerOpen, setDrawerOpen] = useState(false);

  if (user?.role !== "admin") return <Navigate to="/" replace />;

  const navItems: NavEntry[] = [
    { to: "/admin", label: "Console", icon: LayoutDashboard, exact: true },
    { to: "/admin/settings", label: "Settings", icon: Settings },
  ];

  const isActive = ({ to, exact }: NavEntry) =>
    exact ? location.pathname === to : location.pathname.startsWith(to);

  const activeLabel = navItems.find(isActive)?.label ?? "Admin";

  const SidebarContent = (
    <div className="flex h-full flex-col">
      {/* Admin branding */}
      <div className="p-4">
        <div className="flex items-center gap-3 rounded-xl border border-primary/20 bg-primary/5 p-3">
          <div className="h-9 w-9 rounded-lg bg-primary flex items-center justify-center shrink-0">
            <ShieldCheck className="h-5 w-5 text-white" />
          </div>
          <div className="min-w-0">
            <h2 className="text-sm font-bold text-[#201515] leading-tight">Admin Console</h2>
            <p className="text-[10px] text-muted-foreground mt-0.5">Riskily SME</p>
          </div>
        </div>
      </div>

      <Separator className="bg-border/30" />

      <nav className="flex flex-1 flex-col gap-1 px-3 py-4">
        {navItems.map((item) => {
          const active = isActive(item);
          const Icon = item.icon;
          return (
            <Link
              key={item.to}
              to={item.to}
              onClick={() => setDrawerOpen(false)}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all relative group",
                active
                  ? "bg-primary/5 text-primary font-semibold"
                  : "text-muted-foreground hover:bg-card hover:text-[#201515]",
              )}
            >
              {active && (
                <span className="absolute left-0 top-2.5 bottom-2.5 w-1 rounded-r bg-primary" />
              )}
              <Icon
                className={cn(
                  "h-4 w-4 shrink-0 transition-colors",
                  active ? "text-primary" : "text-muted-foreground group-hover:text-foreground",
                )}
              />
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className="mt-auto border-t border-border/30 p-4 bg-card/45">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="h-9 w-9 rounded-full bg-primary/10 border border-primary/20 text-primary flex items-center justify-center font-bold text-xs shrink-0">
              {(user?.firstName || "A").charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-[#201515] truncate leading-tight">
                {user?.firstName || "Admin"}
              </p>
              <p className="text-[10px] text-muted-foreground truncate mt-0.5">{user?.email}</p>
            </div>
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => logout()}
            className="h-8 w-8 rounded-lg hover:bg-destructive/10 hover:text-destructive shrink-0 text-muted-foreground"
            title="Sign out"
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
              <Button variant="ghost" size="icon" onClick={() => setDrawerOpen(false)}>
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
          >
            <Menu className="h-4 w-4 text-foreground" />
          </Button>

          <div className="flex items-center gap-2 text-sm select-none">
            <span className="text-muted-foreground font-medium">Admin Console</span>
            <span className="text-muted-foreground/40 font-semibold">/</span>
            <span className="font-display font-medium text-[#201515]">{activeLabel}</span>
          </div>
        </header>

        <div className="flex-1 overflow-y-auto">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
