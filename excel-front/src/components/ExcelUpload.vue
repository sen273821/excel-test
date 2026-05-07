<template>
  <el-upload
    action=""
    :http-request="handleUpload"
    :show-file-list="false"
    accept=".xlsx,.xls"
  >
    <el-button type="primary" icon="el-icon-upload2">
      导入 Excel
    </el-button>
  </el-upload>
</template>

<script>
import { importExcel } from '@/api/excel'

export default {
  name: 'ExcelUpload',
  methods: {
    async handleUpload(params) {
      try {
        await importExcel(params.file)
        this.$message.success('导入成功')
        this.$emit('upload-success')
      } catch (error) {
        this.$message.error('导入失败：' + error.message)
      }
    }
  }
}
</script>
