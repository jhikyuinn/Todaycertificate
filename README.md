WEAVE: 


Prerequisites

MacOS
Docker:
Install the latest version of Docker Desktop if it is not already installed. Since Docker Desktop is a UI application on Mac, use cask to install it.

Go:
Optional: Install the latest Fabric supported version of Go if it is not already installed (only required if you will be writing Go chaincode or SDK applications).

JQ:
Optional: Install the latest version of jq if it is not already installed (only required for the tutorials related to channel configuration transactions).

linux
Docker:
Install the latest version of Docker if it is not already installed.

sudo apt-get -y install docker-compose

Once installed, confirm that the latest versions of both Docker and Docker Compose executables were installed.
Make sure the Docker daemon is running.

sudo systemctl start docker

Add your user to the Docker group.

sudo usermod -a -G docker <username>

Go:
Optional: Install the latest version of Go if it is not already installed (only required if you will be writing Go chaincode or SDK applications).

JQ:
Optional: Install the latest version of jq if it is not already installed (only required for the tutorials related to channel configuration transactions).
