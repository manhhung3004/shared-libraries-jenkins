#!/usr/bin/env groovy

/**
 * Model Deployment Stage
 * Deploys model to different environments (dev, staging, production)
 */
def call(Map config) {
    echo "🚀 Starting Model Deployment..."
    
    try {
        // Determine deployment environment based on branch
        def environment = getDeploymentEnvironment()
        def namespace = config.namespace ?: "mlops-${environment}"
        
        echo "🌍 Deploying to environment: ${environment}"
        echo "📦 Using namespace: ${namespace}"
        
        // Read Docker image info
        def dockerImage = readFile('artifacts/docker-image.txt').trim()
        echo "🐳 Deploying image: ${dockerImage}"
        
        // Deploy using Kubernetes manifests
        if (!config.useHelm) {
            sh """
                echo "☸️ Deploying with Kubernetes manifests..."
                
                # Apply ConfigMap first (if exists)
                kubectl apply -f artifacts/k8s-manifests/configmap.yml -n ${namespace} || echo "No ConfigMap to apply"
                
                # Apply Deployment
                kubectl apply -f artifacts/k8s-manifests/deployment.yml -n ${namespace}
                
                # Apply Service
                kubectl apply -f artifacts/k8s-manifests/service.yml -n ${namespace}
                
                # Apply Ingress (if exists)
                kubectl apply -f artifacts/k8s-manifests/ingress.yml -n ${namespace} || echo "No Ingress to apply"
                
                # Apply HPA (if exists)
                kubectl apply -f artifacts/k8s-manifests/hpa.yml -n ${namespace} || echo "No HPA to apply"
            """
        } else {
            // Deploy using Helm
            sh """
                echo "⛵ Deploying with Helm..."
                
                # Install or upgrade Helm release
                helm upgrade --install ${config.modelName ?: 'diabetes-prediction'} \\
                    artifacts/helm-charts/${config.modelName ?: 'diabetes-prediction'}-${env.MODEL_VERSION}.tgz \\
                    --namespace ${namespace} \\
                    --create-namespace \\
                    --values artifacts/helm-charts/values-${environment}.yaml \\
                    --set image.tag=${env.MODEL_VERSION} \\
                    --wait --timeout=10m
            """
        }
        
        // Wait for deployment to be ready
        sh """
            echo "⏳ Waiting for deployment to be ready..."
            kubectl rollout status deployment/${config.modelName ?: 'diabetes-prediction'} -n ${namespace} --timeout=600s
        """
        
        // Health check
        script {
            def healthCheckPassed = false
            def retries = 10
            
            for (int i = 0; i < retries; i++) {
                try {
                    sh """
                        echo "🏥 Performing health check (attempt ${i + 1}/${retries})..."
                        
                        # Port forward for health check
                        kubectl port-forward service/${config.modelName ?: 'diabetes-prediction'} 8080:80 -n ${namespace} &
                        PF_PID=\$!
                        
                        sleep 5
                        
                        # Health check
                        curl -f http://localhost:8080/health || exit 1
                        
                        # Kill port forward
                        kill \$PF_PID || echo "Port forward already stopped"
                    """
                    healthCheckPassed = true
                    break
                } catch (Exception e) {
                    echo "❌ Health check failed (attempt ${i + 1}): ${e.getMessage()}"
                    if (i == retries - 1) {
                        throw new Exception("Health check failed after ${retries} attempts")
                    }
                    sleep(30)
                }
            }
            
            if (!healthCheckPassed) {
                error("❌ Deployment health check failed!")
            }
        }
        
        // Smoke tests
        if (config.runSmokeTests) {
            sh """
                echo "💨 Running smoke tests..."
                . venv/bin/activate || venv\\Scripts\\activate
                
                # Port forward for smoke tests
                kubectl port-forward service/${config.modelName ?: 'diabetes-prediction'} 8080:80 -n ${namespace} &
                PF_PID=\$!
                
                sleep 5
                
                # Run smoke tests
                python tests/smoke/smoke_tests.py --base-url http://localhost:8080
                
                # Kill port forward
                kill \$PF_PID || echo "Port forward already stopped"
            """
        }
        
        // Update deployment info
        sh """
            echo "📝 Recording deployment information..."
            
            mkdir -p artifacts/deployment
            
            # Get deployment info
            kubectl get deployment ${config.modelName ?: 'diabetes-prediction'} -n ${namespace} -o yaml > artifacts/deployment/deployment-info.yaml
            kubectl get service ${config.modelName ?: 'diabetes-prediction'} -n ${namespace} -o yaml > artifacts/deployment/service-info.yaml
            
            # Create deployment summary
            cat > artifacts/deployment/deployment-summary.txt << EOF
Deployment Summary
==================
Environment: ${environment}
Namespace: ${namespace}
Image: ${dockerImage}
Version: ${env.MODEL_VERSION}
Build: ${env.BUILD_NUMBER}
Branch: ${env.BRANCH_NAME}
Timestamp: \$(date)
EOF
        """
        
        // Archive deployment artifacts
        archiveArtifacts artifacts: 'artifacts/deployment/**/*', fingerprint: true
        
        echo "✅ Model Deployment completed successfully!"
        echo "🌐 Model is now available in ${environment} environment"
        
    } catch (Exception e) {
        echo "❌ Model Deployment failed: ${e.getMessage()}"
        
        // Rollback on failure (production only)
        if (environment == 'production' && config.autoRollback) {
            echo "🔄 Initiating automatic rollback..."
            try {
                sh """
                    kubectl rollout undo deployment/${config.modelName ?: 'diabetes-prediction'} -n ${namespace}
                    kubectl rollout status deployment/${config.modelName ?: 'diabetes-prediction'} -n ${namespace} --timeout=300s
                """
                echo "✅ Rollback completed successfully"
            } catch (Exception rollbackError) {
                echo "❌ Rollback failed: ${rollbackError.getMessage()}"
            }
        }
        
        throw e
    }
}

/**
 * Determine deployment environment based on branch
 */
def getDeploymentEnvironment() {
    switch(env.BRANCH_NAME) {
        case 'main':
        case 'master':
            return 'production'
        case 'develop':
        case 'staging':
            return 'staging'
        default:
            return 'development'
    }
}
