pipeline {
    agent any

    options {
        githubProjectProperty(projectUrlStr: "https://github.com/SirBlobman/Block-Compressor")
    }

    environment {
        DISCORD_URL = credentials('PUBLIC_DISCORD_WEBHOOK')
    }

    triggers {
        githubPush()
    }

    tools {
        jdk "JDK 17"
    }

    stages {
        stage("Gradle: Build") {
            steps {
                withGradle {
                    sh("./gradlew clean build --refresh-dependencies --no-daemon")
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'build/libs/BlockCompressor-*.jar', fingerprint: true
        }

        always {
            script {
                discordSend webhookURL: DISCORD_URL, title: "Block Compressor", link: "${env.BUILD_URL}",
                        result: currentBuild.currentResult,
                        description: """\
                            **Branch:** ${env.GIT_BRANCH}
                            **Build:** ${env.BUILD_NUMBER}
                            **Status:** ${currentBuild.currentResult}""".stripIndent(),
                        enableArtifactsList: false, showChangeset: true
            }
        }
    }
}
