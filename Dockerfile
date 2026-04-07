# ============================================================
# MULTI-STAGE DOCKER BUILD
# ============================================================
# Why two stages? The build stage needs Maven + JDK (heavy, ~500MB).
# The run stage only needs JRE (light, ~200MB).
# Final image is small = faster deploys, less money on ECR storage.

# ---- STAGE 1: BUILD ----
# Start from an image that has Maven 3.9 + Java 17 JDK pre-installed
# "AS build" — gives this stage a name so we can reference it later
FROM maven:3.9-eclipse-temurin-17 AS build

# Set /app as the working directory inside the container
# All commands after this run inside /app
WORKDIR /app

# Copy ONLY pom.xml first (not the source code yet)
# Why? Docker caches each layer. If pom.xml hasn't changed,
# Docker skips re-downloading all dependencies on next build.
# This is called "layer caching" — saves 2-3 minutes per build.
COPY pom.xml .

# Download all Maven dependencies into the container
# -B = batch mode (no interactive prompts, cleaner logs)
# This layer gets cached — only re-runs if pom.xml changes
RUN mvn dependency:go-offline -B

# NOW copy the source code
# This layer changes every time you edit code
COPY src ./src

# Build the jar file
# -DskipTests = don't run tests here (CI already ran them)
# -B = batch mode
# Creates target/codereview-ai-0.0.1-SNAPSHOT.jar
RUN mvn clean package -DskipTests -B

# ---- STAGE 2: RUN ----
# Start fresh from a JRE-only image (no Maven, no JDK, no source code)
# eclipse-temurin:17-jre = just the Java runtime, nothing else
# The build stage is thrown away — only this stage becomes the final image
FROM eclipse-temurin:17-jre

# Working directory for the running app
WORKDIR /app

# Copy the jar from the build stage into this clean image
# --from=build = "grab this file from the stage named 'build'"
# *.jar = matches whatever jar Maven created in target/
COPY --from=build /app/target/*.jar app.jar

# Tell Docker this container listens on port 8080
# This is documentation — it doesn't actually open the port
# You still need -p 8080:8080 when running the container
EXPOSE 8080

# The command that runs when the container starts
# Same as typing "java -jar app.jar" in a terminal
ENTRYPOINT ["java", "-jar", "app.jar"]
