// @ts-nocheck
import casual from 'casual'
import { rndNumber, rndStatus, rndLatency, rndUptime } from '../helpers/random'

casual.define('device', function () {
  return {
    id: casual.uuid,
    label: `DEVICE-${casual.word}`,
    icmp_latency: rndLatency(),
    snmp_uptime: rndUptime(),
    status: rndStatus(),
    createTime: casual.date()
  }
})

casual.define('listDevices', function () {
  return {
    devices: [casual.device, casual.device, casual.device, casual.device, casual.device, casual.device, casual.device, casual.device, casual.device, casual.device, casual.device, casual.device, casual.device, casual.device, casual.device],
    // count: rndNumber(),
    // totalCount: rndNumber(),
    // offset: rndNumber()
  }
})

const device = casual.device
const listDevices = casual.listDevices

export {
  device,
  listDevices
}