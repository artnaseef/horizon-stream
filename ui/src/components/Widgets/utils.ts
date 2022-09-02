import { AppContext, createVNode, render } from "vue"

const addWidget = (
  appContext: AppContext,
  widget: string,
  element: HTMLElement,
  props: {[x: string] : string} = {}, 
  styles?: { height: string }
  ) => {
  
  // async import a component from a given path
  const component = defineAsyncComponent(() => import( /* @vite-ignore */ widget))
  const div = document.createElement('div')
  div.id = widget

  if (styles) {
    div.style.height = styles.height
  }

  const vNode = createVNode(component, props) // create a vue specific node for that component
  vNode.appContext = appContext // set that node's component context (needed for Pinia)
  render(vNode, div) // renders the component inside the created container div
  element.appendChild(div) // appends the div to th dom
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
}

export {
  addWidget,
  removeWidget
}
