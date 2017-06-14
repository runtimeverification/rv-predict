/* fileno-tc.c : test case for fileno() interface */

#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

#include <tet_api.h>

extern char **environ;

static void cleanup();
static void tp1(), tp2(), tp3(), tp4(), ch4();

/* Initialize TCM data structures */
void (*tet_startup)() = NULL;
void (*tet_cleanup)() = cleanup;
struct tet_testlist tet_testlist[] = {
    { tp1, 1 },
    { tp2, 2 },
    { tp3, 3 },
    { tp4, 4 },
    { NULL, 0 }
};

/* Test Case Wide Declarations */
static char msg[256];			/* buffer for info lines */

static void
cleanup()
{
    (void) unlink("fileno.1");
    (void) unlink("fileno.4");
}

static void
tp1()         /* successful fileno: return fd associated with stream */
{
    FILE *fp;
    struct stat buf1, buf2;
    
    tet_infoline("FD RETURNED BY FILENO REFERS TO FILE OPEN ON STREAM");

    /* open stream to test file */

    if ((fp=fopen("fileno.1", "w")) == NULL)
    {
        (void) sprintf(msg, "fopen(\"fileno.1\", \"w\") failed - errno %d",
	    errno);
        tet_infoline(msg);
        tet_result(TET_UNRESOLVED);
        return;
    }

    /* check device and inode numbers from file descriptor associated
       with the stream match those from the file itself */

    if (stat("fileno.1", &buf1) == -1)
    {
        (void) sprintf(msg, "stat(\"fileno.1\", buf1) failed - errno %d",
            errno);
        tet_infoline(msg);
        tet_result(TET_UNRESOLVED);
	return;
    }
        
    if (fstat(fileno(fp), &buf2) == -1)
    {
        (void) sprintf(msg, "fstat(fileno(fp), buf2) failed - errno %d",
	    errno);
        tet_infoline(msg);
        tet_result(TET_FAIL);
    }
    else if (buf1.st_ino != buf2.st_ino || buf1.st_dev != buf2.st_dev)
    {
        tet_infoline("fileno(fp) does not refer to same file as fp");
        (void) sprintf(msg, "st_dev, st_ino of file: 0x%lx, %ld",
            (long)buf1.st_dev, (long)buf1.st_ino);
        tet_infoline(msg);
        (void) sprintf(msg, "st_dev, st_ino of fileno(fp): 0x%lx, %ld",
            (long)buf2.st_dev, (long)buf2.st_ino);
        tet_infoline(msg);
        tet_result(TET_FAIL);
    }
    else
	tet_result(TET_PASS);

    (void) fclose(fp);
}

static void
tp2()         /* fileno on stdin/stdout/stderr: return 0/1/2 */
{
    int	fd, fail = 0;

    tet_infoline("FILENO ON STDIN/STDOUT/STDERR");

    /* check return value of fileno() for stdin/stdout/stderr */
    /* this code relies on the fact that the TCM does not interfere
       with these streams */

    if ((fd = fileno(stdin)) != 0)
    {
        (void) sprintf(msg, "fileno(stdin) returned %d, expected 0", fd);
        tet_infoline(msg);
        tet_result(TET_FAIL);
        fail = 1;
    }

    if ((fd = fileno(stdout)) != 1)
    {
        (void) sprintf(msg, "fileno(stdout) returned %d, expected 1", fd);
        tet_infoline(msg);
        tet_result(TET_FAIL);
        fail = 1;
    }

    if ((fd = fileno(stderr)) != 2)
    {
        (void) sprintf(msg, "fileno(stderr) returned %d, expected 2", fd);
        tet_infoline(msg);
        tet_result(TET_FAIL);
        fail = 1;
    }

    if (fail == 0)
    	tet_result(TET_PASS);
}

static void
tp3()         /* on entry to main(), stdin is readable, stdout and stderr
		 are writable */
{
    int	flags, fail = 0;

    tet_infoline("ON ENTRY TO MAIN, STDIN IS READABLE, STDOUT AND STDERR ARE WRITABLE");
    /* this code relies on the fact that the TCM does not interfere
       with these streams */

    /* check file descriptor associated with stdin is readable */

    if ((flags = fcntl(fileno(stdin), F_GETFL)) == -1)
    {
        (void) sprintf(msg, "fcntl(fileno(stdin), F_GETFL) failed - errno %d",
            errno);
        tet_infoline(msg);
	tet_result(TET_UNRESOLVED);
	return;
    }

    flags &= O_ACCMODE;
    if (flags != O_RDONLY && flags != O_RDWR)
    {
        tet_infoline("stdin is not readable");
	fail = 1;
    }

    /* check file descriptor associated with stdout is writable */

    if ((flags = fcntl(fileno(stdout), F_GETFL)) == -1)
    {
        (void) sprintf(msg, "fcntl(fileno(stdout), F_GETFL) failed - errno %d",
            errno);
        tet_infoline(msg);
	tet_result(TET_UNRESOLVED);
	return;
    }

    flags &= O_ACCMODE;
    if (flags != O_WRONLY && flags != O_RDWR)
    {
        tet_infoline("stdout is not writable");
	fail = 1;
    }

    /* check file descriptor associated with stderr is writable */

    if ((flags = fcntl(fileno(stderr), F_GETFL)) == -1)
    {
        (void) sprintf(msg, "fcntl(fileno(stderr), F_GETFL) failed - errno %d",
            errno);
        tet_infoline(msg);
	tet_result(TET_UNRESOLVED);
	return;
    }

    flags &= O_ACCMODE;
    if (flags != O_WRONLY && flags != O_RDWR)
    {
        tet_infoline("stderr is not writable");
	fail = 1;
    }

    if (fail == 0)
        tet_result(TET_PASS);
    else
        tet_result(TET_FAIL);
}

static void
tp4()         /* on entry to main(), stream position of stdin, stdout and
		 stderr is same as fileno(stream) */
{
    tet_infoline("ON ENTRY TO MAIN, STREAM POSITION OF STDIN, STDOUT AND STDERR");

    /* fork and execute subprogram, so that unique file positions can be
       set up on entry to main() in subprogram */

    (void) tet_fork(ch4, TET_NULLFP, 30, 0);
}

static void
ch4()
{
    int	fd, ret;
    static char *args[] = { "./fileno-t4", NULL };

    /* set up file positions to be inherited by stdin/stdout/stderr
       in subprogram */

    for (fd = 0; fd < 3; fd++)
    {
        (void) close(fd);
        if ((ret=open("fileno.4", O_RDWR|O_CREAT, S_IRWXU)) != fd)
        {
            (void) sprintf(msg, "open() returned %d, expected %d", ret, fd);
            tet_infoline(msg);
            tet_result(TET_UNRESOLVED);
	    return;
        }
        if (lseek(fd, (off_t)(123 + 45*fd), SEEK_SET) == -1)
        {
            (void) sprintf(msg, "lseek() failed - errno %d", errno);
            tet_infoline(msg);
            tet_result(TET_UNRESOLVED);
	    return;
        }
    }

    /* execute subprogram to carry out remainder of test */

    (void) tet_exec(args[0], args, environ);

    (void) sprintf(msg, "tet_exec(\"%s\", args, env) failed - errno %d",
        args[0], errno);
    tet_infoline(msg);
    tet_result(TET_UNRESOLVED);
}
