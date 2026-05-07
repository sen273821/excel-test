<template>
  <div id="app">
    <excel-view />
  </div>
</template>

<script>
import ExcelView from './views/ExcelView.vue'

export default {
  name: 'App',
  components: { ExcelView }
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
  background-color: #f5f7fa;
}
#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}
</style>
