<template>
  <FeatherButton icon="Increase SZL" @click="increaseSemanticZoomLevel">
    <FeatherIcon :icon="ExpandLess"></FeatherIcon>
  </FeatherButton>
  <FeatherChip>{{ semanticZoomlevel }}</FeatherChip>
  <FeatherButton icon="Decrease SZL" @click="decreaseSemanticZoomLevel">
    <FeatherIcon :icon="ExpandMore"></FeatherIcon>
  </FeatherButton>
</template>

<script setup lang="ts">
import { useTopologyStore } from '@/store/Views/topologyStore'
import { FeatherIcon } from '@featherds/icon'
import { FeatherButton } from '@featherds/button'
import { FeatherChip } from '@featherds/chips'
import ExpandLess from '@featherds/icon/navigation/ExpandLess'
import ExpandMore from '@featherds/icon/navigation/ExpandMore'

const topologyStore = useTopologyStore()
const semanticZoomlevel = computed<number>(() => topologyStore.semanticZoomLevel)

const increaseSemanticZoomLevel = () => topologyStore.setSemanticZoomLevel(semanticZoomlevel.value + 1)

const decreaseSemanticZoomLevel = () => {
  if (semanticZoomlevel.value === 0) return
  topologyStore.setSemanticZoomLevel(semanticZoomlevel.value - 1)
}
</script>
