#!/usr/bin/env groovy

/**
 * Data Validation Stage
 * Validates data quality, schema, and integrity
 */
def call(Map config) {
    echo "🔍 Starting Data Validation..."
    
    try {
        // Setup Python environment
        sh """
            python${config.pythonVersion ?: '3.9'} -m venv venv
            . venv/bin/activate || venv\\Scripts\\activate
            pip install --upgrade pip
            pip install -r requirements.txt
        """
        
        // Data validation steps
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            
            echo "📊 Checking data schema..."
            python src/validation/data_validator.py --check-schema
            
            echo "📈 Validating data quality..."
            python src/validation/data_validator.py --check-quality
            
            echo "🔢 Checking data distribution..."
            python src/validation/data_validator.py --check-distribution
            
            echo "🚫 Detecting data drift..."
            python src/validation/data_validator.py --check-drift
        """
        
        // Generate data validation report
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            python src/validation/generate_report.py --output-dir artifacts/data-validation
        """
        
        // Archive validation results
        archiveArtifacts artifacts: 'artifacts/data-validation/**/*', fingerprint: true
        
        echo "✅ Data Validation completed successfully!"
        
    } catch (Exception e) {
        echo "❌ Data Validation failed: ${e.getMessage()}"
        
        // Capture logs for debugging
        sh """
            mkdir -p artifacts/logs
            cp -r logs/* artifacts/logs/ || echo "No logs to copy"
        """
        
        throw e
    }
}
