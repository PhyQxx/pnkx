export const useTimer = (defText: string) => {
  const text = ref(defText)
  const flag = ref(false)
  function timer(time: number) {
    let count = time
    flag.value = true
    const timeId = setInterval(() => {
      count--
      text.value = count.toString()
      if (count === 0) {
        clearInterval(timeId)
        flag.value = false
        text.value = defText
      }
    }, 1000)
  }

  return { text, flag, timer }
}
