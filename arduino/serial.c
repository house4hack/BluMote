/*
 * serial.c
 *
 *  Created on: Mar 11, 2010
 *      Author: kchester
 */

/** \brief Controls serial communication
 *
 * \file serial.c
 *
 * Initializes and controls serial communication
 *
 * \author keithc@wpi.edu
 * \version 1
 */
#define BAUDRATE 9600
//#define BAUD_CALC ((F_CPU/BAUDRATE/16)-1)
#define BAUD_CALC (F_CPU / 4 / BAUDRATE - 1) / 2


#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>



/** \fn void InitializeSerial()
 * \brief Initializes the USART0.
 * \param void
 * \return void
 */
void InitializeSerial(void){
                UCSRA = 1 << 1;
                UBRRH = (unsigned char)(BAUD_CALC>>8);//Set baud rate
                UBRRL = (unsigned char)BAUD_CALC;//Set baud rate
                UCSRB = _BV(TXEN) | _BV(RXEN) | _BV(RXCIE);
                UCSRC = (1<<USBS)|(3<<UCSZ0);//Set frame format
}


/** \fn void serialTx( unsigned char output )
 * \brief Transmits an individual byte over serial.
 * \param unsigned char output
 * \return void
 */
void serialTx( unsigned char output )
{
                while ( !( UCSRA & (1<<UDRE)) ){//Wait to see if buffer is empty
                }
                UDR = output;//Sends data through buffer
}

/** \fn unsigned char serialRx()
 * \brief Receives an individual byte for serial and echoes it back.
 * \param void
 * \return unsigned char received
 */
unsigned char serialRx( void )
{
                char received = UDR;
                //serialTx(received);//echos data recived
                return received;//Get and return received data
}

/** \fn void serialTxString( char * str)
 * \brief Transmits a string, byte by byte, until it sees a null terminator.
 * \param char * str
 * \return void
 */
void serialTxString(char * str) {
                int i=0;
                while(str[i] != '\0') {
                    serialTx(str[i++]);
                }
                PORTB ^= 0xFF;
                _delay_ms(1000);
}


/** \fn ISR(USART0_RX_vect)
 * \brief Handles serial receive interrupts.
 * \param void
 * \return void
 */
/* 
ISR(USART_RX_vect){
    serialTx(UDR); //Add to buffer
}
*/

