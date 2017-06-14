/* $Header: /local/develop/vsOrb/src/utils/reports/vsorbrpt.c,v 1.3 1997/11/28 16:25:33 ian Exp $
*
* (C) Copyright 1997 X/Open Company Limited, a member of the Open Group
*
* All rights reserved.  No part of this source code may be reproduced,
* stored in a retrieval system, or transmitted, in any form or by any
* means, electronic, mechanical, photocopying, recording or otherwise,
* except as stated in the end-user licence agreement, without the prior
* permission of the copyright owners.
*
* Motif, OSF/1, and UNIX are registered trademarks and the IT DialTone,
* The Open Group, and the "X Device" are trademarks of The Open Group.
*
* Project: VSORB Report Generator
*
* File:	vsorbrpt.c
*
* Description:
*	Report Generator
*
* Modifications:
* $Log: vsorbrpt.c,v $
* Revision 1.3  1997/11/28 16:25:33  ian
* Fixed no time stamp for WIN32.
*
* Revision 1.2  1997/11/02 15:08:00  ian
* Minor bugfixes -AS.
*
*/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>
#ifndef _WIN32
#include <unistd.h>
#include <dirent.h>
#else
#include <io.h>
/* some useful macros */
#define S_ISDIR(x) !(S_IFREG & (x))
#define popen(a, b) _popen((a), (b))
#define R_OK 0
#endif

static void parseinidle(void);

int  VSORBgetopt (int argc, char * const argv [], char *defined_options_p);

#define OPTIONS	":f:d:j:s:t:x:hcuvw"

/* detail of additional information reporting*/
int	detail, scope;

char	verbuf[81];
char	test_dir[16];
char	test_name[16];
char	doc_dir[16];
int	print_config_vars=0;
int	xopen_report = 0;

char	localinfo[81];

/*input file name*/
char 	*infile;
/*where its stored if not user provided*/
char	inbuf[512];

/*a buffer to play with*/
char	tmpbuf[1024];

/*stream for the journal file*/
FILE	*jfile;

/*goodies for the summary report*/
/*time test run started (from TCC start)*/
char	starttime[128];
/*time test run ended (from TCC end)*/
char	endtime[128];
/*test run user (from TCC start)*/
char	username[128];
/*test command line (from TCC start)*/
char	commandline[128];
/*number of build errors*/
int	nbuilderr = 0;
/*number of TCC errors*/
int	ntccerr = 0;
/*number of TCM errors*/
int	ntcmerr = 0;
/*number of NORESULT errors*/
int	nnoresult = 0;

/*report detail on only a specific case?*/
int	freportcase = 0;
/*case to report on*/
char *reportcase;
/*report detail on this case?*/
int	freportthiscase = 0;
/*found the case wanted?*/
int	ffoundcase = 0;
#define NSECTS	1024

/*info about the sections*/
struct sect {
	int	expectedareas;
	int	expectedtests;
	int	expecteduntested;
	int	expectednotinuse;
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
	int	nfip;
	char	name[64];
} sects[NSECTS];

/*index of this section*/
int	thissect = 0;
/*number of sections run*/
int	nsects;

/*name of the current area*/
char areaname[128];
/*name of the current section*/
char sectionname[128] = "Discrete Tests";
/*number of tests in the current area*/
int	expectedtests;
int	actualtests = 0;
/*current filename being built/execed/cleaned*/
char	currentfile[256];

/*message type*/
int	mtype;
/*messages read in here*/
char	linebuf[2048];
/*pointer into where we are in the message*/
char	*pline;

/*state of the enterprise*/
int	state;
/*the states */
#define S_START		0
#define S_CONFIG	1
#define	S_IDLE		2
#define	S_BUILD		3
#define	S_EXEC		4
#define	S_CLEAN		5

/*Whether we have printed the header that starts the detial part of the report*/
int	fDetailHeaderPrinted;
/*Whether we have printed the header that starts the current section*/
int	fSectionNamePrinted;
/*Whether we have printed the header that starts the current area*/
int	fAreaNamePrinted;

/*activity counter*/
int	activity = -1;
/*number of contexts active*/
int	ncontexts;
#define NUM_CONTEXTS 16
/*contexts*/
long	contexts[NUM_CONTEXTS];

#define SBLINESTORE 8192
#define SDLINESTORE 8192

/*number of lines in the store for test output*/
int storecnt, storemax = 0;
/*stored line pointers for test output*/
char **store = NULL;
/*number of lines in the store for build output*/
int bstorecnt;
/*stored line pointers for build output*/
char *bstore[SBLINESTORE];

/*test return values*/
#define R_PASS		0
#define R_FAIL		1
#define R_UNSUPPORTED	2
#define R_UNINITIATED	3
#define R_UNRESOLVED	4
#define R_UNTESTED	5
#define R_NORESULT	6
#define R_NOTINUSE	7
#define R_FIP		102

int	 lineno;

int shour;

/*pointer into where we are in the message*/
char	*pline2, *pline3;


