import { Node } from 'v-network-graph'
import { useTopologyStore } from '@/store/Views/topologyStore'

export const useTopologyFocus = () => {
  const topologyStore = useTopologyStore()

  // add single object to focus
  const addFocusObject = async (obj: Node) => {
    topologyStore.addFocusObject(obj)
  }

  // add multiple objects to focus
  const addFocusObjects = async (objects: Node[]) => {
    for (const obj of objects) {
      topologyStore.addFocusObject(obj)
    }
  }

  // replace focus with these objects
  const replaceFocusObjects = async (objects: Node[]) => {
    topologyStore.replaceFocusObjects(objects)
  }

  // remove focused objects by id
  const removeFocusObjectsByIds = async (ids: string[]) => {
    for (const id of ids) {
      topologyStore.removeFocusObject(id)
    }
  }

  // sets the default focused node
  const useDefaultFocus = () => {
    topologyStore.useDefaultFocus()
  }

  return { addFocusObject, addFocusObjects, replaceFocusObjects, removeFocusObjectsByIds, useDefaultFocus }
}
