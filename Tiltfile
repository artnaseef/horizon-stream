## Tilt config ##
load('ext://tilt_inspector', 'tilt_inspector')
tilt_inspector()

load('ext://k8s_attach', 'k8s_attach')
load('ext://namespace', 'namespace_yaml', 'namespace_inject')

secret_settings(disable_scrub=True)  ## TODO: update secret values so we can reenable scrub
update_settings(suppress_unused_image_warnings=["opennms/horizon-stream-grafana", "opennms/horizon-stream-keycloak"])

## Namespace ##
k8s_yaml(namespace_yaml('tilt-instance'), allow_duplicates=True)
k8s_yaml(namespace_yaml('tilt-operator'), allow_duplicates=True)

## Operator ##
local_resource('chart-deps', cmd='cd charts/opennms-operator && helm dep update')
k8s_yaml(helm('./charts/opennms-operator',
              namespace='tilt-operator',
              values=['./operator/values.yaml'], ))

docker_build("opennms/operator", "operator")

## OpenNMS ##
# k8s_yaml(helm('./charts-skaffold/opennms',
#               namespace='tilt-instance',
#               set=[
#                   'Namespace=tilt-instance',
#                   'OpenNMS.Core.ImagePullPolicy=Never',
#                   'OpenNMS.API.ImagePullPolicy=Never',
#                   'OpenNMS.UI.ImagePullPolicy=Never',
#                   'Grafana.Image=hendrikmaus/kubernetes-dummy-image',
#                   'Grafana.ImagePullPolicy=IfNotPresent',
#               ]))
#
# local_resource('shared-lib',
#                cmd='mvn clean install',
#                dir='shared-lib',
#                deps=['./shared-lib'],
#                ignore=['**/target/'])

k8s_kind('OpenNMS',
         api_version='opennms.k8s.opennms.com/v1alpha1',
         image_json_path=['{.spec.ui.image}', '{.spec.core.image}', '{.spec.api.image}'],
         pod_readiness='ignore')
k8s_yaml(read_file('./operator/local-instance-tilt.yaml'))
k8s_resource('local-instance',
             new_name='opennms-crd', )

### Core ###
local_resource(
    'compile-core',
    'mvn install -f platform/alarms/daemon',
    deps=['./platform/alarms/daemon'],
    ignore=['**/target'], )
custom_build(
    'opennms/horizon-stream-core',
    'mvn install -Pbuild-docker-images-enabled -DskipTests -Ddocker.image.tag=${EXPECTED_TAG} -f platform',
    deps=['./platform'],
    ignore=['**/target'],
    live_update=[
        sync('./platform/*.jar', '/opt/horizon-stream/deploy'),
    ], )

k8s_attach('horizon-stream-core',
           'deployment/opennms-core',
           namespace='tilt-instance', )
k8s_resource('horizon-stream-core',
             port_forwards=['18101:8101', '18181:8181', '5005'], )

### rest server ###
local_resource(
    'compile-rest-server',
    'mvn compile -f rest-server',
    deps=['./rest-server/src', './rest-server/pom.xml'], )

custom_build(
    'opennms/horizon-stream-rest-server',
    'mvn jib:dockerBuild -Dimage=$EXPECTED_REF -f rest-server',
    deps=['./rest-server/target/classes', './rest-server/pom.xml'],
    live_update=[
        sync('./rest-server/target/classes/java/main', '/app/classes'),
    ], )

k8s_attach('horizon-stream-rest-server',
           'deployment/opennms-rest-server',
           namespace='tilt-instance', )
k8s_resource('horizon-stream-rest-server',
             port_forwards=['9090', '5006:5005'],
             resource_deps=['compile-rest-server'], )

### ui ###
docker_build("opennms/horizon-stream-ui", "ui", dockerfile="ui/dev/Dockerfile",
             live_update=[
                 sync('./ui', '/app'),
                 run('yarn install', trigger=['./ui/package.json', './ui/yarn.lock']),
             ], )

k8s_attach('horizon-stream-ui',
           'deployment/opennms-ui',
           namespace='tilt-instance', )
k8s_resource('horizon-stream-ui',
             port_forwards=['3000'],
             )

### grafana ###
docker_build("opennms/horizon-stream-grafana", "grafana")

k8s_attach('horizon-stream-grafana',
           'deployment/grafana',
           namespace='tilt-instance', )
k8s_resource('horizon-stream-grafana',
             )

### keycloak ###
docker_build("opennms/horizon-stream-keycloak", "keycloak-ui",
             live_update=[
                 sync('./themes', '/opt/keycloak/themes')
             ], )

k8s_attach('horizon-stream-keycloak',
           'deployment/keycloak',
           namespace='tilt-instance', )
k8s_resource('horizon-stream-keycloak',
             )

# def init():
#   return "sh -c ./local-sample/run.sh"
#
# k8s_context()
#
# init_complete = os.getenv( "INIT_COMPLETE" , default = "FALSE" )
#
# if init_complete == "FALSE":
#   os.putenv( "INIT_COMPLETE" , "TRUE" )
#   local(init(), quiet=False)
#   k8s_yaml('./operator/local-instance.yaml')
#
# if init_complete == "TRUE":
#   print("Test var: ", init_complete)

# See for more info https://docs.tilt.dev/api.html#api.k8s_kind
# This is to do with the resource kind, not the Kind cluster manager. This
# updates the image field on the opennms crd for the api resource, will have to
# add one for each resource (ui, core, etc). May be better to just update the
# image directly. 
# TODO: The operator has a bug where it does not update yet the deployments and
# redeploys when this is changed, but it should and will be implemented when
# the fix is in place (FIXED, merged into develop, need to test). See the change below.
# k8s_yaml('./operator/local-instance.yaml')
# docker_build('...', '.') # Need to run this first. Instead, use custom_build().
# k8s_kind('opennms', image_json_path='{.spec.api.image}')
# Just apply the change directly to the 

# For importing images into kind cluster, see
# https://github.com/tilt-dev/kind-local for optimizing this process.

# Note: For testing the
# https://github.com/tilt-dev/tilt-example-java/tree/master/101-jib, I had to
# update the record-start-time.sh with the following:
#    
#    # Needed to install 'brew install coreutils' on Mac. And change the date to 
#    # gdate, had to remove the leading zero on startTimeNanos.
#    cat src/main/java/dev/tilt/example/IndexController.java | \
#        sed -e "s/startTimeSecs = .*;/startTimeSecs = $(gdate +%-s);/" | \
#        sed -e "s/startTimeNanos = .*;/startTimeNanos = $(gdate +%-N);/" > \
#        $tmpfile

# This temporary until the opennms operator can automatically update the
# deployment from a CRD change.
# local("kubectl -n opennms rollout restart deployment.apps/opennms-operator", quiet=False)

# Go example:
# docker_build('example-go-image', '.', dockerfile='deployments/Dockerfile')
# k8s_yaml('deployments/kubernetes.yaml')
# TODO: Need a way to update the image directly possibly.

# Should not need this, seeing that we have ingresses installed. They update
# automatically.
# k8s_resource('example-go', port_forwards=8000)
