Server:clean
	javac -cp JarFiles/\* EdgeServer.java
	java -cp .:JarFiles/\* EdgeServer

r:
	java -cp .:JarFiles/\* EdgeServer

clean:
	rm -f *.class

