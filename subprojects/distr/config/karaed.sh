#!/bin/bash
mydir=$(dirname "$0")
"$mydir/../runtime/bin/java" "@$mydir/options.args" "-Dapp.rootDir=$mydir" -jar "$mydir/gui.jar" "$@"
