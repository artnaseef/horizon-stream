import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import { createClient, setActiveClient } from 'villus'
import MinionsTable from '@/components/Appliances/MinionsTable.vue'
import { setActivePinia } from 'pinia'

describe('MinionsTable.vue', () => {
  beforeEach(() => {
    const minionItems = computed(() => [{
      id: '1',
      status: 'ready',
      latency: 'latency1',
      cpu_util: 'cpu1'
    }]) 

    setActivePinia(createTestingPinia({
      initialState: { 
        minionsQueries  : { 
          listMinions: minionItems
        }
      }
    }))

    setActiveClient(createClient({
      url: 'http://test/graphql'
    }))
  })

  describe('Required columns', () => {
    const requiredColumns = [
      ['Label', 'col-label'],
      ['Status', 'col-status'],
      ['Location', 'col-location']
    ]

    it.each(requiredColumns)('should have "%s" column', (_, dataTest) => {
      const wrapper = mount(MinionsTable, { 
        global: { plugins: [createTestingPinia()] }
      })

      const elem = wrapper.find(`[data-test="${dataTest}"]`)
      expect(elem.exists()).toBe(true)
    })
  })
    
  it('should have an empty table when there\'s no minion', () =>{
    const wrapper = mount(MinionsTable, { 
      global: { plugins: [createTestingPinia()] }
    })
      
    const minionItem = wrapper.find('[data-test="minion-item"]')
    expect(minionItem.exists()).toBe(false)
  })
    
  it('should display a list when there\'s minion', () => {
    const wrapper = mount(MinionsTable)
    const minionItem = wrapper.find('[data-test="minion-item"]')
    expect(minionItem.exists()).toBe(true)
  })
})