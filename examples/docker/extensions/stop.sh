#!/bin/bash
host="$1"
echo stop
for i in {1..100}; do
    echo "$i"
    sleep 1
done
# for s in {1..64} ;do
#     trap "echo trap $s" "$s"
# done
docker stop -t 120  "$host" && echo stopping && sleep  60
