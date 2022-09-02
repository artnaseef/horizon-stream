import MapAlarmsGrid from '@/components/Map/MapAlarmsGrid.vue'
import setupWrapper from 'tests/setupWrapper'

let wrapper: any

beforeEach(() => {
  wrapper= setupWrapper({
    component: MapAlarmsGrid
  })
})

const columns = [
  [ 'ID', 'col-id' ],
  [ 'SEVERITY', 'col-severity' ],
  [ 'NODE LABEL', 'col-node-label' ],
  [ 'UEI', 'col-uei' ],
  [ 'COUNT', 'col-count' ],
  [ 'LAST EVENT', 'col-last-event' ],
  [ 'LOG MESSAGE', 'col-log-msg' ]
]
it.each(columns)('should have %s column', (_, col) => {
  const elem = wrapper.get(`[data-test="${col}"]`)
  expect(elem.exists()).toBeTruthy()
})

it('should have a dropdown selection input', () => {
  const elem = wrapper.get('[data-test="select-ack"]')
  expect(elem.exists()).toBeTruthy()
})

describe('selectAlarmAck method', () => {
  test.todo('...', () => {
    // when alarm list data avail, assert the modifyAlarm() has been called correctly
    expect(true).toBeTruthy()
  })
})
