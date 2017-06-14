# Makefile to build .html and man pages from pod source files
# pod2html and pod2man are supplied with perl distribution
# Geoff Smith 23/4/1997
#
# Andrew Josey, The Open Group, November 1997
# Add in install target.

TET_ROOT=../../..
DOC_DIR=$(TET_ROOT)/doc/tetreport

POD2HTML=pod2html
POD2MAN=pod2man --center "HP TETWare" --release A.01.00
VPATH=..

files=  TetJournal.3 TetJournal.html \
	TetReports.3 TetReports.html \
	tetreport.1 tetreport.html

all: $(files)

tetreport.1: ../tetreport.pl
	${POD2MAN} --section 1 $? > $@

tetreport.html: ../tetreport.pl
	${POD2HTML} $?; mv ${VPATH}/$@ .

install:
	if [ ! -d $(DOC_DIR) ] ; then mkdir $(DOC_DIR); chmod 755 $(DOC_DIR); fi       
	cp $(files) $(DOC_DIR)/
	chmod a-wx,u+rw,a+r $(DOC_DIR)/*

.SUFFIXES: .pl .3 .html .1

.pl.3:
	${POD2MAN} --section 3 $? > $*.3


.pl.html:
	cp $? $*; ${POD2HTML} $* ; rm -f $*
