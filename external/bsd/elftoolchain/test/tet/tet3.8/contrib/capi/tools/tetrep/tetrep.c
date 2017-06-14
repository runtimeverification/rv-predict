/*
 * Copyright(c) 1994-1996 Sun Microsystems Inc.
 * Copyright(c) 1994-1996 Information-technology Promotion Agency, Japan
 *
 * Permissions to use, copy, modify and distribute this software are
 * governed by the terms and conditions set forth in the file COPYRIGHT,
 * located in the ADLT release package.
 */

/************************************************************************

SCCS:           @(#)tetrep.c	1.5 12/01/95
PRODUCT:    Assertion Definition Language Translator (ADLT)
NAME:		tetrep.c

PURPOSE:

                    report generator for TET execution results;
		    Obtained from the CORBA testsuite.

HISTORY:
	Matt Evans, Sun Microsystems Inc. - : Created 6/21/95


CHANGE HISTORY:
	Andrew Josey, X/Open Company Ltd - 4/23/96
	Updates for TETware release 3.

***********************************************************************/

/*
Copyright(c) 1994-1996 Information-technology Promotion Agency, Japan

This technology has been developed as part of a collaborative project among
the Information-technology Promotion Agency, Japan (IPA), X/Open Company
Ltd. and Applied Testing and Technology, Incorporated.

Permission to use, copy, modify, and distribute this software and
documentation for any purpose and without fee is hereby granted, provided
that this COPYRIGHT AND LICENSE NOTICE appears in its entirety in all copies
of the software and supporting documentation.

The names Information-technology Promotion Agency, Japan (IPA), X/Open
Company Ltd. and Applied Testing and Technology, Incorporated shall not be
used in advertising or publicity pertaining to distribution of the software
and documentation without specific, written prior permission.

APPLIED TESTING AND TECHNOLOGY, THE INFORMATION-TECHNOLOGY PROMOTION
AGENCY, AND X/OPEN COMPANY LIMITED DISCLAIM ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE AND DOCUMENTATION, INCLUDING ALL IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL APPLIED
TESTING AND TECHNOLOGY, THE INFORMATION-TECHNOLOGY PROMOTION AGENCY,
NOR X/OPEN COMPANY LIMITED BE LIABLE FOR ANY SPECIAL, INDIRECT, OR
CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE OR DOCUMENTATION.
*/

#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <dirent.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/utsname.h>
#include <pwd.h>
#include <time.h>

#include "html.h"
#include "misc.h"


static void parseinidle(void);

const char OPTIONS[] = ":f:j:s:hcuvtq";

struct	utsname	ouruname;

/*results directory*/
char	resdir[512];
/*input file name*/
char 	*infile;
/*where its stored if not user provided*/
char	inbuf[512];

/*a buffer to play with*/
char	tmpbuf[512];

/*stream for the journal file*/
FILE	*jfile;

int	fdetailhead=0;
/*goodies for the summary report*/
/*time test run started (from TCC start)*/
char	starttime[64];
/*time test run ended (from TCC end)*/
char	endtime[64];
/*test run user (from TCC start)*/
char	username[64];
/*test command line (from TCC start)*/
char	commandline[128];
/*number of build errors*/
int	nbuilderr;
/*number of TCC errors*/
int	ntccerr;
/*number of TCM errors*/
int	ntcmerr;

int	total_areas = 0;
int	total_tests = 0;
int	total_pass=0;
int	total_fail=0;
int	total_unres=0;
int	total_notinuse=0;
int	total_unsupported=0;
int	total_untested=0;
int	total_uninitiated=0;
int	total_noresult=0;
int	total_extres=0; /* other extended results codes */
int	cur_tests = 0;
int	cur_pass=0;
int	cur_fail=0;
int	cur_unres=0;
int	cur_notinuse=0;
int	cur_unsupported=0;
int	cur_untested=0;
int	cur_uninitiated=0;
int	cur_noresult=0;
int	cur_extres=0; 

int	cur_rescode=1; /* current result code */

/*current filename being build/tested/cleaned*/
char	currentfile[256];

/*message type*/
int	mtype;
/*messages read in here*/
char	linebuf[512];
/*pointer into where we are in the message*/
char	*pline;

/*output type 0=html >0 = text */
int	textout=0;
/* quiet summary report */
int	quiet=0;

