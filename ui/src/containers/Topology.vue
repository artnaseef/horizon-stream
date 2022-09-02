<template>
  <NetworkGraph
    v-if="displayGraph"
    :refresh="refreshGraph"
  />
  <Teleport to="body">
    <SideControls :refreshGraph="refreshGraph" />
  </Teleport>
</template>

<script
  setup
  lang="ts"
>
import NetworkGraph from '@/components/Topology/NetworkGraph.vue'
import SideControls from '@/components/Topology/SideControls.vue'
import { DisplayType } from '@/components/Topology/topology.constants'
import { useTopologyStore } from '@/store/Views/topologyStore'

const topologyStore = useTopologyStore()
const displayGraph = ref(true)

const refreshGraph = async () => {
  displayGraph.value = false
  await nextTick()
  displayGraph.value = true
}

onMounted(async () => {
  // TODO: Make intitial gql graphs call
  // await store.dispatch('topologyModule/getTopologyGraphs')

  topologyStore.setSelectedDisplay(DisplayType.nodes) // set default graph
})
</script>

