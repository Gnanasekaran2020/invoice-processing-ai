# =============================================================
#  Invoice Processing System — Angular 18 Frontend
#  Multi-stage build: Node build → Nginx runtime
#  Angular 18 | Node 20 | Nginx Alpine
# =============================================================

# ── Stage 1: Build ────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first for Docker layer caching
COPY package.json package-lock.json ./
RUN npm ci --silent

# Copy source and build for production
COPY . .
RUN npm run build -- --configuration production

# ── Stage 2: Runtime (Nginx) ───────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Remove default nginx content
RUN rm -rf /usr/share/nginx/html/*

# Copy built Angular dist from builder
COPY --from=builder /app/dist/invoice-process-ui-angular/browser /usr/share/nginx/html

# Copy custom nginx config (handles Angular routing + proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Security: run nginx as non-root
RUN addgroup -S nginx-app && adduser -S nginx-app -G nginx-app \
    && chown -R nginx-app:nginx-app /usr/share/nginx/html \
    && chown -R nginx-app:nginx-app /var/cache/nginx \
    && chown -R nginx-app:nginx-app /var/log/nginx \
    && touch /var/run/nginx.pid \
    && chown nginx-app:nginx-app /var/run/nginx.pid

USER nginx-app

EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]

