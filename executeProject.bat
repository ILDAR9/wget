CALL mvn clean:clean
CALL mvn install
CALL mvn exec:java -Dexec.args="http://images.search.yahoo.com/"
CALL PAUSE