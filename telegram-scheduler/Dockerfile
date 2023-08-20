FROM airdock/oraclejdk:1.8

COPY target/telegram-scheduler-1.0.0.jar /opt/tmp/telegram-scheduler-1.0.0.jar

ENV TZ=America/Mexico_City
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD java -jar /opt/tmp/telegram-scheduler-1.0.0.jar

# PERMISOS DE DOCKER
#     su - {usuario}
# CREAR JAR
#     mvn clean package
# EJECUCIÓN BÁSICA:
#     docker build -t telegram-scheduler-image-1.0 .
#     docker images
#     docker run --mount type=bind,source=/app/alipse,target=/app/alipse --name telegram-scheduler-docker-1.0 telegram-scheduler-image-1.0
#     docker container stop telegram-scheduler-docker-1.0
#     docker rm telegram-scheduler-schedulerrs-docker-1.0
#     docker ps -a

# EJECUCIÓN CON REPLICAS
# docker swarm init
# docker build -t telegram-scheduler-image-1.0 .
# docker service create --mount type=bind,source=/app/alipse,target=/app/alipse --replicas 1 telegram-scheduler-image-1.0
# docker service ls
# docker service rm beautiful_solomon