/*state of the enterprise*/
int	state;
/*the states */
#define S_START		0
#define S_CONFIG	1
#define	S_IDLE		2
#define	S_BUILD		3
#define	S_EXEC		4
#define	S_CLEAN		5

/*test return values - correspoding to TET result codes*/
#define R_PASS		0
#define R_FAIL		1
#define R_UNRESOLVED	2
#define R_NOTINUSE	3
#define R_UNSUPPORTED	4
#define R_UNTESTED	5
#define R_UNINITIATED	6
#define R_NORESULT	7

int	 lineno;

void print_test_sum(void)
{
	if (quiet)
		return;

	if (!textout) {
		HTML_PREON();
	}
	if (fdetailhead == 0) {
		fdetailhead = 1;
		printf("\t\tRESULTS BY TEST CASE REPORT\n\n");
	}
		
	printf("%d test results for test : %s\n", cur_tests, currentfile);

	if (cur_tests != 0) {

		printf("PASSED     : %7d (%3d%%)\t", cur_pass, (cur_pass *100)/cur_tests);
		printf("FAILED     : %7d (%3d%%)\n", cur_fail, (cur_fail *100)/cur_tests);
		printf("UNSUPPORTED: %7d (%3d%%)\t", cur_unsupported, (cur_unsupported *100)/cur_tests);
		printf("UNINITIATED: %7d (%3d%%)\n", cur_uninitiated, (cur_uninitiated *100)/cur_tests);
		printf("UNTESTED   : %7d (%3d%%)\t", cur_untested, (cur_untested *100)/cur_tests);
		printf("UNRESOLVED : %7d (%3d%%)\n", cur_unres, (cur_unres *100)/cur_tests);
		printf("NOTINUSE   : %7d (%3d%%)\t", cur_notinuse, (cur_notinuse *100)/cur_tests);
		printf("NORESULT   : %7d (%3d%%)\n", cur_noresult, (cur_noresult *100)/cur_tests);
		printf("EXTENDED   : %7d (%3d%%)\n\n", cur_extres, (cur_extres *100)/cur_tests);
	}
	else
		printf("\n");

}

/*parse messages in the start state*/
/*handles message TCC Start (type 0)*/
static void parseinstart(void)
{
	switch (mtype) {
	/*TCC Start*/
	case 0:
		/*skip tet version*/
		pline = strtok(NULL, " ");
		/*get start time*/
		pline = strtok(NULL, "|");
		strcpy(starttime, pline);
		/*skip "User:*/
		pline = strtok(NULL, " ");
		/*get user*/
		pline = strtok(NULL, " ");
		strcpy(username, pline);
		/*get command line*/
		pline = strtok(NULL, ":");
		pline = strtok(NULL, "\n");
		strcpy(commandline, pline);
		state = S_CONFIG;
		break;
	/*TCM message*/
	case 510:
		ntcmerr++;
		break;
	/*operator abort*/
	case 90:
		printf("\nOperator abort\n");
		state=S_IDLE;
		break;
	/*TCC message*/
	case 50:
		ntccerr++;
		break;
	default:
		if (mtype == 5) {
			fprintf(stderr, "tetrep: warning unexpected record (type = %d) in CONFIG state, line %d\n", mtype, lineno);
			return;
		}

		if (!textout) {
			HTML_PREON();
		}
		fprintf(stderr, "tetrep: illegal record (type = %d) in START state, line %d\n", mtype, lineno);
		if (!textout) {
			HTML_PREOFF();
		}
		exit(2);
	}
}

/*parse messages in the config state*/
/*handles message types 20, 30, 40)*/
static void parseinconfig(void)
{
	if (mtype == 70  || mtype == 110 ) { 
		state = S_IDLE;
		parseinidle();
		return;
	}
	if (mtype == 40) {
		state=S_IDLE;
		return;
	}

	/*operator abort*/
	if (mtype == 90) {
		printf("\nOperator abort\n");
		state=S_IDLE;
		return;
	}

	/*TCC message*/
	if (mtype == 50) {
		ntccerr++;
		return;
	}

	if (mtype == 5 ) {
		fprintf(stderr, "tetrep: warning unexpected record (type = %d) in CONFIG state, line %d\n", mtype, lineno);
		return;
	}

	if ((mtype != 20) && (mtype != 30) && (mtype != 40)) {
		if (!textout) {
			 HTML_PREON();
		}
		fprintf(stderr, "tetrep: illegal record (type = %d) in CONFIG state, line %d\n", mtype, lineno);
		if (!textout) {
			 HTML_PREOFF();
		}
		exit(2);
	}
}

