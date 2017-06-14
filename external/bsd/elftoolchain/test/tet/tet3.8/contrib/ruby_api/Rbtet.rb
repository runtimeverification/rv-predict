############################################################################
#
# Copyright (C) 2005 X/Open Company Limited
# 
# Permission is hereby granted, free of charge, to any person obtaining a
# copy of this software and associated documentation files (the "Software"), 
# to deal in the Software without restriction, including without limitation 
# the rights to use, copy, modify, merge, publish, distribute, sublicense, 
# and/or sell copies of the Software, and to permit persons to whom the 
# Software is furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included 
# in all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
# OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
# DEALINGS IN THE SOFTWARE.
#
############################################################################/

############################################################################
#  
# NAME:            Rbtet.rb
# 
# PROJECT:        RbTET
# 
# AUTHOR:        Neil Moses
# 
# DATE CREATED:    March 2005
# 
# DESCRIPTION:    Ruby TET Module.
# 
############################################################################

require 'rbtet_ext'

module Rbtet

    # Some TET API constants
    TET_PASS = Rbtet_ext::TET_PASS
    TET_FAIL = Rbtet_ext::TET_FAIL
    TET_UNRESOLVED = Rbtet_ext::TET_UNRESOLVED
    TET_NOTINUSE = Rbtet_ext::TET_NOTINUSE
    TET_UNSUPPORTED = Rbtet_ext::TET_UNSUPPORTED
    TET_UNTESTED = Rbtet_ext::TET_UNTESTED
    TET_UNINITIATED = Rbtet_ext::TET_UNINITIATED
    TET_NORESULT = Rbtet_ext::TET_NORESULT
    TET_ER_OK = Rbtet_ext::TET_ER_OK
    TET_ER_ERR = Rbtet_ext::TET_ER_ERR
    TET_ER_MAGIC = Rbtet_ext::TET_ER_MAGIC
    TET_ER_LOGON = Rbtet_ext::TET_ER_LOGON
    TET_ER_RCVERR = Rbtet_ext::TET_ER_RCVERR
    TET_ER_REQ = Rbtet_ext::TET_ER_REQ
    TET_ER_TIMEDOUT = Rbtet_ext::TET_ER_TIMEDOUT
    TET_ER_DUPS = Rbtet_ext::TET_ER_DUPS
    TET_ER_SYNCERR = Rbtet_ext::TET_ER_SYNCERR
    TET_ER_INVAL = Rbtet_ext::TET_ER_INVAL
    TET_ER_TRACE = Rbtet_ext::TET_ER_TRACE
    TET_ER_WAIT = Rbtet_ext::TET_ER_WAIT
    TET_ER_XRID = Rbtet_ext::TET_ER_XRID
    TET_ER_SNID = Rbtet_ext::TET_ER_SNID
    TET_ER_SYSID = Rbtet_ext::TET_ER_SYSID
    TET_ER_INPROGRESS = Rbtet_ext::TET_ER_INPROGRESS
    TET_ER_DONE = Rbtet_ext::TET_ER_DONE
    TET_ER_CONTEXT = Rbtet_ext::TET_ER_CONTEXT
    TET_ER_PERM = Rbtet_ext::TET_ER_PERM
    TET_ER_FORK = Rbtet_ext::TET_ER_FORK
    TET_ER_NOENT = Rbtet_ext::TET_ER_NOENT
    TET_ER_PID = Rbtet_ext::TET_ER_PID
    TET_ER_SIGNUM = Rbtet_ext::TET_ER_SIGNUM
    TET_ER_FID = Rbtet_ext::TET_ER_FID
    TET_ER_INTERN = Rbtet_ext::TET_ER_INTERN
    TET_ER_ABORT = Rbtet_ext::TET_ER_ABORT
    TET_ER_2BIG = Rbtet_ext::TET_ER_2BIG
    TET_NULLFP = Rbtet_ext::TET_NULLFP

    # Wrappers to all the TET API calls
    def Rbtet.tet_exit(val)
        Rbtet_ext.tet_exit(val)
    end

    def Rbtet.tet_delete(val, str)
        Rbtet_ext.tet_delete(val, str)
    end

    def Rbtet.tet_getvar(str)
        return Rbtet_ext.tet_getvar(str)
    end

    def Rbtet.tet_infoline(str)
        Rbtet_ext.tet_infoline(str)
    end

    def Rbtet.tet_kill(pid, val)
        return Rbtet_ext.tet_kill(pid, val)
    end

    def Rbtet.tet_logoff()
        Rbtet_ext.tet_logoff()
    end

    def Rbtet.tet_minfoline(str, val)
        return Rbtet_ext.tet_minfoline(str, val)
    end

    def Rbtet.tet_printf(str, *args)
        return Rbtet_ext.tet_printf(str, *args)
    end

    def Rbtet.tet_reason(val)
        return Rbtet_ext.tet_reason(val)
    end

    def Rbtet.tet_remgetlist(val)
        return Rbtet_ext.tet_remgetlist(val)
    end

    def Rbtet.tet_remgetsys()
        return Rbtet_ext.tet_remgetsys()
    end

    def Rbtet.tet_result(val)
        Rbtet_ext.tet_result(val)
    end

    def Rbtet.tet_setblock()
        Rbtet_ext.tet_setblock()
    end

    def Rbtet.tet_setcontext()
        Rbtet_ext.tet_setcontext()
    end

    def Rbtet.tet_spawn(str, arr1, arr2)
        return Rbtet_ext.tet_spawn(str, arr1, arr2)
    end

    def Rbtet.tet_vprintf(str, *args)
        return Rbtet_ext.tet_vprintf(str, *args)
    end

    def Rbtet.tet_wait(pid, val)
        return Rbtet_ext.tet_wait(pid, val)
    end

    def Rbtet.tet_exec(str, arr1, arr2)
        return Rbtet_ext.tet_exec(str, arr1, arr2)
    end

    def Rbtet.tet_fork(ptr1, ptr2, val1, val2)
        return Rbtet_ext.tet_fork(ptr1, ptr2, val1, val2)
    end

    def Rbtet.tet_tcm_main(val, arr)
        Rbtet_ext.tet_tcm_main(val, arr)
    end

    def Rbtet.tet_merror(val1, arr, val2)
        Rbtet_ext.tet_merror(val1, arr, val2)
    end

    def Rbtet.tet_error(val, str)
        Rbtet_ext.tet_error(val, str)
    end

    def Rbtet.tet_thistest()
        Rbtet_ext.tet_thistest
    end

    # Class to hold the dynamic interface callbacks
    class DynInt

        def tet_gettp(icnum, tpnum)
            count = 0
            $testlist.keys.each do | testic |
                if (testic == icnum)
                    count += 1
                    if (count == tpnum)
                        return $testlist[testic]
                    end
                end
            end
            return ""
        end

        def tet_getminic()
            minic = $testlist.length()
            $testlist.keys.each do | testic |
                if (testic < minic)
                    minic = testic
                end
            end
            return minic
        end
        
        def tet_getmaxic()
            maxic = 1
            $testlist.keys.each do | testic |
                if (testic > maxic)
                    maxic = testic
                end
            end
            return maxic
        end
        
        def tet_isdefic(icnum)
            $testlist.keys.each do | testic |
                if (testic == icnum)
                    return 1
                end
            end
            return 0
        end
        
        def tet_gettpcount(icnum)
            count = 0
            $testlist.keys.each do | testic |
                if (testic == icnum)
                    count += 1
                end
            end
            return count
        end
        
        def tet_gettestnum(icnum, tpnum)
            count = icnum
            $testlist.keys.each do | testic |
                if (testic == icnum)
                    if count == icnum + tpnum - 1
                        break
                    end
                    count += 1
                end
            end
            return count
        end

    end
    
    # Testset super class. Hides some of the setup from the user
    class SuperTestSet

        @@testlist = Hash.new

        def SuperTestSet.getTestList
            return @@testlist
        end

    end

    def Rbtet.rbtet_init(recv, startup, cleanup)

        # Global testlist hash used above
        $testlist = TestSet.getTestList
         
        # Register the startup/cleanup callbacks
        if (startup != TET_NULLFP)
           Rbtet_ext.rbtet_set_rbfunc(recv, startup, Rbtet_ext::RBTET_STARTUP)
        end

        if (cleanup != TET_NULLFP)
           Rbtet_ext.rbtet_set_rbfunc(recv, cleanup, Rbtet_ext::RBTET_CLEANUP)
        end

        # Need to register the testset instance
        Rbtet_ext.rbtet_set_rbfunc(recv, "", Rbtet_ext::RBTET_TESTSET)

        dyn = DynInt.new

        # Register the dynamic interface callbacks
        Rbtet_ext.rbtet_set_rbfunc(dyn, "tet_gettp", Rbtet_ext::RBTET_GETTP)
        Rbtet_ext.rbtet_set_rbfunc(dyn, "tet_getminic",
            Rbtet_ext::RBTET_GETMINIC)
        Rbtet_ext.rbtet_set_rbfunc(dyn, "tet_getmaxic", 
            Rbtet_ext::RBTET_GETMAXIC)
        Rbtet_ext.rbtet_set_rbfunc(dyn, "tet_isdefic", 
            Rbtet_ext::RBTET_ISDEFIC)
        Rbtet_ext.rbtet_set_rbfunc(dyn, "tet_gettpcount", 
            Rbtet_ext::RBTET_GETTPCOUNT)
        Rbtet_ext.rbtet_set_rbfunc(dyn, "tet_gettestnum", 
            Rbtet_ext::RBTET_GETTESTNUM)

        # Finally set the iclist (calls tet_main)
        Rbtet_ext.rbtet_set_iclist(ARGV.length, ARGV)

    end

end
