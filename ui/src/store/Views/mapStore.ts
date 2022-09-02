import { defineStore } from 'pinia'
import { FeatherSortObject } from '@/types'
import { Node, Alarm, Coordinates, QueryParameters, AlarmModificationQueryVariable } from '@/types/map'
import { LatLngBounds } from 'leaflet'
import { SORT } from '@featherds/table'
import { numericSeverityLevel } from '@/components/Map/utils'
import { orderBy } from 'lodash'
import { latLng } from 'leaflet'

export interface State {
  nodesWithCoordinates: Node[]
  alarms: Alarm[]
  interestedNodesID: string[]
  mapCenter: Coordinates
  mapBounds: LatLngBounds | undefined
  selectedSeverity: string
  searchedNodeLabels: string[]
  nodeSortObject: FeatherSortObject
  alarmSortObject: FeatherSortObject
}

export const useMapStore = defineStore('mapStore', {
  state: () => 
    <State>{
      nodesWithCoordinates: [],
      alarms: [],
      interestedNodesID: [],
      mapCenter: { latitude: 37.776603506225115, longitude: -33.43824554266541 },
      mapBounds: undefined,
      selectedSeverity: 'NORMAL',
      searchedNodeLabels: [],
      nodeSortObject: { property: 'label', value: SORT.ASCENDING },
      alarmSortObject: { property: 'id', value: SORT.DESCENDING }
    },
  actions: {
    async fetchNodes(queryParameters?: QueryParameters) {
      const defaultParams = queryParameters || { limit: 5000, offset: 0 }
      // todo: add graphQL query
      const resp: any = undefined
  
      if (resp) {
        const nodes = resp.node.filter(
          (node: Node) =>
            !(node.assetRecord.latitude == null || node.assetRecord.latitude.length === 0) &&
            !(node.assetRecord.longitude == null || node.assetRecord.longitude.length === 0)
        )

        this.nodesWithCoordinates = nodes

        this.interestedNodesID = nodes.map((node: Node) => node.id)
      }
    },
    fetchAlarms (queryParameters?: QueryParameters) {
      const defaultParams = queryParameters || { limit: 5000, offset: 0 }
      // todo: add graphQL query
      const resp = [] as Alarm[]
    
      if (resp) {
        this.alarms = resp
      }

      return this.alarms
    },
    async modifyAlarm(alarmQueryVariable: AlarmModificationQueryVariable) {
      // todo: add graphQL query
      const resp = {}
      return resp
    }
  },
  getters: {
    getNodeAlarmSeverityMap(state: State): Record<string, string> {
      const map: { [x: string]: string } = {}
    
      state.alarms.forEach((alarm: Alarm) => {
        if (numericSeverityLevel(alarm.severity) > numericSeverityLevel(map[alarm.nodeLabel])) {
          map[alarm.nodeLabel] = alarm.severity.toUpperCase()
        }
      })
    
      return map
    },
    getNodes(state: State): Node[] {
      const severityMap = this.getNodeAlarmSeverityMap
      const selectedNumericSeverityLevel = numericSeverityLevel(state.selectedSeverity)
    
      // copy the vuex nodes
      let nodes: Node[] = JSON.parse(JSON.stringify(state.nodesWithCoordinates))
    
      // sort the nodes
      nodes = orderBy(nodes, state.nodeSortObject.property, state.nodeSortObject.value)
    
      // filter for nodes within map view-port
      nodes = nodes.filter((node) => {
        const lat = Number(node.assetRecord.latitude)
        const lng = Number(node.assetRecord.longitude)
        const nodeLatLng = latLng(lat, lng)
    
        if (state.mapBounds) {
          return state.mapBounds.contains(nodeLatLng)
        }
    
        return false
      })
    
      // filter for nodes that meet selected severity
      if (state.selectedSeverity !== 'NORMAL') {
        nodes = nodes.filter((node) => {
          const nodeNumericSeverityLevel = numericSeverityLevel(severityMap[node.label])
          return state.selectedSeverity === 'NORMAL' || nodeNumericSeverityLevel >= selectedNumericSeverityLevel
        })
      }
    
      // filter for nodes that have been searched for
      if (state.searchedNodeLabels.length) {
        nodes = nodes.filter((node) => state.searchedNodeLabels.includes(node.label))
      }

      return nodes
    }
  }
})