static void parseinbuild(void);

/*parse messages in the idle state between tests*/
/*handles message types 70, 10, 110, 300, 900)*/
static void parseinidle(void)
{
	int	i;

	switch (mtype) {
	/*TC Start*/
	case 10:
		pline = strtok(NULL, " ");
		pline = strtok(NULL, " ");
		pline++;
		strcpy(currentfile, pline);
		/* currentfile[strlen(currentfile)-1] = 0; --- by okada --- */
		total_areas++;
		cur_tests = cur_pass = cur_fail = cur_untested = cur_unres = 0;
		cur_extres = cur_unsupported = cur_uninitiated = cur_noresult=0;
		cur_notinuse = 0;
		state=S_EXEC;
		break;
	/*Build start*/
	case 110:
		state=S_BUILD;
		parseinbuild();
		return;
	/*Clean start*/
	case 300:
		state=S_CLEAN;
		break;
	/*TCC End*/
	case 900:
		state=S_START;
		/*get end time*/
		pline = strtok(NULL, "|");
		strcpy(endtime, pline);
		break;
	/*Scenario message*/
	case 70:
		break;

	case 20:
		state=S_CONFIG;
		break;
	/*operator abort*/
	case 90:
		state=S_IDLE;
		printf("\nOperator abort\n");
		break;
	/*TCM message*/
	case 510:
		ntcmerr++;
		break;
	/*TCC message*/
	case 50:
		ntccerr++;
		break;
	default:
		if (!textout) {
			 HTML_PREON();
		}
		fprintf(stderr, "tetrep: illegal record (type = %d) in IDLE state, line %d\n", mtype, lineno);
		if (!textout) {
			 HTML_PREOFF();
		}
		exit(2);
	}
}

static void parseinexec(void);

/*parse messages in the build state*/
static void parseinbuild(void)
{
	int	buildret;

	switch (mtype) {

	/*TCM start*/
	case 15:
		break;
	/*IC start*/
	case 400:
		break;
	/*IC end*/
	case 410:
		break;
	/*TP start*/
	case 200:
		break;
	/*TP result*/
	case 220:
		cur_tests++;
		total_tests++;
		pline = strtok(NULL, " "); 
		pline = strtok(NULL, "|");
		pline = strtok(NULL, "\n");
		if (strcmp(pline, "PASS") == 0) {
			cur_pass++;
			total_pass++;
		} else
		if (strcmp(pline, "FAIL") == 0) {
			cur_fail++;
			total_fail++;
		} else
		if (strcmp(pline, "UNRESOLVED") == 0) {
			total_unres++;
			cur_unres++;
		} else
		if (strcmp(pline, "UNTESTED") == 0) {
			total_untested++;
			cur_untested++;
		} else {
			total_extres++;
			cur_extres++;
		}
		break;
	/*TC info*/
	case 520:
		break;

	/*captured*/
	case 100:
	case 110:
		break;
	/*build end*/
	case 130:
		state=S_IDLE;
		break;
	/*operator abort*/
	case 90:
		printf("\nOperator aborted build of file %s\n", currentfile);
		break;
	/*TCC message*/
	case 50:
		ntccerr++;
		break;
	/*TCM message*/
	case 510:
		ntcmerr++;
		break;
	default:
		fprintf(stderr, "tetrep: illegal record (type = %d) in BUILD state, line %d\n", mtype, lineno);
		exit(2);
	}
}

