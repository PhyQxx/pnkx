/**
 * ECharts 按需导入
 * 替代 import * as echarts from 'echarts' 以减小打包体积
 */
import * as echarts from 'echarts/core'

import { BarChart, LineChart, PieChart, RadarChart, GaugeChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  RadarComponent,
  ToolboxComponent,
  DataZoomComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart, LineChart, PieChart, RadarChart, GaugeChart,
  TitleComponent, TooltipComponent, GridComponent, LegendComponent,
  RadarComponent, ToolboxComponent, DataZoomComponent,
  CanvasRenderer
])

export default echarts
