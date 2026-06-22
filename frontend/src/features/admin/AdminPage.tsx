import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { UpgradePrompt } from "@/components/UpgradePrompt";
import { useAuth } from "@/auth/AuthProvider";
import { useLang } from "@/lang/LanguageProvider";
import { apiFetch } from "@/lib/api";
import { AdminEntitlements } from "./AdminEntitlements";

interface CbkRateItem {
  id: number;
  rateType: string;
  rateValue: number;
  effectiveDate: string;
  setBy: string;
}

interface Overview {
  totalUsers: number;
  basicUsers: number;
  standardUsers: number;
}

interface AuditEntry {
  id: number;
  action: string;
  entity: string | null;
  performedBy: string;
  timestamp: string;
}

interface AdminUser {
  id: number;
  email: string;
  firstName: string | null;
  subscriptionTier: "basic" | "standard";
  onboardingCompleted: boolean;
  createdAt: string;
}

interface PreviewTierResponse {
  previewTier: "basic" | "standard" | null;
}

export default function AdminPage() {
  const { t } = useLang();
  const { user } = useAuth();
  const qc = useQueryClient();

  const overview = useQuery({
    queryKey: ["admin", "overview"],
    queryFn: () => apiFetch<Overview>("/admin/overview"),
  });

  const audit = useQuery({
    queryKey: ["admin", "audit-log"],
    queryFn: () => apiFetch<AuditEntry[]>("/admin/audit-log"),
  });

  const users = useQuery({
    queryKey: ["admin", "users"],
    queryFn: () => apiFetch<AdminUser[]>("/admin/users"),
  });

  const previewTier = useQuery({
    queryKey: ["admin", "preview-tier"],
    queryFn: () => apiFetch<PreviewTierResponse>("/admin/preview-tier"),
  });

  const setTier = useMutation({
    mutationFn: ({ id, tier }: { id: number; tier: string }) =>
      apiFetch(`/admin/users/${id}/tier`, {
        method: "PUT",
        body: JSON.stringify({ tier }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "users"] });
      qc.invalidateQueries({ queryKey: ["admin", "overview"] });
      qc.invalidateQueries({ queryKey: ["admin", "audit-log"] });
    },
  });

  const setPreview = useMutation({
    mutationFn: (tier: "basic" | "standard" | null) =>
      apiFetch<PreviewTierResponse>("/admin/preview-tier", {
        method: "POST",
        body: JSON.stringify({ tier }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "preview-tier"] });
    },
  });

  const activePreview = previewTier.data?.previewTier ?? null;

  const cbkRates = useQuery({
    queryKey: ["cbk", "rates"],
    queryFn: () => apiFetch<CbkRateItem[]>("/cbk/rates"),
  });

  // Local edits state: rateType → input value (string)
  const [rateEdits, setRateEdits] = useState<Record<string, string>>({});
  const [rateFeedback, setRateFeedback] = useState<Record<string, "ok" | "err" | null>>({});

  const updateRate = useMutation({
    mutationFn: ({ rateType, value }: { rateType: string; value: string }) =>
      apiFetch(`/cbk/rates/${rateType}`, {
        method: "PUT",
        body: JSON.stringify({ rateValue: parseFloat(value) }),
      }),
    onSuccess: (_, { rateType }) => {
      qc.invalidateQueries({ queryKey: ["cbk", "rates"] });
      setRateFeedback((prev) => ({ ...prev, [rateType]: "ok" }));
      setRateEdits((prev) => { const n = { ...prev }; delete n[rateType]; return n; });
      setTimeout(() => setRateFeedback((prev) => ({ ...prev, [rateType]: null })), 3000);
    },
    onError: (_, { rateType }) => {
      setRateFeedback((prev) => ({ ...prev, [rateType]: "err" }));
      setTimeout(() => setRateFeedback((prev) => ({ ...prev, [rateType]: null })), 3000);
    },
  });

  return (
    <main className="min-h-screen bg-background px-4 py-10 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-4xl flex-col gap-8">

        {/* Header */}
        <header className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <h1 className="font-display text-3xl font-medium tracking-tight text-foreground">
              {t.admin.title}
            </h1>
            {activePreview && (
              <Badge variant="watch" className="text-xs">
                {t.admin.previewActive} {activePreview}
              </Badge>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" asChild>
              <Link to="/">{t.admin.back}</Link>
            </Button>
          </div>
        </header>

        {/* Preview-as-tier */}
        <Card>
          <CardHeader>
            <CardTitle>{t.admin.previewTitle}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap items-center gap-2">
            <Button
              size="sm"
              variant={activePreview === "basic" ? "default" : "outline"}
              onClick={() => setPreview.mutate(activePreview === "basic" ? null : "basic")}
              disabled={setPreview.isPending}
            >
              Basic
            </Button>
            <Button
              size="sm"
              variant={activePreview === "standard" ? "default" : "outline"}
              onClick={() => setPreview.mutate(activePreview === "standard" ? null : "standard")}
              disabled={setPreview.isPending}
            >
              Standard
            </Button>
            {activePreview && (
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setPreview.mutate(null)}
                disabled={setPreview.isPending}
              >
                {t.admin.previewClear}
              </Button>
            )}
          </CardContent>
        </Card>

        {/* Overview */}
        <Card>
          <CardHeader>
            <CardTitle>{t.admin.overviewTitle}</CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <Stat label={t.admin.totalUsers} value={overview.data?.totalUsers} />
            <Stat label={t.admin.basicUsers} value={overview.data?.basicUsers} />
            <Stat label={t.admin.standardUsers} value={overview.data?.standardUsers} />
          </CardContent>
        </Card>

        {/* Users */}
        <Card>
          <CardHeader>
            <CardTitle>{t.admin.usersTitle}</CardTitle>
          </CardHeader>
          <CardContent className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t.admin.colEmail}</TableHead>
                  <TableHead>{t.admin.colName}</TableHead>
                  <TableHead>{t.admin.colOnboarded}</TableHead>
                  <TableHead>{t.admin.colSubscription}</TableHead>
                  <TableHead>{t.admin.colJoined}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.data?.map((u) => (
                  <TableRow key={u.id}>
                    <TableCell className="font-medium">{u.email}</TableCell>
                    <TableCell>{u.firstName ?? "—"}</TableCell>
                    <TableCell>
                      {u.onboardingCompleted ? (
                        <Badge variant="good">✓</Badge>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </TableCell>
                    <TableCell>
                      {/* Don't allow changing own tier */}
                      {u.id === user?.id ? (
                        <Badge variant="outline">{u.subscriptionTier}</Badge>
                      ) : (
                        <Select
                          value={u.subscriptionTier}
                          onValueChange={(val) =>
                            setTier.mutate({ id: u.id, tier: val })
                          }
                          disabled={setTier.isPending}
                        >
                          <SelectTrigger className="h-7 w-28 text-xs">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="basic">Basic</SelectItem>
                            <SelectItem value="standard">Standard</SelectItem>
                          </SelectContent>
                        </Select>
                      )}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {new Date(u.createdAt).toLocaleDateString()}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        {/* Feature entitlements */}
        <Card>
          <CardHeader>
            <CardTitle>{t.admin.entitlementsTitle}</CardTitle>
          </CardHeader>
          <CardContent>
            <AdminEntitlements />
          </CardContent>
        </Card>

        {/* Preview-as-basic upgrade nudge (injection point 4) */}
        {activePreview === "basic" && (
          <UpgradePrompt variant="inline" />
        )}

        {/* CBK Reference Rates */}
        <Card>
          <CardHeader>
            <CardTitle>{t.admin.cbkRatesTitle}</CardTitle>
          </CardHeader>
          <CardContent className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t.admin.cbkRateType}</TableHead>
                  <TableHead>{t.admin.cbkRateValue}</TableHead>
                  <TableHead>{t.admin.cbkRateEffective}</TableHead>
                  <TableHead>{t.admin.cbkRateSetBy}</TableHead>
                  <TableHead className="text-right">{t.admin.cbkRateUpdate}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(cbkRates.data ?? []).map((rate) => {
                  const label =
                    t.admin.cbkRateTypes[rate.rateType as keyof typeof t.admin.cbkRateTypes] ??
                    rate.rateType;
                  const editVal = rateEdits[rate.rateType] ?? String(rate.rateValue);
                  const feedback = rateFeedback[rate.rateType];
                  return (
                    <TableRow key={rate.id}>
                      <TableCell className="font-medium text-sm">{label}</TableCell>
                      <TableCell>
                        <Input
                          type="number"
                          step="0.001"
                          className="h-7 w-28 text-xs"
                          value={editVal}
                          onChange={(e) =>
                            setRateEdits((prev) => ({
                              ...prev,
                              [rate.rateType]: e.target.value,
                            }))
                          }
                        />
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {rate.effectiveDate}
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {rate.setBy}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-2">
                          {feedback === "ok" && (
                            <span className="text-xs text-green-600">{t.admin.cbkRateUpdated}</span>
                          )}
                          {feedback === "err" && (
                            <span className="text-xs text-destructive">{t.admin.cbkRateError}</span>
                          )}
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-7 text-xs"
                            disabled={updateRate.isPending}
                            onClick={() =>
                              updateRate.mutate({
                                rateType: rate.rateType,
                                value: editVal,
                              })
                            }
                          >
                            {t.admin.cbkRateUpdate}
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        {/* Audit log */}
        <Card>
          <CardHeader>
            <CardTitle>{t.admin.auditTitle}</CardTitle>
          </CardHeader>
          <CardContent>
            {audit.data && audit.data.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t.admin.auditEmpty}</p>
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t.admin.colAction}</TableHead>
                      <TableHead>{t.admin.colEntity}</TableHead>
                      <TableHead>{t.admin.colBy}</TableHead>
                      <TableHead>{t.admin.colWhen}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {audit.data?.map((e) => (
                      <TableRow key={e.id}>
                        <TableCell>{e.action}</TableCell>
                        <TableCell>{e.entity ?? "—"}</TableCell>
                        <TableCell>{e.performedBy}</TableCell>
                        <TableCell>{new Date(e.timestamp).toLocaleString()}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>

      </div>
    </main>
  );
}

function Stat({ label, value }: { label: string; value?: number }) {
  return (
    <div className="rounded-lg bg-background p-4">
      <div className="text-sm text-muted-foreground">{label}</div>
      <div className="font-display text-3xl font-medium text-foreground">{value ?? "—"}</div>
    </div>
  );
}
