/* The RV-Predict/C runtime used to crash when ifuncs ran before the
 * runtime had initialized.  This was documented in issue #982 by Maya.
 * She wrote this minimal test case, too.
 */

#include <stdio.h>

void php_base64_encode(void)
    __attribute__((ifunc("resolve_base64_encode")));

void
php_base64_encode_default(void)
{
	printf("default\n");
	return;
}

void *
resolve_base64_encode(void)
{
	printf("resolving\n");
	return php_base64_encode_default;
}

int
main(void)
{
	php_base64_encode();
}
