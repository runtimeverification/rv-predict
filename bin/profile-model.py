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

def format_time(time_seconds):
    """Formats a time duration in a readable way."""
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
    return ''.join(time_pieces)

def run_with_time(args, timeout_seconds):
    """Runs the given command for the given time lenght"""
    start_time_seconds = time.time()
    process = subprocess.Popen(args, stdout=FNULL, stderr=FNULL)
    while (time.time() - start_time_seconds < timeout_seconds) and (process.poll() is None):
        time.sleep(0.1)
    elapsed_time_seconds = time.time() - start_time_seconds
    script_name = os.path.basename(sys.argv[0])
    subprocess.call(
        ['bash', '-c',
         "kill -9 $(ps aux | grep 'rv-predict.jar' | grep -v '%s' "
         + "| grep -v grep | awk '{print $2}')"
         % script_name],
        stdout=FNULL)
    return format_time(elapsed_time_seconds)

def run_rv_predict(rv_predict_jar_path, window_size, timeout_seconds, use_smt, extra_arguments):
    """Runs the rv-predict tool."""
    args = []
    args.extend(['java', '-ea', '-jar',
                 rv_predict_jar_path,])
    if window_size:
        args.extend(['--window', '%s' % 1000])
    if not use_smt:
        args.append('--no-smt')
    args.extend(extra_arguments)

    return run_with_time(args, timeout_seconds)

def run_for_windows(rv_predict_jar_path, windows, name, timeout_seconds, use_smt, extra_arguments):
    """Runs the rv-predict tool for all the window sizes given as argument."""
    if not windows:
        full_name = '%s timeout=%s use_smt=%s' % (name, timeout_seconds, use_smt)
        print >> sys.stderr, full_name
        formatted_time = run_rv_predict(
            rv_predict_jar_path=rv_predict_jar_path, window_size=None,
            timeout_seconds=timeout_seconds,
            use_smt=use_smt, extra_arguments=extra_arguments)
        print full_name, formatted_time
    for window in windows:
        full_name = ('%s %s timeout=%s use_smt=%s, window=%s'
                     % (datetime.datetime.now(), name, timeout_seconds, use_smt, window))
        print >> sys.stderr, full_name
        formatted_time = run_rv_predict(
            rv_predict_jar_path=rv_predict_jar_path, window_size=window,
            timeout_seconds=timeout_seconds, use_smt=use_smt,
            extra_arguments=extra_arguments)
        print full_name, formatted_time
        sys.stdout.flush()

def run_with_and_without_smt(rv_predict_jar_path, windows, name, timeout_seconds, extra_arguments):
    """Runs the rv-predict tool with the smt model first, then with the dynamic programming model,
       for all the window sizes given as argument."""
    run_for_windows(rv_predict_jar_path=rv_predict_jar_path,
                    windows=windows,
                    name=name,
                    timeout_seconds=timeout_seconds,
                    use_smt=True,
                    extra_arguments=extra_arguments)
    run_for_windows(rv_predict_jar_path=rv_predict_jar_path,
                    windows=windows,
                    name=name,
                    timeout_seconds=timeout_seconds,
                    use_smt=False,
                    extra_arguments=extra_arguments)

#TODO(virgil): it would be much nicer to read these from the output of
# dacapo -l.
DACAPO_TESTS = [
    'avrora', 'batik', 'fop', 'h2', 'jython', 'luindex', 'lusearch',
    'pmd', 'sunflow', 'tomcat', 'tradebeans', 'tradesoap', 'xalan']

def run_dacapo_tests(rv_predict_jar_path, dacapo_jar, windows, timeout_seconds):
    """Runs the rv-predict tool for all the dacapo tests, changing the model and window size."""
    for test in DACAPO_TESTS:
        run_with_and_without_smt(
            rv_predict_jar_path=rv_predict_jar_path,
            windows=windows,
            name='dacapo-%s' % test,
            timeout_seconds=timeout_seconds,
            extra_arguments=['-jar', dacapo_jar, test])

def read_extra_arguments(argv):
    """Returns the arguments before the first '--' argument and after it as two separate lists."""
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
    """Main function. Pylint wants a docstring."""
    rv_predict_default_jar_path = os.path.join(
        os.path.dirname(os.path.realpath(sys.argv[0])),
        '../target/release/rv-predict/rv-predict.jar')

    argv, extra_arguments = read_extra_arguments(argv)

    parser = argparse.ArgumentParser(description="Profile the rv-predict tool")

    parser.add_argument('--windows', '-w', action='append')
    parser.add_argument('--timeout-seconds', action='store')
    parser.add_argument('--name', action='store')
    parser.add_argument('--llvm-directory', dest='llvm_directory', action='store')
    parser.add_argument('--dacapo_jar', action='store')
    parser.add_argument('--rv_predict_jar', action='store', default=rv_predict_default_jar_path)
    args = parser.parse_args(argv)

    if args.dacapo_jar:
        run_dacapo_tests(
            rv_predict_jar_path=args.rv_predict_jar,
            dacapo_jar=args.dacapo_jar,
            windows=[int(w) for w in args.windows],
            timeout_seconds=int(args.timeout_seconds))
        return

    if args.llvm_directory:
        extra_arguments.extend(['--llvm-predict', args.llvm_directory])
    run_with_and_without_smt(
        rv_predict_jar_path=args.rv_predict_jar,
        windows=[int(w) for w in args.windows],
        name=args.name,
        timeout_seconds=int(args.timeout_seconds),
        extra_arguments=extra_arguments)

if __name__ == '__main__':
    main(sys.argv[1:])
