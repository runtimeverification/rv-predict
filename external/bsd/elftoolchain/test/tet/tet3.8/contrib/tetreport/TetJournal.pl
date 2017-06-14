package TetJournal;

# Copyright in this TET Journal Reporting Software is owned by Hewlett-Packard 
# Australia Limited. 
#      
# The owner hereby grants permission to use, copy, modify, distribute,
# and license this software and its documentation for any purpose, 
# provided that existing copyright notices are retained in all copies and 
# that this notice is included verbatim in any distributions. No written 
# agreement, license, or royalty fee is required for any of the authorized 
# uses. Ownership of copyright in modifications to this software will vest 
# in the authors and the licensing terms described here need not be 
# followed, provided that the new terms are clearly indicated on the first 
# page of each file where they apply.
#      
# TO THE EXTENT ALLOWED BY LOCAL LAW, IN NO EVENT SHALL THE COPYRIGHT 
# OWNER OR DISTRIBUTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, 
# SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE 
# OF THIS SOFTWARE, ITS DOCUMENTATION, OR ANY DERIVATIVES THEREOF, EVEN 
# IF THE OWNER HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#      
# TO THE EXTENT ALLOWED BY LOCAL LAW, THE COPYRIGHT OWNER AND DISTRIBUTORS 
# SPECIFICALLY DISCLAIM ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
# IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, 
# AND NON-INFRINGEMENT, THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, AND 
# THE COPYRIGHT OWNER AND DISTRIBUTORS HAVE NO OBLIGATION TO PROVIDE 
# MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
#      
# RESTRICTED RIGHTS: Use, duplication or disclosure by the government
# is subject to the restrictions as set forth in subparagraph (c) (1) (ii) 
# of the Rights in Technical Data and Computer Software Clause as DFARS 
# 252.227-7013 and FAR 52.227-19.

=head1 NAME

TetJournal - Interpret TET Journal File

=head1 DESCRIPTION

This class reads a supplied TET journal file and provides methods for
interpreting the journal to varying levels of detail.

=head1 METHODS

=item new    Create a TetJournal object.

  Input: Journal file name
  Returns: Object handle  (zero if error)

=item dump   Return array containing entire journal file

  Input: none
  Returns: @journal array

=item status Return failure count within journal

  Input: none
  Returns: Number of TPs that did not record a PASS result

=item nextBlock Fetch next logical block from journal

  Input: none
  Returns: @block array.  Unformatted contents of current block

=item resetBlockPtr  Reset block pointer to start of journal

  Input: none
  Returns: empty

=item blockOutput

  Input: none
  Returns: Output text contained in the current block

=item blockStatus

  Input: none
  Returns: Failure count within current block (if block is a Test Case)

=item blockType

  Input: none
  Returns: Text string identifying type of current block.  One of:
   "header","build","TC","clean","scenario" or "unknown"

=item blockTimes

  Input: none
  Returns: 2 element array of start and end times for current block.  Times are
           in HH:MM:SS format

=item times

  Input: none
  Returns: 2 element array of start and end times for entire journal

=item modes

  Input: none
  Returns: Array of tcc execution modes used.  (Selected from: 'build',
           'execute','clean')

=item TClist

  Input: none
  Returns: List of Test Cases encountered in journal

=item TCname

  Input: none
  Returns: Name of test case processed by current block

=item IClist

  Input: Optional test case name.  If not specified, current block is used to
         determine test case (TC).
  Returns: 
   1. If test case is specified: 
      Array of all IC numbers processed by all invocations of specified test
      case
   2. If no test case specified:
      Array of IC numbers processed by current test case

=item getIC

  Input: IC number within current test case.
  Returns: Array of journal entries for IC

=item ICpassCount

  Input: (Test Case, IC number)
  Returns: Array of total pass counts for each TP within specified IC

=item ICrunCount

  Input: (Test Case, IC number)
  Returns: Total number of times this IC has been run

=item ICstatus

  Input: IC number within current test case
  Returns: Failure count for IC

=item ICtimes

  Input: IC number within current test case
  Returns: Start and end times for specified IC

=item TPcount

  Input: IC number within current test case
  Returns: Number of Test Purposes for the specified IC

=item getTP

  Input: (IC number, TP number)
  Returns: Unformated journal entries for specified Test Purpose

=item TPresult

  Input: (IC number, TP number)
  Returns: Result code (PASS, FAIL, NORESULT etc) for specified Test Purpose