static void whatj(void)
{
#ifndef _WIN32
	DIR	*dirp;
	struct	 dirent	*dp;
#else
	struct _finddata_t jdir;    
	long hFile;
#endif

	struct	 stat	sbuf;
	char	ttmp[5];
	struct tm mytm;
	time_t	mytime;
	char	starttime[64];
	char	strbuf[64];
	FILE 	*fsort;


	/*find the results directory*/
	if (getenv("TET_ROOT") == NULL) {
		fprintf(stderr, "$TET_ROOT is not set\n");
		exit(2);
	}
	sprintf(tmpbuf, "%s/%s/results", getenv("TET_ROOT"), test_dir);

#ifndef _WIN32
	if ((dirp = opendir(tmpbuf)) == NULL) {
		perror("cannot open results directory");
		exit(2);
	}
#endif

	if ((fsort = popen("sort", "w")) == NULL) {
		perror("Cannot open pipe to sort");
		exit(2);
	}

	printf("                           %s JOURNAL SUMMARY\n\n", test_name);

#ifndef _WIN32
	/*skip . and ..*/
	dp = readdir(dirp);
	dp = readdir(dirp);
#else
	strcat(tmpbuf, "/*");
	hFile = _findfirst(tmpbuf, &jdir);
	_findnext(hFile, &jdir);
	_findnext(hFile, &jdir);
#endif

#ifndef _WIN32
	while ((dp = readdir(dirp)) != NULL) {
			sprintf(tmpbuf, "%s/%s/results/%s", getenv("TET_ROOT"), test_dir, dp->d_name);
#else
	while (_findnext(hFile, &jdir) == 0) {
			sprintf(tmpbuf, "%s/%s/results/%s", getenv("TET_ROOT"), test_dir, jdir.name);
#endif

#ifdef CDEBUG
fprintf(stderr, "dir: %s\n", tmpbuf);
#endif
		if (stat(tmpbuf, &sbuf) != 0) {
			perror("whatj: no results sub-directories");
			exit(2);
		}

		/*ignore regular files that might sneak in*/
		if (S_ISDIR(sbuf.st_mode) == 0)
			continue;

		strcat(tmpbuf, "/journal");

		/*open the journal file for read*/
		jfile = fopen(tmpbuf, "r");
		if (jfile == NULL) {
			perror("Cannot open journal file");
			exit(2);
		}

		fgets(linebuf, sizeof(linebuf), jfile);
#ifdef CDEBUG
fprintf(stderr, "%s", linebuf);
fflush(stderr);
#endif
		/*get the message type number*/
		pline = strtok(&linebuf[0], "|");

		if (atoi(pline) == 0) {
			/*skip tet version*/
			pline = strtok(NULL, " ");
			/*get start time*/
			pline = strtok(NULL, "|");
			strcpy(starttime, pline);
			/*skip "User:*/
			pline = strtok(NULL, " ");
			/*get user*/
			pline3 = strtok(NULL, " ");
			/*get command line*/
			pline = strtok(NULL, ":");
			pline2 = strtok(NULL, "\n");
			pline = strtok(&starttime[0], ":");
			shour=mytm.tm_hour=atoi(pline);
			pline = strtok(NULL, ":");
			mytm.tm_min=atoi(pline);
			pline = strtok(NULL, " ");
			mytm.tm_sec=atoi(pline);
			pline = strtok(NULL, "\0");
			strncpy(ttmp, pline, 4);	
			ttmp[4]= 0;
			mytm.tm_year=atoi(ttmp)-1900;
			strncpy(ttmp, pline+4, 2);	
			ttmp[2]= 0;
			mytm.tm_mon=atoi(ttmp)-1;
			strncpy(ttmp, pline+6, 2);	
			mytm.tm_mday=atoi(ttmp);
			mytm.tm_isdst=-1;
			mytime = mktime(&mytm);
			strftime(strbuf, sizeof(strbuf), "%a %b %d %r", &mytm);
#ifndef _WIN32
			fprintf(fsort, "%-8s  %s   %-6s   %s\n", dp->d_name, strbuf, pline3, pline2);
#else
			fprintf(fsort, "%-8s  %s   %-6s   %s\n", jdir.name, strbuf, pline3, pline2);
#endif
		}
	fclose(jfile);

	}
#ifndef _WIN32
	pclose(fsort);
#else
	_findclose(hFile);
#endif
}

/*save a line for possible printing later*/
static void store_line(char *line, int indent)
{
	char * spaceptr;
	int i;

	if (storecnt == storemax) {
/*
		fprintf(stderr, "Realloc at %d lines\n", storemax);
*/
		store = realloc(store, (storemax * sizeof(char *)) + (4096 *sizeof(char *)));
		if (store == NULL) {
			fprintf(stderr, "Line store overflow, journal line %d, line %d for this test\n", lineno, storemax+1);
			exit(2);
		}
		storemax += 4096;
	}

	if (strlen(line) == 0)
		return;

	store[storecnt] = (char *)malloc(strlen(line)+1+(2*indent));
	if (store[storecnt] == NULL) {
		perror("Cannot malloc buffer space for line");
		exit(2);
	}
	spaceptr = store[storecnt];
	for (i = 0; i < indent; i++) {
		*spaceptr = ' ';
		spaceptr++;
	}
	strcpy(spaceptr, line);
#ifdef CDEBUG
printf("** Stored #%d (%d): %s\n", storecnt, strlen(line), line);
#endif

	storecnt++;
}

/*print the assertion part of the saved lines*/
static void print_assert(void)
{
int	i;

	if (storecnt == 0)
		return;
	for(i = 0; i < storecnt; i++)
		if ((strstr(store[i], "PREP") == 0) && (strstr(store[i], "CLEANUP") == 0) && (strstr(store[i], "TEST") == 0) && (strstr(store[i], "INFO") == 0)  && (strstr(store[i], "ERROR") == 0) && (strstr(store[i], "WARNING") == 0) && (strstr(store[i], "TRACE") == 0)) 
			printf("%s", store[i]);
		else
			return;
}

/*print the saved lines*/
static void print_store(void)
{
int	i;

	if (storecnt == 0)
		return;
	for(i = 0; i < storecnt; i++) {
		printf("%s", store[i]);
#ifdef CDEBUG
printf("** Printed #%d (%d): %s\n", i, strlen(store[i]), store[i]);
#endif
	}
}

/*discard the saved lines*/
static void purge_store(void)
{
int	i;
#ifdef CDEBUG
printf("** Purge %d\n", storecnt);
#endif

	if (storecnt == 0)
		return;

	for(i = 0; i < storecnt; i++)
		free(store[i]);

	storecnt = 0;

}

/*save a build line for possible printing later*/
static void store_bline(char *line)
{
	if (bstorecnt == SBLINESTORE) {
		fprintf(stderr, "Bline store overflow, line %d\n", lineno);
		exit(2);
	}

	if (strlen(line) == 0)
		return;

	bstore[bstorecnt] = (char *)malloc(strlen(line)+1);
	if (bstore[bstorecnt] ==  NULL) {
		perror("Cannot malloc buffer space for bline");
		exit(2);
	}
	strcpy(bstore[bstorecnt], line);
#ifdef CDEBUG
printf("** Stored #%d (%d): %s\n", bstorecnt, strlen(line), line);
#endif

	bstorecnt++;
}

/*print the saved build lines*/
static void print_bstore(int range)
{
int	i;

	if (bstorecnt == 0)
		return;
	if (range == -1)
		range = bstorecnt;
	if (range > bstorecnt)
		range = bstorecnt;
	for(i = 0; i < range; i++) {
		printf("%s", bstore[i]);
#ifdef CDEBUG
printf("** Printed #%d (%d): %s\n", i, strlen(bstore[i]), bstore[i]);
#endif
	}
	if (range < bstorecnt)
		printf("Compiler output truncated at %d lines, %d lines produced\n", range, bstorecnt);
}

/*discard the saved build lines*/
static void purge_bstore(void)
{
int	i;
#ifdef CDEBUG
printf("** Purge %d\n", bstorecnt);
#endif

	if (bstorecnt == 0)
		return;

	for(i = 0; i < bstorecnt; i++)
		free(bstore[i]);

	bstorecnt = 0;

}

/* print all detailed report header info not printed yet*/
static void print_header(void)
{
	if (fDetailHeaderPrinted == 0) {
		if ((xopen_report != 2) && (xopen_report != 3)) {
		printf("                           %s DETAILED RESULTS REPORT\n\n", test_name);
		fDetailHeaderPrinted = 1;
		}
	}

	if (fSectionNamePrinted == 0) {
		printf("\nSECTION: %s\n", sectionname);
		fSectionNamePrinted = 1;
	}

	if (fAreaNamePrinted == 0) {
		printf("\nTEST CASE: %s\n", areaname);
		fAreaNamePrinted = 1;
	}
}

/* check current activity against expected*/
static void check_activity(int current)
{
	(void *)&current;
/*

	if (current != activity) {
		print_header();
		fprintf(stderr, "Activity number is %d, expected %d, line %d\n", current, activity, lineno);
		exit(2);
	}
*/
}

/*parse messages in the start state*/
/*handles message TCC Start (type 0) and machine info (type 5)*/
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
		break;
	/*Machine info*/
	case 5:
		/*get machineinfo*/
		pline = strtok(NULL, "|");
		strcpy(localinfo, pline);
		state = S_IDLE;
		break;
	/*Build start*/
	case 20:
		if (print_config_vars == 1) {
			pline = strtok(NULL, " ");
			printf("\n===> %s\n", pline);
		}
		state=S_CONFIG;
		break;
	/*TCM message*/
	case 510:
		if (detail != 2) {
			print_header();
			printf("\nTCM Error\n");
			pline = strtok(NULL, "|");
			pline = strtok(NULL, "\n");
			printf("%s\n", pline);
		}
		ntcmerr++;
		break;
	/*operator abort*/
	case 90:
		print_header();
		printf("\nOperator abort\n");
		state=S_IDLE;
		break;
	/*TCC message*/
	case 50:
		if (detail != 2) {
			print_header();
			printf("\nTCC Error\n");
			printf("%s", &linebuf[4]);
		}
		ntccerr++;
		break;
	default:

		fprintf(stderr, "Illegal record (type = %d) in START state, line %d\n", mtype, lineno);
		exit(2);
	}
}

