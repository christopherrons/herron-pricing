#!/bin/bash

#
LXC_USER=herron

COMMON_VERSION_FILE="pricing-engine-running.txt"
RELEASE_DIR="releases"
JVM_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"