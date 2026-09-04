#!/usr/bin/env bash
# Acceso directo para iniciar el servidor desde la raíz del proyecto
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR/1-Aplicacion/fainca"
exec ./iniciar-servidor.sh

