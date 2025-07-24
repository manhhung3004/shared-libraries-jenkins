#!/usr/bin/env groovy

/**
 * Model Testing Stage
 * Comprehensive testing including unit tests, integration tests, and API tests
 */
def call(Map config) {
    echo "🧪 Starting Model Testing..."
    
    try {
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
        """
        
        // Unit tests for model functions
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            
            echo "🔬 Running unit tests..."
            python -m pytest tests/unit/ -v --junitxml=test-results/unit-tests.xml --cov=src --cov-report=html:artifacts/coverage/unit
        """
        
        // Integration tests
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            
            echo "🔗 Running integration tests..."
            python -m pytest tests/integration/ -v --junitxml=test-results/integration-tests.xml
        """
        
        // API tests (if FastAPI is used)
        if (config.hasApi) {
            sh """
                . venv/bin/activate || venv\\Scripts\\activate
                
                echo "🚀 Starting API server for testing..."
                uvicorn api.main:app --host 0.0.0.0 --port 8000 &
                API_PID=\$!
                
                echo "⏳ Waiting for API to be ready..."
                sleep 10
                
                echo "🌐 Running API tests..."
                python -m pytest tests/api/ -v --junitxml=test-results/api-tests.xml
                
                echo "🛑 Stopping API server..."
                kill \$API_PID || echo "API server already stopped"
            """
        }
        
        // Performance/Load tests
        if (config.runLoadTests) {
            sh """
                . venv/bin/activate || venv\\Scripts\\activate
                
                echo "⚡ Running performance tests..."
                python tests/performance/load_test.py --output-dir artifacts/performance
            """
        }
        
        // Security tests
        if (config.runSecurityTests) {
            sh """
                . venv/bin/activate || venv\\Scripts\\activate
                
                echo "🛡️ Running security tests..."
                bandit -r src/ -f json -o artifacts/security/bandit-report.json || echo "Security scan completed with findings"
                safety check --json --output artifacts/security/safety-report.json || echo "Safety check completed"
            """
        }
        
        // Model inference tests
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            
            echo "🎯 Testing model inference..."
            python tests/model/test_inference.py --model-path artifacts/models/best_model.pkl --output-dir artifacts/inference-tests
        """
        
        // Publish test results
        publishTestResults testResultsPattern: 'test-results/*.xml'
        
        // Publish coverage report
        publishHTML([
            allowMissing: false,
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: 'artifacts/coverage/unit',
            reportFiles: 'index.html',
            reportName: 'Coverage Report'
        ])
        
        // Archive test artifacts
        archiveArtifacts artifacts: 'artifacts/performance/**/*', allowEmptyArchive: true
        archiveArtifacts artifacts: 'artifacts/security/**/*', allowEmptyArchive: true
        archiveArtifacts artifacts: 'artifacts/inference-tests/**/*', fingerprint: true
        
        echo "✅ Model Testing completed successfully!"
        
    } catch (Exception e) {
        echo "❌ Model Testing failed: ${e.getMessage()}"
        
        // Always publish test results even on failure
        publishTestResults testResultsPattern: 'test-results/*.xml', allowEmptyResults: true
        
        throw e
    }
}
