call mvn clean package
call java -jar .\target\psn-java-0.0.1-SNAPSHOT.jar fix
call mvn clean package
call java -jar .\target\psn-java-0.0.1-SNAPSHOT.jar psn
pause