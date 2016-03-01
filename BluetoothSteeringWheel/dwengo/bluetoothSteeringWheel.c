/*
 * Copyright 2011 Karel Bruneel (www.karelbruneel.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

#include <dwengoConfig.h>
#include <dwengoBoard.h>
#include <dwengoMotor.h>
#include <dwengoUsart.h>
#include <dwengoLCD.h>
#include <dwengoDelay.h>

#define SPEED 1023

char getChar();

char getChar() {
  while(PIR1bits.RCIF==0) {}
  return RCREG;  
}

int handelingMotor1(char x, char y, char z) {
  int result;

  result = 16 * (int)z + 16 * (int)y;

  if ((result<100) && (result>-100)) {
    result = 0;
  } else {
    if (result>=100)result +=300;
    if (result<=-100)result -=300;
  }

  if (result>1023) result=1023;
  if (result<-1023) result=-1023;

  return result;
}

int handelingMotor2(char x, char y, char z) {
  int result;

  result = 16 * (int)z - 16 * (int)y;

  if ((result<100) && (result>-100)) {
    result = 0;
  } else {
    if (result>=100)result +=300;
    if (result<=-100)result -=300;
  }

  if (result>1023) result=1023;
  if (result<-1023) result=-1023;

  return result;
}


char x;
char y;
char z;

void main(void) {
  char c;
  int motor1;
  int motor2;


  initBoard();
  initMotor();
  initLCD();
  initUsartAdj(BAUD9600);

  backlightOn();

  motor1 = 0;
  motor2 = 0;

  c=getChar();
  while (c!='s') {
    c=getChar();
  }

  while(TRUE){

    while(PIR1bits.TXIF==0) {}
	TXREG = 'r';

    c=getChar();

    switch (c) {
      case 'c':
        x=getChar();
        y=getChar();
        z=getChar();

        clearLCD();
        printIntToLCD(c,0,0);
        printIntToLCD(x,0,4);
        printIntToLCD(y,0,8);
        printIntToLCD(z,0,12);

        motor1 = handelingMotor1(x,y,z);
        motor2 = handelingMotor2(x,y,z);
    
        printIntToLCD(motor1,1,0);
        printIntToLCD(motor2,1,8);
    
        setSpeedMotor1(motor1);
        setSpeedMotor2(-motor2);
        break;
      case 's':
      default:
        clearLCD();
        printStringToLCD("Driving stopped!",0,0);
        stopMotors();
        break;
	}

    delay_ms(100);

  }
}
