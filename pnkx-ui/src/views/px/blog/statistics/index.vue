<template>
    <div class="dashboard-editor-container">

        <panel-group @handleSetLineChartData="handleSetLineChartData"/>

        <el-row align="middle" class="chart-row" type="flex">
            <el-col :span="8">
                <el-select v-model="statisticsParams.dateDimension"
                           class="statistics-type"
                           placeholder="请选择"
                           @change="changeStatisticsType">
                    <el-option
                        v-for="(item, index) in statisticsTypeList"
                        :key="item.dictValue + index"
                        :label="item.dictLabel"
                        :value="item.dictValue">
                    </el-option>
                </el-select>
                <el-date-picker
                    v-if="statisticsParams.dateDimension==='00'"
                    v-model="statisticsParams.date"
                    placeholder="选择月"
                    type="month"
                    @change="getChartData">
                </el-date-picker>
                <el-date-picker
                    v-if="statisticsParams.dateDimension==='01'"
                    v-model="statisticsParams.date"
                    placeholder="选择年"
                    type="year"
                    @change="getChartData">
                </el-date-picker>
            </el-col>
            <el-col v-if="statisticsParams.dateDimension === '00'" :span="4" class="view-number">
                月浏览量：<span>{{viewNumber}}</span>
            </el-col>
            <el-col v-if="statisticsParams.dateDimension === '01'" :span="4" class="view-number">
                年浏览量：<span>{{viewNumber}}</span>
            </el-col>
        </el-row>
        <el-row class="chart-row mb-4">
            <line-chart :chartData="lineChart"/>
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
import PanelGroup from '@/views/dashboard/PanelGroup.vue'
import LineChart from './components/LineChart.vue'
import RaddarChart from '@/views/dashboard/RaddarChart.vue'
import PieChart from '@/views/dashboard/PieChart.vue'
import {getLineChart, getPieChart} from '@/api/px/blog/statistics'

export default {
    name: 'Statistics',
    components: {
        PanelGroup,
        LineChart,
        RaddarChart,
        PieChart,
    },
    data() {
        return {
            // 当前折线图
            lineChart: [],
            // 文章分类饼形图数据
            articlePieData: [],
            // 图片分类饼形图数据
            picturePieData: [],
            // 统计类型列表
            statisticsTypeList: [],
            // 统计参数
            statisticsParams: {
                // 统计维度-时间
                dateDimension: '00',
                // 统计维度-业务
                businessDimension: 'visit',
                // 统计时间
                date: this.parseTime(new Date())
            },
            // 浏览量
            viewNumber: 0
        }
    },
    mounted() {
        this.getDicts('px_statistics_type').then(res => {
            this.statisticsTypeList = res.data;
            this.statisticsParams.dateDimension = this.statisticsTypeList[0].dictValue;
            this.getChartData()
        });
    },
    methods: {
        /**
         * 选择统计类型
         */
        changeStatisticsType(type) {
            this.statisticsParams.dateDimension = type;
            this.getChartData()
        },
        /**
         * 获取折线图数据
         * @param type
         */
        getChartData(type) {
            this.statisticsParams.date = this.parseTime(this.statisticsParams.date);
            getLineChart(this.statisticsParams).then(res => {
                this.lineChart = res.data;
                this.viewNumber = this.lineChart.reduce((last, next) => {
                    if (!isNaN(last.number)) {
                        last = last.number
                    }
                    return last + next.number
                })
            })
            getPieChart(this.statisticsParams).then(res => {
                this.articlePieData = res.data.articlePieData.map(item => {
                    return {
                        name: item.typeName,
                        value: item.articleNumber
                    }
                })
                this.picturePieData = res.data.picturePieData
            })
        },
        /**
         * 选择类型
         * @param type
         */
        handleSetLineChartData(type) {
            this.statisticsParams.businessDimension = type;
            this.getChartData()
        }
    }
}
</script>

<style lang="scss" scoped>
.statistics-type {
    margin-left: 1rem;
}

.dashboard-editor-container {
    padding: var(--space-6);
    background-color: var(--bg-body);
    position: relative;
    min-height: calc(100vh - var(--header-height, 64px) - 50px);
    overflow-y: auto;

    .chart-row {
        background: var(--bg-card);
        padding: var(--space-4) var(--space-4) 0;
        border-radius: var(--radius-lg);
        border: 1px solid var(--border-primary);
        margin-bottom: var(--space-4);
    }

    .mb-4 {
        margin-bottom: var(--space-4);
    }

    .view-number {
        font-size: var(--text-lg);
        span {
            font-weight: bold;
            color: var(--color-danger);
        }
    }

    .chart-wrapper {
        background: var(--bg-card);
        padding: var(--space-4) var(--space-4) 0;
        margin-bottom: var(--space-4);
        border-radius: var(--radius-lg);
        border: 1px solid var(--border-primary);
    }
}

@media (max-width: 1024px) {
    .chart-wrapper {
        padding: 8px;
    }
}
</style>
