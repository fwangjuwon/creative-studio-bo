FROM nsm-cloud-registry.nsmall.com/nsm-images/openjdk:17.0.2
RUN mkdir -p /app
WORKDIR /app
RUN mv /workspace/source/source/build/libs/ns-creative-studio-bo*-plain.jar app-plain.jar; exit 0
RUN mv /workspace/source/source/build/libs/ns-creative-studio-bo*.jar app.jar
ENTRYPOINT ["java","-jar","./app.jar"]