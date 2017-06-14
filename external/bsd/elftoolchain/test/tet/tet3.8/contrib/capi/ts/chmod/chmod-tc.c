/* chmod-tc.c : test case for chmod() interface */

#include <stdio.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <tet_api.h>

static void tp1(), tp2(), tp3();
static void startup(), cleanup();

/* Initialize TCM data structures */
void (*tet_startup)() = startup;
void (*tet_cleanup)() = cleanup;
struct tet_testlist tet_testlist[] = {
    { tp1, 1 },
    { tp2, 2 },
    { tp3, 3 },
    { NULL, 0 }
};


/* Test Case Wide Declarations */
static char *tfile = "chmod.1"; 	/* test file name */
static char *tndir = "chmod.1/chmod.1";	/* path with non-directory in prefix */
static struct stat buf;			/* buffer for stat(ing) file */
static char msg[256];			/* buffer for info lines */

static void
startup()
{
    int fd;
    static char *reason = "Failed to create test file in startup";

    if ((fd=creat(tfile, S_IRWXU)) < 0)
    {
        (void) sprintf(msg,
            "creat(\"%s\", S_IRWXU) failed in startup - errno %d",
	    tfile, errno);
        tet_infoline(msg);

        /* Prevent tests which use this file from executing */
        tet_delete(1, reason);
        tet_delete(3, reason);
    }
    else
	(void) close(fd);
}

static void
cleanup()
{
    /* remove file created by start-up */
    (void) unlink(tfile);
}

static void
tp1()         /* successful chmod of file: return 0 */
{
    int ret, err;
    mode_t mode;

    tet_infoline("SUCCESSFUL CHMOD OF FILE");

    /* change mode of file created in startup function */

    errno = 0;
    if ((ret=chmod(tfile, (mode_t)0)) != 0)
    {
        err = errno;
        (void) sprintf(msg, "chmod(\"%s\", 0) returned %d, expected 0",
            tfile, ret);
        tet_infoline(msg);
        if (err != 0)
        {
            (void) sprintf(msg, "errno was set to %d", err);
            tet_infoline(msg);
        }
        tet_result(TET_FAIL);
        return;
    }

    /* check mode was changed correctly */

    if (stat(tfile, &buf) == -1)
    {
        (void) sprintf(msg,
            "stat(\"%s\", buf) failed - errno %d", tfile, errno);
        tet_infoline(msg);
        tet_result(TET_UNRESOLVED);
        return;
    }

    mode = buf.st_mode & O_ACCMODE;
    if (mode != 0)
    {
        (void) sprintf(msg, "chmod(\"%s\", 0) set mode to 0%lo, expected 0",
            tfile, (long)mode);
        tet_infoline(msg);
        tet_result(TET_FAIL);
    }
    else
        tet_result(TET_PASS);
}

static void
tp2()       /* chmod of non-existent file: return -1, errno ENOENT */
{
    int ret, err;

    tet_infoline("CHMOD OF NON-EXISTENT FILE");

    /* ensure file does not exist */

    if (stat("chmod.2", &buf) != -1 && unlink("chmod.2") == -1)
    {
        tet_infoline("could not unlink chmod.2");
        tet_result(TET_UNRESOLVED);
        return;
    }

    /* check return value and errno set by call */

    errno = 0;
    ret = chmod("chmod.2", (mode_t)0);

    if (ret != -1 || errno != ENOENT)
    {
        err = errno;
        if (ret != -1)
        {
            (void) sprintf(msg,
                "chmod(\"chmod.2\", 0) returned %d, expected -1", ret);
            tet_infoline(msg);
        }

        if (err != ENOENT)
        {
            (void) sprintf(msg,
                "chmod(\"chmod.2\", 0) set errno to %d, expected %d (ENOENT)",
                err, ENOENT);
            tet_infoline(msg);
        }

        tet_result(TET_FAIL);
    }
    else
        tet_result(TET_PASS);
}

static void
tp3()       /* non-directory path component: return -1, errno ENOTDIR */
{
    int ret, err;

    tet_infoline("CHMOD OF NON-DIRECTORY PATH PREFIX COMPONENT");

    /* tndir is a pathname containing a plain file (created by the
       startup function) in the prefix */

    errno = 0;
    ret = chmod(tndir, (mode_t)0);

    /* check return value and errno set by call */

    if (ret != -1 || errno != ENOTDIR)
    {
        err = errno;
        if (ret != -1)
        {
            (void) sprintf(msg,
                "chmod(\"%s\", 0) returned %d, expected -1", tndir, ret);
            tet_infoline(msg);
        }

        if (err != ENOTDIR)
        {
            (void) sprintf(msg,
                "chmod(\"%s\", 0) set errno to %d, expected %d (ENOTDIR)",
                tndir, err, ENOTDIR);
            tet_infoline(msg);
        }

        tet_result(TET_FAIL);
    }
    else
        tet_result(TET_PASS);
}
