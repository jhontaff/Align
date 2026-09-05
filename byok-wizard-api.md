# Wizard de API key (BYOK) — guía de consumo para el frontend

> Backend: `ai.credential` + `ai.llm`. Implementado 2026-09-05.
> Contexto de diseño completo en `CLAUDE.md`, sección *BYOK — per-user LLM credentials*.

## Qué cambió y por qué te importa

Align pasó a **BYOK puro** (*bring your own key*): cada usuario configura su propia API key de Gemini y consume su propia cuota gratuita. Antes había un pool de keys del operador; ya no existe.

La consecuencia directa para el frontend es que **el chat ya no funciona out of the box**. Un usuario recién registrado que abra el chat sin haber configurado su key recibe un error, no una respuesta. El wizard existe para cubrir ese hueco.

Repartición de responsabilidades: **el wizard es casi todo tuyo**. El backend no guarda en qué paso va el usuario ni orquesta el flujo — expone un recurso (la credencial) con tres operaciones y un contrato de errores. Los pasos, las pantallas, las capturas de Google AI Studio y el copy son UI.

---

## Endpoints

Los tres van bajo `/api/ai/credentials` y **requieren el JWT** como cualquier endpoint autenticado (`Authorization: Bearer <token>`).

Todas las respuestas usan el envelope `ApiResponse` estándar del proyecto:

```json
{
  "timestamp": "2026-09-05T14:23:11.482Z",
  "status": 200,
  "success": true,
  "message": "...",
  "data": { },
  "errors": null
}
```

### `GET /api/ai/credentials` — ¿tiene key configurada?

Es el endpoint que se consulta **al cargar la app** para decidir si hay que mostrar el wizard. No falla nunca por falta de credencial: "no hay" es una respuesta normal.

Con key configurada:

```json
{
  "status": 200,
  "success": true,
  "message": "Credential status retrieved successfully.",
  "data": {
    "configured": true,
    "lastFour": "aB3d",
    "updatedAt": "2026-09-05T14:20:00Z"
  }
}
```

Sin key configurada:

```json
{
  "status": 200,
  "success": true,
  "message": "Credential status retrieved successfully.",
  "data": {
    "configured": false,
    "lastFour": null,
    "updatedAt": null
  }
}
```

- `lastFour` son los últimos 4 caracteres de la key, para que el usuario reconozca cuál tiene cargada (mostralo como `••••aB3d`).
- **La API key completa nunca se devuelve**, ni siquiera a su dueño. No existe endpoint para recuperarla. Si el usuario la perdió, la única salida es cargar una nueva.

### `PUT /api/ai/credentials` — guardar (o reemplazar) la key

```http
PUT /api/ai/credentials
Content-Type: application/json

{ "apiKey": "AIzaSy..." }
```

**El backend valida la key contra Gemini antes de guardarla.** Si no sirve, no se persiste nada y te responde 409. O sea: cuando esta llamada devuelve 200, la key funciona de verdad — no hace falta que el wizard haga una prueba aparte.

```json
{
  "status": 200,
  "success": true,
  "message": "Credential saved successfully.",
  "data": { "configured": true, "lastFour": "aB3d", "updatedAt": "2026-09-05T14:20:00Z" }
}
```

Es idempotente: como cada usuario tiene a lo sumo una credencial, guardar es reemplazar. No hace falta borrar antes de cargar una nueva.

El backend recorta espacios y saltos de línea, así que no te preocupes por limpiar lo que el usuario pegó.

### `DELETE /api/ai/credentials` — borrar la key

```json
{
  "status": 200,
  "success": true,
  "message": "Credential removed successfully.",
  "data": null
}
```

Idempotente: borrar cuando no había nada tampoco es error. Después de esto, el chat vuelve a responder 428.

---

## Contrato de errores — la parte importante

`ApiResponse` no tiene un campo de código de error propio, así que **el status HTTP es el único discriminador confiable**. No parsees `message` para tomar decisiones: es texto para mostrarle al usuario, y puede cambiar.

Estos cuatro status pueden venir tanto del **chat** (`POST /api/agent/chat`) como del `PUT` de credenciales:

| Status | Qué pasó | Qué debe hacer el frontend |
|---|---|---|
| **428** | El usuario no tiene key configurada (o la guardada quedó ilegible) | **Abrir el wizard desde cero.** Copy: "Configurá tu API key para usar el chat" |
| **409** | El proveedor rechazó la key guardada: inválida, revocada, o proyecto sin acceso | **Reabrir el wizard**, con copy distinto: "Tu API key dejó de funcionar. Cargá una nueva" |
| **429** | La key es válida pero agotó su cuota gratuita | **NO abrir el wizard.** No hay nada que reconfigurar. Copy: "Alcanzaste el límite de tu cuota. Probá más tarde" |
| **503** | Gemini está caído | Error transitorio, ajeno al usuario. Sugerir reintentar |

