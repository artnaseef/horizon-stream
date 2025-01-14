import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import { 
  ListNodesForTableDocument, 
  ListMinionsForTableDocument, 
  ListMinionsAndDevicesForTablesDocument, 
  ListMinionMetricsDocument,
  ListNodeMetricsDocument,
  TsResult,
  Minion,
  Node
} from '@/types/graphql'
import { ExtendedMinion } from '@/types/minion'
import { ExtendedNode } from '@/types/node'
import useSpinner from '@/composables/useSpinner'
import { Monitor } from '@/types'

export const useAppliancesQueries = defineStore('appliancesQueries', {
  state: () => {
    const tableMinions = ref<ExtendedMinion[]>([])
    const tableNodes = ref<ExtendedNode[]>([])
    
    const { startSpinner, stopSpinner } = useSpinner()

    const fetchMinionsForTable = () => {
      const { data: minionsData, isFetching } = useQuery({
        query: ListMinionsForTableDocument,
        cachePolicy: 'network-only'
      })

      watchEffect(() => {
        isFetching.value ? startSpinner() : stopSpinner()

        const allMinions = minionsData.value?.findAllMinions as Minion[]
        if(allMinions?.length) {
          addMetricsToMinions(allMinions)
        }
      })
    }

    const fetchMinionMetrics = (instance: string) => useQuery({
      query: ListMinionMetricsDocument,
      variables: { instance, monitor: Monitor.ECHO },
      cachePolicy: 'network-only'
    })
    
    const addMetricsToMinions = ((allMinions: Minion[]) => {
      allMinions.forEach(async minion => {
        const { data, isFetching } = await fetchMinionMetrics(minion.systemId as string)
        const result = data.value?.minionLatency?.data?.result
        if(!isFetching.value) {
          if(result?.length) {
            const [{ value }] = data.value?.minionLatency?.data?.result as TsResult[]
            const [, val] = value as number[]
  
            tableMinions.value.push({
              ...minion,
              latency: {
                timestamp: val
              }
            })
          } else tableMinions.value.push(minion)
        }
      })
    })
    
    const fetchNodesForTable = () => {
      const { data: nodesData, isFetching } = useQuery({
        query: ListNodesForTableDocument,
        cachePolicy: 'network-only'
      })

      watchEffect(() => {
        isFetching.value ? startSpinner() : stopSpinner()
        
        const allNodes = nodesData.value?.findAllNodes as Node[]
        if(allNodes?.length) {
          addMetricsToNodes(allNodes)
        }
      })
    }

    const fetchNodeMetrics = (id: number, instance: string) => useQuery({
      query: ListNodeMetricsDocument,
      variables: { id, instance, monitor: Monitor.ICMP },
      cachePolicy: 'network-only'
    })
    
    const addMetricsToNodes = (allNodes: Node[]) => {
      tableNodes.value = [] // reset

      allNodes.forEach(async node => {
        const { data, isFetching } = await fetchNodeMetrics(node.id as number, node.ipInterfaces?.[0].ipAddress as string) // currently only 1 interface per node
        const latencyResult = data.value?.nodeLatency?.data?.result
        const status = data.value?.nodeStatus?.status

        if(!isFetching.value) {
          let tableNode: ExtendedNode = {
            ...node,
            status
          }

          if(latencyResult?.length) {
            const [{ value }] = latencyResult as TsResult[]
            const [, val] = value as number[]

            tableNode = {
              ...tableNode,
              latency: {
                timestamp: val
              }
            }
          }
          
          tableNodes.value.push(tableNode)
        } 
      })
    }
    
    // minions AND nodes table
    const { data: minionsAndNodes, execute, isFetching } = useQuery({
      query: ListMinionsAndDevicesForTablesDocument,
      cachePolicy: 'network-only'
    })

    watchEffect(() => {
      isFetching.value ? startSpinner() : stopSpinner()

      const allMinions = minionsAndNodes.value?.findAllMinions as Minion[]
      if(allMinions?.length) {
        addMetricsToMinions(allMinions)
      }

      const allNodes = minionsAndNodes.value?.findAllNodes as Node[]
      if(allNodes?.length) {
        addMetricsToNodes(allNodes)
      }
    })

    const locations = computed(() => minionsAndNodes.value?.findAllLocations || [])
    
    return {
      tableMinions,
      fetchMinionsForTable,
      tableNodes,
      fetchNodesForTable,
      locations,
      fetch: execute
    }
  }
})