/*parse messages in the config state*/
/*handles message types 30, 40*/
static void parseinconfig(void)
{
	switch (mtype) {

	/*config line*/
	case 30:
		if (print_config_vars == 1) {
			pline = strtok(NULL, "|");
			printf("%s", pline);
		}
		break;
	/*config end*/
	case 40:
		state = S_IDLE;
		break;
	/*Scenario message*/
	case 70:
		state = S_IDLE;
		parseinidle();
		break;
	/*operator abort*/
	case 90:
		print_header();
		printf("\nOperator abort\n");
		state=S_IDLE;
		break;
	/*TCC message*/
	case 50:
		if (detail != 2) {
			print_header();
			printf("\nTCC Error\n");
			printf("%s", &linebuf[4]);
		}
		ntccerr++;
		break;
	default:
		fprintf(stderr, "Illegal record (type = %d) in CONFIG state, line %d\n", mtype, lineno);
		exit(2);
	}
}

/*parse messages in the idle state between tests*/
/*handles message types 70, 10, 20, 110, 300, 900)*/
static void parseinidle(void)
{
	int	nslash = 0;

	switch (mtype) {
	/*TC Start*/
	case 10:
		if (print_config_vars == 1)
			exit(0);
		pline = strtok(NULL, " ");
		activity++;
		check_activity(atoi(pline));
		pline = strtok(NULL, " ");
		strcpy(currentfile, pline);
		if ((strlen(pline) > 5) && (strncmp(pline, "/tset", 5) == 0)) {
			pline = pline+5;
		}
		pline2 = pline+1;
		while (*pline2 != 0)
			if (*pline2++ == '/')
				nslash++;

		if (nslash == 0) {
			strcpy(tmpbuf, "/");
			strcpy(areaname, pline+1);
		}
		if (nslash == 1) {
			pline2 = pline+1;
			while (*pline2 != '/')
				pline2++;
			*pline2 = 0;
			strcpy(tmpbuf, pline+1);
			strcpy(areaname, pline2+1);
		}
		if (nslash >= 2) {
			strcpy(tmpbuf, pline+1);
			pline2 = tmpbuf;
			while (*pline2 != '/')
				pline2++;
			pline2++;
			while (*pline2 != '/')
				pline2++;
			*pline2 = 0;
			strcpy(areaname, pline2+1);
		}

		if (strcmp(tmpbuf, sectionname) != 0) {
			nsects++;
			if (nsects == NSECTS) {
				printf("Can only handle %d sections, increase NSECTS and rebuild\n", NSECTS);
				exit(2);
			}
			thissect = nsects;
			strcpy(sectionname, tmpbuf);
			strcpy(sects[nsects].name, sectionname);
			fSectionNamePrinted = 0;
		}
		sects[thissect].actualareas++;
		if (freportcase == 1) {
			if (strcmp(areaname, reportcase) == 0) {
				freportthiscase = 1;
				ffoundcase = 1;
			}
			else
				freportthiscase = 0;
		}
		else
			freportthiscase = 1;
		fAreaNamePrinted = 0;
		actualtests = 0;
		state=S_EXEC;
		break;
	/*Config start*/
	case 20:
		if (print_config_vars == 1) {
			pline = strtok(NULL, " ");
			printf("\n===> %s\n", pline);
		}
		state=S_CONFIG;
		break;
	/*Build start*/
	case 110:
		pline = strtok(NULL, " ");
		activity++;
		check_activity(atoi(pline));
		pline = strtok(NULL, " ");
		strcpy(currentfile, pline);
		state=S_BUILD;
		purge_bstore();
		if (freportcase == 1) {
			if (strstr(currentfile, reportcase) != NULL) {
				freportthiscase = 1;
				ffoundcase = 1;
			}
			else
				freportthiscase = 0;
		}
		else
			freportthiscase = 1;
		if ((strlen(pline) > 5) && (strncmp(pline, "/tset", 5) == 0)) {
			pline = pline+5;
		}
		pline2 = pline+1;
		while (*pline2 != 0)
			if (*pline2++ == '/')
				nslash++;

		if (nslash == 0) {
			strcpy(tmpbuf, "/");
			strcpy(areaname, pline+1);
		}
		if (nslash == 1) {
			pline2 = pline+1;
			while (*pline2 != '/')
				pline2++;
			*pline2 = 0;
			strcpy(tmpbuf, pline+1);
			strcpy(areaname, pline2+1);
		}
		if (nslash >= 2) {
			strcpy(tmpbuf, pline+1);
			pline2 = tmpbuf;
			while (*pline2 != '/')
				pline2++;
			pline2++;
			while (*pline2 != '/')
				pline2++;
			*pline2 = 0;
			strcpy(areaname, pline2+1);
		}
		if (strcmp(tmpbuf, sectionname) != 0) {
			nsects++;
			if (nsects == NSECTS) {
				printf("Can only handle %d sections, increase NSECTS and rebuild\n", NSECTS);
				exit(2);
			}
			thissect = nsects;
			strcpy(sectionname, tmpbuf);
			strcpy(sects[nsects].name, sectionname);
			fSectionNamePrinted = 0;
		}
		sects[thissect].actualareas++;
		fAreaNamePrinted = 0;
		break;
	/*Clean start*/
	case 300:
		state=S_CLEAN;
		pline = strtok(NULL, " ");
		activity++;
		check_activity(atoi(pline));
		break;
	/*TCC End*/
	case 900:
		state=S_START;
		/*get end time*/
		pline = strtok(NULL, "|");
		strcpy(endtime, pline);
		break;
	/*operator abort*/
	case 90:
		state=S_IDLE;
		print_header();
		printf("\nOperator abort\n");
		break;
	/*TCM message*/
	case 510:
		if (detail != 2) {
			print_header();
			printf("\nTCM Error\n");
			pline = strtok(NULL, "|");
			pline = strtok(NULL, "\n");
			printf("%s\n", pline);
		}
		ntcmerr++;
		break;
	/*TCC message*/
	case 50:
		if (detail != 2) {
			print_header();
			printf("\nTCC Error\n");
			printf("%s", &linebuf[4]);
		}
		ntccerr++;
		break;
	case 830:
	case 840:
	case 70:
		break;
	default:
		fprintf(stderr, "Illegal record (type = %d) in IDLE state, line %d\n", mtype, lineno);
		exit(2);
	}
}

