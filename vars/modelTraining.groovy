#!/usr/bin/env groovy

/**
 * Model Training Stage
 * Trains ML models with hyperparameter tuning and cross-validation
 */
def call(Map config) {
    echo "Starting Model Training..."
    
    try {
        // Activate Python environment
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
        """
        
        // Model training with hyperparameter tuning
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            
            echo "Training base model..."
            python src/models/train_model.py --config config/training_config.yml
            
            echo "Hyperparameter tuning..."
            python src/models/hyperparameter_tuning.py --config config/training_config.yml
            
            echo "Cross-validation..."
            python src/models/cross_validation.py --config config/training_config.yml
            
            echo "Saving best model..."
            python src/models/save_model.py --model-dir artifacts/models
        """
        
        // Generate training metrics and plots
        sh """
            . venv/bin/activate || venv\\Scripts\\activate
            python src/models/generate_metrics.py --output-dir artifacts/training-metrics
        """
        
        // Model versioning with MLflow (if configured)
        if (config.useMlflow) {
            sh """
                . venv/bin/activate || venv\\Scripts\\activate
                python src/models/mlflow_tracking.py --experiment-name ${config.modelName ?: 'diabetes-prediction'}
            """
        }
        
        // Archive model artifacts
        archiveArtifacts artifacts: 'artifacts/models/**/*', fingerprint: true
        archiveArtifacts artifacts: 'artifacts/training-metrics/**/*', fingerprint: true
        
        echo "Model Training completed successfully!"
        
    } catch (Exception e) {
        echo "Model Training failed: ${e.getMessage()}"
        
        // Capture training logs
        sh """
            mkdir -p artifacts/logs
            cp -r logs/* artifacts/logs/ || echo "No training logs to copy"
        """
        
        throw e
    }
}
