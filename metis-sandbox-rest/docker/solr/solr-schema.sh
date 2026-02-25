#!/bin/bash

function main(){
  declare_common_fields
  declare_multiple_environments_fields
  set_chosen_environment_fields
  print_chosen_environment_and_options
  git_get_selected_branch # Should set the COMMIT_HASH
  rsync_local_dir_to_remote_dir
  git_remove_pull_request_branches
  zookeeper_find_current_and_old_configurations
  zookeeper_upload_and_apply_new_configuration #Expects COMMIT_HASH
  zookeeper_remove_current_and_old_configurations
}

function declare_common_fields() {
  #We assume git is present on workspace from the configuration of the job. Ideally same git directory should not be accessible from multiple jobs, to avoid conflicts.
  GIT_REPO_DIR=$(pwd)/
  GIT_SOLR_CONF_SUBDIR=solr_confs/metadata/conf/
  TARGET_SOLR_CONF_ROOT_DIR=/opt/solr/solr_configurations/
  PULL_REQUEST_PREFIX=pull_request_
}

function declare_multiple_environments_fields() {
  #The server has to have a zookeeper running for uploading the configuration.
  ENVIRONMENT="LOCAL"
  INDEX_ENVIRONMENT="PUBLISH"
  BRANCH_OR_PR_NUMBER="master"
  LOCAL_SOLR_SERVER=metis-sandbox-solr
  LOCAL_ZOOKEEPER_SERVER=localhost
  LOCAL_ZOOKEEPER_PORT="9983"
  LOCAL_SOLR_PORT="8983"
  LOCAL_SOLR_BINARIES_DIR=/opt/solr/
  LOCAL_PUBLISH_COLLECTION=metis_sandbox_publish_local
  LOCAL_PUBLISH_SOLR_CONF_DIR=local_publishConf
}

function set_chosen_environment_fields() {
  #Initialize variables based on the chosen environment
  TARGET_COMMAND_SERVER=${LOCAL_SOLR_SERVER}
  ZOOKEEPER_PORT=${LOCAL_ZOOKEEPER_PORT}
  SOLR_PORT=${LOCAL_SOLR_PORT}
  SOLR_BINARIES_DIR=${LOCAL_SOLR_BINARIES_DIR}

  COLLECTION_NAME=${LOCAL_PUBLISH_COLLECTION}
  TARGET_SOLR_CONF_DIR=${LOCAL_PUBLISH_SOLR_CONF_DIR}
}

function print_chosen_environment_and_options() {
  printf "%-40s \n" "Selected environment is:"
  printf "%-40s %s\n" "Environment selected:" "${ENVIRONMENT}"
  printf "%-40s %s\n" "Index environment selected:" "${INDEX_ENVIRONMENT}"
  printf "%-40s %s\n" "Branch or PR specified:" "${BRANCH_OR_PR_NUMBER}"
  printf "%-40s %s\n" "Server to execute update:" "${TARGET_COMMAND_SERVER}"
  printf "%-40s %s\n" "Collection name chosen:" "${COLLECTION_NAME}"
  printf "%-40s %s\n" "Target solr configuration directory:" "${TARGET_SOLR_CONF_DIR}"
  printf "%-40s %s\n" "Zookeeper port:" "${ZOOKEEPER_PORT}"
  printf "%-40s %s\n" "Solr port:" "${SOLR_PORT}"
}

function git_get_selected_branch() {
  #Check first if there is a branch
  git -C "${GIT_REPO_DIR}" checkout "${BRANCH_OR_PR_NUMBER}"
  if [ "$?" -ne "0" ]; then
    printf "WARNING: Branch: %s, could not be found. Trying pull request..\n" "${BRANCH_OR_PR_NUMBER}"
    #Verify first if the value is actually a number
    number_regex='^[0-9]+$'
    if ! [[ ${BRANCH_OR_PR_NUMBER} =~ ${number_regex} ]]; then
      printf "ERROR: Value %s is not a number. Exiting..\n" "${BRANCH_OR_PR_NUMBER}"
      exit 1
    fi
    git -C "${GIT_REPO_DIR}" fetch -u origin "pull/${BRANCH_OR_PR_NUMBER}/head:${PULL_REQUEST_PREFIX}${BRANCH_OR_PR_NUMBER}"
    #Verify if BRANCH_OR_PR_NUMBER specified could be fetched
    if [ "$?" -ne "0" ]; then
      printf "ERROR: Could not create branch from PR with number: %s. Exiting..\n" "${BRANCH_OR_PR_NUMBER}"
      exit 1
    fi
    git -C "${GIT_REPO_DIR}" checkout "${PULL_REQUEST_PREFIX}${BRANCH_OR_PR_NUMBER}"
    printf "Show git summary of PR:\n"
    git -C "${GIT_REPO_DIR}" show --summary
  else
    git -C "${GIT_REPO_DIR}" pull
  fi
  COMMIT_HASH="$(git rev-parse --short "${BRANCH_OR_PR_NUMBER}" | tr -d '\n')"
}

