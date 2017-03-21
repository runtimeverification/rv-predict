import argparse
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

def runWithTime(args):
    start_time_seconds = time.time()
    p = subprocess.Popen(args, stdout=FNULL, stderr=FNULL)
    p.wait()
    elapsed_time_seconds = time.time() - start_time_seconds
    printTime(elapsed_time_seconds)

"""
def runJavaTool(windowSize, useSmt, javaArguments):
    args = ['timeout', '10m', 'java', '-ea', '-jar', '../../rv-predict/target/release/rv-predict/rv-predict.jar',]
    if windowSize:
        args.extend(['--window', '%s' % 1000])
    if not useSmt:
        args.append('--no-smt')
    args.extend(javaArguments)
    run(args)

def runCppTool(windowSize = None, useSmt):
    args = ['timeout', '10m', 'java', '-ea', '-jar', '../../rv-predict/target/release/rv-predict/rv-predict.jar', '--llvm-predict', '"."',]
    if windowSize:
        args.extend(['--window', '%s' % 1000])
    if not useSmt:
        args.append('--no-smt')
    args.extend(javaArguments)
    run(args)
"""
def runPredictTool(windowSize, timeout, useSmt, extraArguments):
    args = []
    if timeout:
        args.extend(['timeout', '10m', ])

    #TODO(virgil): this is horrible, I should pass the path as an argument or something.
    args.extend(['java', '-ea', '-jar', os.path.join(os.path.dirname(os.path.realpath(sys.argv[0])), '../target/release/rv-predict/rv-predict.jar'),])
    if windowSize:
        args.extend(['--window', '%s' % 1000])
    if not useSmt:
        args.append('--no-smt')
    args.extend(extraArguments)

    runWithTime(args)

def runForWindows(windows, name, timeout, useSmt, extraArguments):
    if not windows:
        print '%s timeout=%s useSmt=%s' % (name, timeout, useSmt),
        sys.stdout.flush()
        runPredictTool(timeout = timeout, useSmt = useSmt, extraArguments = extraArguments)
        return
    for w in windows:
        print '%s timeout=%s useSmt=%s, window=%s' % (name, timeout, useSmt, w),
        sys.stdout.flush()
        runPredictTool(windowSize = w, timeout = timeout, useSmt = useSmt, extraArguments = extraArguments)

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

def main(argv):
    argv, extraArguments = readExtraArguments(argv)

    parser = argparse.ArgumentParser(description = "Profile the rv-predict tool")

    parser.add_argument('--windows', '-w', action='append')
    parser.add_argument('--timeout', action='store')
    #parser.add_argument('--use-smt', action='store_const', const=True, default=False)
    parser.add_argument('--name', action='store')
    parser.add_argument('--llvm-directory', dest='llvm_directory', action='store')
    args = parser.parse_args(argv)

    if args.llvm_directory:
        extraArguments.extend(['--llvm-predict', args.llvm_directory])
    runForWindows(windows = [int(w) for w in args.windows], name = args.name, timeout = args.timeout, useSmt = True, extraArguments = extraArguments)
    runForWindows(windows = [int(w) for w in args.windows], name = args.name, timeout = args.timeout, useSmt = False, extraArguments = extraArguments)

if __name__ == '__main__':
    main(sys.argv[1:])