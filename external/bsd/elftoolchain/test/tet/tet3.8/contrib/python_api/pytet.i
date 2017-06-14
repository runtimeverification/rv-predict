/***************************************************************************
*
* Copyright (C) 2004 X/Open Company Limited
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
 
NAME:		pytet.i

PROJECT:	PyTET

AUTHOR:		Neil Moses

DATE CREATED:	May 2004

DESCRIPTION:	SWIG interface file with the Python TET API bindings

***************************************************************************/

%module pytet
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
extern int tet_vprintf (char *, va_list);
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

/* Typemap to convert Python lists to char ** */
%typemap(python,in) char ** 
{
    if (PyList_Check($input)) 
    {
        int size = PyList_Size($input);
        int i = 0;
        $1 = (char **) malloc((size+1)*sizeof(char *));
        for (i = 0; i < size; i++) {
            PyObject *o = PyList_GetItem($input,i);
            if (PyString_Check(o))
            {
                $1[i] = PyString_AsString(PyList_GetItem($input,i));
            }
            else 
            {
                PyErr_SetString(PyExc_TypeError, "List must contain strings");
                free($1);
                return NULL;
            }
        }
        $1[i] = 0;
    }  
    else 
    {
        PyErr_SetString(PyExc_TypeError,"Not a python list object");
        return NULL;
    }
}

%typemap(python,freearg) char ** 
{
    free((char *) $1);
}

/*
 * Inline C code - gets included in the wrapper code
 */
