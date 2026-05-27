# Build the project
build:
    ./gradlew build

# Run the server
server: build
    ./gradlew run

# Run the client
client host="localhost" port="8080": build
    ./gradlew runClient --args="{{host}} {{port}}"
