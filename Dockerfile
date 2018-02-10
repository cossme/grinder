FROM openjdk:8u151-jdk-alpine3.7

ENV GRINDER_VERSION 4.0.0-SNAPSHOT
ENV GRINDER_BINARY grinder-4.0.0-20180210.134531-5-binary.zip

ENV GRINDER_BASE      /opt
ENV GRINDER_ROOT      ${GRINDER_BASE}/grinder
ENV GRINDER_WORKSPACE ${GRINDER_ROOT}/workspace

RUN apk add --no-cache curl && \
    mkdir ${GRINDER_BASE} && cd ${GRINDER_BASE} && \
    curl https://oss.sonatype.org/service/local/repositories/snapshots/content/io/github/cossme/grinder/${GRINDER_VERSION}/${GRINDER_BINARY} --output ${GRINDER_BINARY} && \
    unzip ${GRINDER_BINARY} && rm -rf ${GRINDER_BINARY} && \
    ln -s ${GRINDER_BASE}/grinder-${GRINDER_VERSION} ${GRINDER_ROOT} && \
    mkdir ${GRINDER_WORKSPACE} && \
    touch ${GRINDER_WORKSPACE}/setGrinderEnv.sh && \
    echo "grinder.console.propertiesFile=${GRINDER_WORKSPACE}/grinder.properties" > ~/.grinder_console && \
    echo "grinder.console.scriptDistributionDirectory=${GRINDER_WORKSPACE}" >> ~/.grinder_console

WORKDIR "${GRINDER_ROOT}"

EXPOSE 6372
EXPOSE 6373

CMD . ${GRINDER_WORKSPACE}/setGrinderEnv.sh && java -cp ${CLASSPATH}:lib/grinder.jar ${JAVA_PROPERTIES} net.grinder.Console
