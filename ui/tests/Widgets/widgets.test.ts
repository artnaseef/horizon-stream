import WidgetBar from '@/components/Layout/Widgetbar.vue'
import { addWidget, removeWidget } from '@/components/Widgets/utils'
import router from '@/router'
import setup from '../setupWrapper'

const wrapper = setup({ 
  component: WidgetBar,
  global: {
    plugins: [router]
  }
})

const widgets = [
  ['Minion', '../Appliances/MinionsTable.vue'],
  ['Device', '../Appliances/DeviceTable.vue'],
  ['Map', '../Map/LeafletMap.vue'],
  ['Tag', '../Tags/Tags.vue']
]

test.each(widgets)('Mounts and unmounts the "%s" widget properly', (_, widget) => {
  const element = wrapper.get('#widget-flex-container').element as HTMLElement

  // add the widget
  addWidget(wrapper.getCurrentComponent().appContext, widget, element)

  // test if it was added properly
  let renderedWidget = wrapper.find(`#${widget}`)
  expect(renderedWidget.exists()).toBe(true)
  
  // remove widget
  removeWidget(renderedWidget.element)

  // test if it was removes
  renderedWidget = wrapper.find(`#${widget}`)
  expect(renderedWidget.exists()).toBe(false)
})