=item TPoutput

  Input: (IC number, TP number)
  Returns: Information text for a specified Test Purpose

=item TPtimes

  Input: (IC number, TP number)
  Returns: Start and End times for specified Test Purpose


=head1 AUTHOR

Geoff Smith

=cut 
    ;

# ===========================================================================
# Method     : new
# Description: Constructor.
# Inputs     : Name of TET journal file, NOTIMES flag (optional)
# Returns    : $object reference
# Example    : $object=Class->new("tet_journal_file");

sub new {
    $class=shift;
    my $this={};
    my $journal=shift;

    # Set times flag
    my $timesFlg=shift;
    $timesFlg=($timesFlg eq 'NOTIMES')? 0 : 1;

    my (@journal);
    
    # Read journal file into an internal array

    return(0) unless (open(JNL,"$journal"));

    chop(@journal=<JNL>);
    close(JNL);
    $this->{journal}=[ @journal ];
    $this->{timesFlg}=$timesFlg;
    $this->{startTime}='unknown';
    $this->{endTime}='unknown';
    $this->{blockPtr}='unset';
    $this->{status}='unset';

    bless $this,$class;
}


# ===========================================================================
# Method     : dump
# Description: Return journal array to caller
# Inputs     : none
# Returns    : Journal array
# Example    : 

sub dump {
    my $this=shift;
    return(@{$this->{journal}});
}


# ===========================================================================
# Method     : status
# Description: Return journal status
# Inputs     : none
# Returns    : Value of status variable.  0=PASS, >0 = FAIL
# Example    : 

sub status {
    my $this=shift;

    my $status=$this->{status};

    if ($status eq 'unset') {
	$status=$this->summarize;
    }

    return($status);
}


# ===========================================================================
# Method     : analyse
# Description: Analyse journal structure into component blocks.  This is the 
#              basis of how this class works.  The blocks are stored in the 
#              object and accessed by a block pointer.  There is a concept
#              of 'current block' which is incremented each time the nextBlock
#              method is called.  Many other methods access the data in the
#              current block.
# Inputs     : none
# Returns    : Array of block pointers
# Example    : 

sub analyse {
    my $this=shift;
    my @journal=@{$this->{journal}};
    my $lastcode="";
    my (@header,@TC,@IC,@block,@struct)=();
    my ($entry,$code,$info,$text,$block,$time);

    #
    # Break journal file into header and logical blocks
    #

    foreach $entry (@journal) {
	($code,$info,$text)=split(/\|/,$entry);

	# Write completed scenario text to block
	if ($lastcode==70 and $code!=70) {
	    push(@struct,[ @text ]);
	}

	# Scan code to determine if starting or completing a block
	if ($code eq '0') {
	    # TCC Start - get time
	    $time=$info;
	    $time=~s/^[^ ]+ (..:..:..) .*/\1/; # extract time string HH:MM:SS
	    $this->{startTime}=$time;

	    # This is part of the header
	    push(@header,$entry);
	}   
	elsif ($code==20 or $code==30 or $code==40) {
	    # Header block
	    push(@header,$entry);
	}
	elsif ($code==70) {
	    # Scenario text
	    if ($lastcode==70) {
		push(@text,$text);
	    } else {
		@text=( $text );
	    }
	}
	elsif ($code==110 or $code==10 or $code==300) {
	    # B,E,C start: begin a block
	    @block=( $entry );
	}
	elsif ($code==130 or $code==80 or $code==320) {
	    # B,E,C end: end a block
	    push(@block,$entry);
	    push(@struct,[ @block ]);
	    @block=();
	}
	elsif ($code==900) {
	    # TCC End
	    $this->{endTime}=$info;
	}
	else {
	    # Append to existing block
	    push(@block,$entry);
	}
	$lastcode=$code;
    }

    # Extract tcc command
    my $tccCmd=$header[0];
    $tccCmd=~s/^.*Command line: //; # Extract invocation command
    $this->{tccCmd}=$tccCmd;

    # Insert header reference into top of @struct
    unshift(@struct,[ @header ]);

    # Store @struct into object
    $this->{struct}=[ @struct ];
    # Set block pointer (Used by nextBlock method) 
    $this->{blockPtr}=-1;

    return(@struct);
}

# ===========================================================================
# Method     : summarize
# Description: Work through journal and generate summary data
# Inputs     : none
# Returns    : Total number of non-PASSes recorded

