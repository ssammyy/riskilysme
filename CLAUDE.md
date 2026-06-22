Plan before code. 
Always show a full implementation plan and wait for my approval before writing any code. No exceptions.
Reuse existing patterns. 
Mirror the service structure, naming conventions, DTO patterns, and repository patterns from the parent Riskily product. Only introduce a new pattern when there is no existing one that fits.
UI: use shadcn/ui components exclusively. Never write a custom UI component from scratch if a shadcn/ui primitive exists that can serve the purpose — Card, Badge, Button, Dialog, Sheet, Table, Tabs, Progress, Separator, Skeleton, etc. Compose layouts from these primitives. The only exception is a page-level layout wrapper that assembles existing components.
UI consistency. Every page must follow the same visual design language — same spacing scale, same color tokens, same typography hierarchy. If a pattern is established on page one, every subsequent page inherits it. Never deviate for a single page.
No placeholder or hardcoded fake data. Every metric shown in the UI must come from a real API call or a clearly marked TODO with the exact endpoint that will supply it. No delta: '+50bps' style filler.
Mobile-aware layouts. Use Tailwind responsive prefixes (sm:, md:, lg:) from the start. SME users are more likely to be on mobile than corporate treasury teams.
Migrations are append-only. Never edit an existing Flyway migration file. Always create a new versioned file (V{n+1}__description.sql).

Make references to notion product backlog and sprints to check what needs to be done and update accordingly when completed

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).


