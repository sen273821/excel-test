<template>
  <div class="preview-view">
    <el-card v-if="rows.length === 0">
      <el-empty description="暂无数据，请先导入 Excel 文件">
        <el-button type="primary" @click="$router.push('/import')">去导入</el-button>
      </el-empty>
    </el-card>

    <template v-else>
      <!-- Toolbar -->
      <el-card style="margin-bottom: 15px;">
        <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px;">
          <div>
            <span style="font-size: 14px; color: #606266;">
              共 {{ rows.length }} 条数据，
              <span style="color: #F56C6C;">{{ errorCount }} 条有错误</span>
            </span>
          </div>
          <div>
            <el-button size="small" icon="el-icon-plus" @click="addRow">新增行</el-button>
            <el-button size="small" icon="el-icon-download" @click="exportExcel">导出 Excel</el-button>
            <el-button type="primary" size="small" icon="el-icon-check"
                       @click="handleSubmit" :disabled="errorCount > 0 || submitting"
                       :loading="submitting">
              提交下单
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- Error Summary Panel -->
      <validation-panel v-if="allErrors.length > 0" :errors="allErrors" style="margin-bottom: 15px;" />

      <!-- Submit Progress -->
      <el-card v-if="submitting" style="margin-bottom: 15px;">
        <el-progress :percentage="submitProgress" :format="submitProgressFormat"></el-progress>
        <p style="color: #909399; font-size: 13px; margin-top: 5px;">{{ submitText }}</p>
      </el-card>

      <!-- Editable Table -->
      <el-card body-style="padding: 0;">
        <editable-table :rows="rows" @update="handleRowUpdate" @delete="handleRowDelete" />
      </el-card>
    </template>
  </div>
</template>

<script>
import { store, mutations } from '@/store/importStore'
import { submitOrders, checkDuplicate, saveTemplateMapping } from '@/api/order'
import EditableTable from '@/components/EditableTable.vue'
import ValidationPanel from '@/components/ValidationPanel.vue'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

export default {
  name: 'PreviewView',
  components: { EditableTable, ValidationPanel },
  data() {
    return {
      submitting: false,
      submitProgress: 0,
      submitText: ''
    }
  },
  computed: {
    rows() { return store.rows },
    errorCount() {
      return this.rows.filter(r => r.errors && r.errors.length > 0).length
    },
    allErrors() {
      const errors = []
      this.rows.forEach(row => {
        if (row.errors) {
          row.errors.forEach(err => {
            errors.push({
              row: row.rowIndex,
              field: err.field,
              fieldLabel: err.fieldLabel,
              message: err.message
            })
          })
        }
      })
      return errors
    }
  },
  methods: {
    addRow() {
      mutations.addEmptyRow()
    },
    handleRowUpdate(index, rowData) {
      mutations.updateRow(index, rowData)
    },
    handleRowDelete(index) {
      mutations.deleteRow(index)
    },
    exportExcel() {
      const data = this.rows.map(r => ({
        '外部编码': r.data.externalCode || '',
        '发件人姓名': r.data.senderName || '',
        '发件人电话': r.data.senderPhone || '',
        '发件人地址': r.data.senderAddress || '',
        '收件人姓名': r.data.receiverName || '',
        '收件人电话': r.data.receiverPhone || '',
        '收件人地址': r.data.receiverAddress || '',
        '重量(kg)': r.data.weight || '',
        '件数': r.data.quantity || '',
        '温层': r.data.tempZone || '',
        '备注': r.data.remark || ''
      }))
      const ws = XLSX.utils.json_to_sheet(data)
      const wb = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(wb, ws, '订单数据')
      const buf = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
      const blob = new Blob([buf], { type: 'application/octet-stream' })
      saveAs(blob, '订单数据_导出.xlsx')
      this.$message.success('导出成功')
    },
    async handleSubmit() {
      if (this.errorCount > 0) {
        this.$message.error('存在错误数据，请先修正后再提交')
        return
      }

      // Check DB duplicates
      const codes = this.rows
        .map(r => r.data.externalCode)
        .filter(c => c && c.trim())
      if (codes.length > 0) {
        try {
          const res = await checkDuplicate(codes)
          if (res.data.code === 200 && res.data.data && res.data.data.length > 0) {
            const dups = res.data.data
            this.$confirm(
              `以下外部编码在数据库中已存在: ${dups.join(', ')}，是否继续提交？`,
              '重复提醒', { type: 'warning' }
            ).catch(() => { return })
          }
        } catch (e) {
          // Non-critical
        }
      }

      this.submitting = true
      this.submitProgress = 0
      this.submitText = '正在提交...'

      const batchId = Date.now().toString(36) + Math.random().toString(36).substr(2, 4)
      const orders = this.rows.map(r => r.data)
      const batchSize = 100
      let submitted = 0
      let totalSuccess = 0
      let totalFailed = 0

      try {
        for (let i = 0; i < orders.length; i += batchSize) {
          const batch = orders.slice(i, i + batchSize)
          const res = await submitOrders({
            batchId,
            orders: batch,
            fingerprint: store.fingerprint,
            columnMapping: store.mapping
          })
          if (res.data.code === 200) {
            totalSuccess += res.data.data.success
            totalFailed += res.data.data.failed
          }
          submitted += batch.length
          this.submitProgress = Math.round((submitted / orders.length) * 100)
          this.submitText = `已提交 ${submitted}/${orders.length} 条`
        }

        this.submitProgress = 100
        this.submitText = '提交完成'
        this.$message.success(`提交完成：成功 ${totalSuccess} 条，失败 ${totalFailed} 条`)

        // Save mapping
        if (store.fingerprint && store.mapping) {
          try {
            await saveTemplateMapping({
              fingerprint: store.fingerprint,
              columnMapping: store.mapping,
              headerRowIndex: store.headerRowIndex,
              sheetName: store.sheetName
            })
          } catch (e) { /* ignore */ }
        }

        mutations.clear()
        this.$router.push('/history')
      } catch (err) {
        this.$message.error('提交失败: ' + (err.response?.data?.message || err.message))
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.preview-view {
  max-width: 1400px;
  margin: 0 auto;
}
</style>
