FROM jeanblanchard/java:8

RUN apk --update add zip && \
	cd /usr/share && \
    wget ftp://mirror.reverse.net/pub/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.zip && \
    unzip apache-maven-3.3.9-bin.zip && \
    ln -s /usr/share/apache-maven-3.3.9/bin/mvn /usr/bin/mvn && \
    rm apache-maven-3.3.9-bin.zip && \
    mkdir /sources

WORKDIR /sources

VOLUME /sources

CMD mvn clean test