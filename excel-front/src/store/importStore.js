import Vue from 'vue'

export const store = Vue.observable({
  parseResult: null,
  rows: [],
  mapping: null,
  fingerprint: null,
  headers: [],
  headerRowIndex: null,
  sheetName: null
})

export const mutations = {
  setParseResult(result) {
    store.parseResult = result
    store.rows = result.rows || []
    store.mapping = result.columnMapping || null
    store.fingerprint = result.fingerprint || null
    store.headers = result.headers || []
    store.headerRowIndex = result.headerRowIndex
    store.sheetName = result.sheetName
  },
  updateRow(index, data) {
    Vue.set(store.rows, index, data)
  },
  deleteRow(index) {
    store.rows.splice(index, 1)
    // Re-index
    store.rows.forEach((row, i) => { row.rowIndex = i + 1 })
  },
  addEmptyRow() {
    store.rows.push({
      rowIndex: store.rows.length + 1,
      data: {
        externalCode: '', senderName: '', senderPhone: '', senderAddress: '',
        receiverName: '', receiverPhone: '', receiverAddress: '',
        weight: '', quantity: '', tempZone: '', remark: ''
      },
      errors: []
    })
  },
  clear() {
    store.parseResult = null
    store.rows = []
    store.mapping = null
    store.fingerprint = null
    store.headers = []
  }
}
