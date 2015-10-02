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
        return "field {locSig}".format(locSig = self.locSig)

class Array(LocSig):
    def __str__(self):
        return "array element {locSig}".format(locSig = self.locSig)

class Access:
    def __str__(self):
        return "    Concurrent {type} in thread {thread} (locks held: {{{locks}}})\n" \
               " ---->  {locations}\n" \
               "{thread_info}".\
            format(type = self.type,
                   thread = self.thread,
                   locks = self.locks,
                   locations = "\n        ".join([str(x) for x in self.stack]),
                   thread_info = self.thread_info)

class Location:
    def __init__(self,class_name,file_name,file_line):
        self.class_name = class_name
        self.file_name = file_name
        self.file_line = file_line
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
location = "at ([^(]*)[(]([^:]*):([^)]*)[)]"
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


for line in sys.stdin:
    info(str(state) + "\t" + line)

    if state == State.initial:
        match = field_race_start_re.match(line)
        if match:
            race = Race()
            race.locSig = Field(match.group(1))
        else:
            match = array_race_start_re.match(line)
            if match:
                race = Race()
                race.locSig = Array(match.group(1))
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
        access.stack.append(Location(*match.groups()))
        race_key+= ":"+match.group(3)
        state = State.leg_stack
        continue
    if state == State.leg_stack:
        match = next_stack_re.match(line)
        if match:
            access.stack.append(Location(*match.groups()))
            continue
        match = lock_stack_re.match(line)
        if match:
            access.stack.append(LockLocation(*match.groups()))
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
        assert match, line
        creation_location = Location(*match.groups())
        access.thread_info = CreatedThreadInfo(parent_thread, creation_location, access.thread)
        finalize_leg()
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
