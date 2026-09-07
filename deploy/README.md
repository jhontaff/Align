# deploy/

Copias de **referencia** de la configuración de infraestructura del VPS de producción.

**Estos archivos NO son la fuente autoritativa.** La configuración real vive en el VPS:

| Archivo acá | Fuente autoritativa (VPS) |
|---|---|
| `align.service` | `/etc/systemd/system/align.service` |
| `align.env.example` | `/etc/align/align.env` (con valores reales, **nunca** en git) |

El pipeline de CI/CD (`.github/workflows/`) **no toca** ninguno de estos — solo mueve el `.jar` y reinicia el servicio. Estos archivos se editan a mano en el VPS; cuando eso pasa, reflejá el cambio acá y commitealo (disciplina manual).

Para qué sirve tenerlos versionados:
- El setup del VPS queda documentado y revisable en un diff.
- Si el VPS se pierde, se reconstruye desde acá + `devops.md`.

El reverse proxy (Nginx Proxy Manager) se configura por su panel web, no por archivo, así que no tiene copia acá. Ver `devops.md` para la arquitectura completa.
