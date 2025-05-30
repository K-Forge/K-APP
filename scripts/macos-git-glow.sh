#!/bin/sh
# Script para instalar el hook de commit-msg para Git Glow
HOOK_PATH=".git/hooks/commit-msg"
cat > "$HOOK_PATH" <<'EOF'
#!/bin/sh
# Hook de ejemplo para forzar el uso de git glow en los commits
commit_msg_file="$1"
if ! grep -Eq '^\[(FEAT|FIX|CHORE|RELEASE|HOTFIX)\]\[[^\]]+\] .+' "$commit_msg_file"; then
  echo "\n[ERROR] El mensaje de commit no sigue la convención Git Glow." >&2
  echo "Formato esperado: [TYPE][Scope] Mensaje de commit" >&2
  echo "Ejemplo: [FEAT][Auth] Ejemplo de commit" >&2
  exit 1
fi
EOF
chmod +x "$HOOK_PATH"
echo "Hook de commit-msg instalado para Git Glow."
