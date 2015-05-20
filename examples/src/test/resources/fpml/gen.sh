#!/bin/bash

export INPUT="test/xsd"
export OUTPUT="../../java"
export PACKAGE="com.opengamma.strata.examples.fpml.generated"


xjc -d "${OUTPUT}" -p ${PACKAGE} ${INPUT}/note.xsd

exit 0
