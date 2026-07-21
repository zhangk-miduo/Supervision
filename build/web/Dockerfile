# 前端构建 + 静态服务
# 构建上下文需为项目根目录（docker compose 中设置 context: . 、dockerfile: build/web/Dockerfile）
FROM node:20-alpine AS build
WORKDIR /app
COPY build/web/package.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY build/web/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
