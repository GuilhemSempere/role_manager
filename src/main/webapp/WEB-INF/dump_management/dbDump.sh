#!/bin/sh


# Arguments parsing
OUTPUT="."
HOST="127.0.0.1:27017"

while [ $# -gt 0 ]; do
	case $1 in
		-h | --host)
			HOST="$2"
			shift; shift
			;;
		-o | --output)
			OUTPUT="$2"
			shift; shift
			;;
		-u | --username)
			USERNAME="$2"
			shift; shift
			;;
		-p | --password)
			PASSWORD="$2"
			shift; shift
			;;
		-d | --db | --database)
			DATABASE="$2"
			shift; shift
			;;
		-l | --log)
			LOGFILE=YES
			shift
			;;
		*)  # Unknown option
			echo "Unknown option $1"
			exit
			;;
	esac
done


# Arguments checks

if [ -z $HOST ]; then
	echo "You must specify the database host"
	exit
fi

if [ -z $DATABASE ]; then
	echo "You must specify a database to export"
	exit
fi

if [ ! -d $OUTPUT ]; then
	mkdir "$OUTPUT"
fi

if [ ! -d "$OUTPUT/$DATABASE" ]; then
	mkdir "$OUTPUT/$DATABASE"
fi

if [ -z $USERNAME ]; then
	CREDENTIAL_OPTIONS = "--username=$USERNAME --password=$PASSWORD --authenticationDatabase=admin"
fi 

FILENAME=$OUTPUT/$DATABASE/$DATABASE"_"`date +%Y-%m-%dT%H%M%S`".gz"



logged_part(){
	set -x
	mongodump -v $CREDENTIAL_OPTIONS --excludeCollectionsWithPrefix=tmpVar_ --excludeCollection=cachedCounts --excludeCollection=brapiGermplasmsSearches --host=$HOST --db=mgdb2_$DATABASE --archive=$FILENAME --gzip
}

if [ ! -z $LOGFILE ]; then
	logged_part 2>&1 | tee "$FILENAME-dump.log"
else
	logged_part
fi
