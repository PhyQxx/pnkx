<script>
import { h } from 'vue'
import SvgIcon from '@/components/SvgIcon/index.vue'

export default {
    name: 'MenuItem',
    props: {
        icon: {
            type: String,
            default: ''
        },
        title: {
            type: String,
            default: ''
        }
    },
    render() {
        const {icon, title} = this
        const vnodes = []
        if (icon) {
            vnodes.push(h(SvgIcon, { class: 'svg-icon', iconClass: icon }))
        }
        if (title) {
            vnodes.push(h('span', null, title))
        }
        return vnodes
    }
}
</script>
