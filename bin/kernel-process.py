#!/usr/bin/env python2
#
# RV-Predict script for aggregating race reports
#
# Python version: 2.7
#
# Prerequisite packages: enum34
#
# Sample usage:
#   find -iname "result.txt" -exec cat "{}" \; | python aggregate-tests.py >races.txt 2>info.txt

from __future__ import print_function
import re
import sys
from enum import Enum
from collections import deque, OrderedDict
from sys import argv
from subprocess import Popen, PIPE
import os

def startGDB(module):
    gdb = Popen(['/usr/bin/gdb', '--quiet'], stderr=PIPE,stdout = PIPE, stdin = PIPE )
    #gdb.stdin.write("set print symbol-filename on\n")
    with open(".sections/.text","r") as f:
        text = f.read()
    gdb.stdin.write("add-symbol-file " + module + " " + text[:-1])
    count = 1
    for filename in os.listdir(".sections"):
        info("segment", filename)
        if filename.startswith("."):
            if filename in {".text", ".strtab", ".symtab"}:
                continue
            count += 1
            with open(".sections/"+filename,"r") as f:
                section = f.read()
                info("(gdb)"," -s " + filename + " " + section)
                gdb.stdin.write(" -s " + filename + " " + section[:-1])
    gdb.stdin.write("\n")
    for i in range(0,count+3):
        line = gdb.stdout.readline()
        info("(gdb) ", line)
    #gdb.stdin.write("info address 0\n")
    #line = gdb.stdout.readline()
    #info("(gdb) ", line)
    return gdb

def lookupAddress(gdb,addr):
    addr = "0x" + addr[1:]
    if addr in lookupAddress.__cache:
        return lookupAddress.__cache[addr]
    gdb.stdin.write("info symbol " + addr + "\n")
    info("gdb<<", "info symbol " + addr + "\n")
    symbol = gdb.stdout.readline()
    if symbol.startswith("(gdb) No symbol matches"):
        symbol = addr
    else:
        symbol = symbol.split()[1]
    info("gdb>>", symbol)
    lookupAddress.__cache[addr] = symbol
    return symbol
lookupAddress.__cache = {}

def lookupLocation(gdb,loc):
    if loc in lookupLocation.__cache:
        return lookupLocation.__cache[loc]
    symbol = lookupAddress(gdb, "."+loc)
    info("gdb<<", "info symbol 0x" + loc + "\n")
    gdb.stdin.write("info line *0x" + loc + "\n")
    info("gdb<<", "info line *0x" + loc + "\n")
    line = gdb.stdout.readline()
    if line.startswith('(gdb) No line number '):
        file = line = "???"
    else:
        parts = line.split()
        line = parts[2]
        file = parts[4]
        gdb.stdout.readline() 
    info("gdb>>", line)
    lookupLocation.__cache[loc] = (symbol,file, line)
    return (symbol,file, line)
lookupLocation.__cache = {}


def info(*objs):
    print("INFO: ", objs, file=sys.stderr)

class State(Enum):
    initial = 1
    leg_initial = 2
    leg_stack_initial = 3
    leg_stack = 4
    leg_thread = 5
    leg_thread_location = 6
    leg_final = 7
    final = 8

class BasicThreadInfo:
    def __init__(self, name):
        self.name = name

class MainThreadInfo(BasicThreadInfo):
    def __str__(self):
        return "    " + self.name + " is the main thread\n"

class CreatedThreadInfo(BasicThreadInfo):
    def __init__(self, parent, location, name):
        BasicThreadInfo.__init__(self, name)
        self.parent = parent
        self.location = location
    def __str__(self):
        return "    {name} is created by {parent}\n" \
               "        {location}\n".format(name = self.name,
                                             parent = self.parent,
                                             location = self.location)

class Race:
    def __str__(self):
        return "Data race on {locSig}: {{{{{{\n" \
               "{left}\n" \
               "{right}\n" \
               "}}}}}}".\
            format(locSig = self.locSig, left = self.left, right = self.right)

class LocSig:
    def __init__(self, locSig):
        self.locSig = locSig

class Field(LocSig):
    def __str__(self):
        return "{locSig}".format(locSig = self.locSig)

class Array(LocSig):
    def __str__(self):
        return "array element {locSig}".format(locSig = self.locSig)

