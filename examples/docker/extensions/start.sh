#! /bin/bash

echo "$(tput  -T xterm setaf 1 ) test" # test make red
for i in {1..50}; do
    echo "$i"
    sleep 1
done;
false
docker run --rm  -d --network  docker_guacamole --name $hostname -e 'VNC_PW=test' consol/centos-xfce-vnc
timeout 20 docker logs $hostname
true