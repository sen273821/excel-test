import axios from 'axios'

const BASE_URL = process.env.VUE_APP_API_URL || ''

const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30000
})

export function importExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post('/api/excel/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getExcelList(page, size) {
  return apiClient.get('/api/excel/list', {
    params: { page, size }
  })
}

export function exportExcel() {
  return apiClient.get('/api/excel/export', {
    responseType: 'blob'
  })
}

export function downloadTemplate() {
  return apiClient.get('/api/excel/template', {
    responseType: 'blob'
  })
}
