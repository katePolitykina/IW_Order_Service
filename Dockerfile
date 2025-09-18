FROM amazoncorretto:17
LABEL authors="ekaterinapolitykina"
WORKDIR /app

COPY build/libs/iw_order_service-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]


