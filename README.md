# DevOps lab with a Java EE app supporting Gitlab review apps

This demonstration contains instructions and files to run a Java application on Kubernetes both in the local development environment provided by Kind and on Azure Kubernetes Service (AKS).

## Prerequisites

* Java development environment (e.g. IntelliJ)
* Docker (e.g. Docker Desktop)
* Azure CLI

## Local development

1. Run PostgreSQL on port 5432, for example with `docker run -p 5432:5432 -e POSTGRES_HOST_AUTH_METHOD=trust postgres`
   * The tests try to create a new database, so it's important that they can connect as the `postgres`-user
2. `mvn test` downloads dependencies, builds the API and the frontend and run the tests
3. Run `npm start` to you want to make changes to the frontend
4. Start the Java-class `com.soprasteria.devopsacademy.ApplicationServer`. This will update the database schema and start a webserver on port 8080
5. Go to `http://localhost:8080`

## Create a docker image to Azure Container Registry

This builds an image that can be used on different clusters, including Kind (next step).

You need an Azure Account in order for the following to work

1. Log in with Azure for the command line: `az login`
2. Create a Resource Group for the workshop `az group create --name devops --location norwayeast`
3. Create a Container Registry (price at time of writing: $1.15 per week): `az acr create --resource-group devops --name devops2023registry --sku standard` (name must be globally unique. This becomes `devops2023registry.azurecr.io`)
4. Log in to the container registry so Docker builds can access it: `az acr login --name devops2023registry`
5. Run build, create docker file and push it to ACR: `mvn -Djib.to.image=devops2023registry.azurecr.io install`

You should now be able to see the container on the Azure portal

## Running locally on Kind

This sets up a local Kubernetes cluster on your local Docker server and installs the application from the previous step into the cluster.

### Create a Kind cluster

