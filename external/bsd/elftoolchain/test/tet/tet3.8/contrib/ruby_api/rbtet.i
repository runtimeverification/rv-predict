/***************************************************************************
*
* Copyright (C) 2005 X/Open Company Limited
* 
* Permission is hereby granted, free of charge, to any person obtaining a
* copy of this software and associated documentation files (the "Software"), 
* to deal in the Software without restriction, including without limitation 
* the rights to use, copy, modify, merge, publish, distribute, sublicense, 
* and/or sell copies of the Software, and to permit persons to whom the 
* Software is furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included 
* in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
* DEALINGS IN THE SOFTWARE.
*
****************************************************************************/

/***************************************************************************
 
NAME:		    rbtet.i

PROJECT:	    RbTET

AUTHOR:		    Neil Moses

DATE CREATED:	March 2005

DESCRIPTION:	SWIG interface file with the Ruby TET API bindings

***************************************************************************/

%module rbtet_ext
%{
%}

/* TET API defines */
#define TET_PASS            0
#define TET_FAIL            1
#define TET_UNRESOLVED      2
#define TET_NOTINUSE        3
#define TET_UNSUPPORTED     4
#define TET_UNTESTED        5
#define TET_UNINITIATED     6
#define TET_NORESULT        7
#define TET_ER_OK           0
#define TET_ER_ERR          1
#define TET_ER_MAGIC        2
#define TET_ER_LOGON        3
#define TET_ER_RCVERR       4
#define TET_ER_REQ          5
#define TET_ER_TIMEDOUT     6
#define TET_ER_DUPS         7
#define TET_ER_SYNCERR      8
#define TET_ER_INVAL        9
#define TET_ER_TRACE        10
#define TET_ER_WAIT         11
#define TET_ER_XRID         12
#define TET_ER_SNID         13
#define TET_ER_SYSID        14
#define TET_ER_INPROGRESS   15
#define TET_ER_DONE         16
#define TET_ER_CONTEXT      17
#define TET_ER_PERM         18
#define TET_ER_FORK         19
#define TET_ER_NOENT        20
#define TET_ER_PID          21
#define TET_ER_SIGNUM       22
#define TET_ER_FID          23
#define TET_ER_INTERN       24
#define TET_ER_ABORT        25
#define TET_ER_2BIG         26
#define TET_NULLFP          0

/* TET API Functions */
extern void tet_delete (int, char *);
extern void tet_exit (int);
extern char * tet_getvar (char *);
extern void tet_infoline (char *);
extern int tet_kill (pid_t, int);
extern void tet_logoff (void);
extern int tet_minfoline (char **, int);
extern int tet_printf (char *, ...);
extern char * tet_reason (int);
extern int tet_remgetlist (int **);
extern int tet_remgetsys (void);
extern void tet_result (int);
extern void tet_setblock (void);
extern void tet_setcontext (void);
extern pid_t tet_spawn (char *, char **, char **);
/* extern int tet_vprintf (char *, va_list); */
extern int tet_wait (pid_t, int *);
extern int tet_exec (char *, char *[], char *[]);
extern int tet_fork (void (*) (void), void (*) (void), int, int);
extern void tet_tcm_main (int, char *[]);
extern void tet_merror (int, char **, int);
extern void tet_error (int, char *);

/* TET API Globals */
extern pid_t tet_child;
extern int tet_errno;
extern char **tet_errlist;
extern int tet_nerr;
extern int tet_nosigreset;
extern char * tet_pname;
extern int tet_thistest;
extern long tet_block;
extern long tet_sequence;

// This tells SWIG to treat char ** as a special case
%typemap(in) char ** {
    int size = RARRAY($input)->len;     
    int i;

    $1 = (char **) malloc((size+1)*sizeof(char *));
    VALUE *ptr = RARRAY($input)->ptr;   
    for (i=0; i < size; i++, ptr++)
        $1[i]= STR2CSTR(*ptr); 
    $1[i]=NULL;
}

// This cleans up the char ** array created before the function call
%typemap(freearg) char ** {
    free((char *) $1);
}

/*
 * Inline C code - gets included in the wrapper code
 */
