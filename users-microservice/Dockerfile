FROM airdock/oraclejdk:1.8

COPY target/users-microservice-1.0.0.jar /opt/tmp/users-microservice-1.0.0.jar

EXPOSE 9080

ENV TZ=America/Mexico_City
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD java -jar /opt/tmp/users-microservice-1.0.0.jar

# CREAR JAR
# clean package
# EJECUCIÓN BÁSICA:
# docker build -t users-image-1.0 .
# docker run -p 9080:9080 --mount type=bind,source=/home/hugod/alipse,target=/app/alipse --name users-docker-1.0 users-image-1.0
# docker container stop users-docker-1.0

# EJECUCIÓN CON REPLICAS
# docker swarm init
# docker build -t users-image-1.0 .
# docker service create -p 9080:9080 --mount type=bind,source=/home/hugod/alipse,target=/app/alipse --replicas 3 users-image-1.0
# docker service rm beautiful_solomon