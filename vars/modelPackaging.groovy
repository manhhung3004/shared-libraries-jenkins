#!/usr/bin/env groovy

/**
 * Model Packaging Stage
 * Packages model into Docker containers and creates deployment artifacts
 */
def call(Map config) {
    echo " Starting Model Packaging..."
    
    try {
        // Build Docker image for model API
        script {
            def dockerImage = "${env.DOCKER_REGISTRY}/${config.modelName ?: 'diabetes-prediction'}:${env.MODEL_VERSION}"
            
            echo "Building Docker image: ${dockerImage}"
            
            sh """
                # Copy model artifacts to Docker context
                mkdir -p docker/models
                cp artifacts/models/best_model.pkl docker/models/
                
                # Build Docker image
                docker build -t ${dockerImage} -f Dockerfile .
                
                # Tag as latest if on main branch
                if [[ "${env.BRANCH_NAME}" == "main" || "${env.BRANCH_NAME}" == "master" ]]; then
                    docker tag ${dockerImage} ${env.DOCKER_REGISTRY}/${config.modelName ?: 'diabetes-prediction'}:latest
                fi
            """
            
            // Security scan of Docker image
            if (config.runSecurityScan) {
                echo "Scanning Docker image for vulnerabilities..."
                sh """
                    # Install and run Trivy for vulnerability scanning
                    docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \\
                        -v \$(pwd)/artifacts:/tmp/artifacts \\
                        aquasec/trivy image --format json --output /tmp/artifacts/trivy-report.json ${dockerImage} || echo "Vulnerability scan completed"
                """
            }
            
            // Push Docker image to registry
            if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop') {
                withCredentials([usernamePassword(credentialsId: config.dockerCredentialsId ?: 'docker-registry', 
                                                usernameVariable: 'DOCKER_USERNAME', 
                                                passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh """
                        echo "Pushing Docker image to registry..."
                        echo \$DOCKER_PASSWORD | docker login ${env.DOCKER_REGISTRY} -u \$DOCKER_USERNAME --password-stdin
                        docker push ${dockerImage}
                        
                        if [[ "${env.BRANCH_NAME}" == "main" || "${env.BRANCH_NAME}" == "master" ]]; then
                            docker push ${env.DOCKER_REGISTRY}/${config.modelName ?: 'diabetes-prediction'}:latest
                        fi
                    """
                }
            }
            
            // Store image info for deployment
            writeFile file: 'artifacts/docker-image.txt', text: dockerImage
        }
        
        // Generate Kubernetes deployment manifests
        sh """
            echo "Generating Kubernetes manifests..."
            
            # Create deployment directory
            mkdir -p artifacts/k8s-manifests
            
            # Generate manifests with correct image tag
            sed 's|{{IMAGE_TAG}}|${env.MODEL_VERSION}|g' k8s/deployment.yml > artifacts/k8s-manifests/deployment.yml
            sed 's|{{IMAGE_TAG}}|${env.MODEL_VERSION}|g' k8s/service.yml > artifacts/k8s-manifests/service.yml
            
            # Copy other manifests
            cp k8s/configmap.yml artifacts/k8s-manifests/ || echo "No configmap found"
            cp k8s/ingress.yml artifacts/k8s-manifests/ || echo "No ingress found"
            cp k8s/hpa.yml artifacts/k8s-manifests/ || echo "No HPA found"
        """
        
        // Create Helm chart (if configured)
        if (config.useHelm) {
            sh """
                echo "Packaging Helm chart..."
                
                # Update Chart.yaml with new version
                sed -i 's/version: .*/version: ${env.MODEL_VERSION}/' helm-chart/Chart.yaml
                sed -i 's/appVersion: .*/appVersion: ${env.MODEL_VERSION}/' helm-chart/Chart.yaml
                
                # Package Helm chart
                helm package helm-chart --destination artifacts/helm-charts/
                
                # Generate values for different environments
                cp helm-chart/values-dev.yaml artifacts/helm-charts/ || echo "No dev values found"
                cp helm-chart/values-prod.yaml artifacts/helm-charts/ || echo "No prod values found"
            """
        }
        
        // Generate model metadata
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            
            echo "Generating model metadata..."
            python src/packaging/generate_metadata.py \\
                --model-path artifacts/models/best_model.pkl \\
                --version ${env.MODEL_VERSION} \\
                --output artifacts/model-metadata.json
        """
        
        // Archive packaging artifacts
        archiveArtifacts artifacts: 'artifacts/k8s-manifests/**/*', fingerprint: true
        archiveArtifacts artifacts: 'artifacts/helm-charts/**/*', allowEmptyArchive: true
        archiveArtifacts artifacts: 'artifacts/model-metadata.json', fingerprint: true
        archiveArtifacts artifacts: 'artifacts/docker-image.txt', fingerprint: true
        archiveArtifacts artifacts: 'artifacts/trivy-report.json', allowEmptyArchive: true
        
        echo "Model Packaging completed successfully!"
        
    } catch (Exception e) {
        echo "Model Packaging failed: ${e.getMessage()}"
        
        // Clean up Docker images on failure
        sh """
            docker rmi ${env.DOCKER_REGISTRY}/${config.modelName ?: 'diabetes-prediction'}:${env.MODEL_VERSION} || echo "Failed to remove Docker image"
        """
        
        throw e
    }
}
