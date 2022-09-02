const typeDefs = `
  type Device {
    id: String!
    label: String!
    status: String!
    icmp_latency: String!
    snmp_uptime: String!
    createTime: String!
  }
  input DeviceInput {
    createTime: String
    domainName: String
    foreignId: String
    foreignSource: String
    id: Int
    label: String
    labelSource: String
    lastEgressFlow: String
    lastIngressFlow: String
    lastPoll: String
    location: LocationDTOInput
    netBiosName: String
    operatingSystem: String
    parentId: Int
    sysContact: String
    sysDescription: String
    sysLocation: String
    sysName: String
    sysOid: String
    type: String
  }
  input LocationDTOInput {
    geolocation: String
    latitude: Float
    locationName: String
    longitude: Float
    monitoringArea: String
    priority: Int
    tags: [String]
  }
  type ListDevices {
    devices: [Device!]!
  }
  type Minion {
    id: String!
    label: String!
    status: String!
    location: String!
    lastUpdated: String!
  }
  type ListMinions {
    minions: [Minion!]!
  }
  type Query {
    device: Device!
    listDevices: ListDevices!
    minion: Minion!
    listMinions: ListMinions!
  },
  type Mutation {
    saveRoutingKey(key: String!): String
  }
  type Mutation {
    addDevice(device: DeviceInput!): String
  }
`

export default typeDefs
