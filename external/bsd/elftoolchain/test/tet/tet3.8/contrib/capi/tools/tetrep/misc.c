
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
NAME:		misc.c

PURPOSE:

	Miscellaneous support routines to enhance the code portability.

HISTORY:
	Andrew Josey, X/Open Company Ltd. 4/23/96 Created .


***********************************************************************/


/*
 * Copyright 1990 Open Software Foundation (OSF)
 * Copyright 1990 Unix International (UI)
 * Copyright 1990 X/Open Company Limited (X/Open)
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of OSF, UI or X/Open not be used in 
 * advertising or publicity pertaining to distribution of the software 
 * without specific, written prior permission.  OSF, UI and X/Open make 
 * no representations about the suitability of this software for any purpose.  
 * It is provided "as is" without express or implied warranty.
 *
 * OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
 * EVENT SHALL OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
 * USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
 * PERFORMANCE OF THIS SOFTWARE.
 *
 */

/************************************************************************

SCCS:           @(#)config.c    1.10 06/23/92
NAME:           config.c
PRODUCT:        TET (Test Environment Toolkit)
AUTHOR:         OSF Validation & SQA
DATE CREATED:    
CONTENTS:

************************************************************************/

/* Special non Posix functions used in tetrep */


int    tetw_opterr = 1;
int    tetw_optind = 1;
char   *tetw_optarg;

#include <string.h>
#include <stdio.h>

int optget(int argc, char *const argv[], const char opts[])
{
    static int sp = 1;
    int c;
    char *cp;

    if (sp == 1)
    {
        if (tetw_optind >= argc || argv[tetw_optind][0] != '-' || argv[tetw_optind][1] == '\0')
            return EOF;
        else if (strcmp(argv[tetw_optind], "--") == 0)
        {
            tetw_optind++;
            return EOF;
        }
    }

    c = argv[tetw_optind][sp];

    if (c == ':' || (cp=strchr(opts, c)) == NULL)
    {
        if (tetw_opterr)
            (void) fprintf(stderr, "%s: illegal option -- %c\n", argv[0], c);
        if (argv[tetw_optind][++sp] == '\0')
        {
            tetw_optind++;
            sp = 1;
        }
        return '?';
    }

    if (*++cp == ':')
    {
        if (argv[tetw_optind][sp+1] != '\0')
            tetw_optarg = &argv[tetw_optind++][sp+1];
        else if (++tetw_optind >= argc)
        {
            if (tetw_opterr)
                (void) fprintf(stderr,
                    "%s: option requires an argument -- %c\n", argv[0], c);
            sp = 1;
            return '?';
        } 
        else
            tetw_optarg = argv[tetw_optind++];
        sp = 1;
    }

    else

    {
        if (argv[tetw_optind][++sp] == '\0')
        {
            sp = 1;
            tetw_optind++;
        }
        tetw_optarg = NULL;
    }
    return c;
}
