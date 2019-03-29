#!/usr/bin/env bash
#
# GeneDescription pipeline cmdline params:
# --jsonApi   # download automatic gene descriptions via JSON API calls
# --tsvFile   # download automatic gene descriptions via TSV file
#
. /etc/profile
APPNAME=GeneDescPipeline
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar lib/${APPNAME}.jar "$@" --tsvFile > run.log 2>&1

mailx -s "[$SERVER] Gene Description Pipeline run" mtutaj@mcw.edu < $APPDIR/logs/summary.log
