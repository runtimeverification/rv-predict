/*
 * Copyright(c) 1996 X/Open Company Ltd.
 *
 * Permissions to use, copy, modify and distribute this software are
 * governed by the terms and conditions set forth in the file COPYRIGHT,
 * located with this software.
 */

/************************************************************************

SCCS:          %W%
PRODUCT:   	TETware 
NAME:		html.h

PURPOSE:

	Header file for HTML routines for use with the TETware report writer.

HISTORY:
	Andrew Josey, X/Open Company Ltd. 4/23/96 Created .


***********************************************************************/

void html_content ( void );
void html_start ( char *);
void html_end (void) ;
void html_h1( char *) ;
void html_h2( char *) ;
void html_hr( void ) ;
void html_preon( void ) ;
void html_preoff( void ) ;


int html_pre_on = 0;

#define HTML_PREON() if ( html_pre_on == 0) html_preon()
#define HTML_PREOFF() if ( html_pre_on == 1) html_preoff()
