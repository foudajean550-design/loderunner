FROM maven:3.9-eclipse-temurin-21

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libgtk-3-0 \
        libxtst6 \
        libxrender1 \
        libxi6 \
        libxext6 \
        xvfb \
        openbox \
        x11vnc \
        novnc \
        websockify \
        fonts-dejavu \
    && rm -rf /var/lib/apt/lists/*
