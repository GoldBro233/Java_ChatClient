JAR := "build/libs/java_chat_backend.jar"
GRADLE_ENV := "GRADLE_USER_HOME=.gradle-home JAVA_HOME=/usr/lib/jvm/java-25-graalvm JAVA_OPTS=\"-Djava.io.tmpdir=$PWD/build/tmp/java -XX:-UsePerfData\""
GRADLE := GRADLE_ENV + " ./gradlew --no-daemon --project-cache-dir .gradle-project-cache"
NATIVE_DIR := "build/native"

_prepare-build-dirs:
    mkdir -p .gradle-home .gradle-project-cache build/tmp/java build/tmp/gradle build/native/tmp

# Build the fat jar
build: _prepare-build-dirs
    {{ GRADLE }} fatJar

# Build native server/client/send binaries
native: _prepare-build-dirs
    {{ GRADLE }} nativeCompile

# Run the server (requires `just build` first)
server:
    java -cp {{ JAR }} Server

# Run the client (requires `just build` first)
client host="127.0.0.1" port="8080":
    java -cp {{ JAR }} Client {{ host }} {{ port }}

# Send a single JSON protocol message to a server
send host port message:
    java -cp {{ JAR }} SimpleClient {{ host }} {{ port }} "{{ message }}"

# Run the native server (requires `just native` first)
native-server port="8080":
    {{ NATIVE_DIR }}/java-chat-server {{ port }}

# Run the native client (requires `just native` first)
native-client host="127.0.0.1" port="8080":
    {{ NATIVE_DIR }}/java-chat-client {{ host }} {{ port }}

# Send a single JSON protocol message with the native sender (requires `just native` first)
native-send host port message:
    {{ NATIVE_DIR }}/java-chat-send {{ host }} {{ port }} "{{ message }}"
