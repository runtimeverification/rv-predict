<?
function phptet_gettp($icnum, $tpnum)
{
    global $php_testlist;
    global $tet_thistest;
    global $tet_pname;
    global $tet_errno;
    global $tet_nerr;
    global $tet_nosigreset;
    global $tet_block;
    global $tet_sequence;
    
    # Initialise globals for use in the tests
    $tet_thistest = tet_thistest_get();
    $tet_pname = tet_pname_get();
    $tet_errno = tet_errno_get();
    $tet_nerr = tet_nerr_get();
    $tet_nosigreset = tet_nosigreset_get();
    $tet_block = tet_block_get();
    $tet_sequence = tet_sequence_get();

    $count = 0;
    foreach ($php_testlist as $testic => $value)
    {
        if ($testic == $icnum)
        {
            $count = $count + 1;
            if ($count == $tpnum)
            {
                return $value;
            }
        }
    }
    return "";
}

function phptet_getminic()
{
    global $php_testlist;

    $minic = count($php_testlist);
    foreach ($php_testlist as $testic => $value)
    {
        if ($testic < $minic)
        {
            $minic = $testic;
        }
    }
    return $minic;
}

function phptet_getmaxic()
{
    global $php_testlist;

    $maxic = 1;
    foreach ($php_testlist as $testic => $value)
    {
        if ($testic > $maxic)
        {
            $maxic = $testic;
        }
    }

    return $maxic;
}

function phptet_isdefic($icnum)
{
    global $php_testlist;

    foreach ($php_testlist as $testic => $value)
    {
        if ($testic == $icnum)
        {
            return 1;
        }
    }
    return 0;
}

function phptet_gettpcount($icnum)
{
    global $php_testlist;

    $count = 0;
    foreach ($php_testlist as $testic => $value)
    {
        if ($testic == $icnum)
        {
            $count += 1;
        }
    }
    return $count;
}

function phptet_gettestnum($icnum, $tpnum)
{
    global $php_testlist;

    $count = $icnum;
    foreach ($php_testlist as $testic => $value)
    {
        if ($testic == $icnum)
        {
		    if ($count == $icnum + $tpnum - 1)
            {
			    break;
            }
            $count += 1;
        }
    }
    return $count;
}

function phptet_init($tlist, $startup, $cleanup)
{
    global $argv;
    global $php_testlist;

    $php_testlist = $tlist;

    if ($startup !== TET_NULLFP)
        phptet_set_func($startup, PHPTET_STARTUP);
    if ($cleanup !== TET_NULLFP)
        phptet_set_func($cleanup, PHPTET_CLEANUP);
    phptet_set_func("phptet_gettp", PHPTET_GETTP);
    phptet_set_func("phptet_getminic", PHPTET_GETMINIC);
    phptet_set_func("phptet_getmaxic", PHPTET_GETMAXIC);
    phptet_set_func("phptet_isdefic", PHPTET_ISDEFIC);
    phptet_set_func("phptet_gettpcount", PHPTET_GETTPCOUNT);
    phptet_set_func("phptet_gettestnum", PHPTET_GETTESTNUM);
    phptet_set_iclist(count($argv), $argv);
}
?>
