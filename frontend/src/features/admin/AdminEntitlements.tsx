import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useLang } from "@/lang/LanguageProvider";
import { apiFetch } from "@/lib/api";
import type { Tier } from "@/entitlement/useEntitlements";

interface EntitlementRow {
  featureKey: string;
  displayName: string;
  minTier: Tier;
  enabled: boolean;
}

export function AdminEntitlements() {
  const { t } = useLang();
  const qc = useQueryClient();

  const { data } = useQuery({
    queryKey: ["admin", "entitlements"],
    queryFn: () => apiFetch<EntitlementRow[]>("/admin/entitlements"),
  });

  const mutation = useMutation({
    mutationFn: (row: EntitlementRow) =>
      apiFetch<EntitlementRow>(`/admin/entitlements/${row.featureKey}`, {
        method: "PUT",
        body: JSON.stringify({ minTier: row.minTier, enabled: row.enabled }),
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "entitlements"] });
      qc.invalidateQueries({ queryKey: ["entitlements"] });
    },
  });

  const toggleTier = (row: EntitlementRow) =>
    mutation.mutate({ ...row, minTier: row.minTier === "basic" ? "standard" : "basic" });

  const toggleEnabled = (row: EntitlementRow) =>
    mutation.mutate({ ...row, enabled: !row.enabled });

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t.admin.colFeature}</TableHead>
          <TableHead>{t.admin.colTier}</TableHead>
          <TableHead>{t.admin.colActive}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data?.map((row) => (
          <TableRow key={row.featureKey}>
            <TableCell>{row.displayName}</TableCell>
            <TableCell>
              <Button
                size="sm"
                variant="outline"
                disabled={mutation.isPending}
                onClick={() => toggleTier(row)}
              >
                {row.minTier === "standard" ? (
                  <Badge variant="act">{t.admin.tierStandard}</Badge>
                ) : (
                  <Badge variant="good">{t.admin.tierBasic}</Badge>
                )}
              </Button>
            </TableCell>
            <TableCell>
              <input
                type="checkbox"
                className="h-4 w-4 accent-primary"
                checked={row.enabled}
                disabled={mutation.isPending}
                onChange={() => toggleEnabled(row)}
                aria-label={t.admin.colActive}
              />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
