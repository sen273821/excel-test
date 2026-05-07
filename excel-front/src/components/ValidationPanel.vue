<template>
  <el-card>
    <div slot="header" style="display: flex; align-items: center; justify-content: space-between;">
      <span style="color: #F56C6C; font-weight: bold;">
        错误汇总（共 {{ errors.length }} 个错误）
      </span>
      <el-button type="text" size="small" @click="collapsed = !collapsed">
        {{ collapsed ? '展开' : '收起' }}
      </el-button>
    </div>
    <div v-show="!collapsed" class="error-list" style="max-height: 200px; overflow-y: auto;">
      <div v-for="(err, idx) in errors" :key="idx" class="error-item">
        <el-tag size="mini" type="danger" style="margin-right: 8px;">第 {{ err.row }} 行</el-tag>
        <span style="color: #606266;">{{ err.fieldLabel }}：</span>
        <span style="color: #F56C6C;">{{ err.message }}</span>
      </div>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'ValidationPanel',
  props: {
    errors: { type: Array, default: () => [] }
  },
  data() {
    return { collapsed: false }
  }
}
</script>

<style scoped>
.error-item {
  padding: 4px 0;
  font-size: 13px;
  border-bottom: 1px solid #f0f0f0;
}
.error-item:last-child {
  border-bottom: none;
}
</style>
