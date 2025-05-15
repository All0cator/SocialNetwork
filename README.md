# SocialNetwork
A Java-based social network application with client-server architecture, file sharing capabilities, and user management features.

### Installation
1. Clone the repository:
```
git clone https://github.com/All0cator/SocialNetwork.git
cd SocialNetwork
```

2. Build the project:
```
gradle wrapper --gradle-version 8.5
```

### Running the Application
#### Starting the Server
```
./gradlew runServer -Phost=127.0.0.1 -Pport=8080
```

#### Starting the Client
```
./gradlew runClient -PserverHost=127.0.0.1 -PserverPort=8080 -PclientHost=127.0.0.1 -PclientPort=8081
```
