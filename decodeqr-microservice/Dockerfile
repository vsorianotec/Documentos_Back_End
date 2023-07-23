FROM ubuntu:20.04

ENV TZ='America/Mexico_City'

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone;

# Install Opencv
RUN apt update;

RUN apt install -y libopencv-dev python3-opencv;

# Install ffmpeg

RUN apt install -y ffmpeg;

# Install OpenJDK-8
RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y ant && \
    apt-get clean;
    
# Fix certificate issues
RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

# Setup JAVA_HOME -- useful for docker commandline
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN export JAVA_HOME

# Install microservice

COPY target/decodeqr-microservice-1.0.jar /opt/tmp/decodeqr-microservice-1.0.jar

ENTRYPOINT exec java $JAVA_OPTS -jar /opt/tmp/decodeqr-microservice-1.0.jar

# PERMISOS DE DOCKER
#     su - {usuario}
# CREAR JAR
#     mvn clean package
# EJECUCIÓN BÁSICA:
#     docker build -t decodeqr-image-1.0 .
#     docker images
#     docker run -p 9083:9083 --mount type=bind,source=/app/alipse,target=/app/alipse --name decodeqr-docker-1.0 decodeqr-image-1.0
#     docker container stop decodeqr-docker-1.0
#     docker rm decodeqr-docker-1.0
#     docker rmi image decodeqr-image-1.0
#     docker ps -a

# EJECUCIÓN CON REPLICAS
# docker swarm init
# docker build -t decodeqr-image-1.0 .
# docker service create -p 9083:9083 --mount type=bind,source=/app/alipse,target=/app/alipse --replicas 3 decodeqr-image-1.0
# docker service ls
# docker service rm beautiful_solomon
