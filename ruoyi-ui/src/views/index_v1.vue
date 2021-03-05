<template>
  <div class="dashboard-editor-container">

    <panel-group @handleSetLineChartData="handleSetLineChartData" />

    <el-row style="background:#fff;padding:16px 16px 0;margin-bottom:32px;">
      <line-chart :chart-data="lineChartData" />
    </el-row>

    <!--<el-row :gutter="32">
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <raddar-chart />
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <pie-chart />
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <bar-chart />
        </div>
      </el-col>
    </el-row>-->


  </div>
</template>

<script>
import PanelGroup from './dashboard/PanelGroup'
import LineChart from './dashboard/LineChart'
import RaddarChart from './dashboard/RaddarChart'
import PieChart from './dashboard/PieChart'
import BarChart from './dashboard/BarChart'
import { getLineChart } from '@/api/px/customer/statistics'

const lineChartData = {
    visitor: {
    phyData: [0, 0, 0, 0, 0, 0, 0],
    qxxData: [0, 0, 0, 0, 0, 0, 0]
  },
  messages: {
      phyData: [0, 0, 0, 0, 0, 0, 0],
      qxxData: [0, 0, 0, 0, 0, 0, 0]
  },
  article: {
      phyData: [0, 0, 0, 0, 0, 0, 0],
      qxxData: [0, 0, 0, 0, 0, 0, 0]
  },
  picture: {
      phyData: [0, 0, 0, 0, 0, 0, 0],
      qxxData: [0, 0, 0, 0, 0, 0, 0]
  }
};

export default {
  name: 'Index',
  components: {
    PanelGroup,
    LineChart,
    RaddarChart,
    PieChart,
    BarChart
  },
  data() {
    return {
      lineChartData: lineChartData.visitor
    }
  },
    mounted() {
      this.getLineChart();
    },
    methods: {
      /**
       * 获取折线图数据
       * @param type
       */
      getLineChart() {
          getLineChart({}).then(res => {
              console.log(res)
          })
      },
    handleSetLineChartData(type) {
      this.lineChartData = lineChartData[type]
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-editor-container {
  padding: 32px;
  background-color: rgb(240, 242, 245);
  position: relative;
  height: 90vh;
  .chart-wrapper {
    background: #fff;
    padding: 16px 16px 0;
    margin-bottom: 32px;
  }
}

@media (max-width:1024px) {
  .chart-wrapper {
    padding: 8px;
  }
}
</style>
