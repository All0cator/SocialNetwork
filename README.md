# SocialNetwork

## Overview
A Java-based social network application with client-server architecture, file sharing capabilities, and user management features.

## Quick Start

### Install
1. Clone the repository:
```bash
git clone https://github.com/All0cator/SocialNetwork.git
cd SocialNetwork
```

2. Build the project:
```bash
gradle wrapper --gradle-version 8.5
./gradlew build
```

### Run
#### Starting the Server
```
./gradlew runServer -Phost=127.0.0.1 -Pport=8080
```

#### Starting the Client
```
./gradlew runClient -PserverHost=127.0.0.1 -PserverPort=8080 -PclientHost=127.0.0.1 -PclientPort=8081
```

## Troubleshooting

### Common Issues

1. bash: ./gradlew: Permission denied
```bash
chmod +x gradlew
```
