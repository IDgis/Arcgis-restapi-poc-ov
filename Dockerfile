FROM azul/zulu-openjdk

EXPOSE 8080

# COPY jar to the container
COPY build/libs/arcgis-restapi-ov-0.1.3.jar /app/arcgis-restapi-ov-0.1.3.jar

# COPY needed files to container
COPY examples/ /examples/

ENTRYPOINT ["java", \
			"-jar", \
			"-D DB_USER=$DB_USER", \
			"-D DB_PASSWORD=$DB_PASSWORD", \
			"-D DB_HOST=$DB_HOST", \
			"-D DB_PORT=$DB_PORT", \
			"/app/arcgis-restapi-ov-0.1.3.jar"]