function rsync_local_dir_to_remote_dir() {
  #TODO This could be avoided and the config send directly to zookeeper. The downside would be that we always send all files.
  local source="${GIT_REPO_DIR}${GIT_SOLR_CONF_SUBDIR}"
  #local destination="$TARGET_COMMAND_SERVER:${TARGET_SOLR_CONF_ROOT_DIR}${TARGET_SOLR_CONF_DIR}"
  local destination="${TARGET_SOLR_CONF_ROOT_DIR}${TARGET_SOLR_CONF_DIR}"
  printf "Starting rsync from local directory: %s -> to directory: %s\n" "${source}" "${destination}"
  mkdir -p "${destination}"
  rsync --archive --compress --verbose --delete "${source}" "${destination}"
}

function git_remove_pull_request_branches() {
  #Delete pull requests to avoid excessive diskspace
  #Sed trims leading and trailing spaces
  git checkout master
  local pull_request_branches
  pull_request_branches=$(git branch | sed 's/^ *//;s/ *$//' | grep ${PULL_REQUEST_PREFIX})

  #Set the field separator to new line
  IFS=$'\n'
  for pull_request_branch in $pull_request_branches
  do
    printf "Delete pull request: %s\n" "${pull_request_branch}"
    git branch --delete --force "${pull_request_branch}"
  done
  #Reset IFS
  IFS=$' \t\n'
}

function zookeeper_find_current_and_old_configurations() {
  printf "Check if there is current and old configuration.\n"
  local zookeeper_command
  zookeeper_command=$(zookeeper_create_command "-cmd ls /configs")

  #Finds configurations of format #/configs/${COLLECTION_NAME}_<anything that is not a /> or auto created configurations such as /configs/${COLLECTION_NAME}.AUTOCREATED
  #Temporary also remove the #/configs/${TARGET_SOLR_CONF_DIR}_<anything that is not a /> (the first sed group)
  CURRENT_AND_OLD_CONFIGURATION_PATHS=$(eval "$(echo "${zookeeper_command} | sed -n 's/^\s*\(\/configs\/\(${TARGET_SOLR_CONF_DIR}_[^\/]*\|${COLLECTION_NAME}_[^\/]*\|${COLLECTION_NAME}.AUTOCREATED\)\)\s.*$/\1/p'")")
  echo "current and old configurations: ${CURRENT_AND_OLD_CONFIGURATION_PATHS}"
}

function zookeeper_upload_and_apply_new_configuration() {
  local date_stamp
  date_stamp=$(date --iso-8601=seconds)
  local new_configuration_name=${COLLECTION_NAME}_${COMMIT_HASH}_${date_stamp}

  printf "Uploading zookeeper new configuration: %s\n" "${new_configuration_name}"
  local zookeeper_command
  zookeeper_command=$(zookeeper_create_command "-cmd upconfig --confdir ${TARGET_SOLR_CONF_ROOT_DIR}${TARGET_SOLR_CONF_DIR} --confname ${new_configuration_name}")
  $(echo "${zookeeper_command}")
  #Update collection to take effect of the new configuration.
  #Using MODIFYCOLLECTION instead of RELOAD command because MODIFYCOLLECTION will re-apply the mapping between the collection with the configuration name and then reload it. This helps to avoid the collection being mapped to another configuration name.
  printf "Starting solr MODIFYCOLLECTION command\n"
  $(echo "curl -v --get --data-urlencode collection=${COLLECTION_NAME} --data-urlencode collection.configName=${new_configuration_name} http://localhost:${SOLR_PORT}/solr/admin/collections?action=MODIFYCOLLECTION")
}

function zookeeper_remove_current_and_old_configurations() {
  local zookeeper_command
  #Set the field separator to new line
  echo "Remove current and old configuration"
  IFS=$'\n'
  for configuration_path in $CURRENT_AND_OLD_CONFIGURATION_PATHS
  do
    printf "Removing zookeeper configuration: %s\n" "${configuration_path}"
    zookeeper_command=$(zookeeper_create_command "-cmd clear $(echo "${configuration_path}")")
    $(eval "${zookeeper_command}")
  done
  #Reset IFS
  IFS=$' \t\n'
}

function zookeeper_create_command() {
  #First argument should be the '-cmd' part onwards
  local common_command_part="java -Dlog4j.configurationFile=file://${SOLR_BINARIES_DIR}server/resources/log4j2.xml -classpath .:${SOLR_BINARIES_DIR}server/lib/ext/*:${SOLR_BINARIES_DIR}server/solr-webapp/webapp/WEB-INF/lib/* org.apache.solr.cloud.ZkCLI -zkhost ${LOCAL_ZOOKEEPER_SERVER}:${ZOOKEEPER_PORT}"
  local unique_command_part="${1}"
  echo "${common_command_part} ${unique_command_part}"
}

function execute_remote_ssh_command(){
  #ssh ${TARGET_COMMAND_SERVER}  "${1}"
  "${1}"
}

main "$@"
