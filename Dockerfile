FROM anapsix/alpine-java:latest
ADD classpath /app/classpath
ADD src /app/src
EXPOSE 8080
CMD [ "java", "-Ddb.port=3306", "-Ddb.host=mariadb", "-Dport=8080", "-cp", "/app/classpath/*:/app/src", "clojure.main", "-m", "social-app.server" ]