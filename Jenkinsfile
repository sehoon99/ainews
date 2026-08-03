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
                script {
                    env.IMAGE_TAG = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
                }
            }
        }

        stage('Backend Build') {
            steps {
                sh '''
                    export JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto
                    export PATH=$JAVA_HOME/bin:$PATH
                    ./gradlew :backend:ainews-server:build
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
                    sh 'TF_VAR_backend_image_tag="$IMAGE_TAG" terraform plan'
                }
            }
        }

        stage('Bootstrap ECR') {
            steps {
                input message: 'Apply Terraform changes?', ok: 'Apply'
                dir('infra/envs/p') {
                    sh 'terraform apply -target=module.ecs_backend.aws_ecr_repository.app -auto-approve'
                }
            }
        }

        stage('Build and Push Backend Image') {
            steps {
                sh '''
                    ECR_REPOSITORY=$(cd infra/envs/p && terraform output -raw ecr_repository_url)
                    ECR_REGISTRY=$(echo "$ECR_REPOSITORY" | cut -d/ -f1)
                    aws ecr get-login-password --region "$AWS_DEFAULT_REGION" \
                      | docker login --username AWS --password-stdin "$ECR_REGISTRY"
                    docker build -t "$ECR_REPOSITORY:$IMAGE_TAG" .
                    docker push "$ECR_REPOSITORY:$IMAGE_TAG"
                '''
            }
        }

        stage('Terraform Apply') {
            steps {
                dir('infra/envs/p') {
                    sh 'TF_VAR_backend_image_tag="$IMAGE_TAG" terraform apply -auto-approve'
                }
            }
        }

        stage('Deploy to ECS') {
            steps {
                sh '''
                    ECS_CLUSTER=$(cd infra/envs/p && terraform output -raw ecs_cluster_name)
                    ECS_SERVICE=$(cd infra/envs/p && terraform output -raw ecs_service_name)

                    aws ecs wait services-stable \
                      --cluster "$ECS_CLUSTER" \
                      --services "$ECS_SERVICE"
                '''
            }
        }

        stage('Deploy Frontend to CloudFront') {
            steps {
                sh '''
                    npm ci --prefix frontend
                    npm run build --prefix frontend

                    WEB_BUCKET=$(cd infra/envs/p && terraform output -raw web_bucket)
                    DISTRIBUTION_ID=$(cd infra/envs/p && terraform output -raw cloudfront_distribution_id)

                    aws s3 sync frontend/dist/ "s3://$WEB_BUCKET/" --delete
                    aws cloudfront create-invalidation \
                      --distribution-id "$DISTRIBUTION_ID" \
                      --paths '/*'
                '''
            }
        }

        stage('Deploy to Legacy EC2') {
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
