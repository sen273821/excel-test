<template>
  <div class="import-view">
    <el-card>
      <div slot="header"><span>上传 Excel 文件</span></div>
      <el-upload
        ref="upload"
        drag
        action=""
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        :on-exceed="handleExceed"
        accept=".xlsx,.xls"
        :file-list="fileList">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">支持 .xlsx / .xls 格式，支持多种模板自动识别</div>
      </el-upload>

      <div v-if="uploading" style="margin-top: 20px;">
        <el-progress :percentage="uploadProgress" :format="progressFormat"></el-progress>
        <p style="color: #909399; font-size: 13px; margin-top: 8px;">
          {{ progressText }}
        </p>
      </div>

      <div v-if="parseError" style="margin-top: 20px;">
        <el-alert :title="parseError" type="error" show-icon :closable="false"></el-alert>
      </div>

      <div v-if="recognized === false && headers.length > 0" style="margin-top: 20px;">
        <el-alert title="自动识别未完成，请手动映射列关系" type="warning" show-icon :closable="false"></el-alert>
        <el-button type="primary" style="margin-top: 10px;" @click="showMappingDialog = true">
          手动映射
        </el-button>
      </div>
    </el-card>

    <!-- Mapping Dialog -->
    <mapping-dialog
      :visible.sync="showMappingDialog"
      :headers="headers"
      :current-mapping="currentMapping"
      @confirm="handleMappingConfirm"
    />
  </div>
</template>

<script>
import { parseExcel, parseExcelWithMapping, saveTemplateMapping } from '@/api/order'
import { store, mutations } from '@/store/importStore'
import MappingDialog from '@/components/MappingDialog.vue'

export default {
  name: 'ImportView',
  components: { MappingDialog },
  data() {
    return {
      fileList: [],
      currentFile: null,
      uploading: false,
      uploadProgress: 0,
      progressText: '',
      parseError: null,
      recognized: null,
      headers: [],
      currentMapping: {},
      showMappingDialog: false
    }
  },
  methods: {
    progressFormat(percentage) {
      return percentage === 100 ? '解析完成' : `${percentage}%`
    },
    handleExceed() {
      this.$message.warning('只能上传一个文件，请先移除已选文件')
    },
    handleFileChange(file) {
      this.currentFile = file.raw
      this.parseError = null
      this.recognized = null
      this.headers = []
      this.startParse()
    },
    async startParse() {
      if (!this.currentFile) return

      this.uploading = true
      this.uploadProgress = 0
      this.progressText = '正在上传文件...'

      try {
        const res = await parseExcel(this.currentFile, (e) => {
          if (e.total > 0) {
            this.uploadProgress = Math.round((e.loaded / e.total) * 50)
          }
        })

        this.uploadProgress = 70
        this.progressText = '正在解析数据...'

        const data = res.data
        if (data.code !== 200) {
          this.parseError = data.message || '解析失败'
          this.uploading = false
          return
        }

        const result = data.data
        this.uploadProgress = 90
        this.progressText = `已解析 ${result.totalRows} 条数据`

        this.recognized = result.recognized
        this.headers = result.headers || []
        this.currentMapping = result.columnMapping || {}

        if (result.recognized) {
          // Auto-recognized, go to preview
          this.uploadProgress = 100
          this.progressText = `解析完成，共 ${result.totalRows} 条数据`
          mutations.setParseResult(result)

          setTimeout(() => {
            this.$router.push('/preview')
          }, 500)
        } else {
          // Need manual mapping
          this.uploadProgress = 100
          this.progressText = '需要手动映射列关系'
          this.showMappingDialog = true
        }
      } catch (err) {
        this.parseError = err.response?.data?.message || err.message || '网络错误'
      } finally {
        this.uploading = false
      }
    },
    async handleMappingConfirm(mapping, saveMapping) {
      this.showMappingDialog = false
      this.uploading = true
      this.uploadProgress = 50
      this.progressText = '正在使用自定义映射重新解析...'

      try {
        const res = await parseExcelWithMapping(
          this.currentFile, mapping, null, null
        )

        const data = res.data
        if (data.code !== 200) {
          this.parseError = data.message || '解析失败'
          this.uploading = false
          return
        }

        const result = data.data
        this.uploadProgress = 100
        this.progressText = `解析完成，共 ${result.totalRows} 条数据`

        // Save mapping if requested
        if (saveMapping && result.fingerprint) {
          try {
            await saveTemplateMapping({
              fingerprint: result.fingerprint,
              columnMapping: mapping,
              headerRowIndex: result.headerRowIndex,
              sheetName: result.sheetName
            })
            this.$message.success('映射规则已保存，下次将自动识别')
          } catch (e) {
            // Non-critical, just notify
            console.warn('保存映射失败', e)
          }
        }

        mutations.setParseResult(result)
        setTimeout(() => {
          this.$router.push('/preview')
        }, 500)
      } catch (err) {
        this.parseError = err.response?.data?.message || err.message || '网络错误'
      } finally {
        this.uploading = false
      }
    }
  }
}
</script>

<style scoped>
.import-view {
  max-width: 800px;
  margin: 0 auto;
}
</style>
