import { ExtendedDeviceDTO } from '@/types/device'
import { ExtendedMinionDTO } from '@/types/minion'
import { add, intervalToDuration, formatDuration } from 'date-fns'

export interface BGColors {
  statusBgColor: string
  latencyBgColor: string
  uptimeBgColor: string
}

/**
 * Change color background on metrics value
 * @param list 
 * @returns list of items with added metrics background color props
 */
export const formatItemBgColor = (list: ExtendedMinionDTO[] | ExtendedDeviceDTO[]) => list.map(item => {
  const {icmp_latency: latency, snmp_uptime: uptime, status} = item
  const bg = {
    ok: 'bg-ok',
    failed: 'bg-failed',
    unknown: 'bg-unknown'
  }

  return {
    ...item,
    latencyBgColor: latency >= 0 ? bg.ok : bg.failed,
    uptimeBgColor: uptime >= 0 ? bg.ok : bg.failed,
    statusBgColor: status === 'UP' ? bg.ok : bg.failed
  }
})

export const getHumanReadableDuration = (uptimeInSeconds: number) => {
  let durationDisplay = '--' // undefined

  if(uptimeInSeconds >= 0) {
    if (uptimeInSeconds < 60) durationDisplay = `${uptimeInSeconds} ${uptimeInSeconds <= 0 ? '' : 'seconds'}` // 0-59secs
    else {
      const duration = intervalToDuration({
        start: new Date(),
        end: add(new Date(), {seconds: uptimeInSeconds})
      })
  
      durationDisplay = formatDuration(duration, { format: ['days', 'hours', 'minutes']}) // +1min
    }
  }

  return durationDisplay
}

export const formatLatencyDisplay = (latency: number) => {
  let displayLatency = '--'

  if(latency >= 0) displayLatency = `${latency}ms`

  return displayLatency
}
