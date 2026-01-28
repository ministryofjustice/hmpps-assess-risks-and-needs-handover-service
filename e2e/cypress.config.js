const { defineConfig } = require('cypress')

module.exports = defineConfig({
  chromeWebSecurity: false,
  e2e: {
    baseUrl: 'http://aap-ui:3000',
    supportFile: false,
  },
})
