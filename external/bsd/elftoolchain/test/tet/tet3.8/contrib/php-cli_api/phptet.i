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
 
NAME:		    phptet.i

PROJECT:	    PhpTET

AUTHOR:		    Neil Moses

DATE CREATED:	March 2005

DESCRIPTION:	SWIG interface file with the PHP TET API bindings

***************************************************************************/

%module phptet
%{
%}

/* Typemap to convert PHP lists to char ** */
%typemap(php4,in) char ** 
{
    zval **tmp;
    convert_to_array_ex($input);
    int size = zend_hash_num_elements(Z_ARRVAL_PP($input));
    $1 = (char **) malloc((size+1)*sizeof(char *));
    zend_hash_internal_pointer_reset(Z_ARRVAL_PP($input));
    int i;
    for (i = 0; i < size; i++)
    {
        zend_hash_get_current_data (Z_ARRVAL_P($input[0]), (void **) &tmp);
        convert_to_string(*tmp);
        $1[i] = Z_STRVAL_P(*tmp);
    }
    $1[i] = 0;
}

%typemap(php4,freearg) char ** 
{
    free((char *) $1);
}

%typemap(php4,out) char * 
{
    if ($input == NULL)
    {
        $input = "";
    }

    ZVAL_STRING(return_value,$input, 1);
}

/* 
 * This typemap is to overcome problem with tet_delete() where
 * the reason string argument address is being overwritten by
 * the PHP code. Maybe the TET code should be malloc'ing a new
 * buffer for the reason and not just assigning the reason pointer
 * to the argument address?
 */
%typemap(php4,in) char * 
{
    convert_to_string_ex($input);
    $1 = (char *) Z_STRVAL_PP($input);
    char *s = (char *)malloc(strlen($1) + 1);
    strcpy(s, $1);
    $1 = s;
}


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

/*
 * Inline C code - gets included in the wrapper code
 */
