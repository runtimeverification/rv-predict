/* stat-tc.c : test case for stat() interface */

#include <stdio.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>

#include <tet_api.h>

static void tp1(), tp2(), tp3(), tp4(), tp5(), tp6(), tp7();
static void startup(), cleanup();

/* Initialize TCM data structures */
void (*tet_startup)() = startup;
void (*tet_cleanup)() = cleanup;
struct tet_testlist tet_testlist[] = {
    { tp1, 1 },
    { tp2, 2 },
    { tp3, 3 },
    { tp4, 4 },
    { tp5, 5 },
    { tp6, 6 },
    { tp7, 7 },
    { NULL, 0 }
};


/* Test Case Wide Declarations */
static char *tfile = "stat.1"; 		/* test file name */
static char *tndir = "stat.1/stat.1";	/* path with non-directory in prefix */
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
        tet_delete(7, reason);
    }
    else
	(void) close(fd);
}

static void
cleanup()
{
    /* remove file created by start-up */
    (void) unlink(tfile);

    /* remove files created by test purposes, in case they don't run
       to completion */
    (void) rmdir("stat.d");
    (void) unlink("stat.p");
}

static void
tp1()         /* successful stat of plain file: return 0 */
{
    int ret, err;

    tet_infoline("SUCCESSFUL STAT OF PLAIN FILE");

    /* stat file created in startup function and check mode indicates
       a plain file */

    errno = 0;
    if ((ret=stat(tfile, &buf)) != 0)
    {
        err = errno;
        (void) sprintf(msg, "stat(\"%s\", buf) returned %d, expected 0",
            tfile, ret);
        tet_infoline(msg);
        if (err != 0)
        {
            (void) sprintf(msg, "errno was set to %d", err);
            tet_infoline(msg);
        }
        tet_result(TET_FAIL);
    }
    else if (!S_ISREG(buf.st_mode))
    {
        tet_infoline("S_ISREG(st_mode) was not true for plain file");
        (void) sprintf(msg, "st_mode = 0%lo", (long)buf.st_mode);
        tet_infoline(msg);
        tet_result(TET_FAIL);
    }
    else
        tet_result(TET_PASS);
}

static void
tp2()         /* successful stat of directory: return 0 */
{
    int ret, err;
    char *tdir = "stat.d";

    tet_infoline("SUCCESSFUL STAT OF DIRECTORY");

    /* create a test directory */

    if (mkdir(tdir, S_IRWXU) == -1)
    {
        (void) sprintf(msg,
            "mkdir(\"%s\", S_IRWXU) failed in startup - errno %d",
	    tdir, errno);
        tet_infoline(msg);
        tet_result(TET_UNRESOLVED);
	return;
    }

    /* stat the directory and check mode indicates a directory */

    errno = 0;
    if ((ret=stat(tdir, &buf)) != 0)
    {
        err = errno;
        (void) sprintf(msg, "stat(\"%s\", buf) returned %d, expected 0",
            tdir, ret);
        tet_infoline(msg);
        if (err != 0)
        {
            (void) sprintf(msg, "errno was set to %d", err);
            tet_infoline(msg);
        }
        tet_result(TET_FAIL);
    }
    else if (!S_ISDIR(buf.st_mode))
    {
        tet_infoline("S_ISDIR(st_mode) was not true for directory");
        (void) sprintf(msg, "st_mode = 0%lo", (long)buf.st_mode);
        tet_infoline(msg);
        tet_result(TET_FAIL);
    }
    else
        tet_result(TET_PASS);
    
    (void) rmdir(tdir);
}

static void
tp3()         /* successful stat of FIFO file: return 0 */
{
    int ret, err;
    char *tfifo = "stat.p";

    tet_infoline("SUCCESSFUL STAT OF FIFO");

    /* create a test FIFO */

    if (mkfifo(tfifo, S_IRWXU) == -1)
    {
        (void) sprintf(msg,
            "mkfifo(\"%s\", S_IRWXU) failed in startup - errno %d",
	    tfifo, errno);
        tet_infoline(msg);
        tet_result(TET_UNRESOLVED);
	return;
    }

    /* stat the FIFO and check mode indicates a FIFO */

    errno = 0;
    if ((ret=stat(tfifo, &buf)) != 0)
    {
        err = errno;
        (void) sprintf(msg, "stat(\"%s\", buf) returned %d, expected 0",
            tfifo, ret);
        tet_infoline(msg);
        if (err != 0)
        {
            (void) sprintf(msg, "errno was set to %d", err);
            tet_infoline(msg);
        }
        tet_result(TET_FAIL);
    }
    else if (!S_ISFIFO(buf.st_mode))
    {
        tet_infoline("S_ISFIFO(st_mode) was not true for FIFO file");
        (void) sprintf(msg, "st_mode = 0%lo", (long)buf.st_mode);
        tet_infoline(msg);
        tet_result(TET_FAIL);
    }
    else
        tet_result(TET_PASS);
    
    (void) unlink(tfifo);
}

