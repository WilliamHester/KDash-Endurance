
function formatNumberAsDuration(number, minutesOptional) {
  var sign = '+';
  if (number == 0) {
    return '--';
  }
  if (number < 0) {
    sign = '-';
  }
  number = Math.abs(number);
  const minutes = Math.floor(number / 60);
  var seconds = (number % 60).toFixed(3);
  if (seconds < 10) {
    seconds = `0${seconds}`;
  }
  if (minutesOptional && minutes == 0) {
    return seconds;
  }
  return `${minutes}:${seconds}`;
};

export { formatNumberAsDuration };