/*parse messages in the build state*/
static void parseinbuild(void)
{
	int	buildret;

	switch (mtype) {
	/*captured*/
	case 100:
		pline = strtok(NULL, "|");
		check_activity(atoi(pline));
		pline = strtok(NULL, "\0");
		store_bline(pline);
		break;
	/*build end*/
	case 130:
		pline = strtok(NULL, " ");
		check_activity(atoi(pline));
		pline = strtok(NULL, " ");
		buildret = atoi(pline);
		if (buildret != 0) {
			nbuilderr++;
			if ((xopen_report != 2) && (detail < 2) && (scope < 3) && (freportthiscase == 1)) {
				print_header();
				printf("\nBuild tool error %d on build of file %s\n", buildret, currentfile);
				if (detail == 0)
					print_bstore(-1);
				if (detail == 1)
					print_bstore(10);
			}
			purge_bstore();
		}
		state=S_IDLE;
		break;
	/*operator abort*/
	case 90:
		if ((detail < 2) && (scope < 3) && (freportthiscase == 1)) {
			print_header();
			printf("\nOperator aborted build of file %s\n", currentfile);
		}
		purge_store();
		break;
	/*TCC message*/
	case 50:
		if ((detail < 2) && (scope < 3) && (freportthiscase == 1)) {
			print_header();
			printf("\nTCC Error during build of file %s\n", currentfile);
			printf("%s", &linebuf[4]);
		}
		purge_store();
		ntccerr++;
		break;
	/*TCM message*/
	case 510:
		if ((detail < 2) && (scope < 3) && (freportthiscase == 1)) {
			print_header();
			printf("\nTCM Error during build of file %s\n", currentfile);
			pline = strtok(NULL, "|");
			pline = strtok(NULL, "\n");
			printf("%s\n", pline);
		}
		purge_store();
		ntcmerr++;
		break;
	default:
		fprintf(stderr, "Illegal record (type = %d) in BUILD state, line %d\n", mtype, lineno);
		pline = strtok(NULL, "\n");
		fprintf(stderr, "%s\n", pline);
		exit(2);
	}
}

/*parse messages in the clean state*/
static void parseinclean(void)
{
	switch (mtype) {
	/*captured*/
	case 100:
		pline = strtok(NULL, "|");
		check_activity(atoi(pline));
		break;
	/*clean end*/
	case 320:
		state=S_IDLE;
		break;
	/*operator abort*/
	case 90:
		if ((detail < 2) && (scope < 2) && (freportthiscase == 1)) {
			print_header();
			printf("\nOperator aborted clean of file %s\n", currentfile);
		}
		break;
	/*TCC message*/
	case 50:
		if ((detail < 2) && (scope < 2) && (freportthiscase == 1)) {
			print_header();
			printf("\nTCC Error during clean of file %s\n", currentfile);
			printf("%s", &linebuf[4]);
		}
		ntccerr++;
		break;
	/*TCM message*/
	case 510:
		if ((detail < 2) && (scope < 3) && (freportthiscase == 1)) {
			print_header();
			printf("\nTCM Error during clean of file %s\n", currentfile);
			pline = strtok(NULL, "|");
			pline = strtok(NULL, "\n");
			printf("%s\n", pline);
		}
		ntcmerr++;
		break;
	default:
		fprintf(stderr, "Illegal record (type = %d) in CLEAN state, line %d\n", mtype, lineno);
		exit(2);
	}
}