/*parse messages in the clean state*/
static void parseinclean(void)
{
	switch (mtype) {
	/*TCM start*/
	case 15:
		break;
	/*IC start*/
	case 400:
		break;
	/*IC end*/
	case 410:
		break;
	/*TP start*/
	case 200:
		break;
	/*TP result*/
	case 220:
		cur_tests++;
		total_tests++;
		pline = strtok(NULL, " ");
		pline = strtok(NULL, "|");
		pline = strtok(NULL, "\n");
		if (strcmp(pline, "PASS") == 0) {
			cur_pass++;
			total_pass++;
		} else
		if (strcmp(pline, "FAIL") == 0) {
			cur_fail++;
			total_fail++;
		} else
		if (strcmp(pline, "UNRESOLVED") == 0) {
			total_unres++;
			cur_unres++;
		} else
		if (strcmp(pline, "UNTESTED") == 0) {
			total_untested++;
			cur_untested++;
		} else {
			total_extres++;
			cur_extres++;
		}
		break;
	/*TC info*/
	case 520:
		break;

	/*captured*/
	case 100:
		break;
	/*clean end*/
	case 320:
		state=S_IDLE;
		break;
	/*operator abort*/
	case 90:
		printf("\nOperator aborted clean of file %s\n", currentfile);
		break;
	/*TCC message*/
	case 50:
		ntccerr++;
		break;
	/*TCM message*/
	case 510:
		ntcmerr++;
		break;
	default:
		fprintf(stderr, "tetrep: illegal record (type = %d) in CLEAN state, line %d\n", mtype, lineno);
		exit(2);
	}
}


void check_exp_rescode ( int exp_rescode, int rescode)
{
	if (exp_rescode != rescode ) {
		fprintf(stderr, "tetrep: illegal record : Result code expected %d  got %d\n", exp_rescode, rescode);
		if (!textout) {
			HTML_PREOFF();
		} 
		exit(2);
	}
} 

/*parse messages in the exec state*/
static void parseinexec(void)
{
	switch (mtype) {
	/*TCM start*/
	case 15:
		break;
	/*TC end*/
	case 80:
		print_test_sum();
		state=S_IDLE;
		break;
	/*IC start*/
	case 400:
		break;
	/*IC end*/
	case 410:
		break;
	/*TP start*/
	case 200:
		break;
	/*TP result*/

	case 220:
		cur_tests++;
		total_tests++;
		pline = strtok(NULL, " "); /* get to activity */
		pline = strtok(NULL, " "); /* get to testnum */
		pline = strtok(NULL, " "); /* get to resultcode */
		cur_rescode=atoi(pline);
		pline = strtok(NULL, "|");
		pline = strtok(NULL, "\n");
		if (strcmp(pline, "PASS") == 0) {
			check_exp_rescode ( R_PASS, cur_rescode);
			cur_pass++;
			total_pass++;
		} else
		if (strcmp(pline, "FAIL") == 0) {
			check_exp_rescode ( R_FAIL, cur_rescode);
			cur_fail++;
			total_fail++;
		} else
		if (strcmp(pline, "UNRESOLVED") == 0) {
			check_exp_rescode ( R_UNRESOLVED, cur_rescode);
			total_unres++;
			cur_unres++;
		} else
		if (strcmp(pline, "UNTESTED") == 0) {
			check_exp_rescode ( R_UNTESTED, cur_rescode);
			total_untested++;
			cur_untested++;
		} else
		if (strcmp(pline, "NOTINUSE") == 0) {
			check_exp_rescode ( R_NOTINUSE, cur_rescode);
			total_notinuse++;
			cur_notinuse++;
		}else 
		if (strcmp(pline, "UNSUPPORTED") == 0) {
			check_exp_rescode ( R_UNSUPPORTED, cur_rescode);
			total_unsupported++;
			cur_unsupported++;
		}else
		if (strcmp(pline, "UNINITIATED") == 0) {
			check_exp_rescode ( R_UNINITIATED, cur_rescode);
			total_uninitiated++;
			cur_uninitiated++;
		}else
		if (strcmp(pline, "NORESULT") == 0) {
			check_exp_rescode ( R_NORESULT, cur_rescode);
			total_noresult++;
			cur_noresult++;
		}else {
			total_extres++;
			cur_extres++;
		}
		break;
	/*TC info*/
	case 520:
		break;
	/*operator abort*/
	case 90:
		printf("\nOperator aborted exec of file %s\n", currentfile);
		break;
	/*captured*/
	case 100:
		break;
	/*TCM message*/
	case 510:
		ntcmerr++;
		break;
	/*TCC message*/
	case 50:
		ntccerr++;
		break;
	default:
		fprintf(stderr, "tetrep: illegal record (type = %d) in EXEC state, line %d\n", mtype, lineno);
		exit(2);
	}
}

