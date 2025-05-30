#!/usr/bin/env pwsh
# Script para instalar el hook de commit-msg para Git Glow en PowerShell
$hookPath = ".git/hooks/commit-msg"
@'
#!/bin/sh
# Hook de ejemplo para forzar el uso de git glow en los commits
commit_msg_file="$1"
if ! grep -Eq '^\[(FEAT|FIX|CHORE|RELEASE|HOTFIX)\]\[[^\]]+\] .+' "$commit_msg_file"; then
  echo "\n[ERROR] El mensaje de commit no sigue la convención Git Glow." >&2
  echo "Formato esperado: [TYPE][Scope] Mensaje de commit" >&2
  echo "Ejemplo: [FEAT][Auth] Ejemplo de commit" >&2
  exit 1
fi
'@ | Set-Content -Path $hookPath -Encoding UTF8
icacls $hookPath /grant Everyone:RX > $null
Write-Host "Hook de commit-msg instalado para Git Glow."
