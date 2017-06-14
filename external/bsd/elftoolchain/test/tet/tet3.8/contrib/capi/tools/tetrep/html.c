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
NAME:		html.c

PURPOSE:

	Simple HTML routines for use with the TETware report writer.

HISTORY:
	Andrew Josey, X/Open Company Ltd. 4/23/96 Created .


***********************************************************************/

#include <string.h>
#include <stdio.h>

extern int html_pre_on;

void html_content( void)
{
	(void) printf("Content-Type: text/html \n\n");
}

void html_start ( char *title )
{
	(void) printf (
         "<HTML><HEAD>\n<TITLE>%s</TITLE>\n</HEAD>\n<BODY>\n<H1>%s</H1><P>\n",
  	 title, title);
}                                     

void html_end (void)
{
	(void) printf ("</BODY></HTML>\n");
}

void html_h1( char *title )
{
	(void) printf ( "<H1>%s</H1>\n", title);
}

void html_h2( char *title )
{
	(void) printf ( "<H2>%s</H2>\n", title);
}

void html_hr( void )
{
	(void) printf ( "<HR>\n");
}

void html_preon( void )
{
	html_pre_on = 1;
	(void) printf ( "<PRE>\n");
}

void html_preoff( void )
{
	html_pre_on = 0;
	(void) printf ( "</PRE>\n");
}

#ifdef TEST
main()
{
	html_start("TITLEXX");
	html_h1("Header1");
	html_h2("Header1");
	html_end();
}
#endif
