# Introduction

This is the ergonomics guide for C code in the Predict
project.  It incorporates [The NetBSD source code style
guide](https://nxr.netbsd.org/xref/src/share/misc/style) in its entirety.
Where the rules in this guide differ from the NetBSD guide, follow
this guide.

I call this an "ergonomics guide" rather than a "style guide" because the
idea is to avoid writing code that unduly burdens the person who has to
read or modify it, not to enforce an expression of somebody's personal
taste.  Where there is not reliable evidence or an established design
principle to inform the guidance, I rely on the established conventions
of the BSD community.

Insofar as C and C++ are alike, this guide also applies to the C++
code in the Predict project.

Not all of the code in the Predict project complies with this guidance.
The Predict code has been written over a period of years by different
persons following different standards.
Furthermore, the project also uses 3rd-party source code---most of that
software is under `external/`---that it is undesirable to reformat.

Virtually all of the code in the runtime (under `llvm/ngrt/`)
complies with this guide.  Older C++ source code in the instrumentation
(`llvm/pass/`) appears to comply with the LLVM guidance.  New code in
the instrumentation must comply with this guidance.  The old code may
be brought into compliance on an as-needed basis.

# Declarations

# Comments

Try to write your code so that it does not require a lot of comments for
a moderately experienced C programmer to understand.  If you must write
some clever code, explain it with a comment.  Use comments to describe
the purpose of your code and to give an overview of how it works, but
do not write comments for minutiae like `i++; // increase i by 1`.

# Identifiers

Variable identifiers are in lower case.  Multiword identifiers should not
use MixedCapitals.  Rather, words should be separated with underscores.
Identifiers should have meaningful names unless their functional role
is obvious from the context.  For example, it's not necessary to name
the index in a for-loop `index_of_current_doodad`.  Just call it `i`.

# Use of whitespace

## Indentation

Indent using tabs.  Every tab position is 8 columns right of the previous
tab position or the left margin.  If you find yourself running out of
horizontal space on a page of code, consider shortening staircases or
extracting a subroutine.

To continue a line, indent to the level of the line you are continuing,
and then type 4 spaces.  You may use more spaces to align parentheses
and such, if it improves the readability of the code:

```
	if ((x + y) > 7 &&
	    (p + q) < 5)
		stmt;
```

In if-, while-, and for-statements, there is a space between the
keyword and the open parenthesis.  Following the close parenthesis,
there is a space followed by the opening curly brace, if any.
You do not have to enclose a single statement in curly braces if
the statement fits on one line.  This is ok:

```
	if (condition) {
		stmt1;
		stmt2;
	}

	if (condition) {
		stmt1;
	}
```

This is also ok:

```
	if (condition)
		stmt;
```

This is not ok, because the statement spans two lines:

```
	if (condition)
		f(arg1, arg2, arg3, arg4, arg5, arg6, arg7,
		  arg8, arg9, arg10, arg11);
```

If there is a curly brace left or right of an `else` keyword,
put it on the same line as the `else` with a space between:

```
	if (condition1) {
		stmt1;
		stmt2;
	} else if (condition2) {
		stmt3;
		stmt4;
	} else {
		stmt5;
	}
```

Only the closing brace in an if-, for-, or while-statement, or the
closing brace after the last `else` in an `if-else*` chain, may be on
its own line.

Try to avoid adding whitespace at the ends of lines.

Separate operators from their arguments with a single space.
This is ok:

```
	y = a * x + b;
```

This is not:

```
	y=a*x+b;
```

Neither is this:

```
	for (i=0; i<5; i++)
		stmt;
```

# Using the C preprocessor

## Writing preprocessor directives

Preprocessor directives start with a `#`.  The `#` should always be in
column 1.  To indent a preprocessor directive, insert tabs between
the `#` and the subsequent text.  Do not indent a preprocessor directive
unless it is "nested".  This is not ok:

```
#	ifdef XYZ
```

This is ok:

```
#ifdef ABC
#	ifdef XYZ
```

## Conditional compilation

Avoid using conditional compilation (#ifdef, et cetera) to configure
the project.  Frequently it is possible to configure the project using
conditional statements in a makefile.

If you must use conditional compilation, then try to consolidate all of
the conditionally-compiled code into as few consecutive lines as possible.
Consider extracting a subroutine---instead of this:

```
void
f(void)
{
	stmt1;
#ifdef FLAG
	optional_stmt1;
	optional_stmt2;
	optional_stmt3;
#endif
	stmt2;
#ifdef FLAG
	optional_stmt4;
	optional_stmt5;
	optional_stmt6;
#endif
	stmt3;
}
```

Write this:

```
#ifdef FLAG
static void
optional_behavior1(void)
{
	optional_stmt1;
	optional_stmt2;
	optional_stmt3;
}

static void
optional_behavior1(void)
{
	optional_stmt4;
	optional_stmt5;
	optional_stmt6;
}
#else
#define optional_behavior1()	do { } while (true)
#define optional_behavior2()	do { } while (true)
#endif

void
f(void)
{
	stmt1;
	optional_behavior1();
	stmt2;
	optional_behavior2();
	stmt3;
}
```
