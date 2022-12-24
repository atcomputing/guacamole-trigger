#!/bin/bash
host="$1"
echo start stop
for s in {1..64} ;do
    trap "echo trap $s" "$s"
done
docker stop -t 120  $host && echo stopping && sleep  60
