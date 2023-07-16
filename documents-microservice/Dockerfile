FROM airdock/oraclejdk:1.8

COPY target/authentication-microservice-1.0.0.jar /opt/tmp/documents-microservice-1.0.0.jar

EXPOSE 9081

ENV TZ=America/Mexico_City
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD java -jar /opt/tmp/documents-microservice-1.0.0.jar

# PERMISOS DE DOCKER
#     su - {usuario}
# CREAR JAR
#     mvn clean package
# EJECUCIÓN BÁSICA:
#     docker build -t documents-image-1.0 .
#     docker images
#     docker run -p 9081:9081 --mount type=bind,source=/home/alipsed/alipse,target=/app/alipse --name documents-docker-1.0 documents-image-1.0
#     docker container stop documents-docker-1.0
#     docker rm documents-docker-1.0
#     docker rmi image documents-image-1.0
#     docker ps -a

# EJECUCIÓN CON REPLICAS
# docker swarm init
# docker build -t documents-image-1.0 .
# docker service create -p 9081:9081 --mount type=bind,source=/home/alipsed/alipse,target=/app/alipse --replicas 3 documents-image-1.0
# docker service ls
# docker service rm beautiful_solomon
