import { defineStore } from 'pinia'

export const useAppliancesStore = defineStore('appliancesStore', {
  state: () => ({
    minionsTableOpen: true
  }),
  actions: {
    hideMinionsTable() {
      this.minionsTableOpen = false
    },
    showMinionsTable() {
      this.minionsTableOpen = true
    }
  }
})
