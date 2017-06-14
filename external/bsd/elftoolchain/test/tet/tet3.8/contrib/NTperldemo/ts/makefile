# Check that the definitions below are correct for your system

PERL_SUFFIX	=  .pl
CHMOD	= chmod
RM	= rm
TOUCH	= touch

all:	extarget
	@$(CHMOD) a+x tc1$(PERL_SUFFIX) tc2$(PERL_SUFFIX) tc3$(PERL_SUFFIX)

clean:	
	@$(RM) extarget
	@$(CHMOD) a-x tc1$(PERL_SUFFIX) tc2$(PERL_SUFFIX) tc3$(PERL_SUFFIX)

tc1$(PERL_SUFFIX):	extarget
	@$(CHMOD) a+x tc1$(PERL_SUFFIX)

tc2$(PERL_SUFFIX):	extarget
	@$(CHMOD) a+x tc2$(PERL_SUFFIX)

tc3$(PERL_SUFFIX):	extarget
	@$(CHMOD) a+x tc3$(PERL_SUFFIX)

extarget:
	@$(TOUCH) extarget

