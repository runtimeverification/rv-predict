/*
 *	SCCS: @(#)svector.c	1.2 (02/11/06)
 *
 * Copyright (c) 2000 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 */

#ifndef lint
static char sccsid[] = "@(#)svector.c	1.2 (02/11/06) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)svector.c	1.2 02/11/06 TETware release 3.8
NAME:		svector.c
PRODUCT:	TETware Generic Report Writer.
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	19 July 2000
SYNOPSIS:

	grw_svector_t * grw_createsvector(int allocincrement)
	int grw_getsize(grw_svector_t *vec)
	char ** grw_getentries(grw_svector_t *vec)
	char * grw_findentry(grw_svector_t *vec, char *entry)
	char * grw_addentry(grw_svector_t *vec, char *entry)
	void grw_setentry(grw_svector_t *vec, int idx, char *entry)
	void grw_insertentry(grw_svector_t *vec, int idx, char *entry)
	void grw_emptysvector(grw_svector_t *vec)
	void grw_freesvector(grw_svector_t *vec)

DESCRIPTION:

	Code to implement a vector of strings, i.e. a growable array.

************************************************************************/

#include <stdlib.h>
#include <string.h>
#include "grw.h"


/*
 * The string vector structure.
 */
struct grw_svector
{
	/* Allocated size */
	int nalloced;

	/* Increment for increasing allocation size of `entries' array */
	int allocincrement;

	/* Number of entries */
	int nents;

	/* Array of entries */
	char **entries;
};


/*
 * grw_createsvector()
 *
 * Create a new string vector.
 *
 *	allocincremement	Number of entries to add each time the vector
 *				needs to expand.
 *
 * Returns a pointer to the new vector.
 */
grw_svector_t *
grw_createsvector(int allocincrement)
{
	struct grw_svector *vec;

	vec = grw_malloc(sizeof(*vec));
	vec->nalloced = allocincrement;
	vec->allocincrement = allocincrement;
	vec->nents = 0;
	vec->entries = grw_malloc(sizeof(*vec->entries) * vec->nalloced);

	return vec;
}


/*
 * grw_getsize()
 *
 * Get the size of a string vector.
 *
 *	vec	The string vector.
 *
 * Returns the size of the specified vector, i.e. the number of entries it
 * contains.
 */
int
grw_getsize(grw_svector_t *vec)
{
	return vec->nents;
}


/*
 * grw_getentries()
 *
 * Get the entries of a string vector.
 *
 *	vec	The string vector.
 *
 * Returns the entries of the specified vector.
 */
char **
grw_getentries(grw_svector_t *vec)
{
	return vec->entries;
}


/*
 * grw_findentry()
 *
 * Find an entry in a string vector.
 *
 *	vec	The string vector.
 *	entry	The entry to find.
 *
 * Returns the entry, or NULL if the entry is not present in the vector.
 */
char *
grw_findentry(grw_svector_t *vec, char *entry)
{
	char **ep;

	for (ep = vec->entries; ep < vec->entries + vec->nents; ep++)
	{
		if (strcmp(*ep, entry) == 0)
			return *ep;
	}

	return NULL;
}


/*
 * grw_addentry()
 *
 * Add an entry to a string vector. If the entry is already in the vector, it
 * is not added a second time. If it is not in the vector, it is copied and
 * added into the vector, i.e. new memory is allocated and the string
 * duplicated.
 *
 *	vec	The string vector.
 *	entry	The entry to be added.
 * 
 * The entry in the vector. If the entry already existed in the table, then
 * a pointer to it is returned; if it did not, a pointer to the new entry is
 * returned. 
 */
char *
grw_addentry(grw_svector_t *vec, char *entry)
{
	char *ent;

	/* If the entry is in the table already, then return it */
	ent = grw_findentry(vec, entry);
	if (ent != NULL)
		return ent;

	/* Increase the number of elements allocated, if necessary */
	if (vec->nents + 1 >= vec->nalloced)
	{
		vec->nalloced += vec->allocincrement;
		vec->entries = grw_realloc(vec->entries,
			vec->nalloced * sizeof(*vec->entries));
	}

	/* Add the new entry */
	vec->entries[vec->nents++] = grw_strdup(entry);

	return vec->entries[vec->nents - 1];
}


/*
 * grw_setentry()
 *
 * Set the string vector entry at a particular index to a new value.
 *
 *	vec	The string vector.
 *	idx	The index in the vector which is to be set.
 *		An invalid index value is considered a fatal error, and
 *		grw_fatal() is called which terminates the process.
 *	entry	The new value of the entry.
 *		Note that the new entry is added into the vector by
 *		duplication, i.e. new memory is allocated and the string
 *		duplicated.
 */
void
grw_setentry(grw_svector_t *vec, int idx, char *entry)
{
	/* Check for illegal index */
	if (idx < 0 || idx >= vec->nents)
		grw_fatal("illegal index passed to grw_setentry()");

	/* Free the existing entry */
	if (vec->entries[idx] != NULL)
		free(vec->entries[idx]);

	/* Add the new entry */
	vec->entries[idx] = grw_strdup(entry);
}


/*
 * grw_insertentry()
 *
 * Insert and entry into a string vector.
 *
 *	vec	The string vector.
 *	idx	The index at which the entry is to be inserted.
 *		The existing entries from idx upwards will be moved.
 *		An invalid index value is considered a fatal error, and
 *		grw_fatal() is called which terminates the process.
 *	entry	The entry to insert.
 *		Note that the new entry is added into the vector by
 *		duplication, i.e. new memory is allocated and the string
 *		duplicated.
 */
void
grw_insertentry(grw_svector_t *vec, int idx, char *entry)
{
	/* Check for illegal index */
	if (idx < 0 || idx > vec->nents)
		grw_fatal("illegal index passed to grw_insertentry()");

	/* Increase the number of elements allocated, if necessary */
	if (vec->nents + 1 >= vec->nalloced)
	{
		vec->nalloced += vec->allocincrement;
		vec->entries = grw_realloc(vec->entries,
			vec->nalloced * sizeof(*vec->entries));
	}

	/* If this entry is not being added at the end of the vector, move
	 * the existing entries up by one.
	 * N.B. memmove() is used rather than memcpy(), since the memory blocks
	 * will overlap if more than one entry is being moved.
	 */
	if (idx < vec->nents)
	{
		memmove(vec->entries + idx + 1, vec->entries + idx,
			(vec->nents - idx) * sizeof(*vec->entries));
	}

	/* Add the new entry */
	vec->entries[idx] = grw_strdup(entry);
	vec->nents++;
}


/*
 * grw_emptysvector()
 *
 * Empty a string vector. Frees the memory used by the entries and sets the
 * entry count to zero.
 *
 *	vec	The string vector.
 */
void
grw_emptysvector(grw_svector_t *vec)
{
	while (vec->nents > 0)
		free(vec->entries[--vec->nents]);
}


/*
 * grw_freesvector()
 *
 * Free the memory used by a string vector.
 *
 *	vec	The string vector.
 */
void
grw_freesvector(grw_svector_t *vec)
{
	grw_emptysvector(vec);
	free(vec->entries);
	free(vec);
}
