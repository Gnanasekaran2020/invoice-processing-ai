# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

This is an Angular 18 CLI project. All commands run from the repo root.

- **Install deps:** `npm install`
- **Dev server:** `npm start` (alias for `ng serve`) — serves at `http://localhost:4200`, proxies to backend at `environment.apiUrl`
- **Production build:** `npm run build` (defaults to production config; outputs to `dist/invoice-process-ui-angular/`)
- **Dev build with watch:** `npm run watch`
- **Tests:** `npm test` (Karma + Jasmine). No `*.spec.ts` files currently exist in `src/`, so the runner has nothing to execute until specs are added.
- **Run a single test (once specs exist):** `ng test --include='**/path/to/file.spec.ts'`

There is no lint script configured in `package.json`; Angular CLI's default `ng lint` would need ESLint added before it works.

## Architecture

### Bootstrap & DI
- Standalone-API only — **no NgModules**. Entry at `src/main.ts` does `bootstrapApplication(AppComponent, appConfig)`.
- All app-wide providers live in `src/app/app.config.ts`: `provideRouter`, `provideHttpClient(withInterceptors([authInterceptor]))`, `provideAnimations`, `provideNzI18n(en_US)`. English locale data is registered at module load via `registerLocaleData(en)`.

### Routing
- All routes in `src/app/app.routes.ts` are **lazy-loaded** via `loadComponent`.
- Two functional guards:
  - `authGuard` (`src/app/core/guards/auth.guard.ts`) — protects the `MainLayoutComponent` shell and all its children (dashboard, invoices, upload, profile, reports). Redirects to `/login` if `AuthService.isAuthenticated()` is false.
  - `guestGuard` — protects `/login` and `/register` against already-authenticated users.
- The authenticated routes are children of a single `MainLayoutComponent` route, so the sidebar/header shell stays mounted across navigation.

### Auth
- `AuthService` (`src/app/core/services/auth.service.ts`) is the source of truth for auth state. It exposes **signals**: `user`, `isAuthenticated` (computed), `isAdmin` (computed). JWT and user JSON persist in `localStorage` under keys `token` and `user`.
- `authInterceptor` (`src/app/core/interceptors/auth.interceptor.ts`) is a functional `HttpInterceptorFn` registered in `app.config.ts`. It attaches `Authorization: Bearer <token>` when a token exists and, on a `401` response, calls `authService.logout()` and routes to `/login`.
- `AuthApiService` hits `{apiUrl}/auth/login` and `/auth/register`; `AuthService.login(user, token)` is what writes localStorage and updates the signal.

### API layer & dummy-data fallback
- All HTTP services live in `src/app/core/services/` and build URLs as `environment.apiUrl + '/...'`:
  - `InvoiceApiService` — `/invoices/*` (list, get, upload, update status, retry, delete, stats)
  - `ProfileApiService` — `/profile`
  - `ReportApiService` — `/reports` (returns `Blob`)
- **Important convention:** Page components catch API errors and fall back to demo data from `src/app/shared/dummy-data.ts` (`DUMMY_INVOICES`, `DUMMY_STATS`, etc.). They flip a `usingDummy` signal and surface a "showing sample data" alert. Preserve this pattern when adding new pages — the app must remain usable when the backend is down.

### State management
- **Signals for UI state** (loading flags, fetched data, derived counts via `computed()`).
- **RxJS for async** — `.subscribe()` for single-shot API calls, `forkJoin()` for parallel fetches (see `DashboardComponent` loading stats + recent invoices together).
- Filter/pagination controls in `InvoiceListComponent` use plain class fields, not signals — only the *data* is signal-backed.
- There is no NgRx / Akita / global store; cross-page state lives in services with `providedIn: 'root'`.

### Models
Domain types in `src/app/core/models/`:
- `invoice.model.ts` — `Invoice`, `InvoiceDetail`, `PagedResponse<T>`, `DashboardStats`. Invoices carry both a business `status` (PENDING/APPROVED/REJECTED/DUPLICATE/PAID) and a `processingStatus` (UPLOADED/EXTRACTING/AI_PROCESSING/COMPLETED/FAILED/MANUAL_REVIEW) — they are independent and both drive UI badges.
- `user.model.ts` — `User`, `AuthResponse` (User + `accessToken`).

### Styling
- Global stylesheet `src/styles.scss` defines reusable classes referenced across pages: `.auth-wrapper`, `.auth-card`, `.page-header`, `.invoice-table`, `.stat-card`.
- ng-zorro's stylesheet (`ng-zorro-antd.min.css`) is loaded globally via `angular.json`; components import individual ng-zorro modules in their `imports: []` array.
- Component schematics default to SCSS (`angular.json` → `schematics.@schematics/angular:component.style`).

### Environments
- `src/environments/environment.ts` → dev (`apiUrl: 'http://localhost:8080'`)
- `src/environments/environment.prod.ts` → prod (AWS Elastic Beanstalk URL)
- Change `apiUrl` here, not in service files.

## Conventions worth preserving

- **Standalone components only** — never introduce an `NgModule`. Each component's `imports: []` array lists the ng-zorro modules + Angular directives it needs.
- **Signal-first for UI state**, RxJS only for async; don't reach for `BehaviorSubject` when a `signal` will do.
- **Functional guards/interceptors** (`CanActivateFn`, `HttpInterceptorFn`) — match the existing pattern; don't add class-based ones.
- **Dummy-data fallback** in any new page that loads from the API.
