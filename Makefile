
# Makefile for JAVA network programs

default: chat_server.class chat_client.class

# Server
chat_server.class: chat_server.java
	javac chat_server.java

# Client
chat_client.class: chat_client.java
	javac chat_client.java

clean:
	rm -rf *.class \