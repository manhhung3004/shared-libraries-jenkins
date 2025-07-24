#!/usr/bin/env groovy

/**
 * MLOps Pipeline - Main pipeline orchestrator
 * Defines the complete ML pipeline with all stages
 */
def call(Map config) {
    pipeline {
        agent any
        
        environment {
            // Environment variables
            DOCKER_REGISTRY = config.dockerRegistry ?: 'docker.io'
            MODEL_NAME = config.modelName ?: 'diabetes-prediction'
            MODEL_VERSION = "${env.BUILD_NUMBER}"
            PYTHON_VERSION = config.pythonVersion ?: '3.9'
            KUBECONFIG = credentials('kubeconfig')
        }
        
        stages {
            stage('Data Validation') {
                steps {
                    script {
                        dataValidation(config)
                    }
                }
            }
            
            stage('Model Training') {
                steps {
                    script {
                        modelTraining(config)
                    }
                }
            }
            
            stage('Model Validation') {
                steps {
                    script {
                        modelValidation(config)
                    }
                }
            }
            
            stage('Model Testing') {
                steps {
                    script {
                        modelTesting(config)
                    }
                }
            }
            
            stage('Model Packaging') {
                when {
                    anyOf {
                        branch 'main'
                        branch 'master'
                        branch 'develop'
                    }
                }
                steps {
                    script {
                        modelPackaging(config)
                    }
                }
            }
            
            stage('Model Deployment') {
                when {
                    anyOf {
                        branch 'main'
                        branch 'master'
                    }
                }
                steps {
                    script {
                        modelDeployment(config)
                    }
                }
            }
            
            stage('Model Monitoring Setup') {
                when {
                    anyOf {
                        branch 'main'
                        branch 'master'
                    }
                }
                steps {
                    script {
                        modelMonitoring(config)
                    }
                }
            }
        }
        
        post {
            always {
                // Archive artifacts
                archiveArtifacts artifacts: 'artifacts/**/*', fingerprint: true, allowEmptyArchive: true
                
                // Publish test results
                publishTestResults(
                    testResultsPattern: 'test-results/*.xml',
                    allowEmptyResults: true
                )
                
                // Clean workspace
                cleanWs()
            }
            
            success {
                echo "MLOps Pipeline completed successfully!"
                // Send notification
                sendNotification(config, 'SUCCESS')
            }
            
            failure {
                echo "MLOps Pipeline failed!"
                // Send notification
                sendNotification(config, 'FAILURE')
            }
        }
    }
}