%inline %{

/* Dynamic interface functions */
#define RBTET_STARTUP       0
#define RBTET_CLEANUP       1
#define RBTET_GETTP         2
#define RBTET_GETMINIC      3
#define RBTET_GETMAXIC      4
#define RBTET_ISDEFIC       5
#define RBTET_GETTPCOUNT    6
#define RBTET_GETTESTNUM    7
#define RBTET_TESTSET       8

/* Ruby function ids */
static ID rb_gettp = 0;
static ID rb_startup = 0;
static ID rb_cleanup = 0;
static ID rb_getminic = 0;
static ID rb_getmaxic = 0;
static ID rb_isdefic = 0;
static ID rb_gettpcount = 0;
static ID rb_gettestnum = 0;

/* Receiving object */
static VALUE rb_testset_obj = 0;
static VALUE rb_test_obj = 0;

/* Initialise the IC list */
void rbtet_set_iclist(int argc, char **argv)
{
    tet_main(argc, argv);
}

/* 
 * Called by the Ruby program to initialise the Ruby callback function pointers 
 */ 
void
rbtet_set_rbfunc(VALUE recv, VALUE rbfunc, VALUE rbtype) 
{
    char *name;
    int type;
    ID id;

    name = STR2CSTR(rbfunc);
    type = NUM2INT(rbtype);
    id = rb_intern(name);

    switch (type)
    {
    case RBTET_STARTUP:
        rb_startup = id;
        rb_testset_obj = recv;
        break;
    case RBTET_CLEANUP:
        rb_cleanup = id;
        rb_testset_obj = recv;
        break;
    case RBTET_GETTP:
        rb_gettp = id;
        rb_test_obj = recv;
        break;
    case RBTET_GETMAXIC:
        rb_getmaxic = id;
        rb_test_obj = recv;
        break;
    case RBTET_GETMINIC:
        rb_getminic = id;
        rb_test_obj = recv;
        break;
    case RBTET_ISDEFIC:
        rb_isdefic = id;
        rb_test_obj = recv;
        break;
    case RBTET_GETTPCOUNT:
        rb_gettpcount = id;
        rb_test_obj = recv;
        break;
    case RBTET_GETTESTNUM:
        rb_gettestnum = id;
        rb_test_obj = recv;
        break;
    case RBTET_TESTSET:
        rb_testset_obj = recv;
        break;
    }
}

int
tet_getmaxic(void)
{
    VALUE result;
    int retval = 0;

    if (rb_getmaxic)
    {
        result = rb_funcall(rb_test_obj, rb_getmaxic, 0);
        retval = NUM2INT(result);
    }

    return retval;
}

int
tet_getminic(void)
{
    VALUE result;
    int retval = 0;

    if (rb_getminic)
    {
        result = rb_funcall(rb_test_obj, rb_getminic, 0);
        retval = NUM2INT(result);
    }

    return retval;
}

int
tet_isdefic(int icnum)
{
    VALUE result;
    int retval = 0;

    if (rb_isdefic)
    {
        result = rb_funcall(rb_test_obj, rb_isdefic, 1, INT2NUM(icnum));
        retval = NUM2INT(result);
    }

    return retval;
}

int
tet_gettpcount(int icnum)
{
    VALUE result;
    int retval = 0;

    if (rb_gettpcount)
    {
        result = rb_funcall(rb_test_obj, rb_gettpcount, 1, INT2NUM(icnum));
        retval = NUM2INT(result);
    }

    return retval;
}

int
tet_gettestnum(int icnum, int tpnum)
{
    VALUE result;
    int retval = 0;

    if (rb_gettestnum)
    {
        result = rb_funcall(rb_test_obj, rb_gettestnum, 2, INT2NUM(icnum), 
            INT2NUM(tpnum));
        retval = NUM2INT(result);
    }

    return retval;
}

int
tet_invoketp(int icnum, int tpnum)
{
    VALUE calltp;
    ID id;
    char *name;

    if (rb_gettp)
    {
        calltp = rb_funcall(rb_test_obj, rb_gettp, 2, INT2NUM(icnum), 
            INT2NUM(tpnum));

        if (calltp)
        {
            name = STR2CSTR(calltp);
            id = rb_intern(name);
            calltp = rb_funcall(rb_testset_obj, id, 0);
        }
    }

    return 0;
}

static void
startup(void)
{
    if (rb_startup)
        rb_funcall(rb_testset_obj, rb_startup, 0);
}

static void
cleanup(void)
{
    if (rb_cleanup)
        rb_funcall(rb_testset_obj, rb_cleanup, 0);
}

void (*tet_startup)() = startup;
void (*tet_cleanup)() = cleanup;

int
tet_main(int argc, char **argv)
{
    tet_tcm_main(argc, argv);
}

int
tet_tcmchild_main(int argc, char **argv)
{
    return 0;
}
%}
