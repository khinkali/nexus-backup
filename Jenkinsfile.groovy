@Library('semantic_releasing') _

podTemplate(label: 'mypod', containers: [
        containerTemplate(name: 'kubectl', image: 'lachlanevenson/k8s-kubectl:v1.8.0', command: 'cat', ttyEnabled: true)
],
        volumes: [
                hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
        ]) {
    try {
        node('mypod') {
            properties([
                    buildDiscarder(
                            logRotator(artifactDaysToKeepStr: '',
                                    artifactNumToKeepStr: '',
                                    daysToKeepStr: '',
                                    numToKeepStr: '30'
                            )
                    ),
                    pipelineTriggers([cron('0 2 * * *')])
            ])

            stage('create backup') {
                currentBuild.displayName = getTimeDateDisplayName()

                def kc = 'kubectl'
                def containerPath = '/nexus-data'
                def containerName = 'nexus'
                def podLabel = 'app=nexus'
                def repositoryUrl = 'bitbucket.org/khinkali/nexus_backup'
                container('kubectl') {
                    backup(podLabel, containerName, containerPath, repositoryUrl, kc)
                }
            }

        }
    } catch (all) {
        slackSend channel: '#jenkins',
                color: 'good',
                message: "Build Failed - ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)",
                teamDomain: 'khikali',
                token: 'slack-token'
        currentBuild.result = 'FAILURE'
    }
}

