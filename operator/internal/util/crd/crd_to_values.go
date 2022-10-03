/*
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package crd

import (
	"github.com/OpenNMS/opennms-operator/api/v1alpha1"
	"github.com/OpenNMS/opennms-operator/internal/model/values"
)

// ConvertCRDToValues - convert an ONMS crd into a set of template values
func ConvertCRDToValues(crd v1alpha1.OpenNMS, defaultValues values.TemplateValues) values.TemplateValues {
	templateValues := defaultValues

	v := templateValues.Values
	spec := crd.Spec

	v.Namespace = spec.Namespace
	v.Host = spec.Host

	if spec.HttpPort != 0 {
		v.Ingress.HttpPort = spec.HttpPort
	}
	if spec.HttpsPort != 0 {
		v.Ingress.HttpsPort = spec.HttpsPort
	}

	//ONMS Core
	v.OpenNMS = getCoreValues(spec, v.OpenNMS)

	//ONMS API
	v.OpenNMS = getAPIValues(spec, v.OpenNMS)

	//ONMS UI
	v.OpenNMS = getUIValues(spec, v.OpenNMS)

	//ONMS Minion
	v.OpenNMS = getMinionValues(spec, v.OpenNMS)

	//ONMS Notification
	v.OpenNMS = getNotificationValues(spec, v.OpenNMS)

	//Keycloak
	v.Keycloak = getKeycloakValues(spec, v.Keycloak)

	//Grafana
	v.Grafana = getGrafanaValues(spec, v.Grafana)

	//Postgres
	v.Postgres = getPostgresValues(spec, v.Postgres)

	if spec.TestDeploy {
		v.TestDeploy = spec.TestDeploy
		v = overrideImages(v)
	}

	templateValues.Values = v

	return templateValues
}

// getCoreValues - get ONMS core values from the crd
func getCoreValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.Core.Image != "" {
		v.Core.Image = spec.Core.Image
	}
	if spec.Core.CPU != "" {
		v.Core.Resources.Requests.Cpu = spec.Core.CPU
		v.Core.Resources.Limits.Cpu = spec.Core.CPU
	}
	if spec.Core.MEM != "" {
		v.Core.Resources.Requests.Memory = spec.Core.MEM
		v.Core.Resources.Limits.Memory = spec.Core.MEM
	}
	if spec.Core.Disk != "" {
		v.Core.VolumeSize = spec.Core.Disk
	}
	return v
}

// getAPIValues - get ONMS API values from the crd
func getAPIValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.API.Image != "" {
		v.API.Image = spec.API.Image
	}
	if spec.API.CPU != "" {
		v.API.Resources.Requests.Cpu = spec.API.CPU
		v.API.Resources.Limits.Cpu = spec.API.CPU
	}
	if spec.API.MEM != "" {
		v.API.Resources.Requests.Memory = spec.API.MEM
		v.API.Resources.Limits.Memory = spec.API.MEM
	}
	if spec.API.Disk != "" {
		v.API.VolumeSize = spec.API.Disk
	}
	return v
}

// getUIValues - get ONMS UI values from the crd
func getUIValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.UI.Image != "" {
		v.UI.Image = spec.UI.Image
	}
	if spec.UI.CPU != "" {
		v.UI.Resources.Requests.Cpu = spec.UI.CPU
		v.UI.Resources.Limits.Cpu = spec.UI.CPU
	}
	if spec.UI.MEM != "" {
		v.UI.Resources.Requests.Memory = spec.UI.MEM
		v.UI.Resources.Limits.Memory = spec.UI.MEM
	}
	if spec.UI.Disk != "" {
		v.UI.VolumeSize = spec.UI.Disk
	}
	return v
}

// getMinionValues - get ONMS Minion values from the crd
func getMinionValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.Minion.Image != "" {
		v.Minion.Image = spec.Minion.Image
	}
	if spec.Minion.CPU != "" {
		v.Minion.Resources.Requests.Cpu = spec.Minion.CPU
		v.Minion.Resources.Limits.Cpu = spec.Minion.CPU
	}
	if spec.Minion.MEM != "" {
		v.Minion.Resources.Requests.Memory = spec.Minion.MEM
		v.Minion.Resources.Limits.Memory = spec.Minion.MEM
	}
	if spec.Minion.Disk != "" {
		v.Minion.VolumeSize = spec.Minion.Disk
	}
	return v
}

// getNotificationValues - get ONMS Notification values from the crd
func getNotificationValues(spec v1alpha1.OpenNMSSpec, v values.OpenNMSValues) values.OpenNMSValues {
	if spec.Notification.Image != "" {
		v.Notification.Image = spec.Notification.Image
	}
	if spec.Notification.CPU != "" {
		v.Notification.Resources.Requests.Cpu = spec.Notification.CPU
		v.Notification.Resources.Limits.Cpu = spec.Notification.CPU
	}
	if spec.Notification.MEM != "" {
		v.Notification.Resources.Requests.Memory = spec.Notification.MEM
		v.Notification.Resources.Limits.Memory = spec.Notification.MEM
	}
	if spec.Notification.Disk != "" {
		v.Notification.VolumeSize = spec.Notification.Disk
	}
	return v
}

// getKeycloakValues - get Keycloak values from the crd
func getKeycloakValues(spec v1alpha1.OpenNMSSpec, v values.KeycloakValues) values.KeycloakValues {
	if spec.Keycloak.Image != "" {
		v.Image = spec.Keycloak.Image
	}
	return v
}

// getGrafanaValues - get ONMS Notification values from the crd
func getGrafanaValues(spec v1alpha1.OpenNMSSpec, v values.GrafanaValues) values.GrafanaValues {
	if spec.Grafana.Image != "" {
		v.Image = spec.Grafana.Image
	}
	return v
}

// overrideImages - overrides images with noop images for deployment testing purposes
func overrideImages(v values.Values) values.Values {
	noopServiceImage := "lipanski/docker-static-website:latest"
	noopJobImage := "alpine:latest"

	v.OpenNMS.Core.Image = noopServiceImage
	v.OpenNMS.API.Image = noopServiceImage
	v.OpenNMS.Core.Image = noopServiceImage
	v.Postgres.Image = noopServiceImage
	v.Grafana.Image = noopServiceImage

	v.Ingress.ControllerImage = noopServiceImage
	v.Ingress.SecretJobImage = noopJobImage
	v.Ingress.WebhookPatchJobImage = noopJobImage

	return v
}

// getPostgresValues - get Postgres DB values from the CRD
func getPostgresValues(spec v1alpha1.OpenNMSSpec, v values.PostgresValues) values.PostgresValues {
	if spec.Postgres.Disk != "" {
		v.VolumeSize = spec.Postgres.Disk
	}
	return v
}
