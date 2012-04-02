// Use board = ATTiny 2313 @ 1 MHz

#include "serial.c"

char lastchar = '0';
int delayval=  200;
char counter = '1';

ISR(USART_RX_vect){
    lastchar = UDR;
}
void setup() 
{ 

  pinMode(1, INPUT);
  pinMode(7, INPUT);
  pinMode(8, INPUT);
  digitalWrite(9, LOW);
  //pinMode(0, OUTPUT);  
  //Serial.begin(9600);

  InitializeSerial();
  //setupBlueToothConnection();
  pinMode(4, OUTPUT);
} 
 

void loop() 
{ 
  //serialTxString("asdf");
  if(digitalRead(7)==HIGH){
        serialTx(counter);
        serialTx('\r');
        serialTx('\n');
        counter++;
        if(counter=='a') counter = '0';
  }
  if(digitalRead(8) == HIGH){
      pairMe(); 
    
    
  }
  
  digitalWrite(4, HIGH);   // set the LED on
  delay(delayval);              // wait for a second
  digitalWrite(4, LOW);    // set the LED off
  delay(delayval);              // wait for a second*/
  

} 
 
 
 
void pairMe()
{

  //serialTxString("Setting up Bluetooth link");       //For debugging, Comment this line if not required    
   // serialTxString("\r\n+STWMOD=0\r\n");
    //serialTxString("\r\n+STNA=blueskull\r\n");
    //serialTxString("\r\n+STAUTO=1\r\n");
    //serialTxString("\r\n+STOAUT=1\r\n");
    //serialTxString("\r\n+STPIN=0000\r\n");
    //delay(2000);
    while(digitalRead(7) == HIGH){
      delay(1000);
      digitalWrite(9, HIGH);
      delay(100);
      digitalWrite(9, LOW);
    }
    
    serialTxString("\r\n+STWMOD=0\r\n");
    


    delay(2000); 
    serialTxString("\r\n+INQ=1\r\n");
    while(digitalRead(8) == HIGH) ;

    if(digitalRead(7)==LOW){    
         serialTxString("\r\n+STWMOD=0\r\n");
    }
}

 

