#!/usr/bin/env bash

usage="
Manage the CORBA app build and run state.

$(basename "$0") [start|restart|stop|rebuild|client] [--no-rebuild]

    where:
      start: rebuild files and then this will check if services are running and start if not.
      restart: preforms a kill and then runs the start process
      stop: kills running services
      rebuild: rebuild files only
      client: rebuild and run client
      --no-rebuild: do not rebuild files automatically on the restart or start process

"

idl_name='Order'
idl_hash=$(md5sum "$idl_name.idl")
server_name='OrderServer'
client_name='OrderClient'
app_name='OrderApp'
orbd_port=22568
server_port=22569
services=( 'orbd' "$server_name")
rebuild=true

function server_rebuild {

  if [[ -d server ]]; then
    app_rebuild
    find server/ -type f -name "*.java" -exec javac {} \;
  else
    echo -e "This isn't the right directory. I'm expecting to find 'server' here.\n\tCurrently in: $(pwd)"
  fi
}

function client_rebuild {

  if [[ -d client ]]; then
    app_rebuild
    find client/ -type f -name "*.java" -exec javac {} \;
  else
    echo -e "This isn't the right directory. I'm expecting to find 'client' here.\n\tCurrently in: $(pwd)"
  fi
}

function app_rebuild {
  test -f idl_hash || touch idl_hash

  if [[ "$idl_hash" = $(< idl_hash) ]]; then
    :
  else
    idlj -fall "${idl_name}.idl"
    md5sum "${idl_name}.idl" > idl_hash
    if [[ -d $app_name ]]; then
      find "$app_name/" -type f -name "*.java" -exec javac {} \;
    fi
  fi

}

function start_orbd {

  touch orbd.pid
  running=$(<orbd.pid)

  if [[ -z "${running}" ]]; then

    orbd \
      -ORBInitialPort $server_port \
      -ORBInitialHost localhost \
      -port $orbd_port &

    pid=$!

    echo "Running orbd($pid) on $orbd_port"
    echo $pid > orbd.pid
  else
    echo -e "Already running orbd($running) on $orbd_port"
  fi

}

function start_server {

  touch "$server_name".pid
  running=$(<"$server_name".pid)

  if [[ -z "${running}" ]]; then

    java \
      -classpath "$(pwd):$(pwd)/server" \
      "$server_name" \
      -ORBInitialPort $server_port \
      -ORBInitialHost localhost &
    
    pid=$!  

    echo "Running $server_name($pid) on $server_port"
    echo "$pid" > "$server_name.pid"
  else
    echo -e "Already running $server_name($running) on $server_port"
  fi

}

function client {

  java \
    -classpath "$(pwd):$(pwd)/client" \
    "$client_name" \
    -ORBInitialPort $server_port \
    -ORBInitialHost localhost

}

function killer {
  # I kill things
  for service in "${services[@]}"; do
    if [[ -f "$service.pid" ]]; then
      echo "Killing $service"
      pkill -F "$service.pid"
      rm -f "$service.pid"
    fi
  done
}

case $2 in
  --no-rebuild)
    rebuild=false
  ;;
  --service)
    services=( "$3" )
    echo "Only preforming operations on:"
    printf '• %s\n' "${services[@]}"
  ;;
esac

case $1 in
  start)
    if [[ $rebuild = true ]]; then
      server_rebuild
    fi
    start_orbd
    sleep 5
    start_server
    ;;
  restart)
    killer
    if [[ $rebuild = true ]]; then
      server_rebuild
    fi
    start_orbd
    sleep 2
    start_server
    ;;
  stop)
    killer
    ;;
  rebuild)
    server_rebuild
    client_rebuild
    ;;
  client)
    if [[ $rebuild = true ]]; then
      client_rebuild
    fi
    client
  ;;
  *)
    echo -e "$usage"
    ;;
esac