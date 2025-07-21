#! /bin/bash
cat << EOF

You custom script running here:

You could use it to start Host $hostaname

For a user: $guacamoleUsername

guacamole will automaticly connect to $hostname via $protocol on port $port

EOF

sleep 10
docker run --rm  -d --network  docker_guacamole --name "$hostname" -e 'VNC_PW=test' consol/rocky-xfce-vnc
timeout 20 docker logs -f "$hostname"
true
