import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import { ListDevicesForGeomapDocument } from '@/types/graphql'

export const useGeomapQueries = defineStore('geomapQueries', {
  state: () => {
    // devices
    const { data: devices } = useQuery({
      query: ListDevicesForGeomapDocument
    })

    return {
      devicesForGeomap: computed(() => devices?.value?.listDevices?.devices || [])
    }
  }
})
