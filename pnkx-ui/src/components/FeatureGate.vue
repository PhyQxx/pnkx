<template><slot v-if="allowed" /></template>
<script>
import { getFeatureCapabilities } from '@/utils/featureCapabilities';
export default {
    name: 'FeatureGate',
    props: { feature: { type: String, required: true } },
    data: () => ({ allowed: false, generation: 0, timer: null }),
    computed: {
        sessionToken() {
            return this.$store.getters.token;
        }
    },
    watch: {
        sessionToken() {
            this.allowed = false;
            this.refresh();
        },
        feature() {
            this.allowed = false;
            this.refresh();
        }
    },
    mounted() {
        this.refresh();
        this.timer = setInterval(() => this.refresh(), 30000);
    },
    beforeUnmount() {
        this.generation++;
        clearInterval(this.timer);
    },
    methods: {
        async refresh() {
            const generation = ++this.generation;
            try {
                const flags = await getFeatureCapabilities();
                if (generation === this.generation) this.allowed = flags[this.feature] === true;
            } catch (_) {
                if (generation === this.generation) this.allowed = false;
            }
        }
    }
};
</script>