static void
tp4()         /* successful stat of character device file: return 0 */
{
    int ret, err;
    char *chardev;

    tet_infoline("SUCCESSFUL STAT OF CHARACTER DEVICE FILE");

    /* obtain device name from execution configuration parameter */

    chardev = tet_getvar("CHARDEV");
    if (chardev == NULL || *chardev == '\0')
    {
	tet_infoline("parameter CHARDEV is not set");
	tet_result(TET_UNRESOLVED);
	return;
    }

    /* check if parameter indicates character devices are not supported */

    if (strcmp(chardev, "unsup") == 0)
    {
	tet_infoline("parameter CHARDEV is set to \"unsup\"");
	tet_result(TET_UNSUPPORTED);
	return;
    }

    /* stat the device and check mode indicates a character device */

    errno = 0;
    if ((ret=stat(chardev, &buf)) != 0)
    {
        err = errno;
        (void) sprintf(msg, "stat(\"%s\", buf) returned %d, expected 0",
            chardev, ret);
        tet_infoline(msg);
        if (err != 0)
        {
            (void) sprintf(msg, "errno was set to %d", err);
            tet_infoline(msg);
        }
        tet_result(TET_FAIL);
    }
    else if (!S_ISCHR(buf.st_mode))
    {
        (void) sprintf(msg, "S_ISCHR(st_mode) was not true for \"%s\"",
	    chardev);
        tet_infoline(msg);
        (void) sprintf(msg, "st_mode = 0%lo", (long)buf.st_mode);
        tet_infoline(msg);
        tet_result(TET_FAIL);
    }
    else
        tet_result(TET_PASS);
}

static void
tp5()         /* successful stat of block device file: return 0 */
{
    int ret, err;
    char *blockdev;

    tet_infoline("SUCCESSFUL STAT OF BLOCK DEVICE FILE");

    /* obtain device name from execution configuration parameter */

    blockdev = tet_getvar("BLOCKDEV");
    if (blockdev == NULL || *blockdev == '\0')
    {
	tet_infoline("parameter BLOCKDEV is not set");
	tet_result(TET_UNRESOLVED);
	return;
    }

    /* check if parameter indicates block devices are not supported */

    if (strcmp(blockdev, "unsup") == 0)
    {
	tet_infoline("parameter BLOCKDEV is set to \"unsup\"");
	tet_result(TET_UNSUPPORTED);
	return;
    }

    /* stat the device and check mode indicates a block device */

    errno = 0;
    if ((ret=stat(blockdev, &buf)) != 0)
    {
        err = errno;
        (void) sprintf(msg, "stat(\"%s\", buf) returned %d, expected 0",
            blockdev, ret);
        tet_infoline(msg);
        if (err != 0)
        {
            (void) sprintf(msg, "errno was set to %d", err);
            tet_infoline(msg);
        }
        tet_result(TET_FAIL);
    }
    else if (!S_ISBLK(buf.st_mode))
    {
        (void) sprintf(msg, "S_ISBLK(st_mode) was not true for \"%s\"",
	    blockdev);
        tet_infoline(msg);
        (void) sprintf(msg, "st_mode = 0%lo", (long)buf.st_mode);
        tet_infoline(msg);
        tet_result(TET_FAIL);
    }
    else
        tet_result(TET_PASS);
}

static void
tp6()       /* stat of non-existent file: return -1, errno ENOENT */
{
    int ret, err;

    tet_infoline("STAT OF NON-EXISTENT FILE");

    /* ensure file does not exist */

    if (stat("stat.6", &buf) != -1 && unlink("stat.6") == -1)
    {
        tet_infoline("could not unlink stat.6");
        tet_result(TET_UNRESOLVED);
        return;
    }

    /* check return value and errno set by call */

    errno = 0;
    ret = stat("stat.6", &buf);

    if (ret != -1 || errno != ENOENT)
    {
        err = errno;
        if (ret != -1)
        {
            (void) sprintf(msg,
                "stat(\"stat.6\", 0) returned %d, expected -1", ret);
            tet_infoline(msg);
        }

        if (err != ENOENT)
        {
            (void) sprintf(msg,
                "stat(\"stat.6\", 0) set errno to %d, expected %d (ENOENT)",
                err, ENOENT);
            tet_infoline(msg);
        }

        tet_result(TET_FAIL);
    }
    else
        tet_result(TET_PASS);
}

static void
tp7()       /* non-directory path component: return -1, errno ENOTDIR */
{
    int ret, err;

    tet_infoline("STAT OF NON-DIRECTORY PATH PREFIX COMPONENT");

    /* tndir is a pathname containing a plain file (created by the
       startup function) in the prefix */

    errno = 0;
    ret = stat(tndir, &buf);

    /* check return value and errno set by call */

    if (ret != -1 || errno != ENOTDIR)
    {
        err = errno;
        if (ret != -1)
        {
            (void) sprintf(msg,
                "stat(\"%s\", 0) returned %d, expected -1", tndir, ret);
            tet_infoline(msg);
        }

        if (err != ENOTDIR)
        {
            (void) sprintf(msg,
                "stat(\"%s\", 0) set errno to %d, expected %d (ENOTDIR)",
                tndir, err, ENOTDIR);
            tet_infoline(msg);
        }

        tet_result(TET_FAIL);
    }
    else
        tet_result(TET_PASS);
}
