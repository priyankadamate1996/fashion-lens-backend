# Fashion Lens backend

Quarkus REST API for the Fashion Lens Expo app.

## Requirements

- Java 17 or newer
- Maven 3.9.16 or newer

## Run locally

From this directory:

```powershell
.\mvnw.cmd quarkus:dev
```

The API starts at `http://localhost:8080`. OpenAPI is available at
`http://localhost:8080/q/openapi` and Swagger UI at
`http://localhost:8080/q/swagger-ui` while running in dev mode.

<<<<<<< HEAD
## Deploy backend to Render

The repository includes a `Dockerfile` and `render.yaml` for deploying this
API as a Render Web Service. In Render, create a Blueprint from the backend
GitHub repository and select the `main` branch. Set the `SERPAPI_KEY` secret
when prompted; do not commit it to the repository. The service uses Render's
`PORT` and exposes `/api/health` as its health check.

After deployment, set `EXPO_PUBLIC_API_BASE_URL` in the frontend's
`.env.local` to the Render service's HTTPS URL (without `/api`), then rebuild
and deploy the frontend to Firebase Hosting. The backend's CORS allowlist is
configured by `FASHION_LENS_CORS_ORIGINS` for the Firebase Hosting origin.

=======
>>>>>>> 8a0d59a26a72f81696f0435bde37c96432e94416
Without provider credentials, the API returns clearly labelled sample products.
To enable SerpApi search for local development, create `.env` in this directory
using `.env.example` as a template, then replace the placeholder with your key:

```powershell
Copy-Item .env.example .env
notepad .env
```

Set the line to `SERPAPI_KEY=your-key` (without quotes or spaces around `=`),
save the file, and start Quarkus with the helper script:

```powershell
.\run-dev.ps1
```

The launcher prefers `SERPAPI_KEY` in the backend `.env` file, then the
persistent Windows user variable. This means a key explicitly updated in
`.env` is not overridden by an older Windows variable. It passes the selected
value to this backend process without printing it. The `.env` file is ignored
by Git and must not be committed or copied into the frontend. The backend uses
only `SERPAPI_KEY`; it does not fall back to a legacy key variable.

To enter a key without displaying it and verify it before saving, run:

```powershell
.\configure-serpapi.ps1
```

The script checks SerpApi's free account endpoint (no search quota is used) and
saves the key only if the account is reported active.

Optionally set `SERPAPI_BASE_URL` for a compatible endpoint. Search results are
provided by Google Shopping and Google Lens through SerpApi; this is not an
official Myntra, Amazon, or Meesho feed. Results and prices can be incomplete
or stale, and requests consume provider searches (image search makes multiple
provider requests). The Free plan currently includes 250 successful searches
per month. Additional seller offers are fetched only after a user taps the
comparison button, using one more search for that product. Google may return
different sellers for different products and may omit marketplaces. Review
SerpApi pricing and terms before enabling it for users.

## API

### `GET /api/health`

Returns service health.

### `GET /api/products`

Returns `{ products, demoData, message }`. Optional query parameters:

- `query`: search product names, e.g. `?query=cotton`
- `audience`: `all` (default), `women`, `men`, or `kids`
- `category`: clothing subcategory, such as `Kurtas & Kurtis` or `Shirts`
- `sort`: `match` (default) or `price`

Example:

```text
GET /api/products?audience=women&category=Kurtas%20%26%20Kurtis&sort=price
```

The app only invokes product search when the user presses **Search stores**.
Some results include an optional immersive product token. The app requests
additional retailer offers from `GET /api/products/offers?pageToken=...` only
when the user asks to compare sellers, minimizing free-plan search usage.
Retailer links in that response point to the seller's product listing when
provided by Google Shopping.

### `POST /api/search/image`

Accepts `multipart/form-data` with `image` and optional `audience` fields.
JPEG, PNG, and WebP images up to 500 KB are accepted.

```powershell
curl.exe -F "image=@kurta.jpg" -F "audience=women" http://localhost:8080/api/search/image
```

Returns `{ matches, demoData, message }`. The app keeps the selected photo local
until the user taps **Search this photo**. With SerpApi configured, that action
sends the image to SerpApi and Google Lens. Without a key, the API does not send
the image to the provider and returns labelled sample matches.

## Frontend connection

The Expo app defaults to `http://localhost:8080` on web and
`http://10.0.2.2:8080` on the Android emulator. Set
`EXPO_PUBLIC_API_BASE_URL` before starting Expo to use a different backend URL
(for a physical phone, use the computer's reachable LAN address). Configure
`FASHION_LENS_CORS_ORIGINS` as a comma-separated list of trusted web origins
when deploying the frontend and backend on different hosts.

## Tests

```powershell
.\mvnw.cmd test
```
