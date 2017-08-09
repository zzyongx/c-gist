#!/bin/bash

BINDIR=$(dirname "${BASH_SOURCE[0]}")
cd $BINDIR

if [[ -f apiorder ]]; then
  for file in `cat apiorder`; do
    echo "TEST $file.groovy"
    groovy $file
    if [[ $? != 0 ]]; then
      echo "TEST $file FAILED"
      exit -1
    fi
  done
else
  test -f prepare.groovy && groovy prepare.groovy
  for file in *.groovy ; do
    if [[ $file != "prepare.groovy" && $file != "destroy.groovy" ]]; then
      echo "TEST $file"
      groovy $file
      if [[ $? != 0 ]]; then
        echo "TEST $file FAILED"
        exit -1
      fi
    fi
  done

  test -f destroy.groovy && groovy destroy.groovy
fi
