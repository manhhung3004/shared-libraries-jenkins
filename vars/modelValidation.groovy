#!/usr/bin/env groovy

/**
 * Model Validation Stage
 * Validates model performance against baseline and business requirements
 */
def call(Map config) {
    echo "Starting Model Validation..."
    
    try {
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
        """
        
        // Model performance validation
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            
            echo "Validating model performance..."
            python src/validation/model_validator.py --model-path artifacts/models/best_model.pkl
            
            echo "Checking performance thresholds..."
            python src/validation/performance_checker.py --config config/validation_config.yml
            
            echo "Comparing with baseline model..."
            python src/validation/baseline_comparison.py --baseline-path models/baseline_model.pkl
            
            echo "Model bias and fairness check..."
            python src/validation/bias_checker.py --model-path artifacts/models/best_model.pkl
        """
        
        // Generate validation report
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            python src/validation/validation_report.py --output-dir artifacts/model-validation
        """
        
        // Check if model meets quality gates
        script {
            def validationResult = sh(
                script: """
                    . venv/bin/activate || venv\\Scripts\\activate
                    python src/validation/quality_gate.py --model-path artifacts/models/best_model.pkl
                """,
                returnStatus: true
            )
            
            if (validationResult != 0) {
                error("Model failed quality gates! Check validation report for details.")
            }
        }
        
        // Archive validation results
        archiveArtifacts artifacts: 'artifacts/model-validation/**/*', fingerprint: true
        
        echo "Model Validation completed successfully!"
        
    } catch (Exception e) {
        echo "Model Validation failed: ${e.getMessage()}"
        
        // Generate failure report
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            python src/validation/failure_report.py --output-dir artifacts/validation-failure || echo "Could not generate failure report"
        """
        
        throw e
    }
}
