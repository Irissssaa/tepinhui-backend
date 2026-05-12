#!/usr/bin/env bash

# Wrapper script for systemd FailureAction
# systemd FailureAction only accepts a single executable path (no args)
# This wrapper calls the actual rollback script with sudo

exec /usr/bin/sudo /home/tph/scripts/rollback-on-failure.sh
