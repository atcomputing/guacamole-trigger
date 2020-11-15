#!/bin/bash
# requruires:
#   gcloud

iam="$(basename "$0" )"


function usage(){

    printf -- "This script starts all vm's in a project for a specific user.\n"
    printf -- "including shared vm's\n"
    echo
    printf -- "Usage:"
    printf -- "  %s --host <hostname> ACTION \n" "$iam"
    echo
    printf -- "  Action: start, stop\n"
    printf -- "  -h, --help: Print this help message\n"
    printf -- "  -p, --project PROJECT-NAME\n"
    printf -- "  -z, --project ZONE-NAME\n"
    printf -- "  -n, --dry-run: Only show what it's going to do\n"
    printf -- "  -i, --service-account SERVICE-ACCOUNT-FILE\n"
    printf -- "     service account needs compute-rw permissions"
    printf -- "     no service-account is needed if guacamole host runc in google cloud platform and has a service account attached"
    echo
}
# default

while [ "$#" -gt 0 ]
do
    case $1 in

        "-h"|"--help")
            usage
            exit 0
        ;;
        "--host")
            shift
            host="$1"
            ;;

        --host=*)
            host=$(echo "$1"|cut -d= -f2-)
            ;;

        "-p"|"--project")
            shift
            project="$1"
        ;;

        -p=*|--project=*)
            project=$(echo "$1"|cut -d= -f2-)
        ;;

        "-z"|"--zone")
            shift
            project="$1"
        ;;

        -z=*|--zone=*)
            zone=$(echo "$1"|cut -d= -f2-)
        ;;

        "-n"|"--dry-run")
            dryRun=1
        ;;

        "-i"|"--service-account")
            shift
            serviceAccount="$1"
        ;;

        -i=*|--service-account=*)
            serviceAccount=$(echo "$1"|cut -d= -f2-)
        ;;

        *)
            if [ "$#" -eq 1 ]
            then
                if [[ $1 =~ ^(start|stop)$ ]]
                then
                    action="$1"
                else
                    echo Action can only be start or stop.
                fi
            else
                echo invalid argument: "$1"
                echo
                usage
                exit 1
            fi
        ;;
    esac
    shift
done


if [ -z "$host" ]
then
    echo no host specified,
    exit 1
fi
if [ -z "$action" ]
then
    echo no action specified,
    exit 1
fi

if [ -z "$project" ]
then
    project=atc-cursist-omgeving-test
    echo "warning: no project specified,"
    echo "warning: default to ${project}"
fi

if [ -z "$zone" ]
then
    zone="europe-west4-a"
    echo "warning: no zone specified,"
    echo "warning: default to ${zone}"
fi

[ -n "${serviceAccount}" ] && gcloud auth activate-service-account --key-file="${serviceAccount}"

if [ -n "${dryRun}" ]
then
    GCLOUD="echo gcloud"
else
    GCLOUD=gcloud
fi

${GCLOUD} compute instances "${action}" --zone "$zone" --quiet --project="${project}" "${host}" &

if [ -n "$host" ]
then
    timeout 60 gcloud compute instances tail-serial-port-output "$host" --zone "$zone" --port 1 || true
fi
