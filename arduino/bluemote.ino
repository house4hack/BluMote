// Use board = ATTiny 2313 @ 1 MHz
#include <avr/sleep.h>
#include "serial.c"


#define BTPOWER 6
#define BUTTON1 10
#define BUTTON2 11
#define BUTTON3 12
#define BUTTON4 13
#define STATUSLED 5
#define BTCONNECTED 7
#define PAIRINGPIN 8
#define BTDISCONNECT 9
#define INT0PIN 4
#define TX_PIN 1
#define TIMEOUT 10000

char sendchar = '0';
char pairing=0;


ISR(USART_RX_vect){
    if(UDR == '*') pairing = 0;
}
void setup() 
{ 

  pinMode(TX_PIN, INPUT);
  pinMode(BTCONNECTED, INPUT);
  pinMode(PAIRINGPIN, OUTPUT);
  pinMode(STATUSLED, OUTPUT);
  pinMode(INT0PIN, INPUT);
  pinMode(BTDISCONNECT, OUTPUT);
  pinMode(BTPOWER, OUTPUT);

  digitalWrite(BTDISCONNECT, LOW);

  pinMode(BUTTON1, INPUT);
  pinMode(BUTTON2, INPUT);
  pinMode(BUTTON3, INPUT);
  pinMode(BUTTON4, INPUT); 
  digitalWrite(BUTTON1, HIGH); 
  digitalWrite(BUTTON2, HIGH);
  
  InitializeSerial();
  
   PCMSK |= (1<<PCINT1); //  tell pin change mask to listen to D10  
   PCMSK |= (1<<PCINT2); //  tell pin change mask to listen to D11  
  // PCMSK |= (1<<PCINT2); //  tell pin change mask to listen to D12
  // PCMSK |= (1<<PCINT3); //  tell pin change mask to listen to D13

   GIMSK  |= (1<<PCIE); // enable PCINT interrupt in the general interrupt mask

} 
 

void loop() 
{ 
//  system_sleep();
  if(sendchar != '0'){
    //turnBlueTooth(HIGH);
    long started = millis();
    while((digitalRead(BTCONNECTED) == LOW) & ((millis()-started) < TIMEOUT)){
        doBlink(1000);
    }
    
    if(digitalRead(BTCONNECTED)==HIGH){
          serialTx(sendchar);
          sendchar = '0';
    }
  }
  doBlink(200);
} 
 
 
 
void pairMe()
{
  //serialTxString("Setting up Bluetooth link");       //For debugging, Comment this line if not required    
   // serialTxString("\r\n+STWMOD=0\r\n");
    //serialTxString("\r\n+STNA=BlueMote\r\n");
    //serialTxString("\r\n+STAUTO=1\r\n");
    //serialTxString("\r\n+STOAUT=1\r\n");
    //serialTxString("\r\n+STPIN=0000\r\n");
    //delay(2000);

    disconnect();    
    serialTxString("\r\n+STWMOD=0\r\n");
    delay(2000); 
    disconnect();
    serialTxString("\r\n+INQ=1\r\n");
    while(pairing == 1) ; // wait for pairing to complete

}


void disconnect() {
 
     while(digitalRead(BTCONNECTED) == HIGH){
      delay(1000);
      digitalWrite(BTDISCONNECT, HIGH);
      delay(100);
      digitalWrite(BTDISCONNECT, LOW);
    }
  
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



SIGNAL (SIG_PCINT)
{
  sendchar = '0'; 
  if(pairing == 0) {
    digitalWrite(STATUSLED, HIGH);
    digitalWrite(PAIRINGPIN, LOW);
    
    
    if(digitalRead(BUTTON1) == HIGH) {
         sendchar = '1';
         doBlink(1000);
    }
    if(digitalRead(BUTTON2) == HIGH) {
      if(sendchar=='1') {
           digitalWrite(PAIRINGPIN, HIGH);
           pairing = 1;
           sendchar = '0';
           pairMe();
      } else sendchar = '2'; 
    }
    
   // if(digitalRead(BUTTON3)) sendchar = '3';    
   // if(digitalRead(BUTTON4)) sendchar = '4';    
  } else {
    pairing = 0;
  }
}


void turnBlueTooth(int state){
   digitalWrite(BTPOWER,state); 
}


void doBlink(int delayval){
      digitalWrite(STATUSLED, HIGH);   // set the LED on
      delay(delayval);              // wait for a second
      digitalWrite(STATUSLED, LOW);
      delay(delayval);              // wait for a second*/
}
