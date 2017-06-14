# This file was created automatically by SWIG.
# Don't modify this file, modify the SWIG interface instead.
# This file is compatible with both classic and new-style classes.

import _pytet

def _swig_setattr(self,class_type,name,value):
    if (name == "this"):
        if isinstance(value, class_type):
            self.__dict__[name] = value.this
            if hasattr(value,"thisown"): self.__dict__["thisown"] = value.thisown
            del value.thisown
            return
    method = class_type.__swig_setmethods__.get(name,None)
    if method: return method(self,value)
    self.__dict__[name] = value

def _swig_getattr(self,class_type,name):
    method = class_type.__swig_getmethods__.get(name,None)
    if method: return method(self)
    raise AttributeError,name

import types
try:
    _object = types.ObjectType
    _newclass = 1
except AttributeError:
    class _object : pass
    _newclass = 0
del types


TET_PASS = _pytet.TET_PASS
TET_FAIL = _pytet.TET_FAIL
TET_UNRESOLVED = _pytet.TET_UNRESOLVED
TET_NOTINUSE = _pytet.TET_NOTINUSE
TET_UNSUPPORTED = _pytet.TET_UNSUPPORTED
TET_UNTESTED = _pytet.TET_UNTESTED
TET_UNINITIATED = _pytet.TET_UNINITIATED
TET_NORESULT = _pytet.TET_NORESULT
TET_ER_OK = _pytet.TET_ER_OK
TET_ER_ERR = _pytet.TET_ER_ERR
TET_ER_MAGIC = _pytet.TET_ER_MAGIC
TET_ER_LOGON = _pytet.TET_ER_LOGON
TET_ER_RCVERR = _pytet.TET_ER_RCVERR
TET_ER_REQ = _pytet.TET_ER_REQ
TET_ER_TIMEDOUT = _pytet.TET_ER_TIMEDOUT
TET_ER_DUPS = _pytet.TET_ER_DUPS
TET_ER_SYNCERR = _pytet.TET_ER_SYNCERR
TET_ER_INVAL = _pytet.TET_ER_INVAL
TET_ER_TRACE = _pytet.TET_ER_TRACE
TET_ER_WAIT = _pytet.TET_ER_WAIT
TET_ER_XRID = _pytet.TET_ER_XRID
TET_ER_SNID = _pytet.TET_ER_SNID
TET_ER_SYSID = _pytet.TET_ER_SYSID
TET_ER_INPROGRESS = _pytet.TET_ER_INPROGRESS
TET_ER_DONE = _pytet.TET_ER_DONE
TET_ER_CONTEXT = _pytet.TET_ER_CONTEXT
TET_ER_PERM = _pytet.TET_ER_PERM
TET_ER_FORK = _pytet.TET_ER_FORK
TET_ER_NOENT = _pytet.TET_ER_NOENT
TET_ER_PID = _pytet.TET_ER_PID
TET_ER_SIGNUM = _pytet.TET_ER_SIGNUM
TET_ER_FID = _pytet.TET_ER_FID
TET_ER_INTERN = _pytet.TET_ER_INTERN
TET_ER_ABORT = _pytet.TET_ER_ABORT
TET_ER_2BIG = _pytet.TET_ER_2BIG
TET_NULLFP = _pytet.TET_NULLFP

tet_delete = _pytet.tet_delete

tet_exit = _pytet.tet_exit

tet_getvar = _pytet.tet_getvar

tet_infoline = _pytet.tet_infoline

tet_kill = _pytet.tet_kill

tet_logoff = _pytet.tet_logoff

tet_minfoline = _pytet.tet_minfoline

tet_printf = _pytet.tet_printf

tet_reason = _pytet.tet_reason

tet_remgetlist = _pytet.tet_remgetlist

tet_remgetsys = _pytet.tet_remgetsys

tet_result = _pytet.tet_result

tet_setblock = _pytet.tet_setblock

tet_setcontext = _pytet.tet_setcontext

tet_spawn = _pytet.tet_spawn

tet_vprintf = _pytet.tet_vprintf

tet_wait = _pytet.tet_wait

tet_exec = _pytet.tet_exec

tet_fork = _pytet.tet_fork

tet_tcm_main = _pytet.tet_tcm_main

tet_merror = _pytet.tet_merror

tet_error = _pytet.tet_error
PYTET_STARTUP = _pytet.PYTET_STARTUP
PYTET_CLEANUP = _pytet.PYTET_CLEANUP
PYTET_GETTP = _pytet.PYTET_GETTP
PYTET_GETMINIC = _pytet.PYTET_GETMINIC
PYTET_GETMAXIC = _pytet.PYTET_GETMAXIC
PYTET_ISDEFIC = _pytet.PYTET_ISDEFIC
PYTET_GETTPCOUNT = _pytet.PYTET_GETTPCOUNT
PYTET_GETTESTNUM = _pytet.PYTET_GETTESTNUM

pytet_set_iclist = _pytet.pytet_set_iclist

pytet_set_pyfunc = _pytet.pytet_set_pyfunc

tet_getmaxic = _pytet.tet_getmaxic

tet_getminic = _pytet.tet_getminic

tet_isdefic = _pytet.tet_isdefic

tet_gettpcount = _pytet.tet_gettpcount

tet_gettestnum = _pytet.tet_gettestnum

tet_invoketp = _pytet.tet_invoketp

startup = _pytet.startup

cleanup = _pytet.cleanup

tet_main = _pytet.tet_main

tet_tcmchild_main = _pytet.tet_tcmchild_main
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


cvar = _pytet.cvar

