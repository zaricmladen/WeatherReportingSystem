FROM lfedge/ekuiper:1.6-alpine

COPY ./mqtt_source.yaml ./etc/
COPY ./connection.yaml ./etc/connections/
COPY ./stream.txt ./tmp/
COPY ./rule.txt ./tmp/
COPY ./script.sh ./etc/

ENTRYPOINT ["/usr/bin/docker-entrypoint.sh"]
CMD [ "./bin/kuiperd" ]
