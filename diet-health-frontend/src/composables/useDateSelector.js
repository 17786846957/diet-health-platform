import { ref } from 'vue'
import { getToday } from '../utils/date'

export function useDateSelector(onChange) {
  const selectedDate = ref(getToday())

  function handleDateChange(date) {
    selectedDate.value = date
    onChange?.(date)
  }

  return { selectedDate, handleDateChange }
}
