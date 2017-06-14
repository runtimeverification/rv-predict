# Check that the definitions below are correct for your system

CHMOD	= chmod
RM	= rm
TOUCH	= touch

all:	extarget
	@$(CHMOD) a+x tc1 tc2 tc3

clean:	
	@$(RM) extarget
	@$(CHMOD) a-x tc1 tc2 tc3

tc1:	extarget
	@$(CHMOD) a+x tc1

tc2:	extarget
	@$(CHMOD) a+x tc2

tc3:	extarget
	@$(CHMOD) a+x tc3

extarget:
	@$(TOUCH) extarget

