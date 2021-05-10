#!/bin/bash

SN=$1
PACKAGE_NAME='com.boolbird.keepalive'

for i in {1..100}
do
echo "kill process $i times"
echo $(adb -s $SN shell ps | grep "$PACKAGE_NAME")
adb -s $SN shell am force-stop $PACKAGE_NAME
# 0.01=10ms
#sleep 0.005
#sleep 0.01
sleep 1
done

sleep 1
echo $(adb -s $SN shell ps | grep "$PACKAGE_NAME")