1. Install the necessary dependencies
    * Docker desktop
    * [kind](https://kind.sigs.k8s.io/)
2. Create a cluster on your local machine: `kind create cluster --config setup/create-cluster.yaml`
    * `create-cluster.yaml` contains configuration to forward port 80 and 443 on your local machine to this cluster
    * This configuration includes support for ingress controllers. If you don't need this, you can do `kind create cluster`
    * The `kind create cluster` command updates `~/.kube/config` to make `kubectl` connect to the new cluster
    * You can see the cluster nodes running with `docker ps`
    * You can see the nodes from the perspective of the cluster with `kubectl get nodes`
3. Install an Ingress controller (in our case: Nginx):
    * `kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml`
    * Wait for the pods to start `kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=90s`
4. Install the postgresql operator: `kubectl apply -k github.com/zalando/postgres-operator/manifests`

### Create a namespace that can pull from ACR

Replace `devops2023registry.azurecr.io` with your own Docker Registry.

1. Get the full id for the registry `az acr show --name devops2023registry --query "id" --output tsv` (something like `/subscriptions/..../resourceGroups/devops/providers/Microsoft.ContainerRegistry/registries/...`)
2. Create a service principal with access to the registry: `az ad sp create-for-rbac --name devops-javaee-pull-secret --role acrpull --scope <value from step 1>` (this will output an `appId` and `password`)
3. Create test namespace: `kubectl create namespace devops-javaee`
4. Create a pull-token for the namespace: `kubectl --namespace devops-javaee create secret docker-registry pull-secret --docker-username=<appId> --docker-password=<password> --docker-server=devops2023registry.azurecr.io`


### Run the application on Kind

Requires the application to have been uploaded to ACR (see above). Replace `devops2023registry` with your own ACR registry.

1. Test out the helm template: `helm template src/main/kubernetes/`
2. Deploy the application: `helm template --set image=devops2023registry.azurecr.io/devops-javaee src/main/kubernetes | kubectl --namespace devops-javaee apply -f -`
3. `curl http://javaee.example.localhost` shows the running application. You will be able to interact with the database
4. `kubectl delete namespace devops-javaee` tears down everything (NB: includes the pull-secret)

### Destroy the cluster

1. `kind delete cluster`

### Running on Azure (AKS)

In order to have any fun, we have to run this in the cloud.

This description assumes you have already deployed to Azure Container Registry as described above.

Please note: Application Gateway is pretty expensive. Remember to delete your resource group after use.

1. Log into Azure: `az login`
2. Create a Kubernetes cluster with an appGateway ingress: `az aks create --resource-group devops --network-plugin azure --enable-managed-identity -a ingress-appgw --appgw-name myApplicationGateway --appgw-subnet-cidr "10.225.0.0/16" --generate-ssh-keys --name <cluster name>`
   (this uses the resource group created together with the Docker Registry)
   * You could also leave out `-a` and `--appg-name` and instead call `az network application-gateway create`
   * You could leave out `--appgw-subnet-cidr` and instead call `az network public-ip create` and `az network vnet create`
   * See [Microsoft's documentation on AKS and AppGateway](https://learn.microsoft.com/en-us/azure/application-gateway/tutorial-ingress-controller-add-on-new) for details
3. Connect kubectl to the AKS cluster: `az aks get-credentials --resource-group devops --name <cluster-name>`
4. Install the postgresql operator: `kubectl apply -k github.com/zalando/postgres-operator/manifests`

If you want to test it out, you can deploy Microsoft's sample application: `kubectl apply -f https://raw.githubusercontent.com/Azure/application-gateway-kubernetes-ingress/master/docs/examples/aspnetapp.yaml`

### Create a namespace that can pull from ACR

Replace `devops2023registry.azurecr.io` with your own Docker Registry.

1. Get the full id for the registry `az acr show --name devops2023registry --query "id" --output tsv` (something like `/subscriptions/..../resourceGroups/devops/providers/Microsoft.ContainerRegistry/registries/...`)
2. Create a service principal with access to the registry: `az ad sp create-for-rbac --name devops-javaee-pull-secret --role acrpull --scope <value from step 1>` (this will output an `appId` and `password`)
3. Create test namespace: `kubectl create namespace devops-javaee-test`
4. Create a pull-token for the namespace: `kubectl --namespace devops-javaee-test create secret docker-registry pull-secret --docker-username=<appId> --docker-password=<password> --docker-server=devops2023registry.azurecr.io`

### Setup hosts-file

It's possible to find the Frontend public IP address in the Azure Portal or the `az` command line, but it's a bit tricky.

You should add the following entries to your hosts file to point to the ingress gateway:

```
<Ingress Gateway IP> devops-javaee.example.com devops-javaee.test.example.com devops-javaee-demo.test.example.com
```

### Deploy the application

This is the same as deploying to Kind (above), except with a different ingressClassName and domain.

1. Deploy primary test instance: `helm template src/main/kubernetes --set image=devops2023registry.azurecr.io/devops-javaee --set ingressClassName=azure-application-gateway --set domain=test.example.com | kubectl --namespace devops-javaee-test apply -f -`
   * You should be able to verify this deployment with `curl http://devops-javaee.test.example.com`
2. Deploy test branch instance:  `helm template src/main/kubernetes --set image=devops2023registry.azurecr.io/devops-javaee --set ingressClassName=azure-application-gateway --set suffix=-demo --set domain=test.example.com | kubectl --namespace devops-javaee-test apply -f -`
   * You should be able to verify this deployment with `curl http://devops-javaee-demo.test.example.com`
   * Notice that this has a separate contents in the database
3. Deploy production instance to the production namespace `helm template src/main/kubernetes --set image=devops2023registry.azurecr.io/devops-javaee --set ingressClassName=azure-application-gateway --set domain=example.com | kubectl --namespace devops-javaee apply -f -`
   * You should be able to verify this deployment with `curl http://devops-javaee.example.com`

### Deploy with Gitlab

We need to update the namespaces created manually to use Gitlab package registry instead of ACR (this only covers the test namespaces. Production is left as an exercise to the reader)

1. Create a Gitlab repository
2. (Already done) Create a namespace for the application: `kubectl create namespace devops-javaee-test`
3. Create a service account: `kubectl --namespace devops-javaee-test apply -f setup/service-account.yaml` (this used by Gitlab to access the Kubernetes custer)
4. Get the service account token: `kubectl --namespace devops-javaee-test create token build-robot` ("build-robot" is the name from `service-account.yaml`)
5. Save the following variables at the Gitlab Repository: Settings > CI/CD > Variables
   * `TEST_KUBECTL_TOKEN`: The output from `create token` (should be masked)
   * `TEST_KUBECTL_SERVER`: the Kubernetes control plane from `kubectl cluster-info` (starts with "https:")
   * `TEST_KUBECTL_NAMESPACE`: `devops-javaee-test`
   * `TEST_DOMAIN`: `test.example.com`
6. In the Gitlab Project (above the Repository), issue a new access token: Settings > Repository > Deploy tokens
   * Token name can be anything you want. I'm using "aks_token"
   * Scopes should include "read_registry"
7. Find the docker-server under the GitLab Repository > Packages and registries > Container Registry
8. Create a pull-token for the namespace: `kubectl --namespace devops-javaee-test create secret docker-registry pull-secret --docker-username=aks_token --docker-password=<password> --docker-server=<GITLAB container registry hostname>` (this lets Kubernetes pull from Gitlab)
9. Make a change in `src/main/frontend/application.tsx` and this repository to your Gitlab repository
10. When the build is complete you should be able to access by going to the Gitlab repository > Deployments > Environments

### Deploy to review branch

1. Create a branch named `test/demo` on Gitlab
2. When the build is complete you should be able to access by going to the Gitlab repository > Deployments > Environments
3. You can delete the environment when it's no longer needed or it will be deleted automatically after 1 day
