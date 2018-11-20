/* Ensure that errno is 0 at program startup. This is stated in the
 * C standard. This was originally issue #1031.
 */

#include <errno.h>

int
main(void)
{
	return errno;
}
