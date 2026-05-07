<template>
  <div class="excel-view">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-button
          type="warning"
          icon="el-icon-document"
          @click="handleDownloadTemplate"
        >
          下载模板
        </el-button>
        <excel-upload @upload-success="handleUploadSuccess" />
        <excel-export />
      </div>
    </el-card>
    <el-card class="table-card">
      <excel-table ref="excelTable" />
    </el-card>
  </div>
</template>

<script>
import ExcelUpload from '@/components/ExcelUpload.vue'
import ExcelTable from '@/components/ExcelTable.vue'
import ExcelExport from '@/components/ExcelExport.vue'
import { downloadTemplate } from '@/api/excel'

export default {
  name: 'ExcelView',
  components: { ExcelUpload, ExcelTable, ExcelExport },
  methods: {
    handleUploadSuccess() {
      this.$refs.excelTable.fetchData()
    },
    async handleDownloadTemplate() {
      try {
        const res = await downloadTemplate()
        this.saveBlob(res.data, 'import_template.xlsx')
      } catch (error) {
        this.$message.error('下载模板失败')
      }
    },
    saveBlob(data, filename) {
      const blob = new Blob([data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.click()
      window.URL.revokeObjectURL(url)
    }
  }
}
</script>

<style scoped>
.excel-view {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}
.toolbar-card {
  margin-bottom: 20px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
