/*
   Copyright (c) 1997 X/Open Company Ltd., A member of The Open Group.
  
   All rights reserved. No part of this source code may be reproduced,
   stored in a retrieval system, or transmitted, in any form or by any
   means, electronic, mechanical, photocopying, recording or otherwise,
   except as stated in the end-user licence agreement, without the prior
   permission of the copyright owners.
  
   Developed for X/Open by ApTest Ireland Limited.
  
   Project: VSORB
  
   File: src/utils/reports/vsorbrptm.c
  
   Purpose: Multi-Report Generator
  
   Modifications:
   $Log: vsorbrptm.c,v $
   Revision 1.2  1997/11/02 15:08:03  ian
   Minor bugfixes -AS.

   Revision 1.1  1997/10/31 12:39:15  ian
   First version of report generators.

*/

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>

/*
#include <unistd.h>
#include <dirent.h>
*/

static void parseinidle(void);

#define OPTIONS	":s:ufah"

int lineno = 0;
char * reportcase;
int freportcase = 0;
int freportunin = 0;
int freportall = 0;
int freportfail = 0;
int finscope;

/* program version*/
char	verbuf[512];

/*a buffer to play with*/
char    tmpbuf[512];

/*number of files*/
int	nfiles;
int	curfile;
char 	*curfilename;

int	current_area = 0;

/*this is the max number of files to compare*/
#define NSYS	6
char *fnames[NSYS];

/*file descriptors for files to compare*/
FILE	*files[NSYS];

/*name of the current area*/
char areaname[128];

int	nareas;

int hits = 0;

#define NAREAS	5000
struct	area {
	char	name[512];
	int	firstresult;
	int	nresults;
} areas[NAREAS];

#define NRESULTS	10000
int	results[NSYS][NRESULTS];

/*count of messages during build*/
int bstore = 0;

/*info about errors in files*/
struct file_err {
	int	actualareas;
	int	actualtests;
	int	npass;
	int	nfail;
	int	nunresolved;
	int	nuninitiated;
	int	nunsupported;
	int	nuntested;
	int	nnotinuse;
	int	nnoresult;
	int	nwarning;
	int	nfip;
	int	nbuildfail;
	int	ntccerr;
	int	ntcmerr;
} file_errs[NSYS];

int	nextresult = 0;
int	nextfreeresult = 0;
	
/*message type*/
int	mtype;
/*messages read in here*/
char	linebuf[2048];
/*pointer into where we are in the message*/
char	*pline, *pline2;

/*state of the enterprise*/
int	state;
/*the states */
#define S_START		0
#define S_CONFIG	1
#define	S_IDLE		2
#define	S_BUILD		3
#define	S_EXEC		4
#define	S_CLEAN		5

/*test return values*/
#define R_PASS		0+1
#define R_FAIL		1+1
#define R_UNSUPPORTED	2+1
#define R_UNINITIATED	3+1
#define R_UNRESOLVED	4+1
#define R_UNTESTED	5+1
#define R_NORESULT	6+1
#define R_NOTINUSE	7+1
#define R_WARNING	101+1
#define R_FIP		102+1
#define R_BUILDFAIL		103+1


/*parse messages in the start state*/
/*handles message TCC Start (type 0)*/
static void parseinstart(void)
{
	switch (mtype) {
	/*TCC Start*/
	case 0:
	case 5:
	/*TCM message*/
	case 510:
	/*operator abort*/
	case 90:
	/*TCC message*/
	case 50:
		break;
	/*Build end*/
	case 20:
		state = S_CONFIG;
		break;
	default:
		fprintf(stderr, "Illegal record (type = %d) in START state in file %s: line %d\n", mtype, fnames[curfile], lineno);
		exit(2);
	}
}

/*parse messages in the config state*/
/*handles message types 30, 40)*/
static void parseinconfig(void)
{
	switch (mtype) {
		/*TCC message*/
		case 50:
		/*Build message*/
		case 30:
			break;

		/*Build end*/
		case 40:
		/*operator abort*/
		case 90:
			state=S_IDLE;
			break;

		default:
			fprintf(stderr, "Illegal record (type = %d) in CONFIG state in file %s line %d\n", mtype, fnames[curfile], lineno);
			exit(2);
	}
}

