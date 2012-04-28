// Use board = ATTiny 2313 @ 1 MHz
#include <avr/sleep.h>
#include "serial.c"

#define BTPOWER       6
#define BUTTON1       11
#define BUTTON2       9
#define BUTTON3       10
#define BUTTON4       8

#define RED_LED       12
#define BLUE_LED      13

#define BTCONNECTED   5
#define BTDISCONNECT  4
#define INT0PIN       4
#define TX_PIN        1
#define TIMEOUT       10000
#define PAIRING_TIMEOUT  120000

#define PRESSED       LOW

char sendchar = '0';
char pairing = 0;

ISR(USART_RX_vect){
    if(UDR == '*') pairing = 0;
}

void setup() { 
  pinMode(BLUE_LED, OUTPUT);
  pinMode(RED_LED, OUTPUT);
  pinMode(BTDISCONNECT, OUTPUT);
  pinMode(BTPOWER, OUTPUT);

  pinMode(TX_PIN, INPUT);
  pinMode(INT0PIN, INPUT);
  pinMode(BTCONNECTED, INPUT);

  pinMode(BUTTON1, INPUT);
  pinMode(BUTTON2, INPUT);
  pinMode(BUTTON3, INPUT);
  pinMode(BUTTON4, INPUT); 
  
  //activate internal pull-ups
  digitalWrite(BUTTON1, HIGH); 
  digitalWrite(BUTTON2, HIGH);
  digitalWrite(BUTTON3, HIGH); 
  digitalWrite(BUTTON4, HIGH);
  
  digitalWrite(BTDISCONNECT, LOW);

  InitializeSerial();
  
   PCMSK |= (1<<PCINT0); // button 3
   PCMSK |= (1<<PCINT1); // button 2
   PCMSK |= (1<<PCINT2); // button 1

   GIMSK  |= (1<<PCIE); // enable PCINT interrupt in the general interrupt mask
  turnBlueTooth(LOW);
} 
 
void loop() { 
   system_sleep();

  // wait for switch press and debounce
  //while (anyButtonPressed()); // wait for all buttons to be released
  while (!anyButtonPressed()); // now wait for button to be pressed
  delay(250); // debounce  

  if (digitalRead(BUTTON1) == PRESSED && digitalRead(BUTTON2) == PRESSED) {
    pairing = 1;
  } else {
    if (digitalRead(BUTTON1) == PRESSED) {
      sendchar = '1';
    }
  
    if (digitalRead(BUTTON2) == PRESSED) {
      sendchar = '2';
    }
  
    if (digitalRead(BUTTON3) == PRESSED) {
      sendchar = '3';
    }
  
    if (digitalRead(BUTTON4) == PRESSED) {
      sendchar = '4';
    }
  }
  
  if (pairing == 1) {
    pairMe();
  }
  
  if(sendchar != '0'){
    turnBlueTooth(HIGH);
    long started = millis();
    while((digitalRead(BTCONNECTED) == LOW)) { //&& ((millis()-started) < TIMEOUT)){
      doBlink(1000);
    }
    
    if(digitalRead(BTCONNECTED) == HIGH){
      serialTx(sendchar);
      delay(1000);
    }
    
    sendchar = '0';
    turnBlueTooth(LOW);
  }
  
  //doBlink(1000);
} 

boolean anyButtonPressed() {
  if (digitalRead(BUTTON1) == PRESSED) return true;
  if (digitalRead(BUTTON2) == PRESSED) return true;
  if (digitalRead(BUTTON3) == PRESSED) return true;
  if (digitalRead(BUTTON4) == PRESSED) return true;  
  return false;
}
 
SIGNAL (SIG_PCINT) {
 /// just wake up
}
 
void pairMe() {
    turnBlueTooth(HIGH);
    serialTxString("\r\n+STWMOD=0\r\n");
    //serialTxString("\r\n+STNA=BlueMote\r\n");
    //serialTxString("\r\n+STAUTO=1\r\n");
    //serialTxString("\r\n+STOAUT=1\r\n");
    //serialTxString("\r\n+STPIN=0000\r\n");
    delay(2000); 
    //disconnect();
    serialTxString("\r\n+INQ=1\r\n");

    while (pairing == 1) {
      if (digitalRead(BTCONNECTED) == HIGH ||
        digitalRead(BUTTON1) == HIGH ||
        digitalRead(BUTTON2) == HIGH ||
        digitalRead(BUTTON3) == HIGH ||
        digitalRead(BUTTON4) == HIGH) {
          pairing = 0;
      }
      
      doBlinkAlt(1000);
    }; // wait for pairing to complete
}

void disconnect() {
     while(digitalRead(BTCONNECTED) == HIGH){
      delay(1000);
      digitalWrite(BTDISCONNECT, HIGH);
      delay(100);
      digitalWrite(BTDISCONNECT, LOW);
    }
   digitalWrite(BLUE_LED, LOW); 
}

void system_sleep() {
  //cbi(ADCSRA,ADEN);                    // switch Analog to Digitalconverter OFF
  set_sleep_mode(SLEEP_MODE_PWR_DOWN); // sleep mode is set here
  sleep_enable();
  sleep_mode();                        // System sleeps here
  sleep_disable();                     // System continues execution here when watchdog timed out 
  delay(400);
  //sbi(ADCSRA,ADEN);                    // switch Analog to Digitalconverter ON
}

void turnBlueTooth(int state){
   digitalWrite(BTPOWER, state); 
   digitalWrite(BLUE_LED, state); 
}

void doBlink(int delayval){
      digitalWrite(RED_LED, HIGH);   // set the LED on
      delay(delayval);              // wait for a second
      digitalWrite(RED_LED, LOW);
      delay(delayval);              // wait for a second*/
}

void doBlinkAlt(int delayval){
      digitalWrite(RED_LED, HIGH);   // set the LED on
      digitalWrite(BLUE_LED, LOW);   // set the LED on
      delay(delayval);              // wait for a second
      digitalWrite(RED_LED, LOW);
      digitalWrite(BLUE_LED, HIGH);   // set the LED on
      delay(delayval);              // wait for a second*/
}