%inline %{

/* Dynamic interface functions */
#define PHPTET_STARTUP       0
#define PHPTET_CLEANUP       1
#define PHPTET_GETTP         2
#define PHPTET_GETMINIC      3
#define PHPTET_GETMAXIC      4
#define PHPTET_ISDEFIC       5
#define PHPTET_GETTPCOUNT    6
#define PHPTET_GETTESTNUM    7
#define PHPTET_TESTSET       8

static char *php_startup = 0;
static char *php_cleanup = 0;
static char *php_gettp = 0;
static char *php_getminic = 0;
static char *php_getmaxic = 0;
static char *php_isdefic = 0;
static char *php_gettpcount = 0;
static char *php_gettestnum = 0;

/* Initialise the IC list */
void 
phptet_set_iclist(int argc, char **argv)
{
    tet_main(argc, argv);
}

void
phptet_set_func(char *fname, int type) 
{
    int size = strlen(fname) + 1;
    switch (type)
    {
    case PHPTET_STARTUP:
        php_startup = malloc(size);
        strcpy(php_startup, fname);
        break;
    case PHPTET_CLEANUP:
        php_cleanup = malloc(size);
        strcpy(php_cleanup, fname);
        break;
    case PHPTET_GETTP:
        php_gettp = malloc(size);
        strcpy(php_gettp, fname);
        break;
    case PHPTET_GETMAXIC:
        php_getmaxic = malloc(size);
        strcpy(php_getmaxic, fname);
        break;
    case PHPTET_GETMINIC:
        php_getminic = malloc(size);
        strcpy(php_getminic, fname);
        break;
    case PHPTET_ISDEFIC:
        php_isdefic = malloc(size);
        strcpy(php_isdefic, fname);
        break;
    case PHPTET_GETTPCOUNT:
        php_gettpcount = malloc(size);
        strcpy(php_gettpcount, fname);
        break;
    case PHPTET_GETTESTNUM:
        php_gettestnum = malloc(size);
        strcpy(php_gettestnum, fname);
        break;
    }
}

int
tet_getmaxic(void)
{
    zval php_ret;
    zval *php_func;

    if (php_getmaxic)
    {
        MAKE_STD_ZVAL(php_func);
        ZVAL_STRING(php_func, php_getmaxic, 1);
        if (call_user_function(CG(function_table), NULL, php_func, &php_ret, 
            0, NULL) != SUCCESS)
        {
            zend_error(E_ERROR, "Call to %s failed\n", php_getmaxic);
            return 0;
        }
    }
 
    return Z_LVAL(php_ret);
}

int
tet_getminic(void)
{
    zval php_ret;
    zval *php_func;

    if (php_getminic)
    {
        MAKE_STD_ZVAL(php_func);
        ZVAL_STRING(php_func, php_getminic, 1);
        if (call_user_function(CG(function_table), NULL, php_func, &php_ret, 
            0, NULL) != SUCCESS)
        {
            zend_error(E_ERROR, "Call to %s failed\n", php_getminic);
            return 0;
        }
    }

    return Z_LVAL(php_ret);
}

int
tet_isdefic(int icnum)
{
    zval php_ret;
    zval *php_func;
    zval *php_arg;
    zval *php_args[1];

    if (php_isdefic)
    {
        MAKE_STD_ZVAL(php_func);
        ZVAL_STRING(php_func, php_isdefic, 1);
        MAKE_STD_ZVAL(php_arg);
        ZVAL_LONG(php_arg, icnum);
        php_args[0] = php_arg;
        if (call_user_function(CG(function_table), NULL, php_func, &php_ret, 
            1, php_args) != SUCCESS)
        {
            zend_error(E_ERROR, "Call to %s failed\n", php_isdefic);
            return 0;
        }
    }

    return Z_LVAL(php_ret);
}

int
tet_gettpcount(int icnum)
{
    zval php_ret;
    zval *php_func;
    zval *php_arg;
    zval *php_args[1];

    if (php_gettpcount)
    {
        MAKE_STD_ZVAL(php_func);
        ZVAL_STRING(php_func, php_gettpcount, 1);
        MAKE_STD_ZVAL(php_arg);
        ZVAL_LONG(php_arg, icnum);
        php_args[0] = php_arg;

        if (call_user_function(CG(function_table), NULL, php_func, &php_ret, 
            1, php_args) != SUCCESS)
        {
            zend_error(E_ERROR, "Call to %s failed\n", php_gettpcount);
            return 0;
        }
    }

    return Z_LVAL(php_ret);
}

int
tet_gettestnum(int icnum, int tpnum)
{
    zval php_ret;
    zval *php_func;
    zval *php_arg1;
    zval *php_arg2;
    zval *php_args[2];

    if (php_gettestnum)
    {
        MAKE_STD_ZVAL(php_func);
        ZVAL_STRING(php_func, php_gettestnum, 1);
        MAKE_STD_ZVAL(php_arg1);
        ZVAL_LONG(php_arg1, icnum);
        MAKE_STD_ZVAL(php_arg2);
        ZVAL_LONG(php_arg2, tpnum);
        php_args[0] = php_arg1;
        php_args[1] = php_arg2;
        if (call_user_function(CG(function_table), NULL, php_func, &php_ret, 
            2, php_args) != SUCCESS)
        {
            zend_error(E_ERROR, "Call to %s failed\n", php_gettestnum);
            return 0;
        }
    }

    return Z_LVAL(php_ret);
}

int
tet_invoketp(int icnum, int tpnum)
{
    char *tp_func;
    zval php_ret;
    zval *php_func;
    zval *php_tp;
    zval *php_arg1;
    zval *php_arg2;
    zval *php_args[2];

    if (php_gettp)
    {
	    MAKE_STD_ZVAL(php_func);
	    ZVAL_STRING(php_func, php_gettp, 1);
	    MAKE_STD_ZVAL(php_arg1);
	    ZVAL_LONG(php_arg1, icnum);
	    MAKE_STD_ZVAL(php_arg2);
	    ZVAL_LONG(php_arg2, tpnum);
	    MAKE_STD_ZVAL(php_tp);
	    php_args[0] = php_arg1;
	    php_args[1] = php_arg2;
	    if (call_user_function(CG(function_table), NULL, php_func, php_tp, 
	        2, php_args) != SUCCESS)
	    {
	        zend_error(E_ERROR, "Call to %s failed\n", php_gettp);
	        return 0;
	    }
	
	    if (call_user_function(CG(function_table), NULL, php_tp, &php_ret, 
	        0, NULL) != SUCCESS)
	    {
	        zend_error(E_ERROR, "Call to %s failed\n", Z_STRVAL_P(php_tp));
	        return 0;
	    }
    }

    return Z_LVAL(php_ret);
}

static void
phptet_startup(void)
{
    zval php_ret;
    zval *php_func;

    if (php_startup != 0)
    {
        MAKE_STD_ZVAL(php_func);
        ZVAL_STRING(php_func, php_startup, 1);
        if (call_user_function(CG(function_table), NULL, php_func, &php_ret, 
            0, NULL) != SUCCESS)
        {
            zend_error(E_ERROR, "Call to %s failed\n", php_startup);
            return;
        }
    }

}

static void
phptet_cleanup(void)
{
    zval php_ret;
    zval *php_func;

    if (php_cleanup != 0)
    {
        MAKE_STD_ZVAL(php_func);
        ZVAL_STRING(php_func, php_cleanup, 1);
        if (call_user_function(CG(function_table), NULL, php_func, &php_ret, 
            0, NULL) != SUCCESS)
        {
            zend_error(E_ERROR, "Call to %s failed\n", php_cleanup);
            return;
        }
    }
}

void (*tet_startup)() = phptet_startup;
void (*tet_cleanup)() = phptet_cleanup;

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

%pragma(php4) include="phptet_dyn.php"
