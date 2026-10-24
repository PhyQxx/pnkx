import { ref, computed, watch, onBeforeUnmount } from 'vue';
import { eligibleItems, businessToday, checkoutError, checkoutPayload, requestKeyCache } from './checkoutModel';
export function useShoppingCheckout(props, emit, api) {
    const visible = ref(false),
        saving = ref(false),
        batchId = ref(null),
        batches = ref([]),
        batchesOpen = ref(false),
        batchLoading = ref(false),
        batchError = ref(''),
        form = ref({ purchasedItemIds: [] }),
        accounts = ref([]),
        categories = ref([]),
        financeLoading = ref(false),
        financeError = ref(''),
        error = ref(''),
        success = ref('');
    let generation = 0,
        batchRequest = 0,
        financeRequest = 0;
    let keyFor = requestKeyCache(() => `purchase_${Date.now()}_${Math.random().toString(36).slice(2)}`);
    const eligible = computed(() => eligibleItems(props.items)),
        selectedCount = computed(() => form.value.purchasedItemIds?.length || 0),
        allSelected = computed(
            () => eligible.value.length > 0 && eligible.value.every((i) => form.value.purchasedItemIds.includes(i.id))
        ),
        accountLabel = computed(
            () => accounts.value.find((a) => a.id === form.value.accountId)?.accountName || '请选择'
        ),
        categoryLabel = computed(
            () => categories.value.find((c) => c.id === form.value.classificationId)?.typeName || '请选择'
        );
    async function loadBatches() {
        batchesOpen.value = true;
        const token = ++batchRequest;
        batchLoading.value = true;
        batchError.value = '';
        try {
            const r = await api.request({ url: `/shoppingList/${props.listId}/batches`, method: 'get' });
            if (token === batchRequest) batches.value = r.data || [];
        } catch {
            if (token === batchRequest) batchError.value = '采购批次加载失败，请重试';
        } finally {
            if (token === batchRequest) batchLoading.value = false;
        }
    }
    async function loadFinance() {
        const token = ++financeRequest;
        financeLoading.value = true;
        financeError.value = '';
        try {
            const [a, c] = await Promise.all([
                api.listAccount({ pageSize: 100 }),
                api.listClassification({ pageSize: 100, typeDifference: '1' })
            ]);
            if (token === financeRequest) {
                accounts.value = a.rows || [];
                categories.value = c.rows || [];
                if (!accounts.value.length || !categories.value.length)
                    financeError.value = '请先在记账模块创建账户和支出分类，或关闭同时记账';
            }
        } catch {
            if (token === financeRequest) financeError.value = '账户或分类加载失败，可以重试或关闭同时记账';
        } finally {
            if (token === financeRequest) financeLoading.value = false;
        }
    }
    function open(id = null) {
        if (saving.value) return;
        generation++;
        batchId.value = id;
        error.value = '';
        success.value = '';
        financeError.value = '';
        form.value = {
            purchasedItemIds: eligible.value.slice(0, 200).map((i) => i.id),
            recordExpense: !!id,
            totalAmount: '',
            paidAt: businessToday(),
            accountId: null,
            classificationId: null
        };
        keyFor = requestKeyCache(() => `purchase_${Date.now()}_${Math.random().toString(36).slice(2)}`);
        visible.value = true;
    }
    watch(
        () => [form.value, form.value.recordExpense],
        ([current, active], [previous, wasActive]) => {
            if (active && (current !== previous || !wasActive)) loadFinance();
        }
    );
    watch(
        form,
        () => {
            error.value = '';
        },
        { deep: true }
    );
    function close() {
        if (!saving.value) {
            visible.value = false;
            generation++;
            financeRequest++;
        }
    }
    function toggleAll() {
        if (saving.value) return;
        form.value.purchasedItemIds = allSelected.value ? [] : eligible.value.slice(0, 200).map((i) => i.id);
    }
    function toggleItem(id) {
        if (saving.value) return;
        const selected = form.value.purchasedItemIds;
        form.value.purchasedItemIds = selected.includes(id) ? selected.filter((x) => x !== id) : [...selected, id];
    }
    async function save() {
        if (saving.value) return;
        error.value = checkoutError(form.value, batchId.value, props.items);
        if (error.value) return;
        if (form.value.recordExpense && (financeLoading.value || financeError.value)) {
            error.value = '账户和分类尚未就绪，请先重试加载';
            return;
        }
        const payload = checkoutPayload(form.value, batchId.value);
        payload.requestId = keyFor(payload);
        const token = generation;
        const url = batchId.value
            ? `/shopping-batches/${batchId.value}/record-expense`
            : `/shoppingList/${props.listId}/checkout`;
        saving.value = true;
        try {
            await api.request({ url, method: 'post', data: payload });
            if (token !== generation) return;
            visible.value = false;
            success.value = batchId.value ? '本批次支出已记录' : '本次采购已结算，未选择的商品保持原状';
            emit('changed');
            await loadBatches();
        } catch (e) {
            if (token === generation)
                error.value = e?.message || '提交未确认成功，请保留当前内容重试，重复请求不会重复结算';
        } finally {
            saving.value = false;
        }
    }
    watch(
        () => props.listId,
        () => {
            generation++;
            batchRequest++;
            financeRequest++;
            visible.value = false;
            batches.value = [];
            batchesOpen.value = false;
            accounts.value = [];
            categories.value = [];
            error.value = '';
            success.value = '';
            batchError.value = '';
            financeError.value = '';
            financeLoading.value = false;
            batchLoading.value = false;
        }
    );
    onBeforeUnmount(() => {
        generation++;
        batchRequest++;
        financeRequest++;
    });
    return {
        visible,
        saving,
        batchId,
        batches,
        batchesOpen,
        batchLoading,
        batchError,
        form,
        accounts,
        categories,
        financeLoading,
        financeError,
        error,
        success,
        eligible,
        selectedCount,
        allSelected,
        accountLabel,
        categoryLabel,
        loadBatches,
        loadFinance,
        open,
        close,
        toggleAll,
        toggleItem,
        save
    };
}
