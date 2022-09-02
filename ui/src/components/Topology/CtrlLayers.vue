<template>
  <FeatherButton
    :disabled="disable"
    icon="Layers"
    @click="triggerLayersSidebar"
  >
    <FeatherIcon :icon="Menu"></FeatherIcon>
  </FeatherButton>
</template>

<script
  setup
  lang="ts"
>
import { useTopologyStore } from '@/store/Views/topologyStore'
import { FeatherIcon } from '@featherds/icon'
import { FeatherButton } from '@featherds/button'
import Menu from '@featherds/icon/navigation/Menu'

const topologyStore = useTopologyStore()
const disable = computed<boolean>(() => !topologyStore.topologyGraphsSubLayers.length)
const isOpen = computed<boolean>(() => topologyStore.isRightDrawerOpen)
const triggerLayersSidebar = () => topologyStore.isRightDrawerOpen = !isOpen.value
</script>