/*parse messages in the idle state between tests*/
/*handles message types 70, 10, 110, 300, 900)*/
static void parseinidle(void)
{
	int	i, fmatch;

	switch (mtype) {
	/*Config Start*/
	case 20:
		state=S_CONFIG;
		break;
	/*TC Start*/
	case 10:
		state=S_EXEC;
		pline = strtok(NULL, " ");
		pline = strtok(NULL, " ");
		if (strncmp(pline, "/tset/", 6) == 0)
			pline+=6;
		else
			pline++;
		strcpy(areaname, pline);
		if (freportcase == 1) {
			if (strncmp(areaname, reportcase, strlen(reportcase)) == 0)
				finscope = 1;
			else {
				finscope = 0;
				break;
			}
		}
		file_errs[curfile].actualareas++;
		fmatch = 0;
#ifdef CDEBUG
printf("CASE %s in file %s\n", pline, fnames[curfile]);
#endif
		for(i = 0; i < nareas; i++)  {
			if (strcmp(areaname, areas[i].name) == 0) {
#ifdef CDEBUG
printf("	MATCH\n");
#endif
				fmatch = 1;
				current_area = i;
				nextresult = areas[i].firstresult;
				break;
			}
		}
		if (fmatch == 0) {
			strcpy(areas[nareas].name, areaname);
			areas[nareas].firstresult=nextfreeresult;
			areas[nareas].nresults=1;
			nextresult = areas[nareas].firstresult;
			current_area = nareas;
			nextfreeresult++;
			if (nextfreeresult == NRESULTS) {
				printf("Can only handle %d results, increase NRESULTS and rebuild\n", NRESULTS);
				exit(2);
			}
			nareas++;
			if (nareas == NAREAS) {
				printf("Can only handle %d areas, increase NAREAS and rebuild\n", NAREAS);
				exit(2);
			}
#ifdef CDEBUG
printf("	NO MATCH, areas = %d\n", nareas);
#endif
		}
		break;
	/*Build start*/
	case 110:
		state=S_BUILD;
		bstore=0;
		pline = strtok(NULL, " ");
		pline = strtok(NULL, " ");
		if (strncmp(pline, "/tset/", 6) == 0)
			pline+=6;
		else
			pline++;
		strcpy(areaname, pline);
		if (freportcase == 1) {
			if (strncmp(pline, reportcase, strlen(reportcase)) == 0)
				finscope = 1;
			else {
				finscope = 0;
				break;
			}
		}
		break;
	/*Clean start*/
	case 300:
		state=S_CLEAN;
		break;
	/*TCC End*/
	case 900:
		state=S_START;
		break;
	/*Scenario message*/
	case 70:
	case 840:
	case 830:
		break;

	/*operator abort*/
	case 90:
		state=S_IDLE;
		break;
	/*TCM message*/
	case 510:
		file_errs[curfile].ntcmerr++;
		break;
	/*TCC message*/
	case 50:
		file_errs[curfile].ntccerr++;
		break;
	default:
		fprintf(stderr, "Illegal record (type = %d) in IDLE state in file %s line %d\n", mtype, fnames[curfile], lineno);
		exit(2);
	}
}