sub summarize {
    my $this=shift;

    # Initialise
    my @modes=();
    my @TC=();
    my @IC=();
    my $IC;
    my %ICtable;
    my %runTable;
    my %passTable;
    my $failTotal=0;
    
    # Save current block pointer to restore at end of method.  Then initialise
    # pointer to start of journal
    my $savePointer=$this->{blockPtr};

    # Perform analyse step if not done already
    if ($savePointer eq 'unset') {
	$this->analyse;
	$savePointer=-1;
    }

    $this->{blockPtr}=-1;

    #
    # Scan through journal one block at a time to build summary data
    #
    while ($this->nextBlock) {
	# Build up list of tcc modes (@modes)
	my $type=$this->blockType;
	push(@modes,$type) unless grep(/^\Q$type\E$/,@modes);

	next unless ($type eq 'TC');

	# Build up a list of test cases (@TC)
	my $TC=$this->TCname;
	if ($TC) {
	    push(@TC,$TC) unless (grep(/^\Q$TC\E$/,@TC));
	}

	
	# Extract the IC list for the current test case and store in ICtable
	@IC=@{$ICtable{$TC}};
	my @IClocal=$this->IClist;
	foreach $IC (@IClocal) {
	    push(@IC,$IC) unless (grep(/$IC/,@IC));
	}
	
	$ICtable{$TC}=[ @IC ];
	
	# Extract failCount, runCount and PassTable arrays for this TC
	my %ICrunTable=%{ $runTable{$TC} };
	my %ICpassTable=%{ $passTable{$TC} };    
	
	# Investigate each IC, to generate Run and Pass counts
	foreach $IC (@IClocal) {
	    
	    # Run ICstatus method to get data
	    $status=$this->ICstatus($IC);
	    $failTotal+=$status;
	    my @passData=@{$this->{ICpassCounts}}; # generated by ICstatus
	    
	    # Increment failure count
	    $failCount{$IC}++ if ($status);
	    
	    # Increment IC run count
	    $ICrunTable{$IC}++;
	    
	    # Increment IC pass counts
	    my @passCounts=@{$ICpassTable{$IC}};
	    for ($i=0; $i<=$#passData; $i++) {
		$passCounts[$i]+=$passData[$i];
	    }
	    $ICpassTable{$IC}=[ @passCounts ];
	}
	
	# Store IC tables back into TC tables
	$runTable{$TC}=\%ICrunTable;
	$passTable{$TC}=\%ICpassTable;
    }

    # Store mode list
    $this->{modes}=[ @modes ];
    # Store Test Case list
    $this->{TClist}=[ @TC ];

    # Store hash data references back into object for later use
    $this->{runData}=\%runTable;
    $this->{passData}=\%passTable;
    $this->{ICdata}=\%ICtable;	
    $this->{status}=$failTotal;

    # Restore Block pointer to original
    $this->{blockPtr}=$savePointer;

    return($failTotal);
}

# ===========================================================================
# Method     : nextBlock
# Description: Return array of next block
# Inputs     : none
# Returns    : Block array
# Example    : @block=$obj->nextBlock;

