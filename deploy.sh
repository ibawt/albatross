#/bin/sh
set -x
set -e

HOST="ian@192.168.1.5"
RELEASE=`date "+%Y%m%d%H%M%S"`
DIST_DIR="/usr/local/albatross"
RELEASE_DIR="$DIST_DIR/$RELEASE"

JAR="$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')"

ssh $HOST "mkdir -p $RELEASE_DIR"
scp target/albatross-standalone.jar "$HOST:$RELEASE_DIR"

ssh $HOST "ln -sf $RELEASE_DIR/albatross-standalone.jar $DIST_DIR/current"

ssh $HOST "sudo service albatross restart"
