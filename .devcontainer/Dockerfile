FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye

ARG ANDROID_SDK_ROOT=/opt/android-sdk
ARG CMDLINE_TOOLS_VERSION=13114758
ARG GRADLE_VERSION=8.11.1

ENV ANDROID_HOME=${ANDROID_SDK_ROOT}
ENV ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT}
ENV PATH=${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${ANDROID_SDK_ROOT}/emulator:${PATH}
ENV PATH=/opt/gradle/gradle-${GRADLE_VERSION}/bin:${PATH}

USER root

# Kill the unsigned yarn apt source shipped in the base image — it only
# produces a harmless warning, but let's not have noise in every build.
RUN rm -f /etc/apt/sources.list.d/yarn.list

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip libc6-i386 lib32stdc++6 \
    && rm -rf /var/lib/apt/lists/*

# --- Gradle ---------------------------------------------------------------
RUN mkdir -p /opt/gradle \
    && curl -fSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
    && test -s /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt/gradle \
    && rm /tmp/gradle.zip \
    && test -x /opt/gradle/gradle-${GRADLE_VERSION}/bin/gradle

# --- Android cmdline-tools --------------------------------------------------
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && curl -fSL -o /tmp/commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip \
    && test -s /tmp/commandlinetools.zip \
    && unzip -q /tmp/commandlinetools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && rm /tmp/commandlinetools.zip \
    # The zip's top-level folder name has varied across releases
    # ("cmdline-tools" in current ones) — find it instead of hardcoding,
    # so a future version bump can't silently break this again.
    && SRC_DIR=$(find ${ANDROID_SDK_ROOT}/cmdline-tools -mindepth 1 -maxdepth 1 -type d | head -n1) \
    && mv "$SRC_DIR" ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && chmod +x ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/* \
    && test -x ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager

# --- SDK packages (called by absolute path — don't trust PATH mid-build) --
RUN yes | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null || true \
    && ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager \
         "platform-tools" "platforms;android-35" "build-tools;35.0.0" \
    && chown -R vscode:vscode ${ANDROID_SDK_ROOT}

USER vscode