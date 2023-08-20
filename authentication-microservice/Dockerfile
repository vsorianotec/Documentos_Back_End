FROM airdock/oraclejdk:1.8

COPY target/authentication-microservice-1.0.0.jar /opt/tmp/authentication-microservice-1.0.0.jar

EXPOSE 9081

ENV TZ=America/Mexico_City
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD java -jar /opt/tmp/authentication-microservice-1.0.0.jar

# PERMISOS DE DOCKER
#     su - {usuario}
# CREAR JAR
#     mvn clean package
# EJECUCIÓN BÁSICA:
#     docker build -t authentication-image-1.0 .
#     docker images
#     docker run -p 9081:9081 --mount type=bind,source=/app/alipse,target=/app/alipse --name authentication-docker-1.0 authentication-image-1.0
#     docker container stop authentication-docker-1.0
#     docker rm authentication-docker-1.0
#     docker rmi image authentication-image-1.0
#     docker ps -a

# EJECUCIÓN CON REPLICAS
# docker swarm init
# docker build -t authentication-image-1.0 .
# docker service create -p 9081:9081 --mount type=bind,source=/app/alipse,target=/app/alipse --replicas 3 authentication-image-1.0
# docker service ls
# docker service rm beautiful_solomon