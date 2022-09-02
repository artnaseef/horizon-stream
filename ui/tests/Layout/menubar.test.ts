import Menubar from '@/components/Layout/Menubar.vue'
import useKeycloak from '@/composables/useKeycloak'
import useTheme from '@/composables/useTheme'
import { KeycloakInstance } from '@dsb-norge/vue-keycloak-js/dist/types'
import { createTestingPinia } from '@pinia/testing'
import { mount } from '@vue/test-utils'

test('The menubar mounts', () => {
  const wrapper = mount(Menubar, { global: { plugins: [createTestingPinia()] }, props: {} })
  expect(wrapper).toBeTruthy()
})

test('The toggle dark btn triggers the composible function, and the ref updates', async () => {
  // mock authenticate to show dark/light mode btn
  const { setKeycloak } = useKeycloak()
  setKeycloak({ authenticated: true } as KeycloakInstance)

  // get theme hook, mock on change theme callback
  const theme = useTheme()
  const unchangedThemeValue = theme.isDark.value
  const mockFn = vi.fn(() => 'this is a mock callback')
  theme.onThemeChange(mockFn)
  
  // get and trigger dark/light mode btn
  const wrapper = mount(Menubar, { global: { plugins: [createTestingPinia()] }, props: {} })
  const toggleDarkBtn = wrapper.get('[data-test="toggle-dark"]')
  await toggleDarkBtn.trigger('click')

  // expect theme callback to have run and isDark value to have changes
  expect(mockFn).toHaveBeenCalledOnce()
  expect(theme.isDark.value).toBe(!unchangedThemeValue)
})

