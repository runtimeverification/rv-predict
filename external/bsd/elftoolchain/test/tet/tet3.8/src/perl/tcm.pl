# SCCS: @(#)tcm.pl	1.10 (05/11/29) TETware release 3.8
#
# Copyright 2002-2005 The Open Group
#
# Copyright 1992 SunSoft, Inc.
#
# Permission to use, copy, modify, and distribute this software and its
# documentation for any purpose and without fee is hereby granted, provided
# that the above copyright notice appear in all copies and that both that
# copyright notice and this permission notice appear in supporting
# documentation, and that the name of SunSoft, Inc. not be used in 
# advertising or publicity pertaining to distribution of the software 
# without specific, written prior permission.  SunSoft, Inc. makes
# no representations about the suitability of this software for any purpose.
# It is provided "as is" without express or implied warranty.
#
# SunSoft, Inc. DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
# INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
# EVENT SHALL SunSoft Inc. BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
# CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
# USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
# OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
# PERFORMANCE OF THIS SOFTWARE.
#
# Modifications:
#
# June 3rd 1993, Update version number to 1.10.1
#
# July 1st 1993, Code review cleanup
#
# November 1st 1993, Update version number to 1.10.2
#
# March 29th 1994, Update version number to 1.10.3
#
# December 1994, A.Josey, Novell USG
# Update line 185 @main to be \@main for perl5.000 
#
# December 1994, A.Josey, Novell USG
# Update for backwards compatibility to base TET1.10 with handling
# of the tet_xres file. In TET_EXTENDED=T/t mode handle the journal
# file using the ETET style. 
#
# August 1996, A.Josey, X/Open
# For TETware, journal handling defaults to tet_xres only
# for the moment 
# Update the version number to 3.0a
#
# October 1996, Geoff Clare, UniSoft Ltd.
# Moved uname stuff to make_tcm.pl (for Windows NT)
#
# June 1997, Andrew Dingwall, UniSoft Ltd.
# get_reason() removed from api.pl.
# Changed get_reason() call in this file to tet'reason().
#
# May 1999, Andrew Dingwall, UniSoft Ltd.
# changed `pwd` to cwd() so that on NT the MKS toolkit is not required
# at runtime
#
# June 1999, Andrew Dingwall, UniSoft Ltd.
# changed close(<TMPRES>) to close(TMPRES)
# to reflect change in current perl syntax
#
# February 2002, Andrew Dingwall, The Open Group
# Get list of known signal names and their values from $Config{sig_name}
# and $Config{sig_num} at runtime rather than using unreliable edits
# at install time.
# Made signal handling work.
#
# March 2003, Andrew Dingwall, The Open Group
# Upgraded to support perl 5 without generating warning messages.
#
# July 2005, Geoff Clare, The Open Group
# Added support for full timestamps.
#
# November 2005, Geoff Clare, The Open Group
# In sub tpend() prioritise on Abort actions before code values
#

package tet;

# DESCRIPTION:
#	This file contains the support routines for the sequencing and control
#	of invocable components and test purposes.
#	It should be required (by means of the perl 'require' command) into
#	a perl script containing definitions of the invocable components
#	and test purposes that may be executed, after those definitions
#	have been made.
#	Test purposes may be written as perl functions.
#
#	This file 'requires' api.pl which contains the perl API functions.
#
#	The user-supplied shell variable iclist should contain a list of all
#	the invocable components in the testset;
#	these are named ic1, ic2 ... etc.
#	For each invocable component thus specified, the user should define
#	a variable whose name is the same as that of the component.
#	Each such variable should contain the names of the test purposes
#	associated with each invocable component; for example:
#       @iclist=(ic1,ic2,ic3);
#       @ic1=(test1-1,test1-2, test1-3);
#       @ic2=(test2-1, test2-2);
#
#	The NUMBERS of the invocable components to be executed are specified
#	on the command line.
#	In addition, the user may define the variables $tet'startup and
#	$tet'cleanup; if defined, the related functions 
#	are executed at the start and end of processing, respectively.
#
#	The TCM makes the NAME of the currently executing test purpose
#	available in the variable $tet'thistest.
#
#	The TCM reads configuration variables from the file specified by the
#	TET_CONFIG environment variable; these are placed in the environment
#	in the 'tet' package's namespace.
#

