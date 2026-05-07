<template>
  <div class="history-view">
    <el-card>
      <div slot="header"><span>已导入运单列表</span></div>

      <!-- Filters -->
      <el-form :inline="true" size="small" style="margin-bottom: 15px;">
        <el-form-item label="外部编码">
          <el-input v-model="filters.externalCode" placeholder="搜索外部编码" clearable
                    @keyup.enter.native="search"></el-input>
        </el-form-item>
        <el-form-item label="收件人姓名">
          <el-input v-model="filters.receiverName" placeholder="搜索收件人" clearable
                    @keyup.enter.native="search"></el-input>
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker v-model="filters.dateRange" type="daterange"
                          range-separator="至" start-placeholder="开始日期"
                          end-placeholder="结束日期" value-format="yyyy-MM-dd HH:mm:ss">
          </el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- Table -->
      <el-table :data="orders" border size="small" v-loading="loading" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="60"></el-table-column>
        <el-table-column prop="externalCode" label="外部编码" width="120"></el-table-column>
        <el-table-column prop="senderName" label="发件人" width="90"></el-table-column>
        <el-table-column prop="senderPhone" label="发件电话" width="120"></el-table-column>
        <el-table-column prop="senderAddress" label="发件地址" min-width="150" show-overflow-tooltip></el-table-column>
        <el-table-column prop="receiverName" label="收件人" width="90"></el-table-column>
        <el-table-column prop="receiverPhone" label="收件电话" width="120"></el-table-column>
        <el-table-column prop="receiverAddress" label="收件地址" min-width="150" show-overflow-tooltip></el-table-column>
        <el-table-column prop="weight" label="重量(kg)" width="80"></el-table-column>
        <el-table-column prop="quantity" label="件数" width="60"></el-table-column>
        <el-table-column prop="tempZone" label="温层" width="70"></el-table-column>
        <el-table-column prop="remark" label="备注" width="100" show-overflow-tooltip></el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160">
          <template slot-scope="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div style="margin-top: 15px; text-align: right;">
        <el-pagination
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size.sync="pageSize"
          :current-page.sync="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          @current-change="fetchOrders"
          @size-change="handleSizeChange">
        </el-pagination>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getOrderList } from '@/api/order'

export default {
  name: 'HistoryView',
  data() {
    return {
      orders: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      loading: false,
      filters: {
        externalCode: '',
        receiverName: '',
        dateRange: null
      }
    }
  },
  mounted() {
    this.fetchOrders()
  },
  methods: {
    async fetchOrders() {
      this.loading = true
      try {
        const params = {
          page: this.currentPage - 1,
          size: this.pageSize
        }
        if (this.filters.externalCode) params.externalCode = this.filters.externalCode
        if (this.filters.receiverName) params.receiverName = this.filters.receiverName
        if (this.filters.dateRange && this.filters.dateRange.length === 2) {
          params.startTime = this.filters.dateRange[0]
          params.endTime = this.filters.dateRange[1]
        }

        const res = await getOrderList(params)
        if (res.data.code === 200) {
          const page = res.data.data
          this.orders = page.content || []
          this.total = page.totalElements || 0
        }
      } catch (err) {
        this.$message.error('查询失败: ' + (err.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },
    search() {
      this.currentPage = 1
      this.fetchOrders()
    },
    resetFilters() {
      this.filters = { externalCode: '', receiverName: '', dateRange: null }
      this.currentPage = 1
      this.fetchOrders()
    },
    handleSizeChange() {
      this.currentPage = 1
      this.fetchOrders()
    },
    formatTime(time) {
      if (!time) return ''
      if (Array.isArray(time)) {
        // Java LocalDateTime serialized as array [year, month, day, hour, min, sec]
        const [y, m, d, h, mi, s] = time
        return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}:${String(s || 0).padStart(2, '0')}`
      }
      return time
    }
  }
}
</script>

<style scoped>
.history-view {
  max-width: 1400px;
  margin: 0 auto;
}
</style>
