# Horizon Stream 🚀

Horizon Stream is an open source network monitoring tool.
It aims to be an easy to use, secure, reliable and extensible open-source network monitoring plattform.
The goal is to give our users network visibility and actionable insights helping them to provide better IT services. 

☣️  The project is in a very early stage and things break and move around. It is not ready to run production workloads.

## Supported Platforms

To deploy the whole application a [Kubernetes (K8s)](https://kubernetes.io/) cluster is required.
Currently you need a K8s cluster with 24GB RAM.

## Documentation

We use the GitHub Wiki in the Horizon Stream GitHub project to document the project.
It gives us enough flexibility and structure to migrate it later to a more sophisticated versioned documentation framework.
The following sections might be of most interest: 
* Installation in the [administrators getting started](https://github.com/OpenNMS/horizon-stream/wiki/Getting-Started---Admin) section.
* Building from source  in the [developers getting started](https://github.com/OpenNMS/horizon-stream/wiki/Getting-Started) section.
* [Contribution guidelines](https://github.com/OpenNMS/horizon-stream/wiki/Development-Guidelines), if you want to contribute code.

* [Troubleshooting](https://github.com/OpenNMS/horizon-stream/wiki/Troubleshooting) for quick help.
If you have questions feel free and talk to people in our community in our [opennms-discussion](https://chat.opennms.com/opennms-discuss) channel in Mattermost.

## Questions

### How does this relate to OpenNMS Horizon?

To get these principles addressed without breaking people's current solutions and implementations.
We know people in the OpenNMS community and commercial customers spent time and money to integrate OpenNMS Horizon into their environments.
OpenNMS Horizon gets the maintenance and enhancements through the OpenNMS Plugin API extensions without a change.
We can't do a major re-architecture of the platform without breaking things for our current users.
Our goal is to take over the OpenNMS Plugin API as an integration contract, and we aim to make the existing plugins compatible.

## License

The software is licensed under the GNU Affero General Public License 3.0.
