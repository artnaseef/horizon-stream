import { AppContext, createVNode, render } from "vue"

enum Widgets {
  DEVICES = 'DeviceWidget',
  MINIONS = 'MinionWidget',
  GEOMAP = 'MapWidget',
  TAGS = 'TagWidget'
}

type Widget = typeof Widgets[keyof typeof Widgets]

const widgets = ref<Widget[]>([])
const displayedWidgets = ref<Widget[]>([])

const useWidgets = () => {

  const addWidget = async (
    appContext: AppContext,
    widget: string,
    element: HTMLElement,
    props: {[x: string] : string} = {}, 
    styles?: { height: string }
    ) => {
    
    // async import a component from a given path
    const component = defineAsyncComponent(() => import(`../components/Widgets/${widget}.vue`))
    const div = document.createElement('div')
    div.id = widget
  
    if (styles) {
      div.style.height = styles.height
    }
  
    const vNode = createVNode(component, props) // create a vue specific node for that component
    vNode.appContext = appContext // set that node's component context (needed for Pinia)
    render(vNode, div) // renders the component inside the created container div
    element.appendChild(div) // appends the div to th dom

    displayedWidgets.value.push(widget as Widget) // add to displayed widgets list
  }
  
  const removeWidget = (widget: HTMLElement | string) => {
    let div: HTMLElement
  
    if (typeof widget === 'string') {
      div = document.getElementById(widget) as HTMLDivElement
    } else {
      div = widget
    }
  
    render(null, div) // unmounts
    div.remove()
    
    displayedWidgets.value = displayedWidgets.value.filter((x) => x !== widget) // rm from displayed widgets list
  }

  const setAvailableWidgets = (...routeWidgets: Widget[]) => widgets.value = routeWidgets

  return { setAvailableWidgets, widgets, displayedWidgets, addWidget, removeWidget }
}

export {
  Widgets,
  useWidgets
}
