# Jenkins Shared Library for MLOps Pipeline

A comprehensive Jenkins shared library for automating complete MLOps pipelines for Machine Learning projects.

## Overview

This library provides reusable pipeline steps for MLOps, including:

- **Data Validation**: Data quality and integrity validation
- **Model Training**: Model training with hyperparameter tuning
- **Model Validation**: Model performance validation and quality gates
- **Model Testing**: Comprehensive testing (unit, integration, API, performance)
- **Model Packaging**: Packaging models into Docker containers
- **Model Deployment**: Model deployment to Kubernetes
- **Model Monitoring**: Monitoring and alerting setup

## Library Structure

```
your-ml-project/
├── requirements.txt
├── Dockerfile
├── config/
│   ├── training_config.yml
│   └── validation_config.yml
├── src/
│   ├── models/
│   │   ├── train_model.py
│   │   └── hyperparameter_tuning.py
│   ├── validation/
│   │   ├── data_validator.py
│   │   └── model_validator.py
│   └── packaging/
│       └── generate_metadata.py
├── tests/
│   ├── unit/
│   ├── integration/
│   ├── api/
│   └── smoke/
├── k8s/
│   ├── deployment.yml
│   ├── service.yml
│   └── monitoring/
├── helm-chart/
└── api/
    └── main.py
```

## How to Use

### 1. Configure Jenkins

Add the shared library to Jenkins:

1. Go to **Manage Jenkins** > **Configure System**
2. Under **Global Pipeline Libraries**, add:
   - Name: `mlops-shared-library`
   - Default version: `main`
   - Source Code Management: Git
   - Repository URL: `https://github.com/your-org/jenkins-shared-library.git`

### 2. Use in Jenkinsfile

```groovy
@Library('mlops-shared-library') _

def config = [
    modelName: 'diabetes-prediction',
    pythonVersion: '3.9',
    dockerRegistry: 'docker.io',
    dockerCredentialsId: 'docker-registry',
    namespace: 'mlops-prod',
    hasApi: true,
    useHelm: true,
    useMlflow: true,
    runLoadTests: true,
    runSecurityTests: true,
    runSmokeTests: true,
    autoRollback: true,
    enablePrometheus: true,
    enableGrafana: true,
    enableAlerting: true,
    enableLogging: true,
    enableExplainability: true,
    enableABTesting: false,
    slackChannel: '#mlops-alerts',
    slackWebhook: 'https://hooks.slack.com/services/...',
    emailRecipients: 'mlops-team@company.com',
    teamsWebhook: 'https://outlook.office.com/webhook/...',
    updateGitHubStatus: true,
    githubRepo: 'your-org/diabetes-prediction',
    githubTokenId: 'github-token'
]

mlOpsPipeline(config)
```

#### Minimal Example

```groovy
@Library('mlops-shared-library') _

mlOpsPipeline([
    modelName: 'my-model',
    hasApi: true
])
```

#### Advanced Example

```groovy
mlOpsPipeline([
    dockerRegistry: 'your-registry.com',
    dockerCredentialsId: 'docker-credentials',
    namespace: 'mlops-production',
    useHelm: true,
    autoRollback: true,
    enablePrometheus: true,
    enableGrafana: true,
    enableAlerting: true,
    enableLogging: true,
    grafanaUrl: 'https://grafana.company.com',
    prometheusUrl: 'https://prometheus.company.com',
    slackChannel: '#ml-deployments',
    slackWebhook: 'https://hooks.slack.com/services/...',
    emailRecipients: 'ml-team@company.com,ops-team@company.com',
    teamsWebhook: 'https://outlook.office.com/webhook/...',
    updateGitHubStatus: true,
    githubRepo: 'company/ml-project',
    githubTokenId: 'github-token'
])
```

### Custom Steps

Create custom steps in `vars/`:

```groovy
// vars/customStep.groovy
def call(Map config) {
    echo "Running custom step..."
    sh """
        echo "Custom step with config: ${config}"
    """
    echo "Custom step completed!"
}
```

Use in pipeline:

```groovy
@Library('mlops-shared-library') _

pipeline {
    agent any

    stages {
        stage('Custom Pre-processing') {
            steps {
                script {
                    customStep([param: 'value'])
                }
            }
        }
        stage('MLOps Pipeline') {
            steps {
                script {
                    mlOpsPipeline(config)
                }
            }
        }
        stage('Custom Post-processing') {
            steps {
                script {
                    customPostStep([param: 'value'])
                }
            }
        }
    }
}
```

## Monitoring and Metrics

### Model Metrics

- `model_accuracy`
- `prediction_confidence`
- `data_drift_score`

### API Metrics

- `http_requests_total`
- `http_requests_errors_total`
- `prediction_duration_seconds`

### System Metrics

- CPU and Memory usage
- Disk usage
- Network metrics

## Alerting

- High Error Rate > 5%
- High Latency > 1s
- Low Model Confidence < 0.7
- Data Drift Detected
- Model Degradation

## Security

### Docker Image Scanning

- Uses Trivy to scan for vulnerabilities

### Code Security

- Bandit: Python code security scanning
- Safety: Dependency vulnerability scanning

### Secrets Management

Managed with Jenkins Credentials:

- Docker credentials
- Kubernetes config
- Webhooks
- GitHub tokens

## Contributing

1. Fork this repository
2. Create a feature branch
3. Implement your changes
4. Test with a sample project
5. Submit a pull request

## Examples

See the `examples/` folder for:

- Basic pipelines
- Advanced configurations
- Custom steps
- Multi-model deployments

## Troubleshooting

### Common Issues

- **Docker build fails**
    ```sh
    docker build -t test-image .
    ```
- **Kubernetes context**
    ```sh
    kubectl config current-context
    kubectl get nodes
    ```
- **Model validation debug**
    ```sh
    python src/validation/model_validator.py --debug
    ```