/*parse messages in the build state*/
static void parseinbuild(void)
{
	int	buildret, fmatch, i;

	switch (mtype) {
	/*captured*/
	case 100:
		bstore++;
#ifdef CDEBUG
fprintf(stderr, "bstore = %d\n", bstore);
#endif
		break;
	/*build end*/
	case 130:
		state=S_IDLE;
		pline = strtok(NULL, " ");
		pline = strtok(NULL, " ");
		buildret = atoi(pline);
#ifdef CDEBUG
fprintf(stderr, "buildret = %d\n", buildret);
#endif
		if (buildret != 0) {
			bstore = 0;
			if (freportcase == 1) {
				if (strncmp(areaname, reportcase, strlen(reportcase)) == 0)
					finscope = 1;
				else {
					finscope = 0;
					break;
				}
			}
			file_errs[curfile].actualareas++;
			fmatch = 0;
#ifdef CDEBUG
printf("CASE %s in file %s, build fail\n", areaname, fnames[curfile]);
#endif
			for(i = 0; i < nareas; i++)  {
				if (strcmp(areaname, areas[i].name) == 0) {
#ifdef CDEBUG
printf("	MATCH\n");
#endif
					fmatch = 1;
					current_area = i;
					nextresult = areas[i].firstresult;
					break;
				}
			}
			if (fmatch == 0) {
				strcpy(areas[nareas].name, areaname);
				areas[nareas].firstresult=nextfreeresult;
				areas[nareas].nresults=1;
				nextresult = areas[nareas].firstresult;
				current_area = nareas;
				nareas++;
				if (nareas == NAREAS) {
					printf("Can only handle %d areas, increase NAREAS and rebuild\n", NAREAS);
					exit(2);
				}
				nextfreeresult++;
				if (nextfreeresult == NRESULTS) {
					printf("Can only handle %d results, increase NRESULTS and rebuild\n", NRESULTS);
					exit(2);
				}
#ifdef CDEBUG
printf("	NO MATCH, areas = %d\n", nareas);
#endif
			}
			file_errs[curfile].nbuildfail++;
			results[curfile][nextresult++]=R_BUILDFAIL;
		}
		break;
	/*operator abort*/
	case 90:
		state=S_IDLE;
		break;
	/*TCC message*/
	case 50:
		file_errs[curfile].ntccerr++;
		break;
	/*TCM message*/
	case 510:
		file_errs[curfile].ntcmerr++;
		break;
	default:
		fprintf(stderr, "Illegal record (type = %d) in BUILD state in file %s line %d\n", mtype, fnames[curfile], lineno);
		exit(2);
	}
}

/*parse messages in the clean state*/
static void parseinclean(void)
{
	switch (mtype) {
	/*captured*/
	case 100:
		break;
	/*clean end*/
	case 320:
		state=S_IDLE;
		break;
	/*operator abort*/
	case 90:
		state=S_IDLE;
		break;
	/*TCC message*/
	case 50:
		break;
	/*TCM message*/
	case 510:
		break;
	default:
		fprintf(stderr, "Illegal record (type = %d) in CLEAN state in file %s line %d\n", mtype, fnames[curfile], lineno);
		exit(2);
	}
}

/*parse messages in the exec state*/
static void parseinexec(void)
{
	int	thisresult=0;
	int	thistp, j, k;

	switch (mtype) {
	/*TCM start*/
	case 15:
		break;
	/*TC end*/
	case 80:
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
		pline = strtok(NULL, " ");
		pline = strtok(NULL, " ");
		thistp = atoi(pline);
		pline = strtok(NULL, "|");
		pline = strtok(NULL, "\n");
		if (finscope != 0) {
			file_errs[curfile].actualtests++;
			if (strcmp(pline, "PASS") == 0) {
				if (bstore == 0)  {
					thisresult = R_PASS;
					file_errs[curfile].npass++;
				}
				else {
					thisresult = R_FIP;
					file_errs[curfile].nfip++;
				}
			}
			if (strcmp(pline, "FAIL") == 0) {
				thisresult = R_FAIL;
				file_errs[curfile].nfail++;
			}
			if (strcmp(pline, "UNRESOLVED") == 0) {
				thisresult = R_UNRESOLVED;
				file_errs[curfile].nunresolved++;
			}
			if (strcmp(pline, "UNINITIATED") == 0) {
				thisresult = R_UNINITIATED;
				file_errs[curfile].nuninitiated++;
			}
			if (strcmp(pline, "UNSUPPORTED") == 0) {
				thisresult = R_UNSUPPORTED;
				file_errs[curfile].nunsupported++;
			}
			if (strcmp(pline, "UNTESTED") == 0) {
				thisresult = R_UNTESTED;
				file_errs[curfile].nuntested++;
			}
			if (strcmp(pline, "NOTINUSE") == 0) {
				thisresult = R_NOTINUSE;
				file_errs[curfile].nnotinuse++;
			}
			if (strcmp(pline, "NORESULT") == 0) {
				thisresult = R_NORESULT;
				file_errs[curfile].nnoresult++;
			}
			if (strcmp(pline, "WARNING") == 0) {
				thisresult = R_WARNING;
				file_errs[curfile].nwarning++;
			}
			if (strcmp(pline, "FIP") == 0) {
				thisresult = R_FIP;
				file_errs[curfile].nfip++;
			}
			/*case where number of tests in an*/
			/*area increases in a later file*/
			if (areas[current_area].nresults < thistp) {

			/*if its the last just add on, saves lots of space*/
			if (current_area == nareas-1) {
				nextfreeresult++;
				if (nextfreeresult == NRESULTS) {
					printf("Can only handle %d results, increase NRESULTS and rebuild\n", NRESULTS);
					exit(2);
				}
				areas[current_area].nresults++;
				} else {

/*
printf("making room nresults = %d, first = %d, thistp = %d, next = %d, free = %d\n", areas[current_area].nresults, areas[current_area].firstresult, thistp, nextresult, nextfreeresult);
*/
					/*make room, make room*/
					for (j = 0; j < areas[current_area].nresults; j++)
						for (k = 0; k < NSYS; k++)
							results[k][nextfreeresult+j]=results[k][areas[current_area].firstresult+j];
					areas[current_area].firstresult = nextfreeresult;
					areas[current_area].nresults++;
					nextresult = areas[current_area].firstresult+areas[current_area].nresults-1;
					nextfreeresult = nextresult+1;
					if (nextfreeresult == NRESULTS) {
						printf("Can only handle %d results, increase NRESULTS and rebuild\n", NRESULTS);
						exit(2);
					}
/*
printf("%s\n", areaname);
printf("made   room nresults = %d, first = %d, thistp = %d, next = %d, free = %d\n", areas[current_area].nresults, areas[current_area].firstresult, thistp, nextresult, nextfreeresult);
*/
				}
			}
			results[curfile][nextresult++]=thisresult;
		}
		break;

	case 520:
		break;
	/*operator abort*/
	case 90:
		break;
	/*captured*/
	case 100:
		break;
	/*TCM message*/
	case 510:
		file_errs[curfile].ntcmerr++;
		break;
	/*TCC message*/
	case 50:
		file_errs[curfile].ntccerr++;
		break;
	default:
		fprintf(stderr, "Illegal record (type = %d) in EXEC state in file %s line %d\n", mtype, fnames[curfile], lineno);
		exit(2);
	}
}