%inline %{

/* Dynamic interface functions */
#define PYTET_STARTUP       0
#define PYTET_CLEANUP       1
#define PYTET_GETTP         2
#define PYTET_GETMINIC      3
#define PYTET_GETMAXIC      4
#define PYTET_ISDEFIC       5
#define PYTET_GETTPCOUNT    6
#define PYTET_GETTESTNUM    7

/* Initialise the IC list */
void pytet_set_iclist(int argc, char **argv)
{
    tet_main(argc, argv);
}

/* Pointers to the Python dynamic interface functions */
static PyObject *py_gettp = NULL;
static PyObject *py_startup = NULL;
static PyObject *py_cleanup = NULL;
static PyObject *py_getminic = NULL;
static PyObject *py_getmaxic = NULL;
static PyObject *py_isdefic = NULL;
static PyObject *py_gettpcount = NULL;
static PyObject *py_gettestnum = NULL;

/* 
 * Called by the Python program to initialise 
 * the Python callback function pointers 
 */ 
void
pytet_set_pyfunc(PyObject *pyfunc, PyObject *pytype) 
{
    int type;

    Py_XINCREF(pyfunc);
    Py_XINCREF(pytype);
    type = PyInt_AsLong(pytype);
    Py_XDECREF(pytype);

    switch (type)
    {
    case PYTET_STARTUP:
        py_startup = pyfunc;
        break;
    case PYTET_CLEANUP:
        py_cleanup = pyfunc;
        break;
    case PYTET_GETTP:
        py_gettp = pyfunc;
        break;
    case PYTET_GETMAXIC:
        py_getmaxic = pyfunc;
        break;
    case PYTET_GETMINIC:
        py_getminic = pyfunc;
        break;
    case PYTET_ISDEFIC:
        py_isdefic = pyfunc;
        break;
    case PYTET_GETTPCOUNT:
        py_gettpcount = pyfunc;
        break;
    case PYTET_GETTESTNUM:
        py_gettestnum = pyfunc;
        break;
    }
    Py_XDECREF(pyfunc);
}

/*
 * Invokes the Python callback function to return 
 * the highest IC number for the test case.
 */
int
tet_getmaxic(void)
{
    PyObject *result;
    int retval = 0;

    if (py_getmaxic)
    {
        Py_XINCREF(py_getmaxic);
        result = PyEval_CallObject(py_getmaxic, NULL);
        Py_XINCREF(result);
        Py_XDECREF(py_getmaxic);
        retval = PyInt_AsLong(result);
        Py_XDECREF(result);
    }

    return retval;
}

/*
 * Invokes the Python callback function to return 
 * the lowest IC number for the test case.
 */
int
tet_getminic(void)
{
    PyObject *result;
    int retval = 0;

    if (py_getminic)
    {
        Py_XINCREF(py_getminic);
        result = PyEval_CallObject(py_getminic, NULL);
        Py_XINCREF(result);
        Py_XDECREF(py_getminic);
        retval = PyInt_AsLong(result);
        Py_XDECREF(result);
    }

    return retval;
}

/*
 * Invokes the Python callback function to return 1 
 * if icnum is defined for the test case.
 */
int
tet_isdefic(int icnum)
{
    PyObject *arglist;
    PyObject *result;
    int retval = 0;

    if (py_isdefic)
    {
        arglist = Py_BuildValue("(i)", icnum);
        Py_XINCREF(py_isdefic);
        Py_XINCREF(arglist);
        result = PyEval_CallObject(py_isdefic, arglist);
        Py_XINCREF(result);
        Py_XDECREF(py_isdefic);
        Py_XDECREF(arglist);
        retval = PyInt_AsLong(result);
        Py_XDECREF(result);
    }

    return retval;
}

/*
 * Invokes the Python callback function to return the
 * number of TPs that have been defined for icnum, or
 * 0 if icnum is not defined. 
 */
int
tet_gettpcount(int icnum)
{
    PyObject *arglist;
    PyObject *result;
    int retval = 0;

    if (py_gettpcount)
    {
        arglist = Py_BuildValue("(i)", icnum);
        Py_XINCREF(py_gettpcount);
        Py_XINCREF(arglist);
        result = PyEval_CallObject(py_gettpcount, arglist);
        Py_XINCREF(result);
        Py_XDECREF(py_gettpcount);
        Py_XDECREF(arglist);
        retval = PyInt_AsLong(result);
        Py_XDECREF(result);
    }

    return retval;
}

/*
 * Invokes the Python callback function to return the
 * absolute test number for an icnum/tpnum combination.
 * Currently PyTET only supports a one-to-one relationship.
 */
int
tet_gettestnum(int icnum, int tpnum)
{
    PyObject *arglist;
    PyObject *result;
    int retval = 0;

    if (py_gettestnum)
    {
        arglist = Py_BuildValue("(ii)", icnum, tpnum);
        Py_XINCREF(py_gettestnum);
        Py_XINCREF(arglist);
        result = PyEval_CallObject(py_gettestnum, arglist);
        Py_XDECREF(py_gettestnum);
        Py_XDECREF(arglist);
        Py_XINCREF(result);
        retval = PyInt_AsLong(result);
        Py_XDECREF(result);
    }

    return retval;
}

int
tet_invoketp(int icnum, int tpnum)
{
    PyObject *arglist;
    PyObject *calltp;

    if (py_gettp)
    {
        arglist = Py_BuildValue("(ii)", icnum, tpnum);
        Py_XINCREF(py_gettp);
        Py_XINCREF(arglist);
        calltp = PyEval_CallObject(py_gettp, arglist);
        Py_XDECREF(py_gettp);
        Py_XDECREF(arglist);
        if (calltp)
            PyEval_CallObject(calltp, NULL);
        Py_XDECREF(calltp);
    }

    return 0;
}

static void
startup(void)
{
    if (py_startup)
    {
        Py_XINCREF(py_startup);
        PyEval_CallObject(py_startup, NULL);
        Py_XDECREF(py_startup);
    }
}

static void
cleanup(void)
{
    if (py_cleanup)
    {
        Py_XINCREF(py_cleanup);
        PyEval_CallObject(py_cleanup, NULL);
        Py_XDECREF(py_cleanup);
    }
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

/*
 * Python code - gets included in the generated python file
 */
%pythoncode %{
import sys

def pytet_gettp(icnum, tpnum):
    count = 0
    for testic in pytet_testlist.keys():
        if testic == icnum:
            count += 1
            if count == tpnum:
                return pytet_testlist[testic]
    return ""

def pytet_getminic():
    minic = len(pytet_testlist)
    for testic in pytet_testlist.keys():
        if testic < minic:
            minic = testic
    return minic

def pytet_getmaxic():
    maxic = 1
    for testic in pytet_testlist.keys():
        if testic > maxic:
            maxic = testic
    return maxic

def pytet_isdefic(icnum):
    for testic in pytet_testlist.keys():
        if testic == icnum:
            return 1
    return 0

def pytet_gettpcount(icnum):
    count = 0
    for testic in pytet_testlist.keys():
        if testic == icnum:
            count += 1
    return count

def pytet_gettestnum(icnum, tpnum):
    count = icnum
    for testic in pytet_testlist.keys():
        if testic == icnum:
		if count == icnum + tpnum - 1:
			break
            	count += 1
    return count

def pytet_init(testlist, startup, cleanup):
    global pytet_testlist
    pytet_testlist = testlist
    if startup != TET_NULLFP:
        pytet_set_pyfunc(startup, PYTET_STARTUP)
    if cleanup != TET_NULLFP:
        pytet_set_pyfunc(cleanup, PYTET_CLEANUP)
    pytet_set_pyfunc(pytet_gettp, PYTET_GETTP)
    pytet_set_pyfunc(pytet_getminic, PYTET_GETMINIC)
    pytet_set_pyfunc(pytet_getmaxic, PYTET_GETMAXIC)
    pytet_set_pyfunc(pytet_isdefic, PYTET_ISDEFIC)
    pytet_set_pyfunc(pytet_gettpcount, PYTET_GETTPCOUNT)
    pytet_set_pyfunc(pytet_gettestnum, PYTET_GETTESTNUM)
    pytet_set_iclist(len(sys.argv), sys.argv)

%}
