FROM mcr.microsoft.com/devcontainers/base:noble

SHELL ["/bin/bash", "-c"]
RUN apt update -y && apt install -y bash plocate curl zip unzip

USER vscode
WORKDIR /home/vscode

# Toàn bộ SDKMAN trong 1 RUN để giữ context
RUN curl -s "https://get.sdkman.io" | bash && \
    source "$HOME/.sdkman/bin/sdkman-init.sh" && \
    sdk install java 25.0.1-tem && \
    sdk default java 25.0.1-tem

RUN apt install git -y

# Set PATH cứng để không phụ thuộc shell login
ENV SDKMAN_DIR="/home/vscode/.sdkman"
ENV JAVA_HOME="/home/vscode/.sdkman/candidates/java/current"
ENV PATH="/home/vscode/.sdkman/candidates/java/current/bin:${PATH}"

WORKDIR /workspace