/*parse messages in the exec state*/
static void parseinexec(void)
{
	int	thisresult=0;
	int	i, finscope;
	long	thiscontext;

	switch (mtype) {
	/*TCM start*/
	case 15:
		pline = strtok(NULL, " ");
		check_activity(atoi(pline));
		break;
	/*TC end*/
	case 80:
		pline = strtok(NULL, " ");
		check_activity(atoi(pline));
		state=S_IDLE;
		break;
	/*IC start*/
	case 400:
		pline = strtok(NULL, " ");
		check_activity(atoi(pline));
		break;
	/*IC end*/
	case 410:
		pline = strtok(NULL, " ");
		check_activity(atoi(pline));
		break;
	/*TP start*/
	case 200:
		pline = strtok(NULL, " ");
		check_activity(atoi(pline));
		sects[thissect].actualtests++;
		actualtests++;
		ncontexts = 0;
		break;
	/*TP result*/
	case 220:
		pline = strtok(NULL, " ");
		check_activity(atoi(pline));
		pline = strtok(NULL, "|");
		pline = strtok(NULL, "\n");
		if (strcmp(pline, "PASS") == 0) {
			if (bstorecnt == 0)  {
				thisresult = R_PASS;
				sects[thissect].npass++;
			}
			else {
				thisresult = R_FIP;
				sects[thissect].nfip++;
				strcpy(pline, "FIP");
			}
		} else
		if (strcmp(pline, "FAIL") == 0) {
			thisresult = R_FAIL;
			sects[thissect].nfail++;
		} else
		if (strcmp(pline, "UNRESOLVED") == 0) {
			thisresult = R_UNRESOLVED;
			sects[thissect].nunresolved++;
		} else
		if (strcmp(pline, "UNINITIATED") == 0) {
			thisresult = R_UNINITIATED;
			sects[thissect].nuninitiated++;
		} else
		if (strcmp(pline, "UNSUPPORTED") == 0) {
			thisresult = R_UNSUPPORTED;
			sects[thissect].nunsupported++;
		} else
		if (strcmp(pline, "UNTESTED") == 0) {
			thisresult = R_UNTESTED;
			sects[thissect].nuntested++;
		} else
		if (strcmp(pline, "NOTINUSE") == 0) {
			thisresult = R_NOTINUSE;
			sects[thissect].nnotinuse++;
		} else
		if (strcmp(pline, "NORESULT") == 0) {
			thisresult = R_NORESULT;
			sects[thissect].nnoresult++;
			nnoresult++;
		} else
		if (strcmp(pline, "FIP") == 0) {
			thisresult = R_FIP;
			sects[thissect].nfip++;
		}

		finscope = 0;
		if (freportthiscase != 0) {
		switch (scope) {

		/*scope: all results*/
		case 0:
			finscope = 1;
			break;

		/*scope: errors, FIPs*/
		case 1:
			if ((thisresult == R_FAIL) || (thisresult == R_UNRESOLVED) || (thisresult == R_UNINITIATED) || (thisresult == R_NORESULT) || (thisresult == R_FIP))
				finscope = 1;
			break;

		/*scope: UNINITIATED*/
		case 2:
			if (thisresult == R_UNINITIATED)
				finscope = 1;
			break;


		/*scope: UNSUPPORTED*/
		case 3:
			if (thisresult == R_UNSUPPORTED)
				finscope = 1;
			break;

		/*scope: FIP*/
		case 4:
			if (thisresult == R_FIP)
				finscope = 1;
			break;


		/*scope: NOTINUSE*/
		case 5:
			if ((thisresult == R_NOTINUSE))
				finscope = 1;
			break;

		/*scope: UNTESTED*/
		case 6:
			if ((thisresult == R_UNTESTED))
				finscope = 1;
			break;

		/*scope: NORESULT*/
		case 7:
			if ((thisresult == R_NORESULT))
				finscope = 1;
			break;
		}


		if (finscope) {

			switch (detail) {

			case 0:
				/*detail: everything*/
				if (xopen_report == 2) {
					if (thisresult == R_FIP) {
						print_header();
						printf("\nTEST PURPOSE #%d\n", actualtests);
						print_store();
						if (bstorecnt != 0) {
							printf("FIP: Compiler messages were produced\n");
							printf("     Manual analysis is required\n");
							print_bstore(-1);
						}
						printf("%d %s\n", actualtests, pline);
						printf("\n%s", "Test Centre sign-off _____________________________\n");
					}
					break;
				}
				if ((xopen_report == 3) && (thisresult == R_FIP))
					break;
				print_header();
				printf("\nTEST PURPOSE #%d\n", actualtests);
				print_store();
				if ((thisresult == R_FIP) && (bstorecnt != 0)) {
					printf("FIP: The following compiler messages were produced\n");
					printf("     Manual analysis is required\n");
					print_bstore(-1);
				}
				printf("%d %s\n", actualtests, pline);
				break;
			case 1:
				/*detail: everything but compiler detail*/
				if (xopen_report == 2) {
					if (thisresult == R_FIP) {
						print_header();
						printf("\nTEST PURPOSE #%d\n", actualtests);
						print_store();
						if (bstorecnt != 0) {
							printf("FIP: Compiler messages were produced\n");
							printf("     Manual analysis is required\n");
							print_bstore(10);
						}
						printf("%d %s\n", actualtests, pline);
						printf("\n%s", "Test Centre sign-off _____________________________\n");
					}
					break;
				}
				if ((xopen_report == 3) && (thisresult == R_FIP))
					break;
				print_header();
				printf("\nTEST PURPOSE #%d\n", actualtests);
				print_store();
				if ((thisresult == R_FIP) && (bstorecnt != 0)) {
					printf("FIP: Compiler messages were produced\n");
					printf("     Manual analysis is required\n");
					print_bstore(10);
				}
				printf("%d %s\n", actualtests, pline);
				break;

			case 3:
				/*detail: results*/
				print_header();
				printf("%d %s\n", actualtests, pline);
				break;

			case 4:
				/*detail: results and assertions*/
				print_header();
				printf("\nTEST PURPOSE #%d\n", actualtests);
				print_assert();
				printf("%d %s\n", actualtests, pline);
				break;
			}
		}
		}
		purge_store();
		break;
	case 520:
		pline = strtok(NULL, " ");
		pline = strtok(NULL, " ");
		pline = strtok(NULL, " ");
		thiscontext = atol(pline);
		if (thiscontext != contexts[ncontexts-1]) {
			contexts[ncontexts++] = thiscontext;
			for (i = 0; i < ncontexts; i++) 
				if (thiscontext == contexts[i])
					ncontexts = i+1;
			if (ncontexts > NUM_CONTEXTS) {
				fprintf(stderr, "FATAL ERROR: Too many contexts, line: %d\n", lineno);
				exit(1);
			}
		}
		pline = strtok(NULL, "|");
		pline = strtok(NULL, "\0");
		if (ncontexts > 2)
			store_line(pline, (ncontexts-2)*2);
		else
			store_line(pline, 0);
		break;
	/*operator abort*/
	case 90:
		if ((detail != 2) && (scope < 2) && (freportthiscase == 1)) {
			print_header();
			print_assert();
			printf("\nOperator aborted exec of file %s\n", currentfile);
		}
		purge_store();
		break;
	/*captured*/
	case 100:
		pline = strtok(NULL, "|");
		check_activity(atoi(pline));
		break;
	/*TCM message*/
	case 510:
		if ((detail < 2) && (freportthiscase == 1)) {
			print_header();
			print_store();
			printf("\nTCM Error during exec of file %s\n", currentfile);
			pline = strtok(NULL, "|");
			pline = strtok(NULL, "\n");
			printf("%s\n", pline);
		}
		purge_store();
		ntcmerr++;
		break;
	/*TCC message*/
	case 50:
		if ((detail != 2) && (scope < 2) && (freportthiscase == 1)) {
			print_header();
			printf("\nTCC Error during exec of file %s\n", currentfile);
			printf("%s", &linebuf[4]);
		}
		ntccerr++;
		purge_store();
		break;
	default:
		fprintf(stderr, "Illegal record (type = %d) in EXEC state, line %d\n", mtype, lineno);
		exit(2);
	}
}

