pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['production'],
            description: 'Deployment environment'
        )
        booleanParam(
            name: 'RUN_DEPLOY',
            defaultValue: true,
            description: 'Run Ansible deployment after build'
        )
        string(
            name: 'ANSIBLE_INVENTORY',
            defaultValue: 'ansible/inventories/production.ini',
            description: 'Path to Ansible inventory file'
        )
        string(
            name: 'ANSIBLE_LIMIT',
            defaultValue: 'demo_message_servers',
            description: 'Inventory host/group limit used by ansible-playbook'
        )
        string(
            name: 'SSH_CREDENTIALS_ID',
            defaultValue: 'ansible-ssh-key',
            description: 'Jenkins SSH private key credential ID used by Ansible'
        )
    }

    environment {
        APP_NAME = 'demo-message-service'
        JAR_FILE = 'target/demo-message-service.jar'
        DEPLOY_BUNDLE_DIR = 'target/deploy'
        VERSION_FILE = 'target/deploy/VERSION'
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate Toolchain') {
            steps {
                sh '''
                    set -eu
                    java -version
                    mvn -version
                    ansible --version
                '''
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B clean test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh '''
                    set -eu
                    mvn -B -DskipTests package
                    test -f "${JAR_FILE}"
                    mkdir -p "${DEPLOY_BUNDLE_DIR}"

                    PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
                    GIT_COMMIT_SHORT=$(git rev-parse --short HEAD 2>/dev/null || echo unknown)

                    cat > "${VERSION_FILE}" <<VERSION_EOF
app=${APP_NAME}
version=${PROJECT_VERSION}
build_number=${BUILD_NUMBER}
git_commit=${GIT_COMMIT_SHORT}
build_url=${BUILD_URL}
built_at_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)
VERSION_EOF

                    sha256sum "${JAR_FILE}" | tee "${DEPLOY_BUNDLE_DIR}/${APP_NAME}.jar.sha256"
                    cp src/main/resources/application.yml "${DEPLOY_BUNDLE_DIR}/application.yml"
                '''
            }
        }

        stage('Archive Build Artifacts') {
            steps {
                archiveArtifacts artifacts: 'target/demo-message-service.jar,target/deploy/**', fingerprint: true
            }
        }

        stage('Ansible Syntax Check') {
            when {
                expression { return params.RUN_DEPLOY }
            }
            steps {
                sh '''
                    set -eu
                    ansible-playbook \
                      -i "${ANSIBLE_INVENTORY}" \
                      ansible/deploy.yml \
                      --limit "${ANSIBLE_LIMIT}" \
                      --syntax-check
                '''
            }
        }

        stage('Deploy with Ansible') {
            when {
                expression { return params.RUN_DEPLOY }
            }
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: params.SSH_CREDENTIALS_ID,
                        keyFileVariable: 'ANSIBLE_PRIVATE_KEY',
                        usernameVariable: 'ANSIBLE_SSH_USER'
                    )
                ]) {
                    sh '''
                        set -eu

                        ansible-playbook \
                        -i "${ANSIBLE_INVENTORY}" \
                        ansible/deploy.yml \
                        --limit "${ANSIBLE_LIMIT}" \
                        --private-key "${ANSIBLE_PRIVATE_KEY}" \
                        --extra-vars "deploy_env=${DEPLOY_ENV}" \
                        --extra-vars "artifact_src=${WORKSPACE}/${JAR_FILE}" \
                        --extra-vars "app_config_src=${WORKSPACE}/${DEPLOY_BUNDLE_DIR}/application.yml" \
                        --extra-vars "version_src=${WORKSPACE}/${VERSION_FILE}"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "CI/CD completed for ${APP_NAME}. Artifact: ${JAR_FILE}"
        }
        failure {
            echo "CI/CD failed for ${APP_NAME}. Check console log and Ansible output."
        }
    }
}
