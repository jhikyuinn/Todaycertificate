# WEAVE: Enhancing Decentralization Property of Hyperledger Fabric Blockchain

To make Fabric blockchain more reliable and trustworthy by connecting it to a programmable distributed offchain storage and another blockchain network.




## Prerequisites

### **linux** 

#### Docker 

Install the latest version of Docker if it is not already installed.

  ```sudo apt-get -y install docker-compose``` 

Once installed, confirm that the latest versions of both Docker and Docker Compose executables were installed.
Make sure the Docker daemon is running.

  ```sudo systemctl start docker```

Add your user to the Docker group.

  ```sudo usermod -a -G docker <username>``` 

#### Go

Install the latest version of Go if it is not already installed (only required if you will be writing Go chaincode or SDK applications).

#### JQ

Install the latest version of jq if it is not already installed (only required for the tutorials related to channel configuration transactions).


### Download Fabric Docker images, and fabric binaries

To get the install script:

```curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh && chmod +x install-fabric.sh```

Run the script with the -h option to see the options:

```./install-fabric.sh -h
Usage: ./install-fabric.sh [-f|--fabric-version <arg>] [-c|--ca-version <arg>] <comp-1> [<comp-2>] ... [<comp-n>] ...
        <comp>: Component to install one or more of  d[ocker]|b[inary]|s[amples]. If none specified, all will be installed
        -f, --fabric-version: FabricVersion (default: '2.5.6')
        -c, --ca-version: Fabric CA Version (default: '1.5.9')

```

We only need docker images and binaries
```
./install-fabric.sh d b
or
./install-fabric.sh docker binary
```


### Download kafka docker images

Start a cluster:

```docker-compose up -d```

Add more brokers:

```docker-compose scale kafka=3```

Broker IDs
You can configure the broker id in different ways

explicitly, using KAFKA_BROKER_ID
via a command, using BROKER_ID_COMMAND


## How to start
