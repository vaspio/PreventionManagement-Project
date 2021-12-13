Server:clean
	javac -cp \* EdgeServer.java
	java -cp .:\* EdgeServer

r:
	java -cp .:\* EdgeServer

clean:
	rm -f *.class

