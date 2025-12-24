import { createI18n } from 'vue-i18n'
import en from '@/locales/en.json'
import tr from '@/locales/tr.json'

export type MessageSchema = typeof en
export type Locale = 'en' | 'tr'

const savedLocale = (localStorage.getItem('locale') as Locale) || 'en'

export const i18n = createI18n<[MessageSchema], Locale>({
  legacy: false,
  locale: savedLocale,
  fallbackLocale: 'en',
  messages: {
    en,
    tr
  }
})

export function setLocale(locale: Locale) {
  ;(i18n.global.locale as unknown as { value: Locale }).value = locale
  localStorage.setItem('locale', locale)
  document.documentElement.lang = locale
}

export function getLocale(): Locale {
  return (i18n.global.locale as unknown as { value: Locale }).value
}