Las dos confusiones que este contrato existe para evitar:

- **429 no es un problema de configuración.** Si lo tratás como 409 y abrís el wizard, el usuario va a cargar la misma key una y otra vez sin entender por qué "no anda".
- **428 y 409 no son 401.** El backend nunca usa 401/403 para problemas de API key, justamente para que no se mezclen con "tu sesión de Align venció", que sí es 401 y sí debe mandar al login.

Cuerpo de un error (mismo envelope, `data: null`):

```json
{
  "timestamp": "2026-09-05T14:23:11.482Z",
  "status": 428,
  "success": false,
  "message": "No tenés una API key configurada. Configurala para poder usar el chat.",
  "data": null,
  "errors": null
}
```

### Errores de validación del `PUT`

Si mandás `apiKey` vacío o ausente, es un 400 con el formato de validación estándar del proyecto:

```json
{
  "status": 400,
  "success": false,
  "message": "Validation failed.",
  "data": null,
  "errors": { "apiKey": "La API key es obligatoria." }
}
```

### Interceptor sugerido

```ts
// Un solo lugar que traduce status → acción, en vez de repetirlo en cada pantalla.
async function handleApiError(response: Response): Promise<never> {
  const body = await response.json();

  switch (response.status) {
    case 428:
      openApiKeyWizard({ reason: 'missing' });
      break;
    case 409:
      openApiKeyWizard({ reason: 'invalid' });
      break;
    case 429:
      showToast(body.message);   // sin wizard: la key está bien
      break;
    case 401:
      redirectToLogin();         // sesión de Align, no API key
      break;
    default:
      showToast(body.message);
  }

  throw new ApiError(response.status, body.message);
}
```

---

## Flujo del wizard

Los pasos 1 a 3 son contenido puro de UI: el backend no participa hasta el paso 4.

**Paso 1 — Por qué te pedimos esto.** Explicar en una pantalla que Align usa Gemini, que la capa gratuita de Google alcanza de sobra para uso personal, y que la key es del usuario (su cuota, su control, la puede revocar cuando quiera).

**Paso 2 — Crear la key en Google AI Studio.** Link directo a `https://aistudio.google.com/apikey`, con los pasos: iniciar sesión con su cuenta de Google → *Create API key* → elegir o crear un proyecto → copiar la key. Conviene una captura por paso.

**Paso 3 — Pegar la key.** Un input (`type="password"` con toggle de "mostrar"). No valides el formato del lado del cliente más allá de "no está vacío": el backend valida contra Gemini, que es la única verificación que vale.

**Paso 4 — Guardar.** `PUT /api/ai/credentials`.
- **200** → listo, mostrar confirmación y habilitar el chat.
- **409** → la key no sirve. Mostrar `message` y dejarlo reintentar **en el mismo paso**, sin reiniciar el wizard.
- **503** → Gemini está caído; no es culpa de la key. Ofrecer reintentar más tarde sin descartar lo que escribió.

**Paso 5 — Gestión posterior.** En la pantalla de ajustes, mostrar el estado (`GET`) con el `lastFour`, más un botón para reemplazar (`PUT`) y otro para borrar (`DELETE`).

### Cuándo consultar el estado

- Al cargar la app, una vez: si `configured: false`, ofrecer el wizard antes de que el usuario intente chatear y se coma un 428.
- No hace falta consultarlo antes de cada mensaje. Alcanza con reaccionar a los status del chat.

---

## Reglas que no hay que romper

- **No guardes la API key en el frontend.** Ni en `localStorage`, ni en `sessionStorage`, ni en un store global. Se manda una vez en el `PUT` y se descarta de memoria. El backend la guarda cifrada y nunca la devuelve.
- **No la loguees.** Ni en la consola durante el desarrollo.
- **No decidas nada a partir de `message`.** Ese texto es para mostrar; la lógica va por status.
- **No implementes reintentos automáticos ante 429.** Es una cuota agotada, no un error transitorio: reintentar solo la consume más rápido.

---

## Referencia rápida

| Operación | Método | Ruta | Body | Éxito |
|---|---|---|---|---|
| Consultar estado | `GET` | `/api/ai/credentials` | — | 200 |
| Guardar / reemplazar | `PUT` | `/api/ai/credentials` | `{ "apiKey": "..." }` | 200 |
| Borrar | `DELETE` | `/api/ai/credentials` | — | 200 |

| Status | Origen | Acción |
|---|---|---|
| 428 | chat | Abrir wizard ("configurá tu key") |
| 409 | chat / PUT | Abrir wizard ("tu key dejó de funcionar") o reintentar en el paso |
| 429 | chat / PUT | Avisar, **sin** wizard |
| 503 | chat / PUT | Reintentar más tarde |
| 401 | cualquiera | Sesión de Align vencida → login |