/*read and parse a journal file*/
static void parse_file(void)
{
	/*open the journal file for read*/
	jfile = fopen(infile, "r");
	if (jfile == NULL) {
		perror("Cannot open journal file");
		exit(2);
	}

	state = S_START;

	/*parse lines one by one*/
	while (fgets(linebuf, sizeof(linebuf), jfile) != NULL) {
		lineno++;
		if (strlen(linebuf) < 2)
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
			fprintf(stderr, "Illegal state: %d, line %d\n", state, lineno);
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

/*print the summary report*/
static void print_summary(void)
{

	int	i;
	char	ttmp[5];
	struct tm mytm;
	time_t	mytime;
	char	strbuf[64];

	if (xopen_report == 0) {
		printf("\f");
		printf("                           %s SUMMARY RESULTS REPORT\n\n", test_name);
		printf("Test suite version: %s", verbuf);
		printf("Test run by: %s\n", username);
	
		printf("System: %s\n", localinfo);
	}

	pline = strtok(&starttime[0], ":");
	if (pline == NULL) {
		fprintf(stderr, "Null pline in start time\n");
		exit(2);
	}
	mytm.tm_hour=atoi(pline);
	if (mytm.tm_hour < shour)
		mytm.tm_mday++;
	pline = strtok(NULL, ":");
	if (pline == NULL) {
		fprintf(stderr, "Null pline in start time\n");
		exit(2);
	}
	mytm.tm_min=atoi(pline);
	pline = strtok(NULL, " ");
	if (pline == NULL) {
		fprintf(stderr, "Null pline in start time\n");
		exit(2);
	}
	mytm.tm_sec=atoi(pline);
	pline = strtok(NULL, "\0");
	if (pline == NULL) {
		fprintf(stderr, "Null pline in start time\n");
		exit(2);
	}
	strncpy(ttmp, pline, 4);	
	ttmp[4]= 0;
	mytm.tm_year=atoi(ttmp)-1900;
	strncpy(ttmp, pline+4, 2);	
	ttmp[2]= 0;
	mytm.tm_mon=atoi(ttmp)-1;
	strncpy(ttmp, pline+6, 2);	
	mytm.tm_mday=atoi(ttmp);
	mytm.tm_isdst=-1;
	mytime = mktime(&mytm);
	
#ifndef _WIN32
	strftime(strbuf, sizeof(strbuf), "%A %B %d, %Y %r", &mytm);
#else
	strftime(strbuf, sizeof(strbuf), "%A %B %d, %Y %I:%M:%S %p", &mytm);
#endif

	printf("Test run started: %s\n", strbuf);
	if (strlen(endtime) != 0) {
		pline = strtok(&endtime[0], ":");
		if (pline == NULL) {
			fprintf(stderr, "Null pline in %s\n", linebuf);
			exit(2);
		}
		mytm.tm_hour=atoi(pline);
		pline = strtok(NULL, ":");
		if (pline == NULL) {
			fprintf(stderr, "Null pline in %s\n", linebuf);
			exit(2);
		}
		mytm.tm_min=atoi(pline);
		pline = strtok(NULL, "\0");
		if (pline == NULL) {
			fprintf(stderr, "Null pline in %s\n", linebuf);
			exit(2);
		}
		mytm.tm_sec=atoi(pline);
		mytime = mktime(&mytm);
	
#ifndef _WIN32
	strftime(strbuf, sizeof(strbuf), "%A %B %d, %Y %r", &mytm);
#else
	strftime(strbuf, sizeof(strbuf), "%A %B %d, %Y %I:%M:%S %p", &mytm);
#endif

		printf("Test run ended:   %s\n", strbuf);
	}
	else
		printf("Test run not completed\n");


	printf("Journal file: %s\n", infile);
	printf("TCC command line:%s\n", commandline);
	if (xopen_report == 0) {
	if (freportcase == 1)
		printf("Report type: -d %d -s %d -t %s\n", detail, scope, reportcase);
	else
		printf("Report type: -d %d -s %d \n", detail, scope);
	}
	printf("\n");

	print_ecount(nbuilderr, "build");
	print_ecount(ntccerr, "TCC");
	print_ecount(ntcmerr, "TCM");
	print_ecount(nnoresult, "NORESULT");
	printf("\n");


	if ((freportcase == 1) && (ffoundcase == 0)) {
		printf("Test Case %s not found in %s\n", reportcase, infile);
		exit(0);
	}
	printf("SECTION                   CASES TESTS  PASS UNSUP UNTST NOTIU   FIP  FAIL UNRES \n\n");

	sects[0].actualareas = 0;
	sects[0].actualtests = 0;
	sects[0].npass = 0;
	sects[0].nunsupported = 0;
	sects[0].nuntested = 0;
	sects[0].nnotinuse = 0;
	sects[0].nfip = 0;
	sects[0].nfail = 0;
	sects[0].nunresolved = 0;
	sects[0].nuninitiated = 0;

	for (i = 1; i<= thissect; i++) {
		printf("%-25.25s", sects[i].name);

		printf(" %5d",  sects[i].actualareas);
		sects[0].actualareas += sects[i].actualareas;
		if (sects[i].actualareas != sects[i].expectedareas) {
			sprintf(tmpbuf, "ERROR: %s section has %d Test Cases, should have %d.\n", sects[i].name, sects[i].actualareas, sects[i].expectedareas);
			store_line(tmpbuf, 0);
		}

		printf(" %5d",  sects[i].actualtests);
		sects[0].actualtests += sects[i].actualtests;
		if (sects[i].actualtests != sects[i].expectedtests) {
			sprintf(tmpbuf, "ERROR: %s section has %d Test Purposes, should have %d.\n", sects[i].name, sects[i].actualtests, sects[i].expectedtests);
			store_line(tmpbuf, 0);
		}

		printf(" %5d",  sects[i].npass);
		sects[0].npass += sects[i].npass;
		printf(" %5d",  sects[i].nunsupported);
		sects[0].nunsupported += sects[i].nunsupported;
		printf(" %5d",  sects[i].nuntested);
		sects[0].nuntested += sects[i].nuntested;
		printf(" %5d",  sects[i].nnotinuse);
		sects[0].nnotinuse += sects[i].nnotinuse;
		printf(" %5d",  sects[i].nfip);
		sects[0].nfip += sects[i].nfip;
		printf(" %5d",  sects[i].nfail);
		sects[0].nfail += sects[i].nfail;
		printf(" %5d",  sects[i].nunresolved);
		sects[0].nunresolved += sects[i].nunresolved;
		printf("\n");
	}

	printf("\n");
	printf("%-25s", "TOTAL");
	printf(" %5d",  sects[0].actualareas);
	printf(" %5d",  sects[0].actualtests);
	printf(" %5d",  sects[0].npass);
	printf(" %5d",  sects[0].nunsupported);
	printf(" %5d",  sects[0].nuntested);
	printf(" %5d",  sects[0].nnotinuse);
	printf(" %5d",  sects[0].nfip);
	printf(" %5d",  sects[0].nfail);
	printf(" %5d",  sects[0].nunresolved);
	printf("\n");

}

extern int VSORBoptind, VSORBoptopt;
extern char *VSORBoptarg;

/*keep ANSI happy*/
void main(char, char * const []);

void main(char argc, char * const argv[])
{
 	int	errflag = 0;
 	int	fcount = 0;
 	int	fuser = 0;
	int	jfileno = 0;
#ifndef _WIN32
	DIR	*dirp;
	struct	 dirent	*dp;
	uid_t	ouruid=0;
#else
	struct _finddata_t jdir;    
	long hFile;
#endif
	struct	 stat	sbuf;
	time_t	best_time=0;
	char	jbuf[5];
	FILE *verfile;
	char *vernum;
	int	optlet;

	strcpy(doc_dir, "doc");
	strcpy(test_dir, "TSNAME");
	strcpy(test_name, "TETware");

	VSORBoptind= 0;
	
	detail = 1;
	scope = 1;

	while ((optlet = VSORBgetopt(argc, argv, OPTIONS)) != -1) {

		switch (optlet) {
		case 'w':
			whatj();
			exit(0);
		case 'f':
			infile = VSORBoptarg;
			break;
		case 'd':
			detail = atoi(VSORBoptarg);
			if ((detail > 4) || (detail < 0)) {
				fprintf(stderr, "Detail value illegal\n");
				errflag++;
			}
			break;
		case 'h':
			errflag++;
			break;
		case 'x':
			xopen_report = atoi(VSORBoptarg);
			break;
		case 'c':
			print_config_vars = 1;
			break;
		case 'j':
			jfileno = atoi(VSORBoptarg);
			break;
		case 's':
			scope = atoi(VSORBoptarg);
			if ((scope > 7) || (scope < 0)) {
				fprintf(stderr, "Scope value illegal\n");
				errflag++;
			}
			break;
		case 't':
			reportcase = VSORBoptarg;
			freportcase = 1;
			ffoundcase = 0;
			break;
		case 'u':
			fuser++;
			break;
		case 'v':
			fprintf(stderr, "%s Report Generator %s\n", test_name, verbuf);
			exit(0);
		case ':':
			fprintf(stderr, "Option -%c requires an operand\n", VSORBoptopt);
			errflag++;
			break;
		case '?':
			fprintf(stderr, "Unknown option -%c\n", VSORBoptopt);
			errflag++;
			break;
		}
	}

	if (errflag) {
		fprintf(stderr, "usage: rpt [-f filename] [-j journal] [-d detail] [-s scope] [-t test] [-u] [ -h ] [-v] [-w]\n");
		fprintf(stderr, "      -f journal file name\n");
		fprintf(stderr, "      -j journal file # (default is latest)\n");
		fprintf(stderr, "      -d detail\n");
		fprintf(stderr, "         0 - everything\n");
		fprintf(stderr, "         1 - reasonable detail (default)\n");
		fprintf(stderr, "         2 - nothing\n");
		fprintf(stderr, "         3 - results only\n");
		fprintf(stderr, "         4 - assertions and results only\n");
		fprintf(stderr, "      -s scope of detail\n");
		fprintf(stderr, "         0 - all result codes\n");
		fprintf(stderr, "         1 - errors, FIP (default)\n");
		fprintf(stderr, "         2 - reserved for future use\n");
		fprintf(stderr, "         3 - UNSUPPORTED\n");
		fprintf(stderr, "         4 - FIP\n");
		fprintf(stderr, "         5 - NOTINUSE\n");
		fprintf(stderr, "         6 - UNTESTED\n");
		fprintf(stderr, "         7 - NORESULT\n");
		fprintf(stderr, "      -t detail only on specified Test Case\n");
		fprintf(stderr, "      -u use latest journal file for current user\n");
		fprintf(stderr, "      -h display this usage message\n");
		fprintf(stderr, "      -v display program version\n");
		fprintf(stderr, "      -w summarize journals\n");
		exit(2);
	}

	/* -j*/
	if (jfileno != 0) {
		/*make the full number*/
		sprintf(jbuf, "%04d", jfileno);
		jbuf[4] = 0;
		/*find the results directory*/
		if (getenv("TET_ROOT") == NULL) {
			fprintf(stderr, "$TET_ROOT is not set\n");
			exit(2);
		}
		sprintf(inbuf, "%s/%s/results", getenv("TET_ROOT"), test_dir);

#ifndef _WIN32
		if ((dirp = opendir(inbuf)) == NULL) {
			perror("cannot open results directory");
			exit(2);
		}
#endif
		/*skip . and ..*/
#ifndef _WIN32
		dp = readdir(dirp);
		dp = readdir(dirp);
#else
		strcat(inbuf, "/*");
		hFile = _findfirst(inbuf, &jdir);
		_findnext(hFile, &jdir);
		_findnext(hFile, &jdir);
#endif

		/*find a matching results sub-directory, ignoring tcc mode*/
#ifndef _WIN32
		while ((dp = readdir(dirp)) != NULL) {
			if (strstr(dp->d_name, jbuf) != 0) {
				sprintf(inbuf, "%s/%s/results/%s/journal", getenv("TET_ROOT"), test_dir, dp->d_name);
				infile = inbuf;
				if (access(infile, R_OK) != 0) {
					perror("Cannot access journal file");
					exit(2);
				}
				break;
			}
		}
#else
		while (_findnext(hFile, &jdir) == 0) {
			if (strstr(jdir.name, jbuf) != 0) {
				sprintf(inbuf, "%s/%s/results/%s/journal", getenv("TET_ROOT"), test_dir, jdir.name);
				infile = inbuf;
				if (_access(infile, 0) != 0) {
					perror("Cannot access journal file");
					exit(2);
				}
				break;
			}
		}
#endif
		if (infile == 0) {
			fprintf(stderr, "Cannot find journal numbered %d\n", jfileno);
			exit(2);
		}
	}

#ifndef _WIN32
/* _WIN32 is not a multiuser system as such, so we ignore this option */
	if (fuser)
		ouruid = getuid();
#endif

	/* -u option or no -f or -j*/
	if ((infile == 0) || (fuser)) {
		/*find the results directory*/
		if (getenv("TET_ROOT") == NULL) {
			fprintf(stderr, "$TET_ROOT is not set\n");
			exit(2);
		}
		sprintf(tmpbuf, "%s/%s/results", getenv("TET_ROOT"), test_dir);

#ifndef _WIN32
		if ((dirp = opendir(tmpbuf)) == NULL) {
			perror("cannot open results directory");
			exit(2);
		}
#endif

#ifndef _WIN32
	/*skip . and ..*/
	dp = readdir(dirp);
	dp = readdir(dirp);
#else
	strcat(tmpbuf, "/*");
	hFile = _findfirst(tmpbuf, &jdir);
	_findnext(hFile, &jdir);
	_findnext(hFile, &jdir);
#endif

#ifndef _WIN32
	while ((dp = readdir(dirp)) != NULL) {
			sprintf(tmpbuf, "%s/%s/results/%s", getenv("TET_ROOT"), test_dir, dp->d_name);
#else
	while (_findnext(hFile, &jdir) == 0) {
			sprintf(tmpbuf, "%s/%s/results/%s", getenv("TET_ROOT"), test_dir, jdir.name);
#endif

#ifdef CDEBUG
fprintf(stderr, "dir: %s\n", tmpbuf);
#endif
			if (stat(tmpbuf, &sbuf) != 0) {
				if (fcount == 0)
					perror("no results sub-directories");
				else
					perror("cannot stat a results sub-directory");
				exit(2);
			}

			/*ignore regular files that might sneak in*/
			if (S_ISDIR(sbuf.st_mode) == 0)
				continue;

#ifndef _WIN32
			/*only look at ones we own if -u specified*/
			if (fuser) 
				if (ouruid != sbuf.st_uid)
					continue;
#endif
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
				fprintf(stderr, "No journal files found for this user.\n");
				exit(2);
			}
			else {
				fprintf(stderr, "No journal files found.\n");
				exit(2);
			}
		}

		/*we'll use that journal file*/
		strcat(inbuf, "/journal");
		infile = inbuf;
		if (access(infile, R_OK) != 0) {
			perror("Cannot access latest journal");
			exit(2);
		}
	}

	/*user defined file*/
	else {
		if (access(infile, R_OK) != 0) {
			perror("Cannot access specified journal file");
			exit(2);
		}
	}

#ifdef VER_FILE
	sprintf(tmpbuf, "%s/%s/%s/RELEASE", getenv("TET_ROOT"), test_dir, doc_dir);

	verfile = fopen(tmpbuf, "r");
	if (verfile == 0) {
		fprintf(stderr, "WARNING: Cannot open file: %s\n", tmpbuf);
		strcpy(verbuf, "Release file not found\n");
	}
	else {
		vernum = fgets(verbuf, sizeof(verbuf), verfile);
		if (vernum == 0)
			fprintf(stderr, "WARNING: Cannot read version\n");
	}
#endif

	parse_file();
	
	if ((xopen_report != 2) && (xopen_report != 3))
		print_summary();

	exit(0);
}
