# docker start via guacamole-trigger

This example config for demostrating how guacamole-trigger works.
It use a docker-compose file to start a guacamole and guacd container.
guacamole is configers so it will start a new vnc container when you start to use it.
guacamole need acces to docker.socket to start a new container.

gucamole start command is configerd via the `START_COMMAND` environment variable.
you can do this because: `enable-environment-properties: true` in guacamole.properties


# Usage

copy guacamole-trigger-<version>.jar naar /extensions
run:
```
docker-compose up # inside this directory
```
now you can acces guacamole via:
> https://localhost/guacamole

username: test
passwords: test


### to stop demo

```
docker-compos down # inside this directory

docker kill vnc # stop the potential started vnc containers
docker kill vnc2
```

#
