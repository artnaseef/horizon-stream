import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import { AlarmsDocument, AlarmsQuery } from '@/types/graphql'

export const useAlarmsQueries = defineStore('alarmsQueries', () => {
  const { data, execute } = useQuery<AlarmsQuery>({
    query: AlarmsDocument
  })

  const alarms = computed(() => data.value?.listAlarms?.alarms || [])

  return {
    alarms,
    fetch: execute
  }
})