# standard signals - may not be specified in TET_SIG_IGN and TET_SIG_LEAVE
# SIGHUP, SIGINT, SIGQUIT, SIGILL, SIGABRT, SIGFPE, SIGPIPE, SIGALRM,
# SIGUSR1, SIGUSR2, SIGTSTP, SIGCONT, SIGTTIN, SIGTTOU
#@STD_SIGNAL_LIST=(1,2,3,4,6,8,13,14,15,16,17,25,26,27,28);

@STD_SIGNAL_NAMES=(HUP,INT,QUIT,ILL,ABRT,FPE,PIPE,ALRM,USR1,USR2,TSTP,
	CONT,TTIN,TTOU);

# signals that are always unhandled
# SIGSEGV is here as well because the shell can't trap it
# SIGKILL, SIGSEGV, SIGCHLD, SIGSTOP
#@SPEC_SIGNAL_LIST=(9,11,18,24);

@SPEC_SIGNAL_NAMES=(KILL,SEGV,CHLD,STOP);


# get the list of signal names for this system;
# set $NSIG to one more than the highest signal number
# (NOTE that the first element in the array (index = 0) is "ZERO"
# thus the index of each name in the array is the signal number)
use Config;
if (!defined($Config{sig_name})) {
	die("No signal names in perl Config module ??");
}
if (!defined($Config{sig_num})) {
	die("No signal numbers in perl Config module ??");
}
@snames = split(' ', $Config{sig_name});
@snums = split(' ', $Config{sig_num});
if ($#snums < $#snames) {
	die(sprintf("Not enough signal numbers defined in perl Config" .
		"module (%d names, %d numbers)", $#snames + 1, $#snums + 1));
}
@signames = ();
for (local $n = 0; $n <= $#snames; $n++) {
	local $signum = $snums[$n];
	if ($signum >= 0 && !defined($signames[$signum])) {
		$signames[$signum] = $snames[$n];
	}
}
$NSIG = $#signames + 1;

# add SIGCLD to the SPEC_SIGNAL_NAMES list if it is supported
# separately from SIGCHLD
for (local $n = 1; $n < $NSIG; $n++) {
	if (defined($signames[$n]) && $signames[$n] eq "CLD") {
		$SPEC_SIGNAL_NAMES[$#SPEC_SIGNAL_NAMES + 1] = "CLD";
		last;
	}
}


$#STD_SIGNAL_LIST=$#STD_SIGNAL_NAMES;
for ($i=0; $i<=$#STD_SIGNAL_NAMES; ++$i) 
{
	$STD_SIGNAL_LIST[$i]=&signum($STD_SIGNAL_NAMES[$i]);
}


$#SPEC_SIGNAL_LIST=$#SPEC_SIGNAL_NAMES;
for ($i=0; $i<=$#SPEC_SIGNAL_NAMES; ++$i) 
{
	$SPEC_SIGNAL_LIST[$i]=&signum($SPEC_SIGNAL_NAMES[$i]);
}


# note these are arrays of signal numbers, not names
@std_signals=@STD_SIGNAL_LIST;
@spec_signals=@SPEC_SIGNAL_LIST;

# add the values of signals that perl doesn't have names for
# to the spec_signals array
for (local $n = 1; $n < $NSIG; $n++) {
	if (!defined($signames[$n]) && !ismember($n, @spec_signals)) {
		$spec_signals[$#spec_signals + 1] = $n;
	}
}


# sig_leave_list and sig_ignore_list may be defined in the test case
@sig_leave=@sig_leave_list;
@sig_ign=@sig_ignore_list;



# TCM global variables
 

$thistest="";

# 
# "private" TCM variables
#
use Cwd;
$tet'cwd=cwd();
$tet_tmp_dir=$ENV{"TET_TMP_DIR"};
if (defined($tet_tmp_dir) && $tet_tmp_dir ne "")
{
	; # ok 
}
else {
	$tet_tmp_dir=$cwd;
}
@tmpfiles=();
$tmpres="$tet_tmp_dir/tet_tmpres";
$tet_lock_dir="$tet_tmp_dir/.tmpres";

$context=0;
$block=0;
$sequence=0;
$tpcount=0;
$exitval=0;
$version=1.1;
$activity = $ENV{"TET_ACTIVITY"};
if (!defined($activity)) {
	$activity = 0;
}
$tpnumber=0;
$caughtsig = "";

local($fulltimestamps);
# see if high resolution time() is available
local($hirestime) = 1;
eval 'use Time::HiRes qw( time );' ;
$hirestime = 0 if $@;

# ***********************************************************************
#	compute tpnumbers for all test cases.
#	use a associative array (easiest way in perl)
#
local($tpcounter)=1;
local ($ic);
foreach $ic (@main'iclist) {
	local(@a)=eval("\@main'"."$ic");
	local ($tp);
	foreach $tp (@a) {
		if (!defined($tp_ids{"$tp"})) {
			$tp_ids{"$tp"}=$tpcounter++;
		}
	}
}
#@k=keys %tp_ids;
#@v=values %tp_ids;

#print "k=@k\n";
#print "v=@v\n";

# ***********************************************************************

# 
# "private" TCM function definitions
# these interfaces may go away one day
#

# tet_ismember - return 1 if $1 is in the set $2 ... 
# otherwise return 0

sub ismember 
{
	local $member;

	# this puts $1 in $item and $2 ... $n in @set
	local($item, @set) = @_;

	# ensure that we have at least an "item" - "set" could be empty
	($#_ < 0) && &wrong_params("ismember");

	foreach $member (@set) {
		if ($member eq $item) {
			return(1);
		}
	}

	return(0);
}



# tet_setsigs - install traps for signals mentioned in the
# signal_actions array
# if the action is leave, the signal is left alone
# if the action is ignore, the signal is ignored
# if the action is default, the signal trap is set to $1
# (signal traps are passed the invoking signal number as argument)
sub setsigs
{
	local $action = $_[0];
	local $signum;

	($#_!=0) && &wrong_params("setsigs");

	for ($signum = 1; $signum < $NSIG; ++$signum) {
		if ($signal_actions[$signum] eq "leave") {
			; # nothing
		}
		elsif ($signal_actions[$signum] eq "ignore") {
			$SIG{$signames[$signum]} = "IGNORE";
		}
		elsif ($signal_actions[$signum] eq "default") {
			$SIG{$signames[$signum]} = $action;
		}
		else {
			# "can't happen"
			&error("unexpected signal_action " .
				"\"$signal_actions[$signum]\" for signal " .
				"$signum ($signame[$signum])");
		}
	}
}

# tet_defaultsigs - restore default action for signals that are not to
# be ignored or left alone
sub defaultsigs
{
	setsigs("DEFAULT");
}


# signum - return the signal number of the specifed signal name
# or -1 if $1 is not the name of a known signal
sub signum 
{
	($#_!=0) && &wrong_params("signum");
	local($i)=0;
	for($i=0;$i<=$#signames;++$i) 
	{
		if ($signames[$i] eq $_[0])
		{
			return $i;
		}
	}
	return -1;
}


# tet_abandon - signal handler used during startup and cleanup
# this function does not return
sub abandon 
{
	my $sig = $_[0];

	# if the signal is SIGTERM:
	#	print a message and perform an orderly cleanup
	#	with signals set to default;
	#	that the cleanup function is called with signals reset -
	#	that way we avoid a loop in the event that a second
	#	SIGTERM arrives before the cleanup function completes
	# otherwise:
	#	just print a message and exit
	if ($sig eq "TERM") {
		&error("Abandoning test case: received SIG$sig signal");
		&defaultsigs;
		&docleanup;
	} 
	else {
		&error("Abandoning testset: caught unexpected SIG$sig signal");
	}

	&cleanup;
	exit(&signum($sig));
}

# tet_sigskip - signal handler used during test execution
sub sigskip 
{
	# set the global variable to show that a signal has arrived
	$caughtsig = $_[0];

	# normally this function will be called when a signal arrives
	# during execution of an eval() statement;
	# this call to die() jumps out of the eval() and sets $@
	# to the message
	# (note that the message normally never appears anywhere)
	# however, there is a small window in which an unexpected signal
	# can arrive and be caught by this handler outside the execution
	# of an eval();
	# in this case this call to die() terminates the test case and
	# the cleanup function won't get called;
	# this is not ideal but it's the best that we can do
	die("unexpected SIG$caughtsig signal received: exiting");
}


sub timestamp
{
	local($timeval,$sec,$min,$hour,$mday,$mon,$year);

	if ($fulltimestamps) {
		if ($hirestime) {
			$timeval = 0;
			eval '$timeval = Time::HiRes::time()';
			if ($timeval <= 0) {
				$timeval = time;  # fallback
			}
			($sec,$min,$hour,$mday,$mon,$year) =
				localtime(int($timeval));
			$r = sprintf("%04d-%02d-%02dT%02d:%02d:%02d.%03d",
				$year+1900,$mon+1,$mday,$hour,$min,$sec,
				1000 * ($timeval - int($timeval)) );
		}
		else {
			($sec,$min,$hour,$mday,$mon,$year) = localtime;
			$r = sprintf("%04d-%02d-%02dT%02d:%02d:%02d",
				$year+1900,$mon+1,$mday,$hour,$min,$sec);
		}
	}
	else {
		($sec,$min,$hour) = localtime;
		$r = sprintf("%02d:%02d:%02d",$hour,$min,$sec);
	}
}



# tet_tpend - report on a test purpose
sub tpend 
{
	local($_);
	
	($#_!=0) && &wrong_params("tpend");
	local($arg)=$_[0];
#	local($TMPRES);
	local($have_abort)="NO";
	$result="";
	seek(TMPRES,0,0);
	READLOOP: while (<TMPRES>) {
		chop;
		if ("$result" eq "") {
			$result="$_";
			next READLOOP;
		}

		# First compare abort flags.  Codes with an Abort action
		# take priority over those with no Abort action.

		if (&getcode("$_")==0) {    # sets $abort
			if ("$have_abort" eq "NO" && "$abort" eq "YES") {
				$result="$_";
				$have_abort="YES";
				next READLOOP;
			}
			if ("$have_abort" eq "YES" && "$abort" eq "NO") {
				next READLOOP;
			}
		}

		# Abort flags are the same, so go by result code priority

		PAT: {
			/PASS/ && (last PAT);

			/FAIL/ && ($result = $_, last PAT);

			/UNRESOLVED|UNINITIATED/ && do
				{if ("$result" ne FAIL) {
					$result=$_;
				} last PAT;};

			/NORESULT/ && do
				{if ( $result eq FAIL || $result eq UNRESOLVED 
					|| $result eq UNINITIATED) {
						$result=$_;
				}  last PAT;};

			/UNSUPPORTED|NOTINUSE|UNTESTED/ && do
				{if ($result eq PASS) {
					$result=$_;
				} last PAT;};

			if (($result eq PASS) || ($result eq UNSUPPORTED) ||
				($result eq NOTINUSE) || ($result eq UNTESTED) ) {
				$result=$_;
			}
		}
	}

	close(TMPRES);	# TMPRES deleted automagically

	$abort="NO";
	if ("$result" eq "") {
		$result=NORESULT;
		$resnum=7;
	} elsif (&getcode($result)!=0) {     # sets $resnum & $abort
		$result="NO RESULT NAME";
		$resnum=-1;
	}

	$time=&timestamp;
	&output(220, "$arg $resnum $time", "$result");

	if ($abort eq YES) {
		&setsigs("tet'abandon");
		&output(510,"","ABORT on result code $resnum \"$result\"");
		&docleanup;
		$exitval=1;
		&cleanup;
		exit($exitval);
	}
}

# docleanup - call the cleanup function if there is one
sub docleanup
{
	if (!defined($cleanup) || $cleanup eq "") {
		return;
	}

	$thistest="";
	$tpcount=0;
	$block=0;
	&setblock;
	eval("&main'"."$cleanup");
	$@ && ($@ =~ s/\(eval\) line (\d+)/$0 . 
		" line " . ($1+$start)/e, die $@);
}



sub cleanup{
	unlink(@tmpfiles);
}



require "$ENV{\"TET_ROOT\"}/lib/perl/api.pl" ;

#eval <<'End_of_Program';

#args already in $0 and @ARGV

#arrange to clean up on exit

#init this here for lack of a better place
@DELETES_FILE=();



# check for journal file descriptor
# note that JOURNAL_HANDLE is an indirect reference to the actual file handle
# and is used that way in the API

##$tet_ext_set=$ENV{"TET_EXTENDED"};
##substr($tet_ext_set,1)='';

##if ($tet_ext_set eq "T" || $tet_ext_set eq "t")
##{
##	$journal_path=$ENV{"TET_JOURNAL_PATH"};
##}
##else
##{
	$journal_path="tet_xres";
##}

if (!defined($journal_path)) 
{
	$journal_fd="/dev/tty";
	$JOURNAL_HANDLE=STDOUT;
}
else 
	{
# always unlink tet_xres file
	unlink(tet_xres);
	if (open(JOURNAL_HANDLE_REAL,">>$journal_path")) {
		$JOURNAL_HANDLE=JOURNAL_HANDLE_REAL;
	} 
else 
	{
		$JOURNAL_HANDLE=STDOUT;
	}
}

#no matter what, make sure output is unbuffered.
select((select($JOURNAL_HANDLE), $|=1)[0]);
	



# read in configuration variables and make them readonly
# strip comments and other non-variable assignments
# protect embedded spaces and single quotes in the value part
#
#

$tet_config=$ENV{"TET_CONFIG"};

if (defined($tet_config) && $tet_config ne "" ) {
	if (-r $tet_config) {
		local($FILE);
		open(FILE,"<$tet_config");
		while (<FILE>) {
			/^#/ && next;
			/^[\b]*$/ && next;
			!/^[^\b]+=/ && next;
			s/^/\$/;
			s/=(.*$)/=\"$1\";/;
#			print;
			eval;
		}
		close(FILE);
	}
	else {
		&error("can't read config file $tet_config");
	}
}

$fulltimestamps = 0;
if (defined($TET_FULL_TIMESTAMPS) && $TET_FULL_TIMESTAMPS =~ /^t/i) {
	$fulltimestamps = 1;
}


&setcontext;

$code=$ENV{"TET_CODE"};

if (defined($code) && $code ne "") {
	; # ok
}
else {
	$code=tet_code;
}


local($TET_CODE_HANDLE);
local($fail)=0;


if (open(TET_CODE_HANDLE,"<$code")) {
	@TET_CODE_FILE=<TET_CODE_HANDLE>;
	close(TET_CODE_HANDLE);
} else {
 
	if (tet_code ne "$code") {
		&error("could not open results code file $code");
	}
	@TET_CODE_FILE=("0   PASS        Continue\n",
		"1   FAIL        Continue\n",
		"2   UNRESOLVED  Continue\n",
		"3   NOTINUSE    Continue\n",
		"4   UNSUPPORTED Continue\n",
		"5   UNTESTED    Continue\n",
		"6   UNINITIATED Continue\n",
		"7   NORESULT    Continue\n");

} 

#process command-line args
$pname=$0;

if ($#ARGV<0) {$ARGV[0]="all";}

$iclast = -1;
#($iclist = $main'iclist)  =~ tr/" 0123456780"#/cd;
@iclist=@main'iclist;

if ($#iclist<0) {
	&error("IClist is not defined");
	die;
}

foreach(@iclist) {
	tr/" 0123456789"//cd;
}

#if("$iclist" eq " ") {$iclist=0;}

$icfirst_def=$iclist[0];
#$icfirst_def =~ s/ .*//;

$iccount=0;

#split comma separate list into separate items
foreach(@ARGV) {
	local(@B)=split(/,/);
	@A=(@A,@B);
};

@ARGV=@A;
foreach(@ARGV) {
	CASE_PAT: {
		/all.*/ && do
			{
				if ($iclast<0) {
					$icfirst=$icfirst_def;
					foreach (@iclist) {
						if ($_<$icfirst) { $icfirst=$_;}
					}
				} else {
					$icfirst=$iclast+1;
				}
				$iclast=$icfirst;
				$_=0;
				foreach(@iclist) {
					if ($_>$iclast) {$iclast=$_;}
				}
				#if ($iclast>$_) {$iclast=$_;}
				last CASE_PAT;
			};
		/.*/ && do
			{
				local($save)=$_;
				s/^([0-9]*).*/\$icfirst=$1;/;
				eval;
				$_=$save;
				s/^[^\-]*-*//;
				s/^([0-9]*).*/\$iclast=$1;/;
				s/=;/="";/;
				eval;
			};
	}
	
	$icno=("$icfirst" eq "") ? $icfirst_def : $icfirst;


	$iclast = ($iclast eq "") ? $icno : $iclast;

	while ($icno <= $iclast) {
		if (grep(/\b$icno\b/,@iclist)) {
			$a="\$#main'ic"."$icno";
			if (eval("\$#main'ic"."$icno") > -1) {
				$tests[$iccount++]="ic$icno";
			} else {
				&error("IC $icno is not defined for this test case\n");
			}
		}
		++$icno;
	}
}


# print startup message to execution results file
&output(15,"3.8 $iccount","TCM Start");

# do initial signal list processing
$#sig_leave2=-1;
foreach (@sig_leave)
{
	print "Process signal $_\n";
	if (&ismember($_, @std_signals) || &ismember($_, @spec_signals)) {
		&error("warning: illegal entry $_ in tet'sig_leave_list ignored");
	} else {
		$sig_leave2[$#sig_leave2+1]=$_;
	}
}

$#sig_ign2=-1;
foreach (@sig_ign)
{
	print "Process signal $_\n";
	if (&ismember($_, @std_signals) || &ismember($_, @spec_signals)) {
		&error("warning: illegal entry $_ in tet'sig_ignore_list ignored");
	} else {
		$sig_ign2[$#sig_ign2+1]=$_;
	}
}

@sig_leave2=(@sig_leave2,@spec_signals);

$signal_actions[$NSIG-1]="";

for (local($S) = 1; $S < $NSIG; ++$S) {
	if (&ismember($S, @sig_leave2)) {
		$signal_actions[$S] = "leave";
	}
	elsif (&ismember($S, @sig_ign2)) {
		$signal_actions[$S] = "ignore";
	}
	else {
		$signal_actions[$S] = "default";
	}
}


# do startup processing
#
# install a signal handler for use during the startup function;
# NOTE that this handler remains the default throughout the IC
# processing loop, except inside the code block that invokes each TP
&setsigs("tet'abandon");
if (defined($startup) && "$startup" ne "") 
{
	eval ("&main'"."$startup");
	$@ && ($@ =~ s/\(eval\) line (\d+)/$0 . 
		" line " . ($1+$start)/e, die $@);
}

# process each IC in turn
for $icname (@tests) {
	$icnumber=$icname;
	$icnumber =~ s/[^0-9]*//;
	$tpmax = $tpcount = eval("\$#main'"."$icname");
	$@ && ($@ =~ s/\(eval\) line (\d+)/$0 .
		" line " . ($1+$start)/e, die $@);

	++$tpmax;

	# report IC start to the journal
	$time=&timestamp;
	&output(400, "$icnumber $tpmax $time", "IC Start");

	# process each TP in this IC
	for ($tpcount=1; $tpcount<=$tpmax; ++$tpcount) {
		$thistest=eval("\$main'"."$icname"."[$tpcount-1]");
		$@ && ($@ =~ s/\(eval\) line (\d+)/$0 . 
			" line " . ($1+$start)/e, die $@);
		local($tpnumber)=$tp_ids{$thistest};
		$time=&timestamp;

		# report TP start to the journal
		&output(200,"$tpnumber $time","TP Start");
		&setcontext;

#		using '$$' would allow for paralle processes to not lock from
#		each other!

		# not sure why $tet_lock_dir is here since tcc handles
		# the locking for us, but will leave it in for now
		# --andrew
		local($timeout_count)=17;

		while (!mkdir("$tet_lock_dir",0700)) {
			sleep(1);
			if (--$timeout_count==0) {
				&error("can't obtain lock dir $tet_lock_dir");
				die;
			}
		}
		open(TMPRES,"+>$tmpres");
		unlink("$tmpres");
		rmdir("$tet_lock_dir");

		# see if this TP has been deleted
		if (($reason_string = &tet'reason($thistest)) ne "") {
			&infoline($reason_string);
			&result("UNINITIATED");
		}
		else {
			# start a new code block and install a signal
			# handler in it, then call this TP
			# the handler sets $caughtsig and calls die();
			# if an unexpected signal arrives during the eval(),
			# the call to die() in the handler jumps out of the
			# eval() and sets $@ to the message passed to die();
			# NOTE that if a signal arrives after the handler is
			# installed but before the eval(), the TCM will exit;
			# this is not quite as bullet-proof as sigsetjmp()
			# in the C TCM but is the best we can do
			{
				$caughtsig = "";
				&tet'setsigs("tet'sigskip");
				eval("\&main'"."$thistest");
			}

			# report an unexpected signal
			if ($caughtsig ne "") {
				&tet'infoline("unexpected SIG$caughtsig " .
					"signal received");
				&tet'result("UNRESOLVED");
			}
			else {
				$@ && ($@ =~ s/\(eval\) line (\d+)/$0 . 
					" line " . ($1+$start)/e, die $@);
			}
		}

		# report TP end the journal
		&tpend($tpnumber);

		# don't step on to the next TP if a SIGTERM signal arrived
		# during the TP just executed
		if ($caughtsig eq "TERM") {
			last;
		}
	}

	# report IC end to the journal
	$time=&timestamp;
	--$tpcount;
	&output(410,"$icnumber $tpcount $time","IC End");

	# if a SIGTERM signal arrived during the TP just executed,
	# cleanup and exit with an exit status of SIGTERM
	if ($caughtsig eq "TERM") {
		my $rc = &signum($caughtsig);
		&error("Abandoning test case: received SIG$caughtsig signal");
		&docleanup;
		&cleanup;
		exit($rc);
	}
}

# here to call the cleanup routine and exit normally
&setsigs("tet'abandon");
&docleanup;
&cleanup;
exit(0);

#End_of_Program

# &cleanup;

# $@ && ($@ =~ s/\(eval\) line (\d+)/$0 . " line " . ($1+$start)/e, die $@);

# exit($exitval);
