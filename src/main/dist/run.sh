#!/usr/bin/env bash
#
# cmdline params:
# --jsonApi   # download automatic gene descriptions via JSON API calls (legacy)
# --tsvFile   # download automatic gene descriptions via TSV file (recommeded)
#
. /etc/profile
APPNAME="gene-description-pipeline"
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/${APPNAME}.jar "$@" --tsvFile > run.log 2>&1

mailx -s "[$SERVER] Gene Description Pipeline run" mtutaj@mcw.edu < $APPDIR/logs/summary.log
