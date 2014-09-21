#include <adk.h>
#include <Servo.h>
#include <SimpleList.h> // GPLv3
#include <avr/wdt.h> // Watchdog library

USB Usb;
ADK adk(&Usb, "DorsetEggs", // Manufacturer Name
              "Waver", // Model Name
              "Plastic prototype for the Robot", // Description (user-visible string)
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
  uint32_t timeToPosition;//Time in micro seconds to reach this point
  uint32_t timeSpent;//Time spent so far reaching this point (in micro seconds)
};
struct KeyframePacket {
  uint8_t servoToApplyTo;
  //The below two are used to instantiate a KeyFrame object
  uint8_t degreesToReach;
  uint16_t timeToPosition;
};
SimpleList <KeyFrame> servoAnimations[NUMOFSERVOS];

void setup() {
  wdt_enable(WDTO_2S); // Initialise the watchdog
  
  Serial.begin(9600);
  while (!Serial); // Wait for serial port to connect - used on Leonardo, Teensy and other boards with built-in USB CDC serial connection
  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }
}

void loop() {
  Usb.Task();

  if (adk.isReady()) {
    wdt_reset(); // Kick the watchdog
    if (!connected) {
      setupServos();
      connected = true;
      Serial.print(F("\r\nConnected to phone"));
    }
    recieveAnimationCommand();
  } else {
    if (connected) {
      connected = false;
      Serial.print(F("\r\nDisconnected from phone"));
    }
  }
  processServos();
}

void recieveAnimationCommand()
{
  if(connected)
  {
    size_t len = sizeof(KeyframePacket);
    size_t packetLen = len;
    uint8_t msg[len];
    uint8_t rcode = adk.RcvData(&len, msg);
    if (rcode && rcode != hrNAK) {
      //Data error
      Serial.print(F("\r\nData rcv error: "));
      Serial.print(rcode, HEX);
    } else if (len == 0)
    {
      //No input recieved
    } else if (len == packetLen) { //If the recieved packet length is the same as the predicted length
      //Data recieved
      KeyframePacket packet;
      memcpy(&packet, msg, len);
      KeyFrame frame;
      frame.degreesToReach = packet.degreesToReach;
      frame.timeToPosition = (uint32_t)(packet.timeToPosition) * 1000;
      frame.timeSpent = 0;
      servoAnimations[packet.servoToApplyTo].push_front(frame);
    }
    else
    {
      Serial.print(F("\r\nData wrong length, len == "));
      Serial.print(len);
      Serial.print(F(", packetLen == "));
      Serial.print(packetLen);
    }
  }
}

void sendConfirmation(uint8_t servo)
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
  timeBetweenProcessing = micros() - start;
  start = micros();
  for(int i = 0; i < NUMOFSERVOS; ++i)
  {
    if(!servoAnimations[i].empty())
    {
      SimpleList<KeyFrame>::iterator keyFrameToUse = servoAnimations[i].end() - 1;
      
      (*keyFrameToUse).timeSpent += timeBetweenProcessing;
      if((*keyFrameToUse).timeSpent > (*keyFrameToUse).timeToPosition)
      {
        (*keyFrameToUse).timeSpent = (*keyFrameToUse).timeToPosition;
      }
      
      int32_t degreeToUse = map((*keyFrameToUse).timeSpent, 0, (*keyFrameToUse).timeToPosition, 0, MAX_SERVO_ROTATION);
      servos[i].write(degreeToUse);
      if((*keyFrameToUse).timeSpent == (*keyFrameToUse).timeToPosition)
      {
        servoAnimations[i].pop_back();
        sendConfirmation(i);
      }
    }
  }
}

