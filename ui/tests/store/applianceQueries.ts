import { createTestingPinia } from '@pinia/testing'
import { setActivePinia } from 'pinia'
import { createClient, setActiveClient } from 'villus'
import { ComputedRef } from 'vue'

const setActive = (tableDevices?: ComputedRef<any[]>, tableMinions?: ComputedRef<any[]>) => {
  setActivePinia(createTestingPinia({
    initialState: { 
      applianceQueries: { 
        tableDevices,
        tableMinions
      }
    }
  }))

  setActiveClient(createClient({
    url: 'http://test/graphql'
  }))
}

export default setActive