/*read and parse a journal file*/
static void parse_file(FILE *jfile)
{
	state = S_START;

	/*parse lines one by one*/
	while (fgets(linebuf, sizeof(linebuf), jfile) != NULL) {
		lineno++;
/*
#ifdef CDEBUG
fprintf(stderr, "%s", linebuf);
fflush(stderr);
#endif
*/
		/*get the message type number*/
		pline = strtok(&linebuf[0], "|");
		mtype = atoi(linebuf);
/*
#ifdef CDEBUG
fprintf(stderr, "State = %d, type = %d\n", state, mtype);
#endif
*/
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
			fprintf(stderr, "Illegal state: %d in file %s line %d\n", state, fnames[curfile], lineno);
			exit(2);
		}
	}
}

/*print the summary report*/
static void print_summary(void)
{
	int	i,j, k, n, mismatch, hack;

	if ((freportcase == 1) && (nareas == 0)) {
		printf("Test Case %s not found\n", reportcase);
		exit(0);
	}

	printf("TEST CASE                           PURPOSE ");
	for (i = 0; i< nfiles; i++)
		printf(" FILE %d", i+1);
	printf("\n\n");

/*
	for (i = 0; i< nareas; i++) {
		printf("CASE = %s, first = %d, n = %d\n", areas[i].name, areas[i].firstresult, areas[i].nresults);
		n=0;
		for (j = areas[i].firstresult; j < areas[i].firstresult+ areas[i].nresults; j++ ) {
			printf("Result %d:", ++n);
			for (k = 0; k < nfiles; k++)
				printf("	%d", results[k][j]);
			printf("\n");
		}
	}
*/


	for (i = 0; i< nareas; i++) {
		for (j = 0; j < nfiles; j++) {
			if ((results[j][areas[i].firstresult] == R_BUILDFAIL) && (areas[i].nresults != 1)) {
				for (k = areas[i].firstresult; k < areas[i].firstresult+ areas[i].nresults; k++ )
				results[j][k] = R_BUILDFAIL;
			}
		}
	}

			
	for (i = 0; i< nareas; i++) {

		n = 0;

		for (j = areas[i].firstresult; j < areas[i].firstresult+ areas[i].nresults; j++ ) {
			n++;
			hack = 0;
			for (k = 0; k < nfiles; k++)
				hack = hack + results[k][j];
			if (freportall == 1)
				mismatch = 1;
			else {
				mismatch = 0;
				for (k = 0; k < nfiles; k++) {
					if (hack/nfiles !=  results[k][j] ) {
						mismatch++;
					}
				}
				if (mismatch != 0)
					hits++;
				if ((freportunin == 1) && (mismatch == 0)){
					for (k = 0; k < nfiles; k++) {
						if (results[k][j]  == R_UNINITIATED)
							mismatch++;
					}
					if (mismatch != nfiles)
						mismatch = 0;
				}
				if ((freportfail == 1) && (mismatch == 0)){
					for (k = 0; k < nfiles; k++) {
						if (results[k][j]  == R_UNINITIATED)
							mismatch++;
						if (results[k][j]  == R_FIP)
							mismatch++;
						if (results[k][j]  == R_UNRESOLVED)
							mismatch++;
						if (results[k][j]  == R_FAIL)
							mismatch++;
					}
				}
			}

			if (mismatch != 0) {
				printf("%-34.34s    %-5d  ", areas[i].name, n);

				for (k = 0; k < nfiles; k++) {
					if (results[k][j] == 0)
						printf("%-7s", "NTRUN");	
					else
					if (results[k][j] == R_PASS)
						printf("%-7s", "pass");	
					else
					if (results[k][j] == R_FAIL)
						printf("%-7s","FAIL");	
					else
					if (results[k][j] == R_UNSUPPORTED)
						printf("%-7s", "unsup");	
					else
					if (results[k][j] == R_UNINITIATED)
						printf("%-7s", "UNINI");	
					else
					if (results[k][j] == R_NOTINUSE)
						printf("%-7s", "ntinu");	
					else
					if (results[k][j] == R_UNRESOLVED)
						printf("%-7s", "UNRES");	
					else
					if (results[k][j] == R_UNTESTED)
						printf("%-7s", "untst");	
					else
					if (results[k][j] == R_NORESULT)
						printf("%-7s", "NORES");	
					else
					if (results[k][j] == R_WARNING)
						printf("%-7s", "WARN");	
					else
					if (results[k][j] == R_FIP)
						printf("%-7s", "FIP");	
					else
					if (results[k][j] == R_BUILDFAIL)
						printf("%-7s", "BFAIL");	
				}
				printf("\n");
			}
		}
	}

	if (freportall == 0)
		if (hits == 1)
			printf("\n%d variance found\n", hits);
		else
			printf("\n%d variances found\n", hits);
	
	printf("\n");
	
	printf("%-37s      ", "TEST CASES");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].actualareas);
	printf("\n");

	printf("%-37s      ", "TEST PURPOSES EXECUTED");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].actualtests);
	printf("\n");
	printf("%-37s      ", "BUILD FAILURES");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].nbuildfail);
	printf("\n");
	printf("%-37s      ", "TCM ERRORS");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].ntcmerr);
	printf("\n");
	printf("%-37s      ", "TCC ERRORS");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].ntccerr);
	printf("\n");


	printf("GOOD RESULTS\n");
	printf("%-37s      ", "  PASS");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].npass);
	printf("\n");

	printf("%-37s      ", "  UNSUPPORTED");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].nunsupported);
	printf("\n");

	printf("%-37s      ", "  UNTESTED");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].nuntested);
	printf("\n");

	printf("%-37s      ", "  NOTINUSE");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].nnotinuse);
	printf("\n");

	printf("ANALYSIS NEEDED\n");
	printf("%-37s      ", "  WARNING");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].nwarning);
	printf("\n");

	printf("%-37s      ", "  FIP");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].nfip);
	printf("\n");

	printf("ERROR RESULTS\n");
	printf("%-37s      ", "  FAIL");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].nfail);
	printf("\n");

	printf("%-37s      ", "  UNRESOLVED");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].nunresolved);
	printf("\n");

	printf("%-37s      ", "  NORESULT");	
	for (i = 0; i<nfiles; i++) 
		printf("%7d", file_errs[i].nnoresult);
	printf("\n");
}		

