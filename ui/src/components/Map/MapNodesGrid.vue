<template>
  <div id="wrap">
    <table class="tl1 tl2 tl3" summary="Nodes">
      <thead>
        <tr>
          <FeatherSortHeader
            scope="col"
            property="id"
            :sort="sortStates.id"
            @sort-changed="sortChanged"
            data-test="col-id"
          >ID</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="foreignSource"
            :sort="sortStates.foreignSource"
            @sort-changed="sortChanged"
            data-test="col-foreign-source"
          >FOREIGN SOURCE</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="foreignId"
            :sort="sortStates.foreignId"
            @sort-changed="sortChanged"
            data-test="col-foreign-id"
          >FOREIGN ID</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="label"
            :sort="sortStates.label"
            @sort-changed="sortChanged"
            data-test="col-label"
          >LABEL</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="labelSource"
            :sort="sortStates.labelSource"
            @sort-changed="sortChanged"
            data-test="col-label-source"
          >LABEL SOURCE</FeatherSortHeader>

          <!-- <FeatherSortHeader
            scope="col"
            property="lastCapabilitiesScan"
            :sort="sortStates.lastCapabilitiesScan"
            @sort-changed="sortChanged"
            data-test="col-last-cap-scan"
          >LAST CAP SCAN</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="primaryInterface"
            :sort="sortStates.primaryInterface"
            @sort-changed="sortChanged"
            data-test="col-primary-interface"
          >PRIMARY INTERFACE</FeatherSortHeader> -->

          <FeatherSortHeader
            scope="col"
            property="sysObjectId"
            :sort="sortStates.sysObjectId"
            @sort-changed="sortChanged"
            data-test="col-sys-object-id"
          >SYSOBJECTID</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="sysName"
            :sort="sortStates.sysName"
            @sort-changed="sortChanged"
            data-test="col-sys-name"
          >SYSNAME</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="sysDescription"
            :sort="sortStates.sysDescription"
            @sort-changed="sortChanged"
            data-test="col-sys-description"
          >SYSDESCRIPTION</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="sysContact"
            :sort="sortStates.sysContact"
            @sort-changed="sortChanged"
            data-test="col-sys-contact"
          >SYSCONTACT</FeatherSortHeader>

          <FeatherSortHeader
            scope="col"
            property="sysLocation"
            :sort="sortStates.sysLocation"
            @sort-changed="sortChanged"
            data-test="col-sys-location"
          >SYSLOCATION</FeatherSortHeader>
        </tr>
      </thead>
      <tbody data-test="node-list">
        <tr v-for="device in geomapQueries.devicesForGeomap" :key="(device?.id as number)" @dblclick="doubleClickHandler(device as Partial<DeviceDto>)">
          <td class="first-td" :class="nodeLabelAlarmServerityMap[device?.label as string]">{{ device?.id }}</td>
          <td>{{ device?.foreignSource }}</td>
          <td>{{ device?.foreignId }}</td>
          <td>{{ device?.label }}</td>
          <td>{{ device?.labelSource }}</td>
          <!-- <td v-date>{{ device?.lastCapabilitiesScan }}</td> -->
          <!-- <td>{{ device?.primaryInterface }}</td> -->
          <td>{{ device?.sysOid }}</td>
          <td>{{ device?.sysName }}</td>
          <td>{{ device?.sysDescription }}</td>
          <td>{{ device?.sysContact }}</td>
          <td>{{ device?.sysLocation }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
<script setup lang="ts">
import { useMapStore } from '@/store/Views/mapStore'
import { useGeomapQueries } from '@/store/Queries/geomapQueries'
import { Coordinates, FeatherSortObject } from '@/types/map'
import { FeatherSortHeader, SORT } from '@featherds/table'
import { DeviceDto } from '@/types/graphql';

const mapStore = useMapStore()
const geomapQueries = useGeomapQueries()
const nodeLabelAlarmServerityMap = computed(() => mapStore.getNodeAlarmSeverityMap)

const doubleClickHandler = (device: Partial<DeviceDto>) => {
  if (device.location?.latitude && device.location.longitude) {
    const coordinate: Coordinates = { latitude: device?.location?.latitude, longitude: device?.location?.longitude }
    mapStore.mapCenter = coordinate
  }
}

const sortStates: any = reactive({
  label: SORT.ASCENDING,
  id: SORT.NONE,
  foreignSource: SORT.NONE,
  foreignId: SORT.NONE,
  labelSource: SORT.NONE,
  lastCapabilitiesScan: SORT.NONE,
  primaryInterface: SORT.NONE,
  sysObjectId: SORT.NONE,
  sysName: SORT.NONE,
  sysDescription: SORT.NONE,
  sysContact: SORT.NONE,
  sysLocation: SORT.NONE
})

const sortChanged = (sortObj: FeatherSortObject) => {
  for (const key in sortStates) {
    sortStates[key] = SORT.NONE
  }
  sortStates[`${sortObj.property}`] = sortObj.value
  mapStore.nodeSortObject = sortObj
}

onMounted(() => {
  const wrap = document.getElementById('wrap')
  const thead = document.querySelector('thead')

  if (wrap && thead) {
    wrap.addEventListener('scroll', function () {
      let translate = 'translate(0,' + this.scrollTop + 'px)'
      thead.style.transform = translate
    })
  }
})
</script>

<style lang="scss" scoped>
@import "@featherds/table/scss/table";
#wrap {
  height: calc(100% - 29px);
  overflow: auto;
  background: var($surface);
}
table {
  @include table;
  @include table-condensed;
  background: var($surface);
  color: var($primary-text-on-surface);
  padding-top: 4px;
  margin-top: 15px;
}
thead {
  z-index: 2;
  position: relative;
  background: var($surface);
  white-space: nowrap;
}
.first-td {
  padding-left: 12px;
  border-left: 4px solid var($success);
}
.WARNING {
  border-left: 4px solid #fffb00ea
}
.MINOR {
  border-left: 4px solid var($warning);
}
.MAJOR {
  border-left: 4px solid #ff3c00;
}

.CRITICAL {
  border-left: 4px solid var($error);
}
</style>
