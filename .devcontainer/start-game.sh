#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

# JavaFX runs in a virtual desktop; the desktop is reachable only through
# the Codespaces-forwarded noVNC port.
Xvfb :0 -screen 0 1280x720x24 -ac +extension GLX +render -noreset >/tmp/loderunner-xvfb.log 2>&1 &
export DISPLAY=:0
openbox >/tmp/loderunner-openbox.log 2>&1 &
x11vnc -display :0 -forever -shared -nopw -localhost -rfbport 5900 >/tmp/loderunner-x11vnc.log 2>&1 &

NOVNC_ROOT="$(mktemp -d)"
ln -s /usr/share/novnc/* "$NOVNC_ROOT/"
cat >"$NOVNC_ROOT/index.html" <<'HTML'
<!doctype html>
<meta charset="utf-8">
<meta http-equiv="refresh" content="0; url=/vnc.html?autoconnect=true&amp;resize=scale">
<a href="/vnc.html?autoconnect=true&amp;resize=scale">Ouvrir le jeu</a>
HTML
websockify --web="$NOVNC_ROOT" 6080 127.0.0.1:5900 >/tmp/loderunner-websockify.log 2>&1 &

sleep 2
mvn --batch-mode javafx:run >/tmp/loderunner-game.log 2>&1 &
