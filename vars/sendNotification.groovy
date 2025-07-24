#!/usr/bin/env groovy

/**
 * Notification Helper
 * Sends notifications to various channels (Slack, email, Teams, etc.)
 */
def call(Map config, String status) {
    echo "Sending notifications..."
    
    try {
        def color = status == 'SUCCESS' ? 'good' : 'danger'
        def emoji = status == 'SUCCESS' ? 'v' : 'x'
        def statusText = status == 'SUCCESS' ? 'completed successfully' : 'failed'
        
        def message = """
${emoji} MLOps Pipeline ${statusText}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Project: ${config.modelName ?: 'diabetes-prediction'}
Build: #${env.BUILD_NUMBER}
Branch: ${env.BRANCH_NAME}
Duration: ${currentBuild.durationString}
Build URL: ${env.BUILD_URL}
        """.trim()
        
        // Slack notification
        if (config.slackChannel && config.slackWebhook) {
            try {
                sh """
                    curl -X POST -H 'Content-type: application/json' \\
                        --data '{
                            "channel": "${config.slackChannel}",
                            "username": "Jenkins MLOps Bot",
                            "icon_emoji": ":robot_face:",
                            "attachments": [{
                                "color": "${color}",
                                "title": "MLOps Pipeline ${status}",
                                "text": "${message}",
                                "footer": "Jenkins MLOps Pipeline",
                                "ts": ${System.currentTimeMillis() / 1000}
                            }]
                        }' \\
                        ${config.slackWebhook}
                """
                echo "Slack notification sent successfully"
            } catch (Exception e) {
                echo "Failed to send Slack notification: ${e.getMessage()}"
            }
        }
        
        // Email notification
        if (config.emailRecipients) {
            try {
                emailext (
                    subject: "${emoji} MLOps Pipeline ${status} - ${config.modelName ?: 'diabetes-prediction'} #${env.BUILD_NUMBER}",
                    body: """
                    <h2>${emoji} MLOps Pipeline ${statusText}</h2>
                    <table border="1" cellpadding="5" cellspacing="0">
                        <tr><td><b>Project</b></td><td>${config.modelName ?: 'diabetes-prediction'}</td></tr>
                        <tr><td><b>Build</b></td><td>#${env.BUILD_NUMBER}</td></tr>
                        <tr><td><b>Branch</b></td><td>${env.BRANCH_NAME}</td></tr>
                        <tr><td><b>Status</b></td><td style="color: ${color == 'good' ? 'green' : 'red'}">${status}</td></tr>
                        <tr><td><b>Duration</b></td><td>${currentBuild.durationString}</td></tr>
                        <tr><td><b>Build URL</b></td><td><a href="${env.BUILD_URL}">View Build</a></td></tr>
                    </table>
                    
                    ${status == 'FAILURE' ? '<p><b>Check the build logs for more details.</b></p>' : ''}
                    """,
                    to: config.emailRecipients,
                    mimeType: 'text/html'
                )
                echo "Email notification sent successfully"
            } catch (Exception e) {
                echo "Failed to send email notification: ${e.getMessage()}"
            }
        }
        
        // Microsoft Teams notification
        if (config.teamsWebhook) {
            try {
                def teamsColor = status == 'SUCCESS' ? '00FF00' : 'FF0000'
                sh """
                    curl -X POST -H 'Content-Type: application/json' \\
                        --data '{
                            "@type": "MessageCard",
                            "@context": "http://schema.org/extensions",
                            "themeColor": "${teamsColor}",
                            "summary": "MLOps Pipeline ${status}",
                            "sections": [{
                                "activityTitle": "${emoji} MLOps Pipeline ${status}",
                                "activitySubtitle": "${config.modelName ?: 'diabetes-prediction'} - Build #${env.BUILD_NUMBER}",
                                "facts": [
                                    {"name": "Project", "value": "${config.modelName ?: 'diabetes-prediction'}"},
                                    {"name": "Build", "value": "#${env.BUILD_NUMBER}"},
                                    {"name": "Branch", "value": "${env.BRANCH_NAME}"},
                                    {"name": "Duration", "value": "${currentBuild.durationString}"}
                                ]
                            }],
                            "potentialAction": [{
                                "@type": "OpenUri",
                                "name": "View Build",
                                "targets": [{"os": "default", "uri": "${env.BUILD_URL}"}]
                            }]
                        }' \\
                        ${config.teamsWebhook}
                """
                echo "Teams notification sent successfully"
            } catch (Exception e) {
                echo "Failed to send Teams notification: ${e.getMessage()}"
            }
        }
        
        // GitHub status (if applicable)
        if (config.updateGitHubStatus && env.CHANGE_ID) {
            try {
                def githubStatus = status == 'SUCCESS' ? 'success' : 'failure'
                def description = status == 'SUCCESS' ? 'MLOps pipeline completed successfully' : 'MLOps pipeline failed'
                
                withCredentials([string(credentialsId: config.githubTokenId ?: 'github-token', variable: 'GITHUB_TOKEN')]) {
                    sh """
                        curl -X POST \\
                            -H "Authorization: token \$GITHUB_TOKEN" \\
                            -H "Accept: application/vnd.github.v3+json" \\
                            https://api.github.com/repos/${config.githubRepo}/statuses/${env.GIT_COMMIT} \\
                            -d '{
                                "state": "${githubStatus}",
                                "target_url": "${env.BUILD_URL}",
                                "description": "${description}",
                                "context": "continuous-integration/jenkins/mlops"
                            }'
                    """
                }
                echo "GitHub status updated successfully"
            } catch (Exception e) {
                echo "Failed to update GitHub status: ${e.getMessage()}"
            }
        }
        
        echo "Notifications sent successfully"
        
    } catch (Exception e) {
        echo "Failed to send notifications: ${e.getMessage()}"
        // Don't fail the pipeline for notification failures
    }
}
