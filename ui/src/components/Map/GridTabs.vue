<template>
  <FeatherTabContainer class="tabs">
    <template v-slot:tabs>
      <FeatherTab ref="alarmTab" @click="goToAlarms" data-test="alarm-tab">Alarms({{ alarms?.length }})</FeatherTab>
      <FeatherTab ref="nodesTab" @click="goToNodes" data-test="nodes-tab">Nodes({{ nodes?.length }})</FeatherTab>
    </template>
  </FeatherTabContainer>
  <router-view />
</template>
<script setup lang="ts">
import { useMapStore } from '@/store/Views/mapStore'
import { useGeomapQueries } from '@/store/Queries/geomapQueries'
import { FeatherTab, FeatherTabContainer } from '@featherds/tabs'
import { Alarm } from '@/types/map'

const mapStore = useMapStore()
const geomapQueries = useGeomapQueries()
const router = useRouter()
const route = useRoute()
const nodes = computed(() => geomapQueries.devicesForGeomap)
const alarms = computed<Alarm[]>(() => mapStore.fetchAlarms())
const alarmTab = ref()
const nodesTab = ref()

const goToAlarms = () => router.push(`/map${route.query.nodeid ? '?nodeid=' + route.query.nodeid : ''}`)

const goToNodes = () => router.push('/map/nodes')

onActivated(() => {
  if (router.currentRoute.value.name === 'MapAlarms') {
    alarmTab.value.tab.click()
  } else {
    nodesTab.value.tab.click()
  }
})
</script>

<style scoped lang="scss">
@import "@featherds/styles/themes/variables";
.tabs {
  z-index: 2;
  padding-bottom: 10px;
  margin-bottom: -29px;
  background: var($surface);
}
</style>
