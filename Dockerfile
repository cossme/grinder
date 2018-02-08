FROM openjdk:8u151-jdk-alpine3.7

ENV GRINDER_VERSION 4.0.0-SNAPSHOT
ENV GRINDER_BINARY grinder-4.0.0-20180208.141204-3-binary.zip

RUN apk add --no-cache curl && \
    mkdir -p /opt/workspace && \
    cd /opt && \
    curl https://oss.sonatype.org/service/local/repositories/snapshots/content/io/github/cossme/grinder/${GRINDER_VERSION}/${GRINDER_BINARY} --output ${GRINDER_BINARY} && \
    unzip ${GRINDER_BINARY} && \
    rm -rf ${GRINDER_BINARY} && \
    echo "grinder.console.propertiesFile=/opt//workspace/grinder.properties" > ~/.grinder_console && \
    echo "grinder.console.scriptDistributionDirectory=/opt/workspace" >> ~/.grinder_console

WORKDIR "/opt/grinder-${GRINDER_VERSION}"

EXPOSE 6372
EXPOSE 6373

CMD java -cp lib/grinder.jar net.grinder.Console
