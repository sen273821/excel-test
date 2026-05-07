<template>
  <div id="app">
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <img src="/logo.svg" alt="Logo" class="app-logo" />
          <h1 class="app-title">万能导入下单系统</h1>
        </div>
        <el-menu mode="horizontal" :router="true" :default-active="$route.path"
                 class="nav-menu"
                 background-color="transparent" text-color="rgba(255,255,255,0.85)" active-text-color="#fff">
          <el-menu-item index="/import">
            <i class="el-icon-upload2"></i> 文件导入
          </el-menu-item>
          <el-menu-item index="/preview">
            <i class="el-icon-document"></i> 数据预览
          </el-menu-item>
          <el-menu-item index="/history">
            <i class="el-icon-tickets"></i> 运单列表
          </el-menu-item>
        </el-menu>
      </el-header>
      <el-main class="app-main">
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
* {
  box-sizing: border-box;
}
body {
  margin: 0;
  padding: 0;
  background-color: #f0f2f5;
}
#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  min-height: 100vh;
}
.app-header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  padding: 0 24px !important;
  height: 56px !important;
  line-height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.app-logo {
  width: 32px;
  height: 32px;
  border-radius: 6px;
}
.app-title {
  color: #fff;
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.nav-menu {
  border-bottom: none !important;
}
.nav-menu .el-menu-item {
  height: 56px !important;
  line-height: 56px !important;
  font-size: 14px;
  border-bottom: 3px solid transparent !important;
  transition: all 0.3s;
}
.nav-menu .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
}
.nav-menu .el-menu-item.is-active {
  border-bottom-color: #fff !important;
  background-color: rgba(255, 255, 255, 0.15) !important;
}
.app-main {
  padding: 24px !important;
  min-height: calc(100vh - 56px);
}
.el-card {
  border-radius: 8px;
  border: none;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08) !important;
}
.el-card__header {
  padding: 14px 20px;
  font-weight: 600;
  font-size: 15px;
}
.el-button--primary {
  background-color: #1890ff;
  border-color: #1890ff;
}
.el-button--primary:hover {
  background-color: #40a9ff;
  border-color: #40a9ff;
}
</style>
