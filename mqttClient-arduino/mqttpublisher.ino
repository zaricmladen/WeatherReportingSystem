#include <ArduinoJson.h>
#include <ArduinoMqttClient.h>
#include <WiFiNINA.h>
#include <HTU21D.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <math.h>
#include <stdio.h>
#include <string.h>
#include "arduino_secrets.h"

///////please enter your sensitive data in the Secret tab/arduino_secrets.h
char ssid[] = SECRET_SSID;        // your network SSID (name)
char pass[] = SECRET_PASS;    // your network password (use for WPA, or use as key for WEP)
// mqtt authentication credentials
const char mqtt_usn[] = SECRET_USN;
const char mqtt_psw[] = SECRET_PSW;
//client id
char client_id[] = "publisher1";

HTU21D sensor;
WiFiClient wifiClient;
WiFiUDP ntpUDP;
MqttClient mqttClient(wifiClient);
NTPClient timeClient(ntpUDP);

const char broker[] = "192.168.1.5";
int        port     = 1883;
const char data_topic[]  = "TempHumid_topic";
const char comm_topic[] = "command_topic";

//set interval for sending messages (milliseconds)
const long interval = 600000;
unsigned long previousMillis = 0;
bool initFlag = 0;
bool commandRead = 0;
int count = 0;

const int capacity = JSON_OBJECT_SIZE(6);
StaticJsonDocument<capacity> doc;
char data_packet[64];

void setup() {
  //Initialize serial and wait for port to open:
  Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  }

  // attempt to connect to Wifi network:
  initWifi();
  initMqtt();

  timeClient.begin();
  sensor.begin();
}

void loop() {

  if(!testWifiConnection()) {
     initWifi();
  }
  if(!mqttClient.connected()) {
    initMqtt();
  }
  // call poll() regularly to allow the library to send MQTT keep alive which
  // avoids being disconnected by the broker
  mqttClient.poll();

  unsigned long currentMillis = millis();

  if (currentMillis - previousMillis >= interval || initFlag == 0 || commandRead) {
    // save the last time a message was sent
    if(!commandRead) {
      previousMillis = currentMillis;
    }
    initFlag = 1;
    if(sensor.measure()) {

      timeClient.update();
      int temperature = round(sensor.getTemperature());
      int humidity = round(sensor.getHumidity());
      int recordDay = timeClient.getDay();
      int recordHours = timeClient.getHours();
      int recordMinutes = timeClient.getMinutes();
      int recordSeconds = timeClient.getSeconds();
 
      doc["temperature"].set(temperature);
      doc["humidity"].set(humidity);
      doc["d"].set(recordDay);
      doc["h"].set(recordHours);
      doc["m"].set(recordMinutes);
      doc["s"].set(recordSeconds);
      serializeJson(doc, data_packet);
     
      // send message, the Print interface can be used to set the message contents
      mqttClient.beginMessage(data_topic);
      mqttClient.print(data_packet);
      mqttClient.endMessage();

      Serial.println(data_packet);

 
    }

    Serial.println();
    commandRead=0; 
  }
}

bool testWifiConnection(){
  int statusWifi = WiFi.status();
  if(statusWifi == WL_CONNECTION_LOST || statusWifi == WL_DISCONNECTED)
      return false;
  else
      return true;
}

void initWifi() {
  Serial.print("Attempting to connect to WPA SSID: ");
  Serial.println(ssid);
  while (WiFi.begin(ssid, pass) != WL_CONNECTED) {
    // failed, retry
    Serial.print(".");
    delay(5000);
  }

  Serial.println("You're connected to the network");
  Serial.println();
}

void initMqtt() {
  Serial.print("Attempting to connect to the MQTT broker: ");
  Serial.println(broker);

  mqttClient.setId(client_id);
  mqttClient.setUsernamePassword(mqtt_usn,mqtt_psw);
  mqttClient.setKeepAliveInterval(900000);

  while(!mqttClient.connect(broker, port)) {
    Serial.print("MQTT connection failed! Error code = ");
    Serial.println(mqttClient.connectError());
    Serial.println("Attempting to connect to the MQTT broker again: ");
  }

  Serial.println("You're connected to the MQTT broker!");
  Serial.println(); 
  mqttClient.onMessage(onMqttMessage);
  mqttClient.subscribe(comm_topic);
  Serial.println("You're subscribed to topic.");
}

void onMqttMessage(int messageSize){
  // we received a message, print out the topic and contents
  Serial.println("Received a message with topic '");
  Serial.print(mqttClient.messageTopic());
  Serial.print("', length ");
  Serial.print(messageSize);
  Serial.println(" bytes:");

  // use the Stream interface to print the contents
  while (mqttClient.available()) {
    Serial.print((char)mqttClient.read());
  }
  Serial.println();
  Serial.println();
  commandRead=1;
}