sub nextBlock {
    my $this=shift;

    # Perform analysis first if not done yet
    if ('unset' eq $this->{blockPtr}) {
	$this->analyse;
    }

    # Retrieve pointer and data
    my $blockPtr=$this->{blockPtr};
    my @struct=@{$this->{struct}};

    # Increment pointer
    $blockPtr++;
    $this->{blockPtr}=$blockPtr;

    # Return empty if at end of journal
    return() if ($blockPtr > $#struct);
   
    return(@{$struct[$blockPtr]});
}

# ===========================================================================
# Method     : tccCmd
# Description: Return tcc command used to create journal
# Inputs     : none
# Returns    : command string

sub tccCmd {
    my $this=shift;
    if ('unset' eq $this->{blockPtr}) {
	$this->analyse;
    }
    
    return($this->{tccCmd});
}


# ===========================================================================
# Method     : resetBlockPtr
# Description: Reset Block Pointer to before start of journal
# Inputs     : none
# Returns    : empty

sub resetBlockPtr {
    my $this=shift;
    if ('unset' eq $this->{blockPtr}) {
	$this->analyse;
    }
    else {
	$this->{blockPtr}=-1;
    }

    return();
}


# ===========================================================================
# Method     : getBlock
# Description: Return array of current block
# Inputs     : none
# Returns    : Block array
# Example    : @block=$obj->getBlock;

sub getBlock {
    my $this=shift;

    # Retrieve object block array
    my $blockPtr=$this->{blockPtr};
    return () if ($blockPtr eq 'unset' or $blockPtr == -1);

    my @struct=@{$this->{struct}};

    return (@{$struct[$blockPtr]});
}

# ===========================================================================
# Method     : getIC
# Description: Return array containing specified IC within the current block
# Inputs     : IC number
# Returns    : IC array - or empty if current block is not a Test Case
# Example    : @IC=$obj->getIC(1);

sub getIC {
    my $this=shift;
    my $IC=shift;
    my @IC=();

    # Validate IC number (and implicitly that this is a TC block)

    return() unless grep(/$IC/,$this->IClist);
    
    # Retrieve block and scan for selected IC
    my @block=$this->getBlock;
    my $current=0;
    for $entry (@block) {
	my ($code,$info,$text)=split(/\|/,$entry);
	# Process IC Start
	if ($code == 400) { 
	    my ($activity,$testIC,$TPcount,$time)=split(/\s+/,$info);
	    if ($testIC == $IC) {
		$current=1;
	    } else {
		$current=0;
		next;
	    }
	}
	# Process IC End
	elsif ($code == 410) {
	    my ($activity,$testIC,$TPcount,$time)=split(/\s+/,$info);
	    $current=0;
	    push(@IC,$entry) if ($testIC == $IC) ;
	}
	push(@IC,$entry) if ($current);
    }

    return(@IC);
}
    
# ===========================================================================
# Method     : getTP
# Description: Return array containing specified TP within the current block
# Inputs     : IC#,TP#
# Returns    : TP array - or empty if current block is not a Test Case
#              or specified IC or TP numbers dont exist
# Example    : @TP=$obj->getTP(2,1);

sub getTP {
    my $this=shift;
    my ($IC,$TP)=@_[0,1];
    my @TP=();

    my @IC=$this->getIC($IC);
    
    return () unless (@IC);

    # Scan for selected TP
    my $current=0;
    my $count=0;
    for $entry (@IC) {
	my ($code,$info,$text)=split(/\|/,$entry);
	# Process TP Start
	if ($code == 200) {
	    $count++;
	    $current=($count == $TP)? 1 : 0;
	}

	push (@TP,$entry) if ($current);

	if ($code==410) {
	    $current=0;
	}
    }

    return(@TP);
}

# ===========================================================================
# Method     : blockOutput
# Description: Return array of output text for current block
# Inputs     : none
# Returns    : Array of text

sub blockOutput {
    my $this=shift;
    my @output=();

    foreach $entry ($this->getBlock) {
	($code,$info,$text)=split(/\|/,$entry);
	push(@output,$text) if ($code==50 or $code==100 or $code==510);
    }

    return(@output);
}


# ===========================================================================
# Method     : blockStatus
# Description: Return TC failure count for all TP's in current TC block
# Inputs     : none
# Returns    : Failure count  (0 if not a test case block)

sub blockStatus {
    my $this=shift;
    my $failCount=0;

    # Get a list of IC's in this block
    my (@IC)=$this->IClist;
    return(0) unless (@IC);

    # Add up ICstatus for each IC
    foreach $IC (@IC) {
	$failCount+=$this->ICstatus($IC);
    }

    return($failCount);
}


# ===========================================================================
# Method     : blockType
# Description: Return string describing contents of current block
# Inputs     : none
# Returns    : "header","build","TC","clean","scenario" or "unknown"

sub blockType {
    my $this=shift;

    # Retrieve object block array
    my @block=$this->getBlock;
    return() unless (@block);

    # Test first line of block
    my $first=shift(@block);
    if    ($first=~/^0\|/) { return "header" }
    elsif ($first=~/^110\|/) { return "build" }
    elsif ($first=~/^10\|/) { return "TC" }
    elsif ($first=~/^300\|/) { return "clean" }
    elsif ($first=~/^\"/) { return "scenario" }
    else { return "unknown" }
}


# ===========================================================================
# Method     : blockTimes
# Description: Return string describing contents of current block
# Inputs     : none
# Returns    : "header","build","TC","clean","scenario" or "unknown"

sub blockTimes {
    my $this=shift;
    my ($start,$end);

    # Check for times flag
    return("xx:xx:xx","xx:xx:xx") unless ($this->{timesFlg});

    # Get Block Type
    my $type=$this->blockType;
    return () if ($type eq "scenario" or ! $type);

    my @block=$this->getBlock;

    if ($type eq "header") {
	$start=$this->{startTime};
	$end=$start;
    }
    else {
	$start=$block[0];
	$start=~s/^\w+\|[0-9]+ [^ ]+ (..:..:..).*/\1/;  # Extract start time field
	$end=pop(@block);
    	$end=~s/^\w+\|[0-9]+ [^ ]+ (..:..:..).*/\1/;  # Extract end time field
    }
    
    return($start,$end);
}


# ===========================================================================
# Method     : times
# Description: Return array of start and finish times for entire test case
# Inputs     : none
# Returns    : (startTime,endTime)

sub times {
    my $this=shift;

    # Check for times flag
    return("xx:xx:xx","xx:xx:xx") unless ($this->{timesFlg});

    my $startTime=$this->{startTime};
    my $endTime=$this->{endTime};
    return($startTime,$endTime);
}

# ===========================================================================
# Method     : modes
# Description: Return list of tcc modes
# Inputs     : none
# Returns    : Array of modes eg (build,execute,clean)

sub modes {
    my $this=shift;
    
    my $modeRef=$this->{modes};
    
    unless ($modeRef) {
	$this->summarize;
	$modeRef=$this->{modes};
    }

    return(@{ $modeRef });
}


# ===========================================================================
# Method     : TClist
# Description: Return list of test cases
# Inputs     : none
# Returns    : Array of test case names

sub TClist {
    my $this=shift;
    
    my $listRef=$this->{TClist};
    
    unless ($listRef) {
	$this->summarize;
	$listRef=$this->{TClist};
    }

    return(@{ $listRef });
}


# ===========================================================================
# Method     : TCname
# Description: Return name of current test case
# Inputs     : none
# Returns    : testcase name (eg /ts/test1.tcl)

sub TCname {
    my $this=shift;
    # Get Block Type
    my $type=$this->blockType;
    return () unless ($type eq 'build' or $type eq 'TC' or $type eq 'clean');

    my @block=$this->getBlock;
    my $name=shift(@block);

    $name=~s/^\w+\|[0-9]+ ([^ ]+).*/\1/;  # Extract name from string

    return($name);
}


# ===========================================================================
# Method     : IClist
# Description: Return list of IC's for a test case
# Inputs     : Optional: Test case name.  If test case is not specified, current
#              block is used to identify test case
# Returns    : Array of IC numbers

sub IClist {
    my $this=shift;
    my $TC=shift;
    my @IC;

    # If $TC not specified, I must get the IC list from the current test case
    if (! $TC) {
	return() unless ('TC' eq $this->blockType);
	my @block=$this->getBlock;
	my $first=shift(@block);
	$first=(split(/\|/,$first))[2];  # Extract text field

	if ($first=~/ICs: {([,|0-9]+)}/) {
	    # This is the easy case: User has specified IC's in scenario file
	    # and this information is printed on the TC start text line
	    $first=$1; # Extract comma separated IC numbers
	    @IC=split(/\,/,$first);
	}
	else {
	    # Scenario does not specify IC's - All are run.  Count up IC's in 
	    # the Test Case
	    my @ICstart=grep(/^400\|/,@block);
	    my $count=1+$#ICstart;  # 1 for 0 index
	    # Build @IC array
	    for ($IC=1;$IC <= $count; $IC++) {
		push(@IC,$IC);
	    }
	}
    }
    else {
	$this->summarize if ($this->{status} eq 'unset');
	# Extract IC's from ICdata
	my %ICtable=%{ $this->{ICdata} };
	@IC=@{ $ICtable{$TC} };
    }

    return(@IC);
}


# ===========================================================================
# Method     : ICtimes
# Description: Return start and end times for a given IC in the current block
# Inputs     : IC number
# Returns    : (start,end)

sub ICtimes {
    my $this=shift;
    my $IC=shift;
    my @IC=$this->getIC($IC);
    my @times=();
    
    return unless (@IC);

    # Check for times flag
    return("xx:xx:xx","xx:xx:xx") unless ($this->{timesFlg});

    my $start=shift(@IC);
    my $end=pop(@IC);

    for $entry ($start,$end) {
    	my ($code,$info,$text)=split(/\|/,$entry);
	my ($activity,$testIC,$TPcount,$time)=split(/\s+/,$info);
	push(@times,$time);
    }

    return(@times);
}


# ===========================================================================
# Method     : ICrunCount
# Description: Return total number of times IC is run
# Inputs     : (Test Case,IC number)
# Returns    : Count

sub ICrunCount {
    my $this=shift;
    my ($TC,$IC)=@_[0,1];

    $this->summarize if ($this->{status} eq 'unset');

    my %runTable=%{$this->{runData}};
    my %ICrunTable=%{ $runTable{$TC} };
    my $runCount=$ICrunTable{$IC};
    
    return($runCount);
}

# ===========================================================================
# Method     : ICpassCount
# Description: Return array of TP pass totals for specified IC
# Inputs     : (Test Case,IC number)
# Returns    : Array of counts

sub ICpassCount {
    my $this=shift;
    my ($TC,$IC)=@_[0,1];

    $this->summarize if ($this->{status} eq 'unset');

    my %passTable=%{$this->{passData}};
    my %ICpassTable=%{ $passTable{$TC} };
    my @passCounts=@{ $ICpassTable{$IC} };
    
    return(@passCounts);
}

# ===========================================================================
# Method     : ICstatus
# Description: Return failure count for specified IC in current block
# Inputs     : IC number
# Returns    : Integer failure count

sub ICstatus {
    my $this=shift;
    my $IC=shift;

    my $failCount=0;
    my @passCounts=();

    my $TPcount=$this->TPcount($IC);

    # Record an error if no TP's are found
    $failCount++ if ($TPcount == 0);

    # Work through each TP, looking for a PASS
    for ($TP=1; $TP <= $TPcount; $TP++) {
	my $result=$this->TPresult($IC,$TP);
	if ($result eq 'PASS') {
	    push(@passCounts,1);
	} else {
	    $failCount++;
	    push(@passCounts,0);
	}
    }

    $this->{ICpassCounts}=[ @passCounts ]; # Used by Summarize method

    return($failCount);
}


# ===========================================================================
# Method     : TPcount
# Description: Return number of Test Purposes in a given IC in the current block
# Inputs     : IC number
# Returns    : count  

sub TPcount {
    my $this=shift;
    my $IC=shift;
    my @IC=$this->getIC($IC);

    return unless (@IC);

    my $first=shift(@IC);

    my ($code,$info,$text)=split(/\|/,$first);
    my ($activity,$testIC,$TPcount,$time)=split(/\s+/,$info);

    return($TPcount);
}

# ===========================================================================
# Method     : TPresult
# Description: Extract result of a specified Test Purpose in current block
# Inputs     : IC number, TP number
# Returns    : Result 

sub TPresult {
    my $this=shift;
    my ($IC,$TP)=@_[0,1];
    my @TP=$this->getTP($IC,$TP);

    my $result=(grep(/^220/,@TP))[-1];

    if ($result) {
	my ($code,$info,$text)=split(/\|/,$result);
	$result=$text;
    }
    else {
	$result='NORESULT';
    }
    return($result);
}


# ===========================================================================
# Method     : TPoutput
# Description: Return the output text for a specified Test purpose
# Inputs     : IC number, TP number
# Returns    : Text array

sub TPoutput {
    my $this=shift;
    my ($IC,$TP)=@_[0,1];
    my @TP=$this->getTP($IC,$TP);
    my @output=();
    
    foreach $entry (@TP) {
	my ($code,$info,$text)=split(/\|/,$entry);
	next if ($code==200 or $code==220);
	push(@output,$text);
    }

    return(@output);
}


# ===========================================================================
# Method     : TPtimes
# Description: Return the Start and End times for a specified Test Purpose
# Inputs     : IC number, TP number
# Returns    : (start,end)

sub TPtimes {
    my $this=shift;

    # Check for times flag
    return("xx:xx:xx","xx:xx:xx") unless ($this->{timesFlg});

    my ($IC,$TP)=@_[0,1];
    my @TP=$this->getTP($IC,$TP);

    my ($start,$end)=();

    foreach $entry (@TP) {
	my ($code,$info,$text)=split(/\|/,$entry);
	if ($code==200) {
	    my ($activity,$TPnum,$time)=split(/\s+/,$info);
	    $start=$time;
	}
	elsif ($code==220) {
	    my ($activity,$TPnum,$result,$time)=split(/\s+/,$info);
	    $end=$time;
	}
    }

    return($start,$end);
}
 
1;
