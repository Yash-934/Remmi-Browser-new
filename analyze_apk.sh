#!/bin/bash
APK=$1
echo "Analyzing $APK"
echo "Total Size: $(du -m "$APK" | cut -f1) MB" > APK_SIZE_REPORT.txt
echo "==============================================" >> APK_SIZE_REPORT.txt
echo "ABI Breakdown:" >> APK_SIZE_REPORT.txt
unzip -l "$APK" | grep "lib/arm64-v8a" | awk '{sum+=$1} END {print "arm64-v8a: " sum/1024/1024 " MB"}' >> APK_SIZE_REPORT.txt
unzip -l "$APK" | grep "lib/armeabi-v7a" | awk '{sum+=$1} END {print "armeabi-v7a: " sum/1024/1024 " MB"}' >> APK_SIZE_REPORT.txt
unzip -l "$APK" | grep "lib/x86/" | awk '{sum+=$1} END {print "x86: " sum/1024/1024 " MB"}' >> APK_SIZE_REPORT.txt
unzip -l "$APK" | grep "lib/x86_64/" | awk '{sum+=$1} END {print "x86_64: " sum/1024/1024 " MB"}' >> APK_SIZE_REPORT.txt
echo "==============================================" >> APK_SIZE_REPORT.txt
echo "Top 30 Largest Files:" >> APK_SIZE_REPORT.txt
unzip -l "$APK" | awk '$1 ~ /^[0-9]+$/ {print $1, $4}' | sort -nr | head -n 30 | awk '{print $1/1024/1024 " MB\t" $2}' >> APK_SIZE_REPORT.txt
cat APK_SIZE_REPORT.txt
