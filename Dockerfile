FROM eclipse-temurin:25-jre-alpine

LABEL org.opencontainers.image.source=https://github.com/SibeiC/api-server/
LABEL org.opencontainers.image.description="Sibei's API Server image"
LABEL org.opencontainers.image.licenses=GPL-3.0-only

# Set working directory
WORKDIR /app

# Doppler CLI injects runtime secrets (DOPPLER_TOKEN supplied via docker-compose env_file)
COPY --from=dopplerhq/cli:3 /bin/doppler /usr/local/bin/doppler

# Copy built JAR from CI
COPY app/api-server.jar ./api-server.jar

# Copy relevant files
COPY entrypoint.sh .

# Make entrypoint executable
RUN chmod +x entrypoint.sh

# Expose ports
EXPOSE 8080 8957

# Mount point for data exchange
VOLUME ["/opt/api-server"]

# Start the app
ENTRYPOINT ["./entrypoint.sh"]
