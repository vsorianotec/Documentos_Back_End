FROM airdock/oraclejdk:1.8

COPY target/users-microservice-1.0.0.jar /opt/tmp/users-microservice-1.0.0.jar

EXPOSE 9080

ENV TZ=America/Mexico_City
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD java -jar /opt/tmp/users-microservice-1.0.0.jar

# PERMISOS DE DOCKER
#     su - {usuario}
# CREAR JAR
#     mvn clean package
# EJECUCIÓN BÁSICA:
#     docker build -t users-image-1.0 .
#     docker images
#     docker run -p 9080:9080 --mount type=bind,source=/app/alipse,target=/app/alipse --name users-docker-1.0 users-image-1.0
#     docker container stop users-docker-1.0
#     docker rm users-docker-1.0
#     docker ps -a

# EJECUCIÓN CON REPLICAS
# docker swarm init
# docker build -t users-image-1.0 .
# docker service create -p 9080:9080 --mount type=bind,source=/app/alipse,target=/app/alipse --replicas 3 users-image-1.0
# docker service ls
# docker service rm beautiful_solomon