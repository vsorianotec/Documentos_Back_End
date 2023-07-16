FROM airdock/oraclejdk:1.8

COPY target/authentication-microservice-1.0.0 /opt/tmp/authentication-microservice-1.0.0

EXPOSE 9081

ENV TZ=America/Mexico_City
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD java -jar /opt/tmp/authentication-microservice-1.0.0

# CREAR JAR
# clean package
# EJECUCIÓN BÁSICA:
# docker build -t base-image-0.1 .
# docker run -p 9090:9090 --mount type=bind,source=c:/app/accendonet,target=/app/accendonet --name base-docker-0.1 base-image-0.1
# docker container stop base-docker-0.1

# EJECUCIÓN CON REPLICAS
# docker swarm init
# docker build -t base-image-0.1 .
# docker service create -p 9090:9090 --mount type=bind,source=c:/app/accendonet,target=/app/accendonet --replicas 3 base-image-0.1
# docker service rm beautiful_solomon