# Invoice Processing AI — Angular 18 Migration

Migrated from **React 19 + Ant Design** → **Angular 18 + NG-ZORRO (Ant Design for Angular)**

---

## 🗂️ Project Structure

```
src/
├── app/
│   ├── app.component.ts          # Root component (RouterOutlet only)
│   ├── app.config.ts             # App-level providers (router, HTTP, i18n, animations)
│   ├── app.routes.ts             # Lazy-loaded routes with auth/guest guards
│   │
│   ├── core/
│   │   ├── models/
│   │   │   ├── user.model.ts     # User, AuthResponse interfaces
│   │   │   └── invoice.model.ts  # Invoice, DashboardStats, PagedResponse interfaces
│   │   ├── services/
│   │   │   ├── auth.service.ts        # Signal-based auth state (login/logout/isAdmin)
│   │   │   ├── auth-api.service.ts    # Login / Register HTTP calls
│   │   │   ├── invoice-api.service.ts # All invoice CRUD + upload + stats
│   │   │   ├── profile-api.service.ts # Get/update profile
│   │   │   └── report-api.service.ts  # Generate report (blob download)
│   │   ├── interceptors/
│   │   │   └── auth.interceptor.ts    # Attaches Bearer token; handles 401 → /login
│   │   └── guards/
│   │       ├── auth.guard.ts          # Protects authenticated routes
│   │       └── guest.guard.ts         # Redirects logged-in users away from /login
│   │
│   ├── layout/
│   │   └── main-layout/
│   │       └── main-layout.component.ts  # Sidebar + header shell (collapsed signal)
│   │
│   ├── shared/
│   │   ├── dummy-data.ts              # Fallback demo data when backend is unreachable
│   │   └── components/
│   │       └── status-badge/
│   │           └── status-badge.component.ts  # InvoiceStatusBadge, ProcessingStatusBadge, ConfidenceScore
│   │
│   └── pages/
│       ├── login/          login.component.ts
│       ├── register/       register.component.ts
│       ├── dashboard/      dashboard.component.ts
│       ├── invoice-list/   invoice-list.component.ts
│       ├── invoice-detail/ invoice-detail.component.ts
│       ├── upload/         upload.component.ts
│       ├── profile/        profile.component.ts
│       └── reporting/      reporting.component.ts
│
├── environments/
│   ├── environment.ts       # Dev → http://localhost:8080
│   └── environment.prod.ts  # Prod → your deployed backend URL
│
├── styles.scss              # Global styles (auth-wrapper, page-header, confidence colors…)
├── index.html
└── main.ts                  # bootstrapApplication (standalone API)
```

---

## ⚙️ React → Angular Migration Map

| React Concept | Angular 18 Equivalent |
|---|---|
| `useState` / `useReducer` | `signal()` / `computed()` |
| `useEffect` | `ngOnInit()` / `ngOnDestroy()` |
| `useContext` (AuthContext) | `AuthService` (singleton, signals) |
| `React.createContext` | Angular `Injectable({ providedIn: 'root' })` |
| `useCallback` | Class methods (no memoisation needed) |
| `axios` + interceptors | `HttpClient` + `HttpInterceptorFn` |
| `react-router-dom` | `@angular/router` (lazy-loaded, standalone) |
| `ProtectedRoute` | `authGuard` / `guestGuard` (`CanActivateFn`) |
| `antd` (Ant Design React) | `ng-zorro-antd` (Ant Design Angular) |
| JSX templates | Angular HTML templates |
| `Props` | `@Input()` decorators |
| `Promise.all` | `forkJoin()` (RxJS) |
| `async/await` fetch | `Observable.subscribe()` |
| `.env` / `process.env` | `environment.ts` |

---

## 🚀 Getting Started

```bash
# 1. Install dependencies
cd invoice_process_ui_angular
npm install

# 2. Start development server (default: http://localhost:4200)
ng serve

# 3. Production build
ng build
```

---

## 🔑 Key Angular 18 Features Used

- **Standalone Components** — No NgModules anywhere; every component declares its own imports
- **Signals** (`signal`, `computed`) — Replaces React `useState`/`useMemo` for reactive state
- **Functional Guards** (`CanActivateFn`) — `authGuard` and `guestGuard`
- **Functional HTTP Interceptor** (`HttpInterceptorFn`) — JWT attachment + 401 redirect
- **Lazy-loaded Routes** — All pages use `loadComponent` for optimal bundle splitting
- **`forkJoin`** — Parallel HTTP calls on dashboard (stats + recent invoices)
- **NG-ZORRO** — Full Ant Design component library port for Angular

---

## 🧪 Environment Configuration

Edit `src/environments/environment.ts` to point at your backend:

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'   // ← change this
};
```

