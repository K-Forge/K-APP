"""End-to-end check of the visitor day pass against the running stack."""
import json
import subprocess
import sys

BASE = "http://localhost:8080"


def call(method, path, token=None, body=None):
    cmd = ["curl", "-sS", "-o", "/dev/stderr", "-w", "%{http_code}",
           "-X", method, BASE + path]
    if token:
        cmd += ["-H", "Authorization: Bearer " + token]
    if body is not None:
        cmd += ["-H", "Content-Type: application/json", "-d", json.dumps(body)]
    p = subprocess.run(cmd, capture_output=True, text=True)
    status = p.stdout.strip()
    try:
        payload = json.loads(p.stderr) if p.stderr.strip() else None
    except json.JSONDecodeError:
        payload = p.stderr.strip()
    return status, payload


def main():
    email = sys.argv[1]
    password = sys.argv[2]

    status, tok = call("POST", "/auth/login", body={"email": email, "password": password})
    if status != "200":
        print(f"  no pude iniciar sesión ({status}): {tok}")
        return 1
    admin = tok["accessToken"]

    failures = []

    def check(label, got, want):
        ok = got == want
        print(f"  {'ok  ' if ok else 'FALLA'}  {label:<58} {got}")
        if not ok:
            failures.append(f"{label}: esperaba {want}, obtuve {got}")

    print("── recepción emite un pase ────────────────────────────────────────────")
    status, pass_ = call("POST", "/auth/admin/visitor-passes", admin,
                         {"notes": "verificación end-to-end"})
    check("POST /auth/admin/visitor-passes", status, "201")
    code = pass_["code"]
    print(f"        código: {code}   redimible hasta {pass_['redeemableUntil']}")
    print(f"        se borra solo el {pass_['purgeAt']}")

    print()
    print("── el visitante lo canjea ─────────────────────────────────────────────")
    status, token = call("POST", f"/auth/visitor-passes/{code}/redeem",
                         body={"documentType": "CC", "documentNumber": "1032456789",
                               "visitorName": "Visitante de prueba"})
    check("POST .../redeem (público, sin token)", status, "200")
    visitor = token["accessToken"]
    print(f"        roles={token['roles']}  sub={token['userId']}  dura {token['expiresIn']}s "
          f"({token['expiresIn'] // 3600} h)")

    print()
    print("── qué abre ese token ─────────────────────────────────────────────────")
    check("GET  /api/map/buildings                 -> 200", call("GET", "/api/map/buildings", visitor)[0], "200")
    check("GET  /api/map/buildings/A/floors/3      -> 200", call("GET", "/api/map/buildings/A/floors/3", visitor)[0], "200")
    check("GET  /api/map/spaces/search?q=301       -> 200", call("GET", "/api/map/spaces/search?q=301", visitor)[0], "200")

    print()
    print("── y qué NO abre ──────────────────────────────────────────────────────")
    check("GET  /api/semaphore/me                  -> 403", call("GET", "/api/semaphore/me", visitor)[0], "403")
    check("GET  /api/catalog/programs              -> 403", call("GET", "/api/catalog/programs", visitor)[0], "403")
    check("GET  /api/schedule/me                   -> 403", call("GET", "/api/schedule/me", visitor)[0], "403")
    check("GET  /api/users/me                      -> 403", call("GET", "/api/users/me", visitor)[0], "403")
    check("GET  /auth/admin/visitor-passes         -> 403", call("GET", "/auth/admin/visitor-passes", visitor)[0], "403")

    print()
    print("── el pase es de un solo uso ──────────────────────────────────────────")
    status, err = call("POST", f"/auth/visitor-passes/{code}/redeem",
                       body={"documentType": "CC", "documentNumber": "9999999999",
                             "visitorName": "Otro"})
    check("segundo canje                           -> 409", status, "409")
    if isinstance(err, dict):
        print(f"        motivo: {err.get('details', [{}])[0].get('issue')}")

    status, _ = call("POST", "/auth/visitor-passes/KL-V-ZZZZ-ZZZZ/redeem",
                     body={"documentType": "CC", "documentNumber": "1032456789",
                           "visitorName": "Nadie"})
    check("código inexistente                      -> 404", status, "404")

    print()
    print("── el registro, y que no se pueda editar ──────────────────────────────")
    status, register = call("GET", "/auth/admin/visitor-passes?redeemed=true", admin)
    check("GET  /auth/admin/visitor-passes         -> 200", status, "200")
    mine = [p for p in register if p["code"] == code]
    if mine:
        r = mine[0]
        print(f"        {r['visitorName']}  {r['documentType']} {r['documentNumber']}  "
              f"acceso hasta {r['accessExpiresAt']}")
    check("DELETE de un pase ya canjeado           -> 409",
          call("DELETE", f"/auth/admin/visitor-passes/{code}", admin)[0], "409")

    status, unused = call("POST", "/auth/admin/visitor-passes", admin)
    check("DELETE de un pase sin canjear           -> 204",
          call("DELETE", f"/auth/admin/visitor-passes/{unused['code']}", admin)[0], "204")

    print()
    print("── el registro abierto de invitados ya no existe ──────────────────────")
    check("POST /auth/register/guest               -> 401",
          call("POST", "/auth/register/guest",
               body={"email": "x@gmail.com", "password": "Password123!",
                     "firstName": "X", "lastName": "Y"})[0], "401")

    print()
    if failures:
        print(f"{len(failures)} fallas:")
        for f in failures:
            print("  - " + f)
        return 1
    print("Todo correcto.")
    return 0


sys.exit(main())
