/*
	Copyright (c) 1997 X/Open Company Ltd., A member of The Open Group.
  
	All rights reserved. No part of this source code may be reproduced,
	stored in a retrieval system, or transmitted, in any form or by any
	means, electronic, mechanical, photocopying, recording or otherwise,
	except as stated in the end-user licence agreement, without the prior
	permission of the copyright owners.
  
	Developed for X/Open by ApTest Ireland Limited.
  
	Project: VSORB
  
	File: src/utils/reports/getopt.c
  
	Purpose: Options parser (use our own since NT does not provide this)

	Modifications:
	$Log: getopt.c,v $
	Revision 1.1  1997/10/31 12:39:14  ian
	First version of report generators.

*/

#include	<string.h>

int	VSORBoptind;	/*index of the current argument (0 before first call)*/

int	VSORBoptopt;	/*current option letter*/

char	*VSORBoptarg;	/*pointer to the option-associated argument*/

int	VSORBgetopt (int argc, char* const argv[], char* defined_options_p);

static void  skip_to_next_options_argument (int argc, char * const argv []);

static	char	*arg_p;			/* pointer to current argument */

static	int	forward_args_used;	/* number of associated forward args already used */

static	int	used_columns;		/* columns used from current argument */


/*
// This procedure parses the next option in a given list of command line
// options. The allowable options are given in the "defined_options_p" string,
// in the form:
//
//		"A:bcdef:ghH"  ...
//
// where each letter in the string is an allowed option letter, and a colon
// following an option letter means that an associated following argument is required.
*/

int  VSORBgetopt (int argc, char *const argv[], char *defined_options_p)
{
	int		arg_index;
	char	expected_option;
	char	*options_p;
	char	*p;

   	VSORBoptarg = NULL;

	if (VSORBoptind == 0)
		skip_to_next_options_argument (argc, argv);

  	while (VSORBoptind < argc) {

		if (arg_p == NULL)
			return -1;

		p = arg_p + used_columns;

		if ((VSORBoptopt = *p++) == 0) {
			skip_to_next_options_argument (argc, argv);
			continue;
		}

		++used_columns;

		/* Look up the found "optopt" option letter in the table of defined */
		/* options. */

		options_p = defined_options_p;

		while ((expected_option = *options_p++) != 0) {

			if (expected_option == ':') continue;	/* colon means argument expected to follow */

			if (VSORBoptopt == expected_option) {

				if (*options_p == ':') {		/* argument required */

					if (*p != 0) {
						VSORBoptarg = p;
						used_columns = strlen (arg_p);

					} else {
						arg_index = VSORBoptind + forward_args_used + 1;	/* expected index of argument */

						if (arg_index >= argc || * (argv [arg_index]) == '-') {
							VSORBoptarg = NULL;
							return((int)':');
						} else {
							VSORBoptarg = (char *) argv [arg_index];
							++ forward_args_used;
						}
					}

				}

				return VSORBoptopt;
			}
		}

		return '?';
	}

	return -1;


} /* getopt */

static void  skip_to_next_options_argument (int argc, char * const argv [])

/*
// This procedure advances "optind" and "arg_p" to the next argument containing
// options. If there is no such argument, then "optind" is set to the index of
// the very next argument number (possibly equal to "argc", i.e. one higher than
// the index of the final argument) and "arg_p" is set to NULL.
*/

{
	int		index;

	used_columns = 0;

	VSORBoptind += forward_args_used;

	forward_args_used = 0;

	index = VSORBoptind++;

	do {
		if (++index >= argc) {
			arg_p = NULL;
			return;
		}

		arg_p = argv[index];

	} while (* arg_p != '-');

	VSORBoptind = index;
	used_columns = 1;

	return;

} /* skip_to_next_options_argument */
