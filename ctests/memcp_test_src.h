/*
 * Source for the memmov, memset, and memcpy tests
 * About ...
 *   This code file is used in two basic ways:
 * 1. An exhaustive test of many cases which generates a huge expect.out file.
 *   This version is in the regression directory. 'short_expect_out' is defined
 * 2. A version that generates a short expect.out that lets mere mortals visuall 
 *  inspect the trace and verify that all the necessary values are inserted into
 *  the ring. 'short_expect_out' is undef'ed
 * 
 * This file is included by a .c that sets one of these define options:
 *     MEMcpy   -tests the memcpy routine
 *     MEMmov   -tests the memcpy routine
 *
 * The define option short_expect_out may also be set.
 *
 */ 
#include <string.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"

#if short_expect_out == 1
  char cc [19];
  int iidst  []={1,7};
  int iisrc  []={3,7};
  int lngths []={1,5,9};

#  define nzfrstinx zmin(dstinx, srcinx) 		 /* index of 1st non-zero */
#  define nzlastinx zmax((dstinx+n2move),(srcinx+n2move)) /* index of last non-zero */

#else
 /* Settings for exhaustive test
  * The trace for these variables helps analyze the trace */
  char cc [29];
  int iidst  []={0,1,2,3,4,6,7,8};
  int iisrc  []={0,1,2,3,5,7};
  int lngths []={1,5,9,17};

#endif

#define n_iidst  (sizeof(iidst)/sizeof(int))
#define n_iisrc  (sizeof(iisrc)/sizeof(int))
#define n_lngths (sizeof(lngths)/sizeof(int))

# define zmin(hi,lo) (((hi) > (lo))? (lo) :(hi)   )
# define zmax(hi,lo) ( (hi) > (lo) ? (hi) :(lo)   )

/*
 * Error reporting within the testit function.
 */
#define errx(str,qq) err_out_proc(str,srcinx,dstinx,n2move,qq)
int 
err_out_proc(char* sss,int s,int d,int l,int qq)
{
	printf("mem* Fail:src=%d,dst=%d,lngth=%d,c=%2x,qq=%d %s"
			,s,d,l,cc[qq],qq,sss);
	return EXIT_FAILURE;
}

int
testit(int dstinx, int srcinx, int n2move)
{
	char c = 0x41 + srcinx; /* a,b,c,... */
#if short_expect_out == 1

	if(dstinx == srcinx)
		return EXIT_SUCCESS;
#else
	/* Observing these values in the trace will help track down problems */
 	int nzfrstinx = zmin(dstinx, srcinx);
	int nzlastinx = zmax( (dstinx+n2move-1) , (srcinx+n2move-1) );
	int ii;
	int lmin   = zmin(srcinx,dstinx);
	int lmax   = zmax(srcinx,dstinx);
	int n_in_middle  = zmax (0,lmax-lmin-n2move);

#endif

	/*
         * clear the cc array 
 	 */
	memset(&cc[0], 0, sizeof(cc)*sizeof(char));
	/*
	 * Note we always write to srcinc, this is then copied/move
 	 * but for memset, srcinx is also the destination
	 */
	memset(&cc[srcinx], c, n2move*sizeof(char));

#	ifdef MEMcpy
		memcpy(&cc[dstinx],&cc[srcinx], n2move*sizeof(char));
#	endif
#	ifdef MEMmove
		memmove(&cc[dstinx],&cc[srcinx], n2move*sizeof(char));
#	endif

	/* data transfer complete, now check result */
#if short_expect_out == 1
	/* Since the test is suppossed to produce short output, we do minimum check */
        if(cc[srcinx] != c || cc[srcinx+n2move-1] !=c)
			return errx("1:incomplete move/set",srcinx);
#else
	/* cc should contain:
	 * 1. zero or more bytes of 0x00 
	 * 2. at least  n2move' bytes of <c> and possibly an area of zeros in a middle region
	 * 3. Some trailing bytes of 0x00
         */ 
	/*
	 * verify leading zeros if any
 	 */
	if(nzfrstinx>0)
	   for(ii=0;ii<nzfrstinx;ii++){
		if(cc[ii] != 0)
			return errx("2:should be non-zero",ii);
		}
	/*
	 * verify the proper number of non-zeros 
   	 */
	if(n_in_middle>0)
	   for(ii=nzfrstinx;ii<=nzlastinx;ii++){
		if(cc[ii] != c)
			n_in_middle--;
	}
        if(n_in_middle != 0)
			return errx("3:Missing middle zeros",n_in_middle);
	/* Make sure that too many didn't get copied */
	if(cc[nzlastinx+1] != 0)
			return errx("4:too many copied/set",ii);
#endif
	return EXIT_SUCCESS;
}

int testdriver(void)
{
	int ii, jj, kk, rr, lcsrc; 

	for(ii=0;ii<n_iidst;ii++)
#ifndef MEMset
		for(jj=0;jj<n_iisrc;jj++)
#endif
			for(kk=0;kk<n_lngths;kk++) 
#if short_expect_out == 1
			  if( ( (ii+jj)!=0 ) && ( kk==0              /* Only do 1 byte transfer once */
			                        ||( (kk==n_lngths-1) /* Use longest length once only */
    				                )
                              )
      			     )
				{ /* Skip some tests */}
			  else
#endif
              		  {
#			ifdef MEMset	/* With memset the src and destination are the same */
				jj = ii;
				lcsrc = iidst[jj];
#			else
				lcsrc = iisrc[jj];
#			endif
				rr =  testit(iidst[ii],lcsrc,lngths[kk]); 
				if( rr == EXIT_FAILURE)
					return rr;
			  }

	return rr;

}
int
main(void)
{
	int rr;
	rr = testdriver();
	return rr;
}
