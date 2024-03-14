#!/usr/bin/env bash


usage="
Manage the CORBA app build and run state.

$(basename "$0") [start|restart|stop|rebuild] [--no-rebuild]

    where:
      start: rebuild files and then this will check if services are running and start if not.
      restart: preforms a kill and then runs the start process
      stop: kills running services
      rebuild: rebuild files only
      --no-rebuild: do not rebuild files automatically on the restart or start process

"

app_name='Hello'
server_name='HelloServer'
orbd_port=22568
server_port=22569
services=( 'orbd' "$server_name")
rebuild=true

function rebuild {
  if [[ -d src ]]; then
    idlj -fall "src/${app_name}.idl"
    find src/ -type f -name "*.java" -exec javac {} \;
  else
    echo -e "This isn't the right directory. I'm expecting to find 'src' here.\n\tCurrently in: $(pwd)"
  fi
}

function start_orbd {

  running=$(pgrep orbd -u "$(whoami)")
  if [[ -z "${running}" ]]; then
    orbd -ORBInitialPort $server_port -ORBInitialHost localhost -port $orbd_port &
    running=$(pgrep orbd -u "$(whoami)")
    echo -e "Running orbd($running) on $orbd_port"
  else
    echo -e "Already running orbd($running) on $orbd_port"
  fi
}

function start_server {
  running=$(pgrep -f "$server_name" -u "$(whoami)")
  if [[ -z "${running}" ]]; then
    java -classpath "$(pwd)/src" "$server_name" -ORBInitialPort $server_port -ORBInitialHost localhost >/dev/null &
    running=$(pgrep -f "$server_name" -u "$(whoami)")
    echo -e "Running $server_name($running) on $server_port"
  else
    echo -e "Already running $server_name($running) on $server_port"
  fi
}

function killer {
  # I kill things
  for service in "${services[@]}"; do
    pkill -u "$(whoami)" -f "$service"
  done
}

case $2 in
  --no-rebuild)
    rebuild=false
  ;;
esac

case $1 in
  start)
    if [[ $rebuild = true ]]; then
      rebuild
    fi
    start_orbd
    sleep 5
    start_server
    ;;
  restart)
    killer
    if [[ $rebuild = true ]]; then
      rebuild
    fi
    start_orbd
    sleep 5
    start_server
    ;;
  stop)
    killer
    ;;
  rebuild)
    rebuild
    ;;
  *)
    echo -e "$usage"
    ;;
esac