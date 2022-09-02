<template>
  <FeatherNavigationRail @update:modelValue="store.triggerNavRail()">
    <template v-slot:main>
      <FeatherRailItem
        :class="{ selected: isSelected('/') }"
        href="/"
        :icon="Server"
        title="Appliances"
      />
      <FeatherRailItem
        :class="{ selected: isSelected('/map') }"
        href="/map"
        :icon="Pin"
        title="Map"
      />
    </template>
  </FeatherNavigationRail>
</template>

<script setup lang=ts>
import { useLayoutStore } from '@/store/Views/layoutStore'
import Server from '@featherds/icon/hardware/Server'
import Pin from '@featherds/icon/action/Pin'

const store = useLayoutStore()
const route = useRoute()

const isSelected = (path: string) => path === route.fullPath
</script>

