FROM airdock/oraclejdk:1.8

COPY target/decodeqr-microservice-1.0.jar /opt/tmp/decodeqr-microservice-1.0.jar

EXPOSE 9081

ENV TZ=America/Mexico_City
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD java -jar /opt/tmp/decodeqr-microservice-1.0.jar

# PERMISOS DE DOCKER
#     su - {usuario}
# CREAR JAR
#     mvn clean package
# EJECUCIÓN BÁSICA:
#     docker build -t decodeqr-image-1.0 .
#     docker images
#     docker run -p 9083:9083 --mount type=bind,source=/home/alipsed/alipse,target=/app/alipse --name documents-docker-1.0 decodeqr-image-1.0
#     docker container stop decodeqr-docker-1.0
#     docker rm decodeqr-docker-1.0
#     docker rmi image decodeqr-image-1.0
#     docker ps -a

# EJECUCIÓN CON REPLICAS
# docker swarm init
# docker build -t decodeqr-image-1.0 .
# docker service create -p 9083:9083 --mount type=bind,source=/home/alipsed/alipse,target=/app/alipse --replicas 3 decodeqr-image-1.0
# docker service ls
# docker service rm beautiful_solomon
