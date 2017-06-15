#!/bin/bash

function cleanup() {
  rm *\.bin
  rm result.txt debug.log
  rm -rf rvtmp
  rm a.out
}

cleanup &>/dev/null
