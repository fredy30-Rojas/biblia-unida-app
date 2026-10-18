# -*- coding: utf-8 -*-
"""Publica el proyecto Android de la Biblia unida en GitHub y lo empuja.

Crea el repositorio si no existe y sube el contenido. No imprime el token.
El remote queda guardado sin el token; se usa solo en el empujon.
"""
import io
import json
import os
import subprocess
import urllib.error
import urllib.request

AQUI = r"C:/Users/Fredy/biblia-unida-android"
TOKEN = json.load(io.open(r"C:/Users/Fredy/.claude/secure/github-token.json",
                          encoding="utf-8"))["token"]
USUARIO = "Fredy30-Rojas"
REPO = "biblia-unida-app"
API = "https://api.github.com"


def api(metodo, ruta, datos=None):
    cuerpo = json.dumps(datos).encode() if datos is not None else None
    req = urllib.request.Request(
        API + ruta, data=cuerpo, method=metodo,
        headers={"Authorization": "token " + TOKEN,
                 "Accept": "application/vnd.github+json",
                 "Content-Type": "application/json",
                 "User-Agent": "fredy-claude"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode() or "{}")


def git(*args, silencioso=False):
    r = subprocess.run(["git"] + list(args), cwd=AQUI, capture_output=True, text=True)
    if r.returncode != 0 and not silencioso:
        raise SystemExit("git %s fallo: %s" % (" ".join(args), r.stderr[-600:]))
    return r


def main():
    estado, datos = api("GET", "/repos/%s/%s" % (USUARIO, REPO))
    if estado == 404:
        estado, datos = api("POST", "/user/repos", {
            "name": REPO,
            "description": "App Android de la Biblia y los libros de fuera (caparazon WebView)",
            "private": False,
            "has_issues": True,
        })
        print("repositorio creado:", datos.get("html_url"))
    elif estado == 200:
        print("repositorio ya existe:", datos.get("html_url"))
    else:
        raise SystemExit("no pude crear el repositorio: %s %s" % (estado, datos))

    if not os.path.isdir(os.path.join(AQUI, ".git")):
        git("init")
        git("checkout", "-b", "main")
    git("add", "-A")
    r = git("commit", "-m",
            "App Biblia y apocrifos: caparazon propio, con boton Atras del sistema\n\n"
            "Co-Authored-By: Claude Code <noreply@anthropic.com>", silencioso=True)
    if "nothing to commit" in (r.stdout + r.stderr):
        print("sin cambios que subir")

    limpio = "https://github.com/%s/%s.git" % (USUARIO, REPO)
    con_token = "https://%s:%s@github.com/%s/%s.git" % (USUARIO, TOKEN, USUARIO, REPO)
    git("remote", "remove", "origin", silencioso=True)
    git("remote", "add", "origin", limpio)
    r = subprocess.run(["git", "push", con_token, "main", "--force"],
                       cwd=AQUI, capture_output=True, text=True)
    if r.returncode != 0:
        raise SystemExit("push fallo: " + r.stderr.replace(TOKEN, "***")[-600:])
    print("subido a", limpio)


if __name__ == "__main__":
    main()
