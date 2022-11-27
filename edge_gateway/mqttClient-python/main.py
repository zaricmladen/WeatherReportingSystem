import paho.mqtt.client as mqttClient
import time
import json
from datetime import datetime, timezone
import influxdb_client, os
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

CONST_LOCATION = "Wine cellar"
INFLUXDB_TOKEN = "z1UJveRLL4uQ-u8aNEh3Fme3HJZYCVIFMCeNkAZuSE-9yADkcNSjXBIT7adXtaSbBW8r3dZXLQKG3RKR6_BW6Q=="


def on_connect(client, userdata, flags, rc):
    if rc == 0:

        print("Connected to broker")

        global Connected  # Use global variable
        Connected = True  # Signal connection
        mqttClient.subscribe("TempHumid_topic")

    else:

        print("Connection failed")


def on_message(client, userdata, msg):
    msgpayload = str(msg.payload.decode())
    payloadJson = json.loads(msgpayload)
    print(datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ'));
    print(msg.topic + " " + str(payloadJson), flush=True)
    point = (
        Point("TempHumidSensor")
            .field("Temperature", payloadJson["temperature"])
            .field("Humidity", payloadJson["humidity"])
            .tag("Location", CONST_LOCATION)
    )
    influxClient = influxdb_client.InfluxDBClient(url=url, token=INFLUXDB_TOKEN, org=org, timeout=120000)
    write_api = influxClient.write_api(write_options=SYNCHRONOUS)
    write_api.write(bucket=bucket, org="mladen019@gmail.com", record=point)
    influxClient.close()


org = "mladen019@gmail.com"
url = "https://eu-central-1-1.aws.cloud2.influxdata.com"
bucket = "DataBucket"

Connected = False  # global variable for the state of the connection

broker_address = "192.168.1.5"  # Broker address
port = 1883  # Broker port
user = "client_login"  # Connection username
password = "zaraanthra"  # Connection password

mqttClient = mqttClient.Client("Python")  # create new instance
mqttClient.username_pw_set(user, password=password)  # set username and password
mqttClient.on_connect = on_connect  # attach function to callback
mqttClient.on_message = on_message  # attach function to callback


mqttClient.connect(broker_address, port=port, keepalive=1000)  # connect to broker
mqttClient.loop_start()  # start the loop

while Connected != True:  # Wait for connection
    time.sleep(0.1)


try:
    while True:
        time.sleep(1)

except KeyboardInterrupt:
    print
    "exiting"
    mqttClient.disconnect()
    mqttClient.loop_stop()
