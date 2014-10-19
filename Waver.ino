/**
 *
 * Copyright (c) 2014 Billy Wood
 * This file is part of Indi.
 * 
 * Indi is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Indi is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Indi.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

#include <adk.h>
#include <Servo.h>
//#include <SimpleList.h> // GPLv3
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
#define SEVO_PIN_DEFINITION {/*DLARM = */4, /*ULARM = */5, /*DRARM = */6, /*URARM = */7}
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
  bool initialised;
  
  KeyFrame() : initialised(false)
  {
  }
};
struct KeyframePacket {
  uint8_t servoToApplyTo;
  //The below two are used to instantiate a KeyFrame object
  uint8_t degreesToReach;
  uint16_t timeToPosition;
};
//SimpleList <KeyFrame> servoAnimations[NUMOFSERVOS];
KeyFrame servoAnimations[NUMOFSERVOS];

void setup() {
  wdt_enable(WDTO_2S); // Initialise the watchdog
  
  Serial.begin(9600);
  while (!Serial); // Wait for serial port to connect - used on Leonardo, Teensy and other boards with built-in USB CDC serial connection
  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }
  Serial.print(F("\r\nRestart"));
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
      frame.initialised = true;
      servoAnimations[packet.servoToApplyTo] = frame;
      Serial.print(F("\r\nAnimation command processed - Servo "));
      Serial.print(packet.servoToApplyTo);
      Serial.print(F(" - Time to position == "));
      Serial.print(packet.timeToPosition);
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
    Serial.print(F("\r\nConfirmation sent - Servo "));
    Serial.print(servo);
  }
}

void setupServos()
{
  int servoPins[NUMOFSERVOS] = SEVO_PIN_DEFINITION;
  for(int i = 0; i < NUMOFSERVOS; ++i)
  {
    servos[i].attach(servoPins[i]);
  }
}

void processServos()
{
  timeBetweenProcessing = micros() - start;
  start = micros();
  for(int i = 0; i < NUMOFSERVOS; ++i)
  {
    if(true == servoAnimations[i].initialised)
    {
      KeyFrame* keyFrameToUse = &(servoAnimations[i]);

      keyFrameToUse->timeSpent += timeBetweenProcessing;
      if(keyFrameToUse->timeSpent > keyFrameToUse->timeToPosition)
      {
        keyFrameToUse->timeSpent = keyFrameToUse->timeToPosition;
      }
      
      int32_t degreeToUse = map(keyFrameToUse->timeSpent, 0, keyFrameToUse->timeToPosition, 0, MAX_SERVO_ROTATION);
      servos[i].write(degreeToUse);
      if(keyFrameToUse->timeSpent == keyFrameToUse->timeToPosition)
      {
        Serial.print(F("\r\nAnimation complete - Servo "));
        Serial.print(i);
        servoAnimations[i].initialised = false;
        sendConfirmation(i);
      }
      else
      {
        Serial.print(F("\r\nProcessing animation frame - Servo "));
        Serial.print(i);
        Serial.print(F(" - timeSpent == "));
        Serial.print(keyFrameToUse->timeSpent);
        Serial.print(F(" - timeToPosition == "));
        Serial.print(keyFrameToUse->timeToPosition);
        Serial.print(F(" - timeBetweenProcessing == "));
        Serial.print(timeBetweenProcessing);
      }
    }
  }
}

