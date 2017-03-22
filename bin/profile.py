#!/usr/bin/env python
import argparse
import datetime
import os
import subprocess
import sys
import time

FNULL = open(os.devnull, 'w')
SECONDS_IN_MINUTE = 60
SECONDS_IN_HOUR = 60 * SECONDS_IN_MINUTE
SECONDS_IN_DAY = 24 * SECONDS_IN_HOUR

def printTime(time_seconds):
    days = int(time_seconds / SECONDS_IN_DAY)
    time_seconds = time_seconds % SECONDS_IN_DAY
    hours = int(time_seconds / SECONDS_IN_HOUR)
    time_seconds = time_seconds % SECONDS_IN_HOUR
    minutes = int(time_seconds / SECONDS_IN_MINUTE)
    seconds = time_seconds % SECONDS_IN_MINUTE

    time_pieces = []
    if days:
        time_pieces.append('%dd' % days)
    if hours or time_pieces:
        time_pieces.append('%dh' % hours)
    if minutes or time_pieces:
        time_pieces.append('%dm' % minutes)
    time_pieces.append('%6.3fs' % seconds)
    print ''.join(time_pieces)

def runWithTime(args, timeoutSeconds):
    start_time_seconds = time.time()
    p = subprocess.Popen(args, stdout=FNULL, stderr=FNULL)
    while (time.time() - start_time_seconds < timeoutSeconds) and (p.poll() is None):
        time.sleep(0.1)
    elapsed_time_seconds = time.time() - start_time_seconds
    try:
        p.kill()
    except:
        pass
    printTime(elapsed_time_seconds)

def runPredictTool(windowSize, timeoutSeconds, useSmt, extraArguments):
    args = []
    #TODO(virgil): this is horrible, I should pass the path as an argument or something.
    args.extend(['java', '-ea', '-jar',
                 os.path.join(os.path.dirname(os.path.realpath(sys.argv[0])),
                              '../target/release/rv-predict/rv-predict.jar'),])
    if windowSize:
        args.extend(['--window', '%s' % 1000])
    if not useSmt:
        args.append('--no-smt')
    args.extend(extraArguments)

    runWithTime(args, timeoutSeconds)

def runForWindows(windows, name, timeoutSeconds, useSmt, extraArguments):
    if not windows:
        print '%s timeout=%s useSmt=%s' % (name, timeoutSeconds, useSmt),
        sys.stdout.flush()
        runPredictTool(windowSize=None, timeoutSeconds=timeoutSeconds, useSmt=useSmt,
                       extraArguments=extraArguments)
        return
    for w in windows:
        print ('%s %s timeout=%s useSmt=%s, window=%s'
               % (datetime.datetime.now(), name, timeoutSeconds, useSmt, w)),
        sys.stdout.flush()
        runPredictTool(windowSize=w, timeoutSeconds=timeoutSeconds, useSmt=useSmt,
                       extraArguments=extraArguments)

def readExtraArguments(argv):
    extra_args = []
    try:
        index = argv.index('--')
        if index >= 0:
            extra_args = argv[index + 1:]
            argv = argv[:index]
    except ValueError:
        pass
    return argv, extra_args

def runWithAndWithoutSmt(windows, name, timeoutSeconds, extraArguments):
    runForWindows(windows=windows,
                  name=name,
                  timeoutSeconds=timeoutSeconds,
                  useSmt=True,
                  extraArguments=extraArguments)
    runForWindows(windows=windows,
                  name=name,
                  timeoutSeconds=timeoutSeconds,
                  useSmt=False,
                  extraArguments=extraArguments)

#TODO(virgil): it would be much nicer to read these from the output of
# dacapo -l.
DACAPO_TESTS = [
    'avrora', 'batik', 'fop', 'h2', 'jython', 'luindex', 'lusearch',
    'pmd', 'sunflow', 'tomcat', 'tradebeans', 'tradesoap', 'xalan']

def runDacapoTests(dacapoJar, windows, timeoutSeconds):
    for test in DACAPO_TESTS:
        runWithAndWithoutSmt(windows=windows,
                             name='dacapo-%s' % test,
                             timeoutSeconds=timeoutSeconds,
                             extraArguments=['-jar', dacapoJar, test])

def main(argv):
    argv, extraArguments = readExtraArguments(argv)

    parser = argparse.ArgumentParser(description="Profile the rv-predict tool")

    parser.add_argument('--windows', '-w', action='append')
    parser.add_argument('--timeout-seconds', action='store')
    #parser.add_argument('--use-smt', action='store_const', const=True, default=False)
    parser.add_argument('--name', action='store')
    parser.add_argument('--llvm-directory', dest='llvm_directory', action='store')
    parser.add_argument('--dacapo', action='store')
    args = parser.parse_args(argv)

    if args.dacapo:
        runDacapoTests(
            dacapoJar=args.dacapo,
            windows=[int(w) for w in args.windows],
            timeoutSeconds=int(args.timeout_seconds))

    if args.llvm_directory:
        extraArguments.extend(['--llvm-predict', args.llvm_directory])
    runWithAndWithoutSmt(
        windows = [int(w) for w in args.windows], 
        name = args.name, 
        timeoutSeconds = int(args.timeout_seconds), 
        extraArguments = extraArguments)

if __name__ == '__main__':
    main(sys.argv[1:])