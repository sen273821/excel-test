<template>
  <div id="app">
    <el-container>
      <el-header style="background: #409EFF; padding: 0 20px; line-height: 60px;">
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <h2 style="color: #fff; margin: 0; font-size: 18px;">万能导入 - 多模板自动导入下单系统</h2>
          <el-menu mode="horizontal" :router="true" :default-active="$route.path"
                   background-color="#409EFF" text-color="#fff" active-text-color="#ffd04b">
            <el-menu-item index="/import">文件导入</el-menu-item>
            <el-menu-item index="/preview">数据预览</el-menu-item>
            <el-menu-item index="/history">运单列表</el-menu-item>
          </el-menu>
        </div>
      </el-header>
      <el-main style="padding: 20px;">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App'
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
.el-header {
  height: 60px !important;
}
.el-menu--horizontal > .el-menu-item {
  height: 60px;
  line-height: 60px;
}
</style>
