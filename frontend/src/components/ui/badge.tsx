import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

/** Badge — pill metadata tag (DESIGN.md badge-pill) + score/severity band variants. */
const badgeVariants = cva(
  "inline-flex items-center rounded-full px-3 py-0.5 text-sm font-medium transition-colors",
  {
    variants: {
      variant: {
        default: "bg-card text-foreground",
        primary: "bg-primary text-primary-foreground",
        outline: "border border-foreground text-foreground",
        good: "bg-band-good/15 text-band-good",
        watch: "bg-band-watch/15 text-band-watch",
        act: "bg-band-act/15 text-band-act",
        urgent: "bg-band-urgent/15 text-band-urgent",
      },
    },
    defaultVariants: { variant: "default" },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
