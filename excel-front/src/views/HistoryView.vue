<template>
  <div class="history-view">
    <!-- Statistics Cards -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :span="6">
        <div class="stat-card stat-total">
          <div class="stat-icon"><i class="el-icon-document"></i></div>
          <div class="stat-info">
            <div class="stat-value">{{ total }}</div>
            <div class="stat-label">总运单数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-today">
          <div class="stat-icon"><i class="el-icon-date"></i></div>
          <div class="stat-info">
            <div class="stat-value">{{ todayCount }}</div>
            <div class="stat-label">今日导入</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-cold">
          <div class="stat-icon"><i class="el-icon-cold-drink"></i></div>
          <div class="stat-info">
            <div class="stat-value">{{ coldCount }}</div>
            <div class="stat-label">冷链运单</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-normal">
          <div class="stat-icon"><i class="el-icon-sunny"></i></div>
          <div class="stat-info">
            <div class="stat-value">{{ normalCount }}</div>
            <div class="stat-label">常温运单</div>
          </div>
        </div>
      </el-col>
    </el-row>

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
      <el-table :data="orders" border size="small" v-loading="loading" style="width: 100%;"
                :header-cell-style="{background:'#fafafa',fontWeight:'600'}">
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
        <el-table-column prop="tempZone" label="温层" width="70">
          <template slot-scope="scope">
            <el-tag :type="getTempTagType(scope.row.tempZone)" size="mini">
              {{ scope.row.tempZone }}
            </el-tag>
          </template>
        </el-table-column>
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
      todayCount: 0,
      coldCount: 0,
      normalCount: 0,
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
    this.fetchStats()
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
    async fetchStats() {
      try {
        // Get all orders for stats (first page with large size)
        const res = await getOrderList({ page: 0, size: 9999 })
        if (res.data.code === 200) {
          const allOrders = res.data.data.content || []
          const today = new Date().toISOString().slice(0, 10)
          this.todayCount = allOrders.filter(o => {
            const t = o.createTime || ''
            return t.startsWith(today)
          }).length
          this.coldCount = allOrders.filter(o =>
            o.tempZone === '冷藏' || o.tempZone === '冷冻'
          ).length
          this.normalCount = allOrders.filter(o => o.tempZone === '常温').length
        }
      } catch (e) {
        // Non-critical
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
    getTempTagType(zone) {
      if (zone === '冷冻') return 'primary'
      if (zone === '冷藏') return 'success'
      return 'warning'
    },
    formatTime(time) {
      if (!time) return ''
      if (Array.isArray(time)) {
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
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
.stat-total .stat-icon { background: #e6f7ff; color: #1890ff; }
.stat-today .stat-icon { background: #f6ffed; color: #52c41a; }
.stat-cold .stat-icon { background: #e6fffb; color: #13c2c2; }
.stat-normal .stat-icon { background: #fff7e6; color: #fa8c16; }
.stat-info {
  flex: 1;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #262626;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 4px;
}
</style>
