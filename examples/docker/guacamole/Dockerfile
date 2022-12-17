FROM guacamole/guacamole:1.4.0
USER root
RUN ln -sf /opt/guacamole/guacamole.war /usr/local/tomcat/webapps/
RUN mkdir -p /etc/guacamole
RUN echo enable-environment-properties: true > /etc/guacamole/guacamole.properties
RUN apt-get update && apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg-agent \
    software-properties-common
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs)  stable"
RUN apt-get  update && apt-get install -y docker-ce-cli
# TODO switch back to root
WORKDIR /usr/local/tomcat
ENTRYPOINT ["catalina.sh","run"]
