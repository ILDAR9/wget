mvn clean:clean
mvn install
mvn exec:java -Dexec.args="http://images.search.yahoo.com/"
PAUSE