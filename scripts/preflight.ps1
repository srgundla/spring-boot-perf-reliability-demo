# Windows PowerShell pre-flight check. Run:  powershell -ExecutionPolicy Bypass -File scripts\preflight.ps1
$ok = $true
function Say($a, $b) { "{0,-34} {1}" -f $a, $b }

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { Say "docker" "MISSING - install Docker Desktop"; exit 1 }
Say "docker" (docker --version)
docker compose version *> $null
if ($LASTEXITCODE -eq 0) { Say "docker compose" (docker compose version --short) } else { Say "docker compose" "MISSING"; $ok = $false }
docker info *> $null
if ($LASTEXITCODE -eq 0) { Say "docker daemon" "running" } else { Say "docker daemon" "NOT RUNNING - start Docker Desktop"; $ok = $false }

$cpus = docker info --format '{{.NCPU}}' 2>$null
$mem  = docker info --format '{{.MemTotal}}' 2>$null
if ($mem) {
  $gb = [math]::Floor([double]$mem / 1GB)
  Say "Docker CPUs / memory" "$cpus CPUs / $gb GB  (want >= 4 CPUs and >= 6 GB)"
  if ($gb -lt 6) { "  -> create %UserProfile%\.wslconfig with:  [wsl2]`n     memory=8GB`n     processors=4   then run: wsl --shutdown"; $ok = $false }
}

foreach ($port in 8080,3000,9090,16686,5432,6379) {
  if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { Say "port $port" "IN USE - stop whatever is using it"; $ok = $false }
  else { Say "port $port" "free" }
}

if (Get-Command java -ErrorAction SilentlyContinue) { Say "java (optional, IDE mode)" ((java -version 2>&1 | Select-Object -First 1)) } else { Say "java (optional, IDE mode)" "not installed" }
if (Get-Command mvn  -ErrorAction SilentlyContinue) { Say "mvn  (optional, IDE mode)" ((mvn -v | Select-Object -First 1)) } else { Say "mvn  (optional, IDE mode)" "not installed" }

if ($ok) { "READY" } else { "FIX THE ITEMS ABOVE FIRST"; exit 1 }