/*read and parse a journal file*/
static void parse_file(void)
{
	/*open the journal file for read*/
	jfile = fopen(infile, "r");
	if (jfile == NULL) {
		perror("tetrep: cannot open journal file");
		exit(2);
	}

	state = S_START;

	/*parse lines one by one*/
	while (fgets(linebuf, sizeof(linebuf), jfile) != NULL) {
		lineno++;
		if ((int)strlen(linebuf) < 2)
			continue;
#ifdef CDEBUG
fprintf(stderr, "%s", linebuf);
fflush(stderr);
#endif
		/*get the message type number*/
		pline = strtok(&linebuf[0], "|");
		mtype = atoi(linebuf);
#ifdef CDEBUG
fprintf(stderr, "State = %d, type = %d\n", state, mtype);
#endif
		switch (state) {
		case S_START:
			parseinstart();
			break;
		case S_CONFIG:
			parseinconfig();
			break;
		case S_IDLE:
			parseinidle();
			break;
		case S_BUILD:
			parseinbuild();
			break;
		case S_CLEAN:
			parseinclean();
			break;
		case S_EXEC:
			parseinexec();
			break;
		default:
			fprintf(stderr, "tetrep: illegal state: %d, line %d\n", state, lineno);
			exit(2);
		}
	}
}

static void print_ecount(int errcnt, char *errstring)
{
	if (errcnt == 0) ;
	else
		if (errcnt == 1)
			printf("1 %s error occurred\n", errstring);
		else
			printf("%d %s errors occurred\n", errcnt, errstring);
}

static char * get_relinfo(void)
{
/* TETware product identification and release details */
	static char relinfo[]="TETware Release 3.0 4/23/96";
	return (relinfo);

}

/*print the summary report*/
static void print_summary(void)
{

	int	i,j, k;
	struct passwd *pwdp;
	char	ttmp[5];
	struct tm mytm;
	time_t	mytime;
	char	strbuf[64];

	if (! quiet) {
		if (!textout)
			html_hr();
		else
			printf("\f");
		printf("\t\t%s\n",get_relinfo());
	}

	printf("\t\tSUMMARY RESULTS REPORT\n\n");
	printf("Test run by: %s\n", username);

	if (uname(&ouruname) != -1) {
		printf("System: %s %s %s %s %s\n", ouruname.sysname, ouruname.nodename, ouruname.release, ouruname.version, ouruname.machine);
	}

	pline = strtok(&starttime[0], ":");
		if (pline == NULL) {
			fprintf(stderr, "tetrep: Null pline in %s\n", linebuf);
			exit(2);
		}
	mytm.tm_hour=atoi(pline);
	pline = strtok(NULL, ":");
		if (pline == NULL) {
			fprintf(stderr, "tetrep: Null pline in %s\n", linebuf);
			exit(2);
		}
	mytm.tm_min=atoi(pline);
	pline = strtok(NULL, " ");
		if (pline == NULL) {
			fprintf(stderr, "tetrep: Null pline in %s\n", linebuf);
			exit(2);
		}
	mytm.tm_sec=atoi(pline);
	pline = strtok(NULL, "\0");
		if (pline == NULL) {
			fprintf(stderr, "tetrep: Null pline in %s\n", linebuf);
			exit(2);
		}
 	strncpy(ttmp, pline, 4);	
 	ttmp[4]= 0;
 	mytm.tm_year=atoi(ttmp) - 1900;
 	strncpy(ttmp, pline+4, 2);	
 	ttmp[2]= 0;
 	mytm.tm_mon=atoi(ttmp)-1;
 	strncpy(ttmp, pline+6, 2);	
 	mytm.tm_mday=atoi(ttmp);
 	mytm.tm_isdst=-1;
 	mytime = mktime(&mytm);
	
	strftime(strbuf, sizeof(strbuf), "%A %B %d, %Y %r", &mytm);
	printf("Test run started: %s\n", strbuf);
	if (strlen(endtime) != 0) {
		pline = strtok(&endtime[0], ":");
		if (pline == NULL) {
			fprintf(stderr, "tetrep: Null pline in %s\n", linebuf);
			exit(2);
		}
		mytm.tm_hour=atoi(pline);
		pline = strtok(NULL, ":");
		if (pline == NULL) {
			fprintf(stderr, "tetrep: Null pline in %s\n", linebuf);
			exit(2);
		}
		mytm.tm_min=atoi(pline);
		pline = strtok(NULL, "\0");
		if (pline == NULL) {
			fprintf(stderr, "tetrep: Null pline in %s\n", linebuf);
			exit(2);
		}
		mytm.tm_sec=atoi(pline);
		mytime = mktime(&mytm);
	
		strftime(strbuf, sizeof(strbuf), "%A %B %d, %Y %r", &mytm);
		printf("Test run ended:   %s\n", strbuf);
	}
	else
		printf("Test run not completed\n");


	printf("Journal file: %s\n", infile);
	printf("TCC command line:%s\n", commandline);
	printf("\n");

	print_ecount(nbuilderr, "build");
	print_ecount(ntccerr, "TCC");
	print_ecount(ntcmerr, "TCM");
	printf("\n");

	printf("TEST CASES   %7d\n", total_areas);
	printf("TEST RESULTS %7d\n", total_tests);

	if (total_tests != 0) {

		printf("PASSED     : %7d (%3d%%)\t", total_pass, (total_pass *100)/total_tests);
		printf("FAILED     : %7d (%3d%%)\n", total_fail, (total_fail *100)/total_tests);
		printf("UNSUPPORTED: %7d (%3d%%)\t", total_unsupported, (total_unsupported *100)/total_tests);
		printf("UNINITIATED: %7d (%3d%%)\n", total_uninitiated, (total_uninitiated *100)/total_tests);
		printf("UNTESTED   : %7d (%3d%%)\t", total_untested, (total_untested *100)/total_tests);
		printf("UNRESOLVED : %7d (%3d%%)\n", total_unres, (total_unres *100)/total_tests);
		printf("NOTINUSE   : %7d (%3d%%)\t", total_notinuse, (total_notinuse *100)/total_tests);
		printf("NORESULT   : %7d (%3d%%)\n", total_noresult, (total_noresult *100)/total_tests);
		printf("EXTENDED   : %7d (%3d%%)\n\n", total_extres, (total_extres *100)/total_tests);
	}


}

/*keep ANSI happy*/
void main(char, char * const []);

void main(char argc, char * const argv[])
{
	char	*sname = NULL;
	int	optlet;
 	int	errflag = 0;
 	int	fcount = 0;
 	int	fuser = 0;
	int	jfileno = 0;
	DIR	*dirp;
	struct	 dirent	*dp;
	struct	 stat	sbuf;
	time_t	best_time=0;
	uid_t	ouruid=0;
	char	jbuf[5];
	FILE *verfile;
	char *vernum;


	if (getenv("TET_ROOT") == NULL) {
		(void) fprintf(stderr, "Warning: TET_ROOT not set, tetrep aborting...\n");
		errflag++;
	}

	tetw_opterr = 1;

	while ((optlet = optget(argc, argv, OPTIONS))!= -1) {
		switch (optlet) {
		case 'f':
			infile = tetw_optarg;
			break;
		case 'h':
			errflag++;
			break;
		case 'j':
			jfileno = atoi(tetw_optarg);
			break;
		case 'u':
			fuser++;
			break;
		case 'v':
			fprintf(stderr, "Test Suite Report Generator: %s\n",get_relinfo());
			exit(0);
		case 's':
			sname = tetw_optarg;
			break;
		case 't': /*text output */
			textout++;
			break;
		case 'q': /*quiet summary output */
			quiet++;
			break;
		case ':':
			/* error message handled by optget*/
			errflag++;
			break;
		case '?':
			errflag++;
			break;
		}
	}
	if (!textout) {
		html_start("TETware Journal Report");
		HTML_PREON();
	}
	printf("\t\t%s\n",get_relinfo());

	if (errflag) {
		fprintf(stderr, "Usage: tetrep [-f filename] [-j journal] [-s suitename] [-u] [ -h ] [-v] [-t] [-q]\n");
		fprintf(stderr, "      -s test suite name\n");
		fprintf(stderr, "      -t output text instead of html\n");
		fprintf(stderr, "      -f journal file name\n");
		fprintf(stderr, "      -q print a quiet summary report\n");
		fprintf(stderr, "      -j journal file # (default is latest)\n");
		fprintf(stderr, "      -u use latest journal file for current user\n");
		fprintf(stderr, "      -h display this usage message\n");
		fprintf(stderr, "      -v display program version\n");
		fprintf(stderr, "      TET_ROOT must be set\n");
	if (!textout) {
		HTML_PREOFF();
		html_end();
	}
		exit(2);
	}

	/* -s*/
	if (sname) {
		/* make a path to the test suite results directory */
		sprintf(resdir, "%s/%s/results", getenv("TET_ROOT"), sname);
	} else {
		/* assume we are in the test suite root directory */
		if (!getcwd(tmpbuf, 512)) {
			fprintf(stderr, "tetrep: getcwd failed! aborting!\n");
			if (!textout) {
				HTML_PREOFF();
				html_end();
			}
			exit(2);
		}
		sprintf(resdir, "%s/results", tmpbuf);
	}
	/* -j*/
	if (jfileno != 0) {
		/*make the full number*/
		sprintf(jbuf, "%04d", jfileno);
		jbuf[4] = 0;
		/*find the results directory*/
		if ((dirp = opendir(resdir)) == NULL) {
			perror("tetrep: cannot open results directory");
			if (!textout) {
				HTML_PREOFF();
				html_end();
			}
			exit(2);
		}

		/*skip . and ..*/
		dp = readdir(dirp);
		dp = readdir(dirp);

		/*find a matching results sub-directory, ignoring tcc mode*/
		while ((dp = readdir(dirp)) != NULL) {
			if (strstr(dp->d_name, jbuf) != 0) {
				sprintf(inbuf, "%s/%s/journal", resdir, dp->d_name);
				infile = inbuf;
				if (access(infile, R_OK) != 0) {
					perror("tetrep: cannot access journal file");
					if (!textout) {
						HTML_PREOFF();
						html_end();
					}
					exit(2);
				}
				break;
			}
		}

		if (infile == 0) {
			fprintf(stderr, "tetrep: cannot find journal numbered %d\n", jfileno);
			if (!textout) {
				HTML_PREOFF();
				html_end();
			}
			exit(2);
		}
	}


	if (fuser)
		ouruid = getuid();

	/* -u option or no -f or -j*/
	if ((infile == 0) || (fuser)) {
		/*find the results directory*/
		if ((dirp = opendir(resdir)) == NULL) {
			perror("tetrep: cannot open results directory");
			if (!textout) {
				HTML_PREOFF();
				html_end();
			}
			exit(2);
		}

		/*skip . and ..*/
		dp = readdir(dirp);
		dp = readdir(dirp);

		/*find the most recently modified results sub-directory*/
		while ((dp = readdir(dirp)) != NULL) {
			sprintf(tmpbuf, "%s/%s", resdir, dp->d_name);
#ifdef CDEBUG
fprintf(stderr, "dir: %s\n", tmpbuf);
#endif
			if (stat(tmpbuf, &sbuf) != 0) {
				if (fcount == 0)
					perror("tetrep: no results sub-directories");
				else
					perror("tetrep: cannot stat a results sub-directory");
					if (!textout) {
						HTML_PREOFF();
						html_end();
					}
				exit(2);
			}

			/*ignore regular files that might sneak in*/
			if (S_ISDIR(sbuf.st_mode) == 0)
				continue;

			/*only look at ones we own if -u specified*/
			if (fuser)
				if (ouruid != sbuf.st_uid)
					continue;

			/*compare times*/
			if (fcount == 0) {
				sprintf(inbuf, "%s", tmpbuf);
				best_time = sbuf.st_mtime;
			}
			else {
				if (difftime(sbuf.st_mtime, best_time) >= 0) {
					sprintf(inbuf, "%s", tmpbuf);
					best_time = sbuf.st_mtime;
				}
			}
			fcount++;
		}

		if (fcount == 0)  {
			if (fuser) {
				fprintf(stderr, "tetrep: No journal files found for this user.\n");
				exit(2);
			}
			else {
				fprintf(stderr, "tetrep: No journal files found.\n");
				if (!textout) {
					HTML_PREOFF();
					html_end();
				}
				exit(2);
			}
		}

		/*we'll use that journal file*/
		strcat(inbuf, "/journal");
		infile = inbuf;
		if (access(infile, R_OK) != 0) {
			perror("tetrep: cannot access latest journal");
			if (!textout) {
				HTML_PREOFF();
				html_end();
			}
			exit(2);
		}
	}
	/*user defined file*/
	else {
		if (access(infile, R_OK) != 0) {
			perror("tetrep: cannot access specified journal file");
			if (!textout) {
				HTML_PREOFF();
				html_end();
			}
			exit(2);
		}
	}

	parse_file();
	
	print_summary();

	if (!textout) {
		HTML_PREOFF();
		html_end();
	}
	exit(0);
}
