#!/usr/bin/env bash
# macOS / Linux pre-flight check. Run:  bash scripts/preflight.sh
ok=1
say() { printf '%-34s %s\n' "$1" "$2"; }

command -v docker >/dev/null || { say "docker" "MISSING - install Docker Desktop"; exit 1; }
say "docker" "$(docker --version)"
docker compose version >/dev/null 2>&1 && say "docker compose" "$(docker compose version --short)" || { say "docker compose" "MISSING"; ok=0; }
docker info >/dev/null 2>&1 && say "docker daemon" "running" || { say "docker daemon" "NOT RUNNING - start Docker Desktop"; ok=0; }

cpus=$(docker info --format '{{.NCPU}}' 2>/dev/null); mem=$(docker info --format '{{.MemTotal}}' 2>/dev/null)
if [ -n "$mem" ]; then
  # Docker reports a bit less than the configured value (6 GB shows as ~5.8 GB), so round to the nearest GB.
  gb=$(( (mem + 536870912) / 1024 / 1024 / 1024 ))
  say "Docker CPUs / memory" "${cpus} CPUs / ${gb} GB  (want >= 4 CPUs and >= 6 GB)"
  [ "$gb" -lt 6 ] && { echo "  -> Docker Desktop > Settings > Resources: raise memory to 6-8 GB"; ok=0; }
fi

for port in 8080 3000 9090 16686 5432 6379; do
  if lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then say "port $port" "IN USE - stop whatever is using it"; ok=0; else say "port $port" "free"; fi
done

command -v java >/dev/null && say "java (optional, IDE mode)" "$(java -version 2>&1 | head -1)" || say "java (optional, IDE mode)" "not installed"
command -v mvn  >/dev/null && say "mvn  (optional, IDE mode)" "$(mvn -v | head -1)" || say "mvn  (optional, IDE mode)" "not installed"

[ "$ok" = 1 ] && echo "READY" || { echo "FIX THE ITEMS ABOVE FIRST"; exit 1; }
