<!--
 * @fileName: more
 * @date: 2022/7/18 9:54
 * @author: 裴浩宇
 * @description: 更多统计
 * @version: V1.0.0
!-->
<template>
    <div class="page">
        <el-row class="page-title">
            全国分布
        </el-row>
        <el-row style="position: absolute; z-index: 999;">
            <el-date-picker
                v-model="regionDate"
                type="daterange"
                unlink-panels
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                @change="getMoreStatistics"
                :picker-options="regionDatePickerOptions">
            </el-date-picker>
        </el-row>
        <el-row>
            <el-col id="region-echarts" ref="regionEcharts" :span="24"></el-col>
        </el-row>
    </div>
</template>

<script>
import echarts from '@/utils/echarts'
import china from './data/china.json'
import {getMoreStatistics} from "../../../../api/px/blog/statistics";

import 'echarts/theme/macarons'; // echarts theme
export default {
    name: "more",
    data() {
        return {
            // 地区时间选择器
            regionDate: [new Date().setFullYear(new Date().getFullYear() - 1), new Date()],
            // 快捷选择时间范围
            regionDatePickerOptions: {
                shortcuts: [{
                    text: '最近一周',
                    onClick(picker) {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
                        picker.$emit('pick', [start, end]);
                    }
                }, {
                    text: '最近一个月',
                    onClick(picker) {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
                        picker.$emit('pick', [start, end]);
                    }
                }, {
                    text: '最近三个月',
                    onClick(picker) {
                        const end = new Date();
                        const start = new Date();
                        start.setTime(start.getTime() - 3600 * 1000 * 24 * 90);
                        picker.$emit('pick', [start, end]);
                    }
                }]
            },
            // 图表
            chart: null,
            // 地区统计数据
            regionStatisticsData: []
        }
    },
    mounted() {
        this.getMoreStatistics()
    },
    methods: {
        /**
         * 获取更多统计数据
         */
        getMoreStatistics() {
            let params = {
                startDate: this.parseTime(this.regionDate[0]),
                endDate: this.parseTime(this.regionDate[1]),
            }
            getMoreStatistics(params).then(res => {
                this.regionStatisticsData = res.data.regionStatisticsData;
                this.initRegionEcharts();
            })
        },
        /**
         * 初始化地区统计图表
         */
        initRegionEcharts() {
            echarts.registerMap('中华人民共和国', china);
            this.chart = echarts.init(document.getElementById('region-echarts'));
            const option = {
                title: {
                    text: '全国访问量分布',
                    x: 'center'
                },
                tooltip: {
                    trigger: 'item',
                    formatter: '{b}<br/>浏览量：{c}'
                },
                dataRange: {
                    max: Math.max(...this.regionStatisticsData.map(item => item.value)),
                    realtime: true,
                    calculable: true,
                    color: ['#CD594B', '#F8CE5E', '#5A8DEE']
                },
                series: [
                    {
                        name: '访问量',
                        type: 'map',
                        map: '中华人民共和国',
                        zoom: 1.5,
                        roam: true,
                        layoutCenter: ['50%', '70%'],
                        layoutSize: 650,
                        data: this.regionStatisticsData,
                        label: {
                            show: true
                        }
                    }
                ],

            };
            this.chart.setOption(option);
        }
    }
}
</script>

<style lang="scss" scoped>
.page {
    padding: var(--space-6);
    background: var(--bg-body);

    .page-title {
        font-size: var(--text-xl);
        font-weight: var(--font-bold);
        color: var(--text-primary);
        margin-bottom: var(--space-4);
    }

    #region-echarts {
        height: 75vh;
        background: var(--bg-card);
        border-radius: var(--radius-lg);
        border: 1px solid var(--border-primary);
        box-shadow: var(--shadow-sm);
    }
}
</style>
