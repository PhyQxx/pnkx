FROM node:22.22.2-bookworm-slim AS build
WORKDIR /app
RUN npm install --global pnpm@10.34.4
COPY . .
# Git hooks are only needed in a developer checkout.
RUN pnpm pkg delete scripts.prepare && pnpm install --frozen-lockfile && pnpm build

FROM node:22.22.2-bookworm-slim AS runtime
WORKDIR /app
ENV NODE_ENV=production NITRO_PORT=3000 NITRO_HOST=0.0.0.0
COPY --from=build --chown=node:node /app/.output ./.output
USER node
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
