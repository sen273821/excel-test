import axios from 'axios'

const BASE_URL = process.env.VUE_APP_API_URL || ''

const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 60000
})

export function parseExcel(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post('/api/order/parse', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}

export function parseExcelWithMapping(file, mapping, headerRowIndex, sheetName) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('mapping', JSON.stringify(mapping))
  if (headerRowIndex !== null && headerRowIndex !== undefined) {
    formData.append('headerRowIndex', headerRowIndex)
  }
  if (sheetName) {
    formData.append('sheetName', sheetName)
  }
  return apiClient.post('/api/order/parse-with-mapping', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function submitOrders(data) {
  return apiClient.post('/api/order/submit', data)
}

export function getOrderList(params) {
  return apiClient.get('/api/order/list', { params })
}

export function checkDuplicate(codes) {
  return apiClient.get('/api/order/check-duplicate', {
    params: { codes: codes.join(',') }
  })
}

export function saveTemplateMapping(data) {
  return apiClient.post('/api/order/mapping/save', data)
}
