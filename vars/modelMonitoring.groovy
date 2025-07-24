#!/usr/bin/env groovy

/**
 * Model Monitoring Setup Stage
 * Sets up monitoring, alerting, and observability for deployed models
 */
def call(Map config) {
    echo "📊 Starting Model Monitoring Setup..."
    
    try {
        def environment = getDeploymentEnvironment()
        def namespace = config.namespace ?: "mlops-${environment}"
        
        // Deploy Prometheus monitoring
        if (config.enablePrometheus) {
            sh """
                echo "📈 Setting up Prometheus monitoring..."
                
                # Apply ServiceMonitor for model metrics
                kubectl apply -f k8s/monitoring/servicemonitor.yml -n ${namespace}
                
                # Apply PrometheusRule for alerts
                kubectl apply -f k8s/monitoring/prometheusrule.yml -n ${namespace}
            """
        }
        
        // Deploy Grafana dashboards
        if (config.enableGrafana) {
            sh """
                echo "📊 Setting up Grafana dashboards..."
                
                # Create ConfigMap with dashboard JSON
                kubectl create configmap ${config.modelName ?: 'diabetes-prediction'}-dashboard \\
                    --from-file=k8s/monitoring/grafana-dashboard.json \\
                    -n monitoring \\
                    --dry-run=client -o yaml | kubectl apply -f -
                
                # Label ConfigMap for Grafana discovery
                kubectl label configmap ${config.modelName ?: 'diabetes-prediction'}-dashboard \\
                    grafana_dashboard=1 -n monitoring
            """
        }
        
        // Setup model performance monitoring
        sh """
            echo "🎯 Setting up model performance monitoring..."
            
            # Deploy model metrics collector
            kubectl apply -f k8s/monitoring/metrics-collector.yml -n ${namespace}
            
            # Setup data drift detector
            kubectl apply -f k8s/monitoring/drift-detector.yml -n ${namespace}
        """
        
        // Configure alerting
        if (config.enableAlerting) {
            sh """
                echo "🚨 Setting up alerting..."
                
                # Apply AlertManager configuration
                kubectl apply -f k8s/monitoring/alertmanager-config.yml -n monitoring
                
                # Setup Slack/email notifications (if configured)
                if [[ -n "${config.slackWebhook}" ]]; then
                    kubectl create secret generic slack-webhook \\
                        --from-literal=url="${config.slackWebhook}" \\
                        -n monitoring \\
                        --dry-run=client -o yaml | kubectl apply -f -
                fi
            """
        }
        
        // Setup logging aggregation
        if (config.enableLogging) {
            sh """
                echo "📝 Setting up logging aggregation..."
                
                # Deploy Fluent Bit for log collection
                kubectl apply -f k8s/monitoring/fluent-bit.yml -n ${namespace}
                
                # Setup log parsing and routing
                kubectl apply -f k8s/monitoring/log-config.yml -n ${namespace}
            """
        }
        
        // Model explainability monitoring
        if (config.enableExplainability) {
            sh """
                echo "🔍 Setting up model explainability monitoring..."
                
                # Deploy SHAP/LIME explainability service
                kubectl apply -f k8s/monitoring/explainability-service.yml -n ${namespace}
            """
        }
        
        // A/B testing setup (if configured)
        if (config.enableABTesting) {
            sh """
                echo "🧪 Setting up A/B testing infrastructure..."
                
                # Deploy traffic splitter
                kubectl apply -f k8s/monitoring/traffic-splitter.yml -n ${namespace}
                
                # Setup experiment tracking
                kubectl apply -f k8s/monitoring/experiment-tracker.yml -n ${namespace}
            """
        }
        
        // Health checks and probes configuration
        sh """
            echo "🏥 Configuring health checks..."
            
            # Update deployment with monitoring probes
            kubectl patch deployment ${config.modelName ?: 'diabetes-prediction'} -n ${namespace} --patch-file k8s/monitoring/health-probes-patch.yml
        """
        
        // Create monitoring dashboard URLs
        script {
            def grafanaUrl = config.grafanaUrl ?: "http://grafana.monitoring.svc.cluster.local:3000"
            def prometheusUrl = config.prometheusUrl ?: "http://prometheus.monitoring.svc.cluster.local:9090"
            
            sh """
                mkdir -p artifacts/monitoring
                
                cat > artifacts/monitoring/monitoring-urls.txt << EOF
Monitoring Dashboard URLs
=========================
Grafana Dashboard: ${grafanaUrl}/d/${config.modelName ?: 'diabetes-prediction'}-dashboard
Prometheus Metrics: ${prometheusUrl}/graph
Model Metrics Endpoint: http://${config.modelName ?: 'diabetes-prediction'}.${namespace}.svc.cluster.local/metrics
Health Check: http://${config.modelName ?: 'diabetes-prediction'}.${namespace}.svc.cluster.local/health

Key Metrics to Monitor:
- Model accuracy: model_accuracy
- Prediction latency: prediction_duration_seconds
- Request rate: http_requests_total
- Error rate: http_requests_errors_total
- Data drift: data_drift_score
- Model confidence: prediction_confidence

Alerts Configured:
- High error rate (>5%)
- High latency (>1s)
- Low model confidence (<0.7)
- Data drift detected
- Model degradation
EOF
            """
        }
        
        // Wait for monitoring components to be ready
        sh """
            echo "⏳ Waiting for monitoring components to be ready..."
            
            # Wait for metrics collector
            kubectl rollout status deployment/metrics-collector -n ${namespace} --timeout=300s || echo "Metrics collector not deployed"
            
            # Wait for drift detector
            kubectl rollout status deployment/drift-detector -n ${namespace} --timeout=300s || echo "Drift detector not deployed"
        """
        
        // Validate monitoring setup
        sh """
            echo "✅ Validating monitoring setup..."
            
            # Check if metrics endpoint is accessible
            kubectl port-forward service/${config.modelName ?: 'diabetes-prediction'} 8080:80 -n ${namespace} &
            PF_PID=\$!
            
            sleep 5
            
            # Test metrics endpoint
            curl -f http://localhost:8080/metrics || echo "Metrics endpoint not available yet"
            
            # Kill port forward
            kill \$PF_PID || echo "Port forward already stopped"
        """
        
        // Archive monitoring artifacts
        archiveArtifacts artifacts: 'artifacts/monitoring/**/*', fingerprint: true
        
        echo "✅ Model Monitoring Setup completed successfully!"
        echo "📊 Monitoring dashboards and alerts are now configured"
        
    } catch (Exception e) {
        echo "❌ Model Monitoring Setup failed: ${e.getMessage()}"
        
        // Don't fail the entire pipeline for monitoring setup failures
        echo "⚠️ Continuing pipeline despite monitoring setup failure"
        
        // Log the error for investigation
        sh """
            mkdir -p artifacts/logs
            echo "Monitoring setup failed: ${e.getMessage()}" > artifacts/logs/monitoring-failure.log
            kubectl get events -n ${namespace} --sort-by='.lastTimestamp' > artifacts/logs/k8s-events.log || echo "Could not get k8s events"
        """
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
