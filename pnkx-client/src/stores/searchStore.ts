export const useSearchStore = defineStore('search', () => {
  const showModal = ref(false)

  function setModal(status: boolean) {
    showModal.value = status
  }

  return { showModal, setModal }
})

// console.log(import.meta.hot)
if (import.meta.hot) import.meta.hot.accept(acceptHMRUpdate(useSearchStore, import.meta.hot))
