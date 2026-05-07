<template>
  <el-button
    type="success"
    icon="el-icon-download"
    :loading="exporting"
    @click="handleExport"
  >
    导出 Excel
  </el-button>
</template>

<script>
import { exportExcel } from '@/api/excel'

export default {
  name: 'ExcelExport',
  data() {
    return { exporting: false }
  },
  methods: {
    async handleExport() {
      this.exporting = true
      try {
        const res = await exportExcel()
        this.downloadFile(res.data)
        this.$message.success('导出成功')
      } catch (error) {
        this.$message.error('导出失败')
      } finally {
        this.exporting = false
      }
    },
    downloadFile(data) {
      const blob = new Blob([data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'exported_data.xlsx'
      link.click()
      window.URL.revokeObjectURL(url)
    }
  }
}
</script>
