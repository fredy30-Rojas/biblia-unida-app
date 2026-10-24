# -*- coding: utf-8 -*-
"""Espera a que GitHub Actions compile el APK y lo baja.

Consulta el estado cada 20 s. Cuando termina, si salio bien, busca el Release
v1.0-unida y se trae el .apk a Downloads.
"""
import io
import json
import os
import time
import urllib.error
import urllib.request

TOKEN = json.load(io.open(r"C:/Users/Fredy/.claude/secure/github-token.json",
                          encoding="utf-8"))["token"]
USUARIO = "Fredy30-Rojas"
REPO = "biblia-unida-app"
API = "https://api.github.com"
DESTINO = r"C:/Users/Fredy/Downloads/BibliaUnida-1.0.apk"


def api(ruta):
    req = urllib.request.Request(
        API + ruta,
        headers={"Authorization": "token " + TOKEN,
                 "Accept": "application/vnd.github+json",
                 "User-Agent": "fredy-claude"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        return e.code, {}


def esperar(limite=1800):
    inicio = time.time()
    while time.time() - inicio < limite:
        estado, datos = api("/repos/%s/%s/actions/runs?per_page=1" % (USUARIO, REPO))
        runs = (datos or {}).get("workflow_runs") or []
        if not runs:
            print("sin ejecuciones todavia", flush=True)
        else:
            r = runs[0]
            print("%s | %s | %s" % (r["name"], r["status"], r.get("conclusion")),
                  flush=True)
            if r["status"] == "completed":
                return r
        time.sleep(20)
    return None


def bajar_apk():
    estado, datos = api("/repos/%s/%s/releases/tags/v1.0-unida" % (USUARIO, REPO))
    if estado != 200:
        print("no encuentro el release v1.0-unida:", estado)
        return None
    for a in datos.get("assets", []):
        if a["name"].endswith(".apk"):
            print("bajando", a["browser_download_url"])
            req = urllib.request.Request(a["browser_download_url"],
                                         headers={"User-Agent": "fredy-claude"})
            with urllib.request.urlopen(req, timeout=300) as r, \
                    io.open(DESTINO, "wb") as f:
                f.write(r.read())
            print("guardado en", DESTINO, os.path.getsize(DESTINO), "bytes")
            return DESTINO
    return None


if __name__ == "__main__":
    r = esperar()
    if r is None:
        print("se acabo el tiempo de espera")
    elif r.get("conclusion") != "success":
        print("la compilacion termino mal:", r.get("conclusion"), r.get("html_url"))
    else:
        print("compilacion correcta:", r.get("html_url"))
        bajar_apk()
