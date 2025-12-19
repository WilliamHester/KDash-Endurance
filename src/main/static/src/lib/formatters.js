export function formatNumberAsDuration(number, minutesOptional, alwaysShowSign) {
  if (number === 0 || number === -1 || number === undefined) {
    return '--';
  }
  let sign = '';
  if (number < 0) {
    sign = '-';
  } else if (number > 0 && alwaysShowSign) {
    sign = '+';
  }
  number = Math.abs(number);
  const minutes = Math.floor(number / 60);
  let seconds = (number % 60).toFixed(3);
  if (seconds < 10) {
    seconds = `0${seconds}`;
  }

  if (minutesOptional && minutes === 0) {
    return `${sign}${seconds}`;
  }
  return `${sign}${minutes}:${seconds}`;
}

export function formatDriverName(name) {
  if (!name) return '';
  const nameParts = name.split(' ');
  const firstInitial = nameParts[0][0];
  const lastName = nameParts[nameParts.length - 1].replace(/\d+/, '');
  return `${firstInitial}. ${lastName}`;
}

// Helper for formatting Sim Time (seconds -> HH:MM:SS AM/PM)
export function timeOfDayFormatter(value) {
  if (!value) return '--:--:--';
  let hoursOfDay = Math.floor(value / 3600);
  let amPm = 'AM';
  if (hoursOfDay > 12) {
    hoursOfDay -= 12;
    amPm = 'PM';
  }
  if (hoursOfDay === 0) hoursOfDay = 12;

  const hours = String(hoursOfDay).padStart(2, '0');
  const minutes = String(Math.floor((value % 3600) / 60)).padStart(2, '0');
  const seconds = String(Math.floor(value % 60)).padStart(2, '0');
  return `${hours}:${minutes}:${seconds} ${amPm}`;
}

export function minuteSecondFormatter(value) {
  if (!value) return '--:--';
  const minutes = String(Math.floor(value / 60));
  const seconds = String(Math.floor(value % 60).toFixed(1)).padStart(4, '0');
  return `${minutes}:${seconds}`;
}

export function hourMinuteSecondFormatter(value) {
  if (!value) return '--:--:--';
  const hours = String(Math.floor(value / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((value % 3600) / 60)).padStart(2, '0');
  const seconds = String(Math.floor(value % 60)).padStart(2, '0');
  return `${hours}:${minutes}:${seconds}`;
}
