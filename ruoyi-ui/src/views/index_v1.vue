<template>
  <div class="dashboard-editor-container">

    <panel-group @handleSetLineChartData="handleSetLineChartData" />

    <el-row style="background:#fff;padding:16px 16px 0;margin-bottom:32px;">
      <line-chart :chartData="lineChartOne" />
    </el-row>

    <el-row :gutter="32">
        <el-col :xs="24" :sm="24" :lg="8">
            <div class="chart-wrapper">
                <pie-chart title="文章分类" :data="articlePieData"/>
            </div>
        </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
            <pie-chart title="图片分类" :data="picturePieData"/>
        </div>
      </el-col>
    </el-row>


  </div>
</template>

<script>
import PanelGroup from './dashboard/PanelGroup'
import LineChart from './dashboard/LineChart'
import RaddarChart from './dashboard/RaddarChart'
import PieChart from './dashboard/PieChart'
import BarChart from './dashboard/BarChart'
import { getLineChart } from '@/api/px/customer/statistics'

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
        //总统计
        lineChartData: {
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
        },
        //当前折线图
        lineChartOne: {},
        //文章分类饼形图数据
        articlePieData: [],
        //图片分类饼形图数据
        picturePieData: [],
    }
  },
    mounted() {
      this.getLineChart();
        this.lineChartOne = this.lineChartData.visitor;
    },
    methods: {
      /**
       * 获取折线图数据
       * @param type
       */
      getLineChart() {
          getLineChart({}).then(res => {
              this.lineChartData.visitor.phyData = [
                  res.data.visitorPhy[0].monday,
                  res.data.visitorPhy[0].tuesday,
                  res.data.visitorPhy[0].wednesday,
                  res.data.visitorPhy[0].thursday,
                  res.data.visitorPhy[0].friday,
                  res.data.visitorPhy[0].saturday,
                  res.data.visitorPhy[0].sunday];
              this.lineChartData.visitor.qxxData = [
                  res.data.visitorQxx[0].monday,
                  res.data.visitorQxx[0].tuesday,
                  res.data.visitorQxx[0].wednesday,
                  res.data.visitorQxx[0].thursday,
                  res.data.visitorQxx[0].friday,
                  res.data.visitorQxx[0].saturday,
                  res.data.visitorQxx[0].sunday];
              this.lineChartData.messages.phyData = [
                  res.data.messagesPhy[0].monday,
                  res.data.messagesPhy[0].tuesday,
                  res.data.messagesPhy[0].wednesday,
                  res.data.messagesPhy[0].thursday,
                  res.data.messagesPhy[0].friday,
                  res.data.messagesPhy[0].saturday,
                  res.data.messagesPhy[0].sunday];
              this.lineChartData.messages.qxxData = [
                  res.data.messagesQxx[0].monday,
                  res.data.messagesQxx[0].tuesday,
                  res.data.messagesQxx[0].wednesday,
                  res.data.messagesQxx[0].thursday,
                  res.data.messagesQxx[0].friday,
                  res.data.messagesQxx[0].saturday,
                  res.data.messagesQxx[0].sunday];
              this.lineChartData.article.phyData = [
                  res.data.articlePhy[0].monday,
                  res.data.articlePhy[0].tuesday,
                  res.data.articlePhy[0].wednesday,
                  res.data.articlePhy[0].thursday,
                  res.data.articlePhy[0].friday,
                  res.data.articlePhy[0].saturday,
                  res.data.articlePhy[0].sunday];
              this.lineChartData.article.qxxData = [
                  res.data.articleQxx[0].monday,
                  res.data.articleQxx[0].tuesday,
                  res.data.articleQxx[0].wednesday,
                  res.data.articleQxx[0].thursday,
                  res.data.articleQxx[0].friday,
                  res.data.articleQxx[0].saturday,
                  res.data.articleQxx[0].sunday];
              this.lineChartData.picture.phyData = [
                  res.data.picturePhy[0].monday,
                  res.data.picturePhy[0].tuesday,
                  res.data.picturePhy[0].wednesday,
                  res.data.picturePhy[0].thursday,
                  res.data.picturePhy[0].friday,
                  res.data.picturePhy[0].saturday,
                  res.data.picturePhy[0].sunday];
              this.lineChartData.picture.qxxData = [
                  res.data.pictureQxx[0].monday,
                  res.data.pictureQxx[0].tuesday,
                  res.data.pictureQxx[0].wednesday,
                  res.data.pictureQxx[0].thursday,
                  res.data.pictureQxx[0].friday,
                  res.data.pictureQxx[0].saturday,
                  res.data.pictureQxx[0].sunday];
              this.articlePieData = res.data.articlePieData;
              this.picturePieData = res.data.picturePieData;
          })
      },
    handleSetLineChartData(type) {
      this.lineChartOne = this.lineChartData[type]
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
