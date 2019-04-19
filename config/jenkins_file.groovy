def pull(rep, branch, folder, creds) {
    checkout changelog: false, poll: false, 
    scm: [$class: 'GitSCM', branches: [[name: branch]], browser: [$class: 'Stash', repoUrl: ''], 
    doGenerateSubmoduleConfigurations: false, 
    extensions: 
    [[$class: 'RelativeTargetDirectory', relativeTargetDir: folder]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: creds, url: rep]]]
}

def executeCommand(String command, String params = "", returnStdOut = false) {
    command = "${command}"
    def cmd = "docker exec ${params} -i ${imageName} bash -c \"${command}\""
    echo "Executing: ${cmd}"
    if (returnStdOut) {
        result = bat returnStdout:true, script: cmd
        return result.trim()
    } else {
        bat cmd
    }
}

//  Docker Image
imageName = "tests"
mountPoint = "/app-src"

//VSC data
gitRepository = "https://github.com/sgennadiy/test_docker.git"
gitCheckoutFolder = "test_docker"
gitCredentials = "082gennadiy@gmail.com"
gitBranch = "master"

node {
  stage('Cloning GIT') {
    pull(
      gitRepository,
      gitBranch,
      gitCheckoutFolder,
      gitCredentials
    )
  }

  //Not necessary, we have -rm parameter in 'docker run' stage  
 /* stage('Clean old containers') {
      sh 'docker ps -f name=grid -q | xargs --no-run-if-empty docker container stop'
      sh 'docker container ls -a -f name=grid -q | xargs -r docker container rm -f'
      sh 'docker ps -f name=c -q | xargs --no-run-if-empty docker container stop'
      sh 'docker container ls -a -f name=tests -q | xargs -r docker container rm -f' 
  }
*/
   stage('Start Selenium Grid') {       
     
       bat "docker pull elgalu/selenium:latest"
       //sh "docker run -d --rm --name=grid -p 4444:24444 -p 5900:25900 -e TZ=\"US/Pacific\" --expose 4444 -v d:/data:/data --privileged elgalu/selenium"
       bat "docker run -d --rm --name=grid -e TZ=\"US/Pacific\" -P --expose 24444 -v d:/testDocker/data:/testDocker/data --privileged elgalu/selenium:latest"
       //sh "docker exec grid wait_all_done 30s"  
    }

    stage('Start Tests Container') {
        bat "docker build -t \"${imageName}\" - < config/Dockerfile"
        echo ("Image built...")
        bat "docker run -it -d --rm --name \"${imageName}\" --link grid -e \"AT_PORT=24444\" -e \"AT_HOST=grid\" -v d:/testDocker/data:/testDocker/data --privileged ${imageName}"
    }

  def errors = [];

  stage('Run tests') {

	 try {
       echo 'Running test in Docker container'
      //  sh "docker exec -i tests bash -c \"git clone ${gitRepository} && cd ${gitCheckoutFolder} && yarn && ./node_modules/.bin/wdio config/wdio.conf.js\""
      executeCommand("git clone ${gitRepository} && cd test_docker && yarn && yarn run test")
    } catch (error) {
      errors.push(error);
  }  

  stage('Check job build') {
      if (errors.length) {
        errors.each {
          echo "Error: ${it}"
        }
        error 'Tests Failed';
      } else {
        echo 'All tests passed success!'
      }
    }

  stage('Remove containers') {
      bat "docker rm -vf grid"
      bat "docker rm -vf tests"
    }
  }
}