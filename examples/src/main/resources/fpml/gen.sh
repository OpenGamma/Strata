#!/bin/bash

export INPUT="v5_7/pretrade"
export OUTPUT="../../java"
export PACKAGE="com.opengamma.strata.examples.fpml.generated"

xjc -d "${OUTPUT}" -p ${PACKAGE} ${INPUT}
#Core 
#xjc -d "${OUTPUT}" -p ${PACKAGE} ${INPUT}/fpml-main-5-7.xsd 
#xjc -d "${OUTPUT}" -p ${PACKAGE} ${INPUT}/fpml-doc-5-7.xsd
#xjc -d "${OUTPUT}" -p ${PACKAGE} ${INPUT}/fpml-shared-5-7.xsd
#xjc -d "${OUTPUT}" -p ${PACKAGE} ${INPUT}/fpml-enum-5-7.xsd 
#xjc -d "${OUTPUT}" -p ${PACKAGE} ${INPUT}/fpml-asset-5-7.xsd 
#CD
#xjc -d "${OUTPUT}" -p ${PACKAGE} ${INPUT}/fpml-cd-5-7.xsd

exit 0
