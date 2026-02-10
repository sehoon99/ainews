pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        AWS_DEFAULT_REGION = "ap-northeast-2"
        DEPLOY_BUCKET      = "ainews-prod-deploy"
        JAVA_HOME          = "/usr/lib/jvm/java-17-amazon-corretto"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Build') {
            steps {
                sh '''
                    export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
                    export PATH=$JAVA_HOME/bin:$PATH
                    ./gradlew :backend:ainews-server:build -x test
                '''
            }
        }

        stage('Upload JAR') {
            steps {
                sh 'aws s3 cp backend/ainews-server/build/libs/*-SNAPSHOT.jar s3://${DEPLOY_BUCKET}/app.jar'
            }
        }

        stage('Package Crawler Lambda') {
            steps {
                sh '''
                    cp news-crawler/*.py lambda/news-crawler/
                    pip3 install -t lambda/news-crawler/ -r lambda/news-crawler/requirements.txt
                    find lambda/news-crawler/ -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
                    find lambda/news-crawler/ -type d -name "*.dist-info" -exec rm -rf {} + 2>/dev/null || true
                    find lambda/news-crawler/ -type d -name "tests" -exec rm -rf {} + 2>/dev/null || true
                '''
            }
        }

        stage('Terraform Init') {
            steps {
                dir('infra/envs/p') {
                    sh 'terraform init'
                }
            }
        }

        stage('Terraform Plan') {
            steps {
                dir('infra/envs/p') {
                    sh 'terraform plan'
                }
            }
        }

        stage('Terraform Apply') {
            steps {
                input message: 'Apply Terraform changes?', ok: 'Apply'
                dir('infra/envs/p') {
                    sh 'terraform apply -auto-approve'
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                script {
                    def instanceId = sh(
                        script: "cd infra/envs/p && terraform output -raw ec2_instance_id",
                        returnStdout: true
                    ).trim()

                    def commandId = sh(
                        script: """
                            aws ssm send-command \
                                --instance-ids '${instanceId}' \
                                --document-name 'AWS-RunShellScript' \
                                --parameters 'commands=["aws s3 cp s3://${DEPLOY_BUCKET}/app.jar /opt/ainews/app.jar","systemctl restart ainews"]' \
                                --query 'Command.CommandId' \
                                --output text
                        """,
                        returnStdout: true
                    ).trim()

                    sh """
                        aws ssm wait command-executed \
                            --command-id '${commandId}' \
                            --instance-id '${instanceId}' || true
                    """
                }
            }
        }
    }
}