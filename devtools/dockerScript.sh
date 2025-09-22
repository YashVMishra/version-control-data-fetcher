#!/bin/bash
##############functions ##################
if [ "$1" = "start" ]; then
 echo "Build and run the migration-helper related dockers now"
 docker-compose -f ./localDevDockerSetup.yml up -d

 elif [ "$1" = "stop" ]; then
 echo "stopping migration-helper related dockers now"
 docker-compose -f ./localDevDockerSetup.yml stop

 elif [ "$1" = "remove" ]; then
 echo "stop and removing the volume of migration-helper related Dockers"
 docker-compose -f ./localDevDockerSetup.yml down -v
else
    echo "valid args -> {  start,stop,remove}"
    exit
fi