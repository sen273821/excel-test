<template>
  <el-dialog title="手动列映射" :visible.sync="dialogVisible" width="700px" @close="handleClose">
    <p style="color: #909399; margin-bottom: 15px;">
      请为每个 Excel 列选择对应的系统字段
    </p>
    <el-table :data="mappingRows" border size="small" max-height="400">
      <el-table-column prop="excelHeader" label="Excel 列名" width="200"></el-table-column>
      <el-table-column label="映射到系统字段">
        <template slot-scope="scope">
          <el-select v-model="scope.row.mappedField" placeholder="请选择" clearable size="small" style="width: 100%;">
            <el-option
              v-for="field in systemFields"
              :key="field.value"
              :label="field.label"
              :value="field.value"
              :disabled="isFieldUsed(field.value, scope.row.excelHeader)">
            </el-option>
          </el-select>
        </template>
      </el-table-column>
    </el-table>

    <el-checkbox v-model="saveRule" style="margin-top: 15px;">
      记住此映射规则（下次自动识别相同模板）
    </el-checkbox>

    <div slot="footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :disabled="!hasRequiredMappings">确认映射</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'MappingDialog',
  props: {
    visible: { type: Boolean, default: false },
    headers: { type: Array, default: () => [] },
    currentMapping: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      mappingRows: [],
      saveRule: true,
      systemFields: [
        { value: 'externalCode', label: '外部编码' },
        { value: 'senderName', label: '发件人姓名' },
        { value: 'senderPhone', label: '发件人电话' },
        { value: 'senderAddress', label: '发件人地址' },
        { value: 'receiverName', label: '收件人姓名' },
        { value: 'receiverPhone', label: '收件人电话' },
        { value: 'receiverAddress', label: '收件人地址' },
        { value: 'weight', label: '重量(kg)' },
        { value: 'quantity', label: '件数' },
        { value: 'tempZone', label: '温层' },
        { value: 'remark', label: '备注' }
      ]
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { this.$emit('update:visible', val) }
    },
    hasRequiredMappings() {
      const mapped = this.mappingRows.filter(r => r.mappedField).map(r => r.mappedField)
      const required = ['senderName', 'senderPhone', 'senderAddress',
                        'receiverName', 'receiverPhone', 'receiverAddress',
                        'weight', 'quantity', 'tempZone']
      return required.filter(f => mapped.includes(f)).length >= 5
    }
  },
  watch: {
    visible(val) {
      if (val) this.initMappingRows()
    }
  },
  methods: {
    initMappingRows() {
      this.mappingRows = this.headers
        .filter(h => h && h.trim())
        .map(h => ({
          excelHeader: h,
          mappedField: this.currentMapping[h] || ''
        }))
    },
    isFieldUsed(fieldValue, currentHeader) {
      return this.mappingRows.some(r =>
        r.mappedField === fieldValue && r.excelHeader !== currentHeader
      )
    },
    handleConfirm() {
      const mapping = {}
      this.mappingRows.forEach(row => {
        if (row.mappedField) {
          mapping[row.excelHeader] = row.mappedField
        }
      })
      this.$emit('confirm', mapping, this.saveRule)
    },
    handleClose() {
      this.dialogVisible = false
    }
  }
}
</script>
