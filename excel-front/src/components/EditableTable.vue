<template>
  <div class="editable-table-wrapper">
    <el-table
      :data="pagedRows"
      border
      size="mini"
      style="width: 100%;"
      :row-class-name="rowClassName"
      max-height="600"
      @cell-click="handleCellClick">
      <el-table-column type="index" label="#" width="50" fixed></el-table-column>
      <el-table-column
        v-for="col in columns"
        :key="col.field"
        :prop="'data.' + col.field"
        :label="col.label"
        :min-width="col.width"
        :fixed="col.fixed">
        <template slot-scope="scope">
          <div v-if="isEditing(scope.$index, col.field)" class="cell-edit">
            <el-input
              :ref="'input-' + scope.$index + '-' + col.field"
              v-model="scope.row.data[col.field]"
              size="mini"
              @blur="finishEdit(scope.$index, col.field, scope.row)"
              @keydown.native.tab="handleTab($event, scope.$index, col.field)"
              @keydown.native.enter="handleEnter($event, scope.$index, col.field)">
            </el-input>
          </div>
          <div v-else class="cell-display" :class="getCellClass(scope.row, col.field)">
            <el-tooltip v-if="getCellError(scope.row, col.field)" :content="getCellError(scope.row, col.field)" placement="top">
              <span>{{ scope.row.data[col.field] || '' }}</span>
            </el-tooltip>
            <span v-else>{{ scope.row.data[col.field] || '' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" style="color: #F56C6C;"
                     @click.stop="handleDelete(scope.$index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="rows.length > pageSize" style="padding: 10px; text-align: center;">
      <el-pagination
        layout="prev, pager, next, total"
        :total="rows.length"
        :page-size="pageSize"
        :current-page.sync="currentPage">
      </el-pagination>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EditableTable',
  props: {
    rows: { type: Array, default: () => [] }
  },
  data() {
    return {
      editingCell: null, // { row, field }
      currentPage: 1,
      pageSize: 100,
      columns: [
        { field: 'externalCode', label: '外部编码', width: '120' },
        { field: 'senderName', label: '发件人姓名', width: '100' },
        { field: 'senderPhone', label: '发件人电话', width: '130' },
        { field: 'senderAddress', label: '发件人地址', width: '200' },
        { field: 'receiverName', label: '收件人姓名', width: '100' },
        { field: 'receiverPhone', label: '收件人电话', width: '130' },
        { field: 'receiverAddress', label: '收件人地址', width: '200' },
        { field: 'weight', label: '重量(kg)', width: '90' },
        { field: 'quantity', label: '件数', width: '70' },
        { field: 'tempZone', label: '温层', width: '80' },
        { field: 'remark', label: '备注', width: '120' }
      ]
    }
  },
  computed: {
    pagedRows() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.rows.slice(start, start + this.pageSize)
    }
  },
  methods: {
    isEditing(rowIdx, field) {
      return this.editingCell &&
        this.editingCell.row === rowIdx &&
        this.editingCell.field === field
    },
    handleCellClick(row, column) {
      if (!column.property || column.property === '') return
      const field = column.property.replace('data.', '')
      const rowIdx = this.pagedRows.indexOf(row)
      if (rowIdx < 0) return
      this.editingCell = { row: rowIdx, field }
      this.$nextTick(() => {
        const ref = this.$refs['input-' + rowIdx + '-' + field]
        if (ref) {
          const input = Array.isArray(ref) ? ref[0] : ref
          if (input && input.focus) input.focus()
        }
      })
    },
    finishEdit(rowIdx, field, row) {
      this.editingCell = null
      this.revalidateRow(rowIdx, row)
    },
    handleTab(event, rowIdx, field) {
      event.preventDefault()
      const colIdx = this.columns.findIndex(c => c.field === field)
      let nextCol = colIdx + 1
      let nextRow = rowIdx
      if (nextCol >= this.columns.length) {
        nextCol = 0
        nextRow = rowIdx + 1
      }
      if (nextRow < this.pagedRows.length) {
        this.editingCell = { row: nextRow, field: this.columns[nextCol].field }
        this.$nextTick(() => {
          const ref = this.$refs['input-' + nextRow + '-' + this.columns[nextCol].field]
          if (ref) {
            const input = Array.isArray(ref) ? ref[0] : ref
            if (input && input.focus) input.focus()
          }
        })
      }
    },
    handleEnter(event, rowIdx, field) {
      event.preventDefault()
      const nextRow = rowIdx + 1
      if (nextRow < this.pagedRows.length) {
        this.editingCell = { row: nextRow, field }
        this.$nextTick(() => {
          const ref = this.$refs['input-' + nextRow + '-' + field]
          if (ref) {
            const input = Array.isArray(ref) ? ref[0] : ref
            if (input && input.focus) input.focus()
          }
        })
      } else {
        this.editingCell = null
      }
    },
    handleDelete(pageIdx) {
      const globalIdx = (this.currentPage - 1) * this.pageSize + pageIdx
      this.$emit('delete', globalIdx)
    },
    revalidateRow(pageIdx, row) {
      const globalIdx = (this.currentPage - 1) * this.pageSize + pageIdx
      const errors = this.validateRow(row.data)
      row.errors = errors
      this.$emit('update', globalIdx, row)
    },
    validateRow(data) {
      const errors = []
      const phoneRegex = /^1[3-9]\d{9}$/
      const landlineRegex = /^0\d{2,3}-?\d{7,8}$/

      if (!data.senderName || !data.senderName.trim()) {
        errors.push({ field: 'senderName', fieldLabel: '发件人姓名', message: '不能为空' })
      }
      if (!data.senderPhone || !data.senderPhone.trim()) {
        errors.push({ field: 'senderPhone', fieldLabel: '发件人电话', message: '不能为空' })
      } else if (!phoneRegex.test(data.senderPhone.trim()) && !landlineRegex.test(data.senderPhone.trim())) {
        errors.push({ field: 'senderPhone', fieldLabel: '发件人电话', message: '格式错误' })
      }
      if (!data.senderAddress || !data.senderAddress.trim()) {
        errors.push({ field: 'senderAddress', fieldLabel: '发件人地址', message: '不能为空' })
      }
      if (!data.receiverName || !data.receiverName.trim()) {
        errors.push({ field: 'receiverName', fieldLabel: '收件人姓名', message: '不能为空' })
      }
      if (!data.receiverPhone || !data.receiverPhone.trim()) {
        errors.push({ field: 'receiverPhone', fieldLabel: '收件人电话', message: '不能为空' })
      } else if (!phoneRegex.test(data.receiverPhone.trim()) && !landlineRegex.test(data.receiverPhone.trim())) {
        errors.push({ field: 'receiverPhone', fieldLabel: '收件人电话', message: '格式错误' })
      }
      if (!data.receiverAddress || !data.receiverAddress.trim()) {
        errors.push({ field: 'receiverAddress', fieldLabel: '收件人地址', message: '不能为空' })
      }

      if (!data.weight || !data.weight.toString().trim()) {
        errors.push({ field: 'weight', fieldLabel: '重量(kg)', message: '不能为空' })
      } else {
        const w = parseFloat(data.weight)
        if (isNaN(w) || w <= 0) {
          errors.push({ field: 'weight', fieldLabel: '重量(kg)', message: '必须为正数' })
        }
      }

      if (!data.quantity || !data.quantity.toString().trim()) {
        errors.push({ field: 'quantity', fieldLabel: '件数', message: '不能为空' })
      } else {
        const q = parseInt(data.quantity)
        if (isNaN(q) || q <= 0 || q !== parseFloat(data.quantity)) {
          errors.push({ field: 'quantity', fieldLabel: '件数', message: '必须为正整数' })
        }
      }

      const validZones = ['常温', '冷藏', '冷冻']
      if (!data.tempZone || !data.tempZone.trim()) {
        errors.push({ field: 'tempZone', fieldLabel: '温层', message: '不能为空' })
      } else if (!validZones.includes(data.tempZone.trim())) {
        errors.push({ field: 'tempZone', fieldLabel: '温层', message: '必须为: 常温/冷藏/冷冻' })
      }

      return errors
    },
    getCellClass(row, field) {
      if (row.errors && row.errors.some(e => e.field === field)) {
        return 'cell-error'
      }
      return ''
    },
    getCellError(row, field) {
      if (!row.errors) return null
      const err = row.errors.find(e => e.field === field)
      return err ? `${err.fieldLabel}: ${err.message}` : null
    },
    rowClassName({ row }) {
      if (row.errors && row.errors.length > 0) return 'error-row'
      return ''
    }
  }
}
</script>

<style scoped>
.editable-table-wrapper {
  overflow: hidden;
}
.cell-display {
  cursor: pointer;
  min-height: 23px;
  padding: 2px 0;
}
.cell-error {
  background-color: #fef0f0 !important;
  border: 1px solid #F56C6C;
  border-radius: 2px;
  color: #F56C6C;
}
.cell-edit {
  margin: -5px;
}
</style>

<style>
.el-table .error-row {
  background-color: #fef0f0 !important;
}
</style>
