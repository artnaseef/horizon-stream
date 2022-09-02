import { SORT } from '@featherds/table'

export interface Category {
  authorizedGroups: string[]
  id: number
  name: string
}
export interface Node {
  location: string
  type: string
  label: string
  id: string
  assetRecord: {
    longitude: string
    latitude: string
  }
  categories: Category[]
  createTime: number
  foreignId: string
  foreignSource: string
  lastEgressFlow: any
  lastIngressFlow: any
  labelSource: string
  lastCapabilitiesScan: string
  primaryInterface: number
  sysObjectId: string
  sysDescription: string
  sysName: string
  sysContact: string
  sysLocation: string
}

export interface Coordinates {
  latitude: number | string
  longitude: number | string
}

export interface Alarm {
  id: string
  severity: string
  nodeId: number
  nodeLabel: string
  uei: string
  count: number
  lastEventTime: number
  logMessage: string
}

export interface QueryParameters {
  limit?: number
  offset?: number
  _s?: string
  orderBy?: string
  order?: SORT
  search?: string
  groupBy?: string
  groupByValue?: string
  [x: string]: any
}

export interface AlarmQueryParameters {
  ack?: boolean
  clear?: boolean
  escalate?: boolean
}

export interface AlarmModificationQueryVariable {
  pathVariable: string
  queryParameters: AlarmQueryParameters
}

export interface FeatherSortObject {
  property: string
  value: SORT | any
}