int  VSORBgetopt (int argc, char * const argv [], char *defined_options_p);
extern int VSORBoptind, VSORBoptopt;
extern char *VSORBoptarg;

int main(char, char * const []);

int main(char argc, char * const argv[])
{
	int	optlet;
	int	errflag = 0;
	struct tm *thetime;
	time_t thetimet;
	char timebuf[512];
	FILE *verfile;
	char *vernum;

	while ((optlet = VSORBgetopt(argc, argv, OPTIONS))!= -1) {

		switch (optlet) {
		case 's':
			reportcase = VSORBoptarg;
			freportcase = 1;
			break;
		case 'f':
			freportfail = 1;
			break;
		case 'u':
			freportunin = 1;
			break;
		case 'a':
			freportall = 1;
			break;
		case 'h':
			errflag++;
			break;
		case ':':
			fprintf(stderr, "Option -%c requires an operand\n", VSORBoptopt);
			errflag++;
			break;
		case '?':
			errflag++;
			break;
		}
	}

	if (errflag) {
		fprintf(stderr, "usage: rptm [-s test] [-a] [-h] [-u] [-f] journal1 journal2 [journal3] [journal4] [journal5] [journal6]\n");
		fprintf(stderr, "      -a show all results (not just variations)\n");
		fprintf(stderr, "      -h print this message\n");
		fprintf(stderr, "      -s report only on specified tests\n");
		fprintf(stderr, "      -u show variations plus build failures common to all reports \n");
		fprintf(stderr, "      -f show variations plus failures in any report \n");
		exit(2);
	}

	if ((freportall == 1) && (freportunin == 1)) {
		freportunin = 0;
	}
	if ((freportall == 1) && (freportfail == 1)) {
		freportfail = 0;
	}
	if ((argc - VSORBoptind)  < 2) {
		fprintf(stderr, "Must specify at least two files\n");
		exit(2);
	}

	if ((argc  - VSORBoptind) > NSYS) {
		fprintf(stderr, "Can specify at most %d files\n", NSYS);
		exit(2);
	}

	if (freportcase == 1)
		finscope = 0;
	else
		finscope = 1;

	nfiles = argc-VSORBoptind;
	
	for (curfile = 0; curfile < nfiles; curfile++) {

		fnames[curfile] = argv[VSORBoptind++];
		fprintf(stderr, "Processing file %s\n", fnames[curfile]);
		/*open the journal file for read*/
		files[curfile] = fopen(fnames[curfile], "r");
		if (files[curfile] == NULL) {
			fprintf(stderr, "Cannot open journal file %s\n", fnames[curfile]);
			perror("");
			exit(2);
		}
		lineno = 0;
		parse_file(files[curfile]);
	}

	printf("                        TETware JOURNAL FILE COMPARISON REPORT\n");

/*
 * if your test suite has version information you 
 * can include it here
 */
#ifdef VER_FILE
	sprintf(verbuf, "%s/TSNAME/RELEASE", getenv("TET_ROOT"));

	verfile = fopen(verbuf, "r");
	if (verfile == 0) {
		fprintf(stderr, "WARNING: Cannot open RELEASE file: %s\n", verbuf);
	}
	else {
		vernum = fgets(verbuf, sizeof(verbuf), verfile);
		if (vernum == 0)
			fprintf(stderr, "WARNING: Cannot read version\n");
	}
	printf("                                 VERSION %s\n\n", verbuf);
#endif

	thetimet = time(0);
	thetime=localtime(&thetimet);
	strftime(timebuf, sizeof(timebuf), "%x %X", thetime);
	printf("Report generated: %s\n\n", timebuf);

	for ( curfile = 0; curfile < nfiles; curfile++) 
		printf("FILE %d = %s\n", curfile+1, fnames[curfile]);
	printf("\n");

	if (freportall) {
		if (freportcase == 1)
			printf("All results for: %s\n", reportcase);
		else
			printf("All results for all Tests\n");
	} else 
	if (freportfail) {
		if (freportcase == 1)
			printf("Variances and all failures for: %s\n", reportcase);
		else
			printf("Variances and all failures for all Tests\n");
	} else 
	if (freportunin) {
		if (freportcase == 1)
			printf("Variances and UNINITIATED everywhere for: %s\n", reportcase);
		else
			printf("Variances and UNINITIATED everywhere for all Tests\n");
	} else {
		if (freportcase == 1)
			printf("Variances for: %s\n", reportcase);
		else
			printf("Variance for all Tests\n");
	}
	printf("\n");

	print_summary();

	return 0;
}
