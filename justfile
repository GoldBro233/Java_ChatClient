JAR := "build/libs/java_chat_backend.jar"

# Build the fat jar
build:
    ./gradlew fatJar

# Run the server (requires `just build` first)
server:
    java -cp {{ JAR }} Server

# Run the client (requires `just build` first)
client host="127.0.0.1" port="8080":
    java -cp {{ JAR }} Client {{ host }} {{ port }}

# Send a raw message to a server
send host port message:
    java -cp {{ JAR }} SimpleClient {{ host }} {{ port }} "{{ message }}"

# Run the UDP server (requires `just build` first)
udp-server port="2020":
    java -cp {{ JAR }} UdpServer {{ port }}

# Run the UDP client (requires `just build` first)
udp-client host="localhost" port="2020":
    java -cp {{ JAR }} UdpClient {{ host }} {{ port }}
