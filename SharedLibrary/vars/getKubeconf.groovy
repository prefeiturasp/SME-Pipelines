#!/usr/bin/env groovy

def call(String branchName) {

    if("main".equals(branchName)) { return "config_prod"; }
    else if ("master".equals(branchName)) { return "config_prod"; }
    else if ("release".equals(branchName)) { return "config_release"; }
    else if ("homolog".equals(branchName)) { return "config_release"; }
    else if ("test".equals(branchName)) { return "config_release"; }
    else if ("testes".equals(branchName)) { return "config_release"; }
    else if ("teste".equals(branchName)) { return "config_release"; }
}