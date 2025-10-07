# ---- Stage 1: Download sops ----
FROM alpine:3.22 AS sops-downloader

ARG SOPS_VERSION=v3.11.0
WORKDIR /tmp

RUN apk add --no-cache curl \
    && curl -sSL -o sops https://github.com/getsops/sops/releases/download/${SOPS_VERSION}/sops-${SOPS_VERSION}.linux.amd64 \
    && chmod +x sops

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:25-jre-alpine

LABEL org.opencontainers.image.source=https://github.com/SibeiC/api-server/
LABEL org.opencontainers.image.description="Sibei's API Server image"
LABEL org.opencontainers.image.licenses=GPL-3.0-only

# Set working directory
WORKDIR /app

# Copy sops binary from downloader stage
COPY --from=sops-downloader /tmp/sops /usr/local/bin/sops

# Copy built JAR from CI
COPY app/api-server.jar ./api-server.jar

# Copy relevant files
COPY entrypoint.sh .
COPY .env.enc .

# Make entrypoint executable
RUN chmod +x entrypoint.sh

# Expose ports
EXPOSE 8080 8957

# Mount point for data exchange
VOLUME ["/opt/api-server"]

# Start the app
ENTRYPOINT ["./entrypoint.sh"]
