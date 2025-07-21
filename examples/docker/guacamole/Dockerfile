FROM guacamole/guacamole:1.6.0
USER root
RUN ln -sf  /opt/guacamole/webapp/guacamole.war  /usr/local/tomcat/webapps/
RUN mkdir -p /etc/guacamole
RUN echo enable-environment-properties: true > /etc/guacamole/guacamole.properties
RUN apt-get update && apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg-agent \
    software-properties-common
RUN curl -fsSL  https://download.docker.com/linux/ubuntu/gpg  | apt-key add -
RUN add-apt-repository "deb [arch=$(dpkg --print-architecture)] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
RUN apt-get  update && apt-get install -y docker-ce-cli
# TODO switch back to root

RUN chown guacamole:guacamole -R  /usr/local/tomcat/webapps/ \
    && mkdir -p /usr/local/tomcat/conf/Catalina/ \
    && chmod -R a+rwx /usr/local/tomcat/conf/Catalina

USER guacamole
WORKDIR /usr/local/tomcat
ENTRYPOINT ["catalina.sh","run"]
