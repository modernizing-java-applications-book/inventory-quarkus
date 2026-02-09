# CI/CD Pipelines

This folder contains the CI/CD pipeline configurations for the inventory-quarkus project.

## Structure

```
CICD/
├── Pipelines/
│   └── Jenkinsfile    # Main Jenkins pipeline definition
└── README.md          # This file
```

## Jenkins Pipeline

The main Jenkinsfile (`Pipelines/Jenkinsfile`) defines a complete CI/CD pipeline with the following stages:

### Pipeline Stages

| Stage | Description |
|-------|-------------|
| **Checkout** | Clone source code from repository |
| **Validate** | Code style check & dependency analysis (parallel) |
| **Build** | Compile the application with Maven |
| **Unit Tests** | Run unit tests and generate coverage reports |
| **SonarQube Analysis** | Static code analysis (optional) |
| **Quality Gate** | Enforce code quality standards |
| **Package** | Build JAR artifacts |
| **Build Docker Image** | Create and push Docker images |
| **Integration Tests** | Run integration tests on main branches |
| **Security Scan** | Scan Docker image for vulnerabilities |
| **Deploy to Development** | Auto-deploy on develop branch |
| **Deploy to Staging** | Manual approval required for staging |
| **Smoke Tests** | Basic health checks after deployment |
| **Deploy to Production** | Manual approval required for production |

### Required Jenkins Plugins

- Pipeline
- Git
- Docker Pipeline
- Kubernetes CLI (kubectl)
- Credentials Binding
- SonarQube Scanner (optional)
- Email Extension
- Slack Notification
- AnsiColor
- Timestamper

### Required Credentials

Configure these credentials in Jenkins:

| Credential ID | Type | Description |
|--------------|------|-------------|
| `docker-registry-url` | Secret text | Docker registry URL |
| `docker-registry-credentials` | Username/Password | Docker registry login |
| `kubeconfig-dev` | Secret file | Kubernetes config for dev |
| `kubeconfig-staging` | Secret file | Kubernetes config for staging |
| `kubeconfig-prod` | Secret file | Kubernetes config for production |
| `sonarqube-token` | Secret text | SonarQube authentication (optional) |

### Branch Strategy

- **develop** → Auto-deploy to Development
- **main/master** → Deploy to Staging (manual approval) → Production (manual approval)
- **feature/*** → Build and test only

### Environment Variables

Key environment variables used in the pipeline:

| Variable | Description | Default |
|----------|-------------|---------|
| `PROJECT_NAME` | Project identifier | `inventory-quarkus` |
| `KUBERNETES_NAMESPACE` | Kubernetes namespace | `inventory` |
| `DOCKER_REGISTRY` | Docker registry URL | `docker.io` |
| `SONARQUBE_ENABLED` | Enable SonarQube | `false` |

### Usage

1. **Create Jenkins Job:**
   - New Item → Pipeline
   - Pipeline script from SCM
   - Point to your Git repository
   - Script path: `CICD/Pipelines/Jenkinsfile`

2. **Configure Webhook:**
   - Add webhook in Git repository to trigger Jenkins on push events

3. **Run Pipeline:**
   - Manual trigger or webhook-triggered on push

### Notifications

The pipeline sends notifications via:
- **Email**: On test failures, deployment failures, and production deployments
- **Slack**: On build success, failure, or unstable status

### Rollback

Automatic rollback is configured for:
- Development deployment failures
- Production deployment failures

Manual rollback command:
```bash
kubectl rollout undo deployment/inventory-quarkus -n inventory
```

## Future Enhancements

Consider adding:
- GitHub Actions workflow (alternative to Jenkins)
- GitLab CI configuration
- Azure DevOps pipeline
- ArgoCD for GitOps deployments
- Helm charts for Kubernetes deployments