# Jenkins Shared Library for MLOps Pipeline

Thư viện chia sẻ Jenkins để triển khai pipeline MLOps tự động hóa hoàn chỉnh cho các dự án Machine Learning.

## 🚀 Tổng quan

Thư viện này cung cấp các bước pipeline tái sử dụng cho MLOps, bao gồm:

- **Data Validation** - Kiểm tra chất lượng và tính toàn vẹn dữ liệu
- **Model Training** - Huấn luyện mô hình với hyperparameter tuning
- **Model Validation** - Kiểm tra hiệu suất mô hình và quality gates
- **Model Testing** - Kiểm thử toàn diện (unit, integration, API, performance)
- **Model Packaging** - Đóng gói mô hình thành Docker container
- **Model Deployment** - Triển khai mô hình lên Kubernetes
- **Model Monitoring** - Thiết lập giám sát và cảnh báo

## 📁 Cấu trúc thư viện

```
jenkins-shared-library/
├── vars/                           # Global variables và pipeline steps
│   ├── mlOpsPipeline.groovy       # Pipeline chính MLOps
│   ├── dataValidation.groovy      # Kiểm tra dữ liệu
│   ├── modelTraining.groovy       # Huấn luyện mô hình
│   ├── modelValidation.groovy     # Kiểm tra mô hình
│   ├── modelTesting.groovy        # Kiểm thử mô hình
│   ├── modelPackaging.groovy      # Đóng gói mô hình
│   ├── modelDeployment.groovy     # Triển khai mô hình
│   ├── modelMonitoring.groovy     # Giám sát mô hình
│   └── sendNotification.groovy    # Gửi thông báo
└── src/com/mlops/                 # Utility classes (nếu cần)
```

## 🛠️ Cách sử dụng

### 1. Cấu hình Jenkins

Thêm thư viện chia sẻ vào Jenkins:
1. Vào **Manage Jenkins** > **Configure System**
2. Trong phần **Global Pipeline Libraries**, thêm:
   - Name: `mlops-shared-library`
   - Default version: `main`
   - Source Code Management: Git
   - Repository URL: `https://github.com/your-org/jenkins-shared-library.git`

### 2. Sử dụng trong Jenkinsfile

```groovy
@Library('mlops-shared-library') _

// Cấu hình pipeline
def config = [
    modelName: 'diabetes-prediction',
    pythonVersion: '3.9',
    dockerRegistry: 'docker.io',
    dockerCredentialsId: 'docker-registry',
    namespace: 'mlops-prod',
    
    // Features
    hasApi: true,
    useHelm: true,
    useMlflow: true,
    runLoadTests: true,
    runSecurityTests: true,
    runSmokeTests: true,
    autoRollback: true,
    
    // Monitoring
    enablePrometheus: true,
    enableGrafana: true,
    enableAlerting: true,
    enableLogging: true,
    enableExplainability: true,
    enableABTesting: false,
    
    // Notifications
    slackChannel: '#mlops-alerts',
    slackWebhook: 'https://hooks.slack.com/services/...',
    emailRecipients: 'mlops-team@company.com',
    teamsWebhook: 'https://outlook.office.com/webhook/...',
    updateGitHubStatus: true,
    githubRepo: 'your-org/diabetes-prediction',
    githubTokenId: 'github-token'
]

// Chạy pipeline MLOps
mlOpsPipeline(config)
```

### 3. Cấu hình tối thiểu

```groovy
@Library('mlops-shared-library') _

mlOpsPipeline([
    modelName: 'my-model',
    hasApi: true
])
```

## ⚙️ Cấu hình chi tiết

### Docker Registry
```groovy
dockerRegistry: 'your-registry.com'
dockerCredentialsId: 'docker-credentials'
```

### Kubernetes Deployment
```groovy
namespace: 'mlops-production'
useHelm: true
autoRollback: true
```

### Monitoring và Alerting
```groovy
enablePrometheus: true
enableGrafana: true
enableAlerting: true
enableLogging: true
grafanaUrl: 'https://grafana.company.com'
prometheusUrl: 'https://prometheus.company.com'
```

### Notifications
```groovy
// Slack
slackChannel: '#ml-deployments'
slackWebhook: 'https://hooks.slack.com/services/...'

// Email
emailRecipients: 'ml-team@company.com,ops-team@company.com'

// Microsoft Teams
teamsWebhook: 'https://outlook.office.com/webhook/...'

// GitHub Status
updateGitHubStatus: true
githubRepo: 'company/ml-project'
githubTokenId: 'github-token'
```

