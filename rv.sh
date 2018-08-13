#!/bin/sh
# This is running under Ubuntu 16.04
# Install necessary packages. 
sudo apt-get install blablabla -y

# Build C program with RV-Match `kcc` command,
# Use `-fissue-report` flag to collect errors to `my_errors.json` file.
kcc -fissue-report=./my_errors.json main.c -o a.out

# Run the compiled program and collect run-time errors to `my_errors.json` file, which 
# will be used next step to generate HTML report.
./a.out 

# Generate a HTML report with `rv-html-report` command,
# and output the HTML report to `./report` directory. 
rv-html-report ./my_errors.json -o report

# Upload your HTML report to RV-Toolkit website with `rv-upload-report` command. 
rv-upload-report `pwd`/repor