class Access:
    def __str__(self):
        return "    Concurrent {type} in {thread} (locks held: {{{locks}}})\n" \
               " ---->  {locations}\n" \
               .\
            format(type = self.type,
                   thread = self.formatThread(),
                   locks = self.locks,
                   locations = "\n        ".join([str(x) for x in self.stack]),
                   )
    def formatThread(self):
        if (self.thread == "T9223372036854775807"):
            return "interrupt context"
        return "thread " + self.thread

class Location:
    def __init__(self,class_name,file_name,file_line):
        self.class_name = class_name
        self.file_name = file_name
        self.file_line = file_line
    #def __init__(self,locId):
    #    self.locId = locId
    def __str__(self):
        return "at {cname}({fname}:{fline})".\
            format(cname = self.class_name, fname = self.file_name, fline = self.file_line)


class LockLocation(Location):
    def __init__(self, lock_name, class_name, file_name, file_line):
        Location.__init__(self, class_name, file_name, file_line)
        self.lock_name = lock_name
    def __str__(self):
        return "- locked " + self.lock_name + " " + Location.__str__(self)

state = State.initial
field_race_start_re = re.compile("^Data race on field ([^:]*): [{][{][{]")
array_race_start_re = re.compile("^Data race on array element ([^:]*): [{][{][{]")
race_op_re = re.compile("\s*Concurrent ([^ ]*) in thread ([^ ]*) [(]locks held: [{]([^}]*)[}][)]")
location = "at (.*)"
first_stack_re = re.compile(" ---->  " + location)
next_stack_re = re.compile("\s*" + location)
lock_stack_re = re.compile("\s*- locked ([^ ]*) " + location)
thread_create_re = re.compile("\s*([^ ]*) is created by ([^\s]*)")
thread_main_re = re.compile("\s*([^ ]*) is the main thread");
race_end_re = re.compile("[}][}][}]")

races = {}

def finalize_leg():
    global state
    if race.left is None:
        race.left = access
        state = State.leg_final
    else:
        race.right = access
        races[race_key] = race
        state = State.final


script, filename, module = argv
txt = open(filename)
gdb = startGDB(module)

for line in txt:
    info(str(state) + "\t" + line)

    if state == State.initial:
        match = field_race_start_re.match(line)
        if match:
            race = Race()
            race.locSig = Field(lookupAddress(gdb,match.group(1)))
        else:
            match = array_race_start_re.match(line)
            if match:
                race = Race()
                race.locSig = Array(lookupAddress(gdb,match.group(1)))
        if match:
            race_key = match.group(1)
            race.left = None
            state = State.leg_initial
        else:
            info("Skipping line '{}'".format(line))
        continue
    if state == State.leg_initial:
        match = race_op_re.match(line)
        assert match, line
        access = Access()
        access.type = match.group(1)
        access.thread = match.group(2)
        access.locks = match.group(3)
        state = State.leg_stack_initial
        continue
    if state == State.leg_stack_initial:
        match = first_stack_re.match(line)
        assert match, line
        access.stack = deque();
        location = lookupLocation(gdb,*match.groups())
        race_key+= ":"+location[2]
        access.stack.append(Location(*location))
        state = State.leg_stack
        continue
    if state == State.leg_stack:
        match = next_stack_re.match(line)
        if match:
            access.stack.append(Location(*lookupLocation(gdb,*match.groups())))
            continue
        match = lock_stack_re.match(line)
        if match:
            access.stack.append(LockLocation(*lookupLocation(gdb,*match.groups())))
            continue
        state = State.leg_thread
    if state == State.leg_thread:
        match = thread_create_re.match(line)
        if match:
            assert access.thread == match.group(1)
            parent_thread = match.group(2)
            state = State.leg_thread_location
        else:
            match = thread_main_re.match(line)
            assert match, line
            assert access.thread == match.group(1)
            access.thread_info = MainThreadInfo(access.thread)
            finalize_leg()
        continue
    if state == State.leg_thread_location:
        match = next_stack_re.match(line)
        finalize_leg()
        if match:
            creation_location = Location(*lookupLocation(gdb,*match.groups()))
            access.thread_info = CreatedThreadInfo(parent_thread, creation_location, access.thread)
            continue
    if state == State.leg_final:
        assert line.isspace(), line
        state = State.leg_initial
        continue
    if (state == State.final):
        assert race_end_re.match(line), line
        state = State.initial

oraces = OrderedDict(sorted(races.items()))
for race in oraces.itervalues():
    print(race,"\n")