## 📋 Yêu cầu dự án

Để sử dụng thư viện này, dự án của bạn cần có cấu trúc:

```
your-ml-project/
├── requirements.txt                # Python dependencies
├── Dockerfile                     # Docker image cho API
├── config/
│   ├── training_config.yml        # Cấu hình training
│   └── validation_config.yml      # Cấu hình validation
├── src/
│   ├── models/
│   │   ├── train_model.py         # Script training
│   │   └── hyperparameter_tuning.py
│   ├── validation/
│   │   ├── data_validator.py      # Data validation
│   │   └── model_validator.py     # Model validation
│   └── packaging/
│       └── generate_metadata.py   # Model metadata
├── tests/
│   ├── unit/                      # Unit tests
│   ├── integration/               # Integration tests
│   ├── api/                       # API tests
│   └── smoke/                     # Smoke tests
├── k8s/
│   ├── deployment.yml             # K8s deployment
│   ├── service.yml                # K8s service
│   └── monitoring/                # Monitoring configs
├── helm-chart/                    # Helm chart (nếu sử dụng)
└── api/
    └── main.py                    # FastAPI application
```

## 🔧 Customization

### Tạo bước pipeline tùy chỉnh

Tạo file mới trong `vars/` directory:

```groovy
// vars/customStep.groovy
#!/usr/bin/env groovy

def call(Map config) {
    echo "🔧 Running custom step..."
    
    // Your custom logic here
    sh """
        echo "Custom step with config: ${config}"
    """
    
    echo "✅ Custom step completed!"
}
```

### Mở rộng pipeline chính

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

## 📊 Monitoring và Metrics

Pipeline tự động thiết lập các metrics sau:

### Model Metrics
- `model_accuracy` - Độ chính xác mô hình
- `prediction_confidence` - Độ tin cậy dự đoán
- `data_drift_score` - Điểm số drift dữ liệu

### API Metrics
- `http_requests_total` - Tổng số requests
- `http_requests_errors_total` - Tổng số lỗi
- `prediction_duration_seconds` - Thời gian dự đoán

### System Metrics
- CPU và Memory usage
- Disk usage
- Network metrics

## 🚨 Alerting

Các cảnh báo được cấu hình tự động:

- **High Error Rate** - Tỷ lệ lỗi > 5%
- **High Latency** - Thời gian phản hồi > 1s
- **Low Model Confidence** - Độ tin cậy < 0.7
- **Data Drift Detected** - Phát hiện drift dữ liệu
- **Model Degradation** - Suy giảm hiệu suất mô hình

## 🔐 Security

### Docker Image Scanning
Pipeline tự động quét Docker images để tìm lỗ hổng bảo mật sử dụng Trivy.

### Code Security
- **Bandit** - Quét lỗ hổng Python
- **Safety** - Kiểm tra dependencies có lỗ hổng

### Secrets Management
Sử dụng Jenkins Credentials để quản lý:
- Docker registry credentials
- Kubernetes configs
- Notification webhooks
- GitHub tokens

## 🤝 Contributing

1. Fork repository
2. Tạo feature branch
3. Implement changes
4. Test với dự án mẫu
5. Submit pull request

## 📚 Examples

Xem thư mục `examples/` để có các ví dụ cụ thể về:
- Basic MLOps pipeline
- Advanced configuration
- Custom steps
- Multi-model deployment

## 🆘 Troubleshooting

### Common Issues

1. **Docker build fails**
   ```bash
   # Kiểm tra Dockerfile và dependencies
   docker build -t test-image .
   ```

2. **Kubernetes deployment fails**
   ```bash
   # Kiểm tra kubectl config
   kubectl config current-context
   kubectl get nodes
   ```

3. **Model validation fails**
   ```bash
   # Kiểm tra validation config
   python src/validation/model_validator.py --debug
   ```

### Debug Mode

Bật debug mode trong pipeline:
```groovy
mlOpsPipeline([
    modelName: 'my-model',
    debug: true  // Bật debug logs
])
```

## 📞 Support

- **Documentation**: [Wiki](https://github.com/your-org/jenkins-shared-library/wiki)
- **Issues**: [GitHub Issues](https://github.com/your-org/jenkins-shared-library/issues)
- **Slack**: #mlops-support
- **Email**: mlops-team@company.com
