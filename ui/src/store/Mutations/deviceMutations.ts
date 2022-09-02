import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import useSpinner from '@/composables/useSpinner'

import { AddDeviceDocument } from '@/types/graphql'

const { startSpinner, stopSpinner } = useSpinner()

export const useDeviceMutations = defineStore('deviceMutations', () => {
  const {
    execute: addDevice,
    isFetching,
    error
  } = useMutation(AddDeviceDocument)

  watchEffect(() => {
    if (isFetching.value) {
      startSpinner()
    } else {
      stopSpinner()
    }
  })

  return {
    addDevice,
    error
  }
})
