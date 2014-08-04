// The source for the Android application can be found at the following link: https://github.com/Lauszus/ArduinoBlinkLED
// The code for the Android application is heavily based on this guide: http://allaboutee.com/2011/12/31/arduino-adk-board-blink-an-led-with-your-phone-code-and-explanation/ by Miguel
#include <adk.h>
#include <Servo.h>
#include <QueueList.h> // GPLv3

USB Usb;
ADK adk(&Usb, "DorsetEggs", // Manufacturer Name
              "Waver", // Model Name
              "Cardboard prototype for the Robot", // Description (user-visible string)
              "1.0", // Version
              "http://www.tkjelectronics.dk/uploads/ArduinoBlinkLED.apk", // URL (web page to visit if no installed apps support the accessory)
              "123456789"); // Serial Number (optional)
boolean connected;
enum ServoTypes {DLARM = 0, ULARM, DRARM, URARM, NUMOFSERVOS};
Servo servos[NUMOFSERVOS];
#define SEVO_PIN_DEFINITION {/*DLARM = */3, /*ULARM = */5, /*DRARM = */6, /*URARM = */11}
#define MAX_SERVO_ROTATION 180
uint32_t start = 0;
uint32_t finished = 0;
uint32_t timeBetweenProcessing = 0;

struct KeyFrame {
  uint8_t degreesToReach; // 0 to 180, 0 is straight down
  //The max amount of time that an animation keyframe can take is 65 seconds.
  //We can up it later if we feel like it by upping these variables to uint32_t
  uint16_t timeToPosition;//Time in ms to reach this point
  uint16_t timeSpent;//Time spent so far reaching this point
};
struct KeyframePacket {
  uint8_t servoToApplyTo;
  //The below two are used to instantiate a KeyFrame object
  uint8_t degreesToReach;
  uint16_t timeToPosition;
};
QueueList <KeyFrame> servoAnimations[NUMOFSERVOS];

void setup() {
  Serial.begin(115200);
  while (!Serial); // Wait for serial port to connect - used on Leonardo, Teensy and other boards with built-in USB CDC serial connection
  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }
  Serial.print("\r\nWaver connected");
  
  setupServos();
}

void loop() {
  Usb.Task();

  if (adk.isReady()) {
    if (!connected) {
      connected = true;
      Serial.print(F("\r\nConnected to phone"));
    }
  } else {
    if (connected) {
      connected = false;
      Serial.print(F("\r\nDisconnected from phone"));
    }
  }
  recieveAnimationCommand();
  processServos();
}

void recieveAnimationCommand()
{
  if(connected)
  {
    size_t len = sizeof(KeyframePacket);
    uint8_t msg[len];
    uint8_t rcode = adk.RcvData(&len, msg);
    if (rcode && rcode != hrNAK) {
      //Data error
      Serial.print(F("\r\nData rcv error: "));
      Serial.print(rcode, HEX);
    } else if (len => len) {
      //Data recieved
      KeyframePacket packet;
      memcpy(&packet, msg, len);
      KeyFrame frame;
      frame.degreesToReach = packet.degreesToReach;
      frame.timeToPosition = packet.timeToPosition;
      frame.timeSpent = 0;
      servoAnimations[packet.servoToApplyTo].push(frame);
      Serial.print(F("\r\nKeyframe received for servo: "));
      Serial.print(packet.servoToApplyTo);
    }
  }
}

void sendConfirmation(uint8_t servo)
{
  if(connected)
  {
    uint8_t confirmationBuffer[1];
    confirmationBuffer[0] = servo;
    uint8_t rcode = adk.SndData(1, confirmationBuffer);
    if (rcode && rcode != hrNAK) {
      //Data error
      Serial.print(F("\r\nData send error: "));
      Serial.print(rcode, HEX);
    } else if (rcode != hrNAK) {
      //Data sent
      Serial.print(F("\r\nConfirmation sent: "));
      Serial.print(servo);
    }
  }
}

void setupServos()
{
  int servoPins[NUMOFSERVOS] = SEVO_PIN_DEFINITION;
  for(int i = 0; i < NUMOFSERVOS; ++i)
  {
    servos[i].attach(servoPins[i]);
    //Make sure the servos are in the default position
    servos[i].write(0);
  }
}

void processServos()
{
  timeBetweenProcessing = millis() - start;
  for(int i = 0; i < NUMOFSERVOS; ++i)
  {
    if(servoAnimations[i].isEmpty() == false)
    {
      KeyFrame* keyFrameToUse = &(servoAnimations[i].peek());
      keyFrameToUse->timeSpent += timeBetweenProcessing;
      if(keyFrameToUse->timeSpent > keyFrameToUse->timeToPosition)
      {
        keyFrameToUse->timeSpent = keyFrameToUse->timeToPosition;
      }
      int32_t degreeToUse = map(keyFrameToUse->timeSpent, 0, keyFrameToUse->timeToPosition, 0, MAX_SERVO_ROTATION);
      servos[i].write(degreeToUse);
      
      if(keyFrameToUse->timeSpent == keyFrameToUse->timeToPosition)
      {
        servoAnimations[i].pop();
        sendConfirmation(i);
      }
    }
  }
  